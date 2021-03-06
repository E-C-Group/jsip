package co.ecg.jain_sip.sip.stack.forkedinvite482;

import co.ecg.jain_sip.sip.*;
import co.ecg.jain_sip.sip.address.Address;
import co.ecg.jain_sip.sip.header.ContactHeader;
import co.ecg.jain_sip.sip.header.ToHeader;
import co.ecg.jain_sip.sip.header.ViaHeader;
import co.ecg.jain_sip.sip.message.Request;
import co.ecg.jain_sip.sip.message.Response;
import co.ecg.jain_sip.tck.TestHarness;
import co.ecg.jain_sip.tck.msgflow.callflows.ProtocolObjects;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * This class is a UAC template. Shootist is the guy that shoots and shootme is
 * the guy that gets shot.
 *
 * @author M. Ranganathan
 */
@Slf4j
public class Shootme implements SipListener {


    private static final String myAddress = "127.0.0.1";

    private Hashtable serverTxTable = new Hashtable();

    private SipProvider sipProvider;

    private int myPort;

    private static String unexpectedException = "Unexpected exception ";

    private ProtocolObjects protocolObjects;

    private boolean inviteSeen;

    private boolean byeSeen;

    private boolean ackSeen;

    private boolean actAsNonRFC3261UAS;

    /**
     * Causes this UAS to act as a non-RFC3261 UAS, i.e. does not set a to-tag
     */
    public void setNonRFC3261(boolean b) {
        this.actAsNonRFC3261UAS = b;
    }

    class MyTimerTask extends TimerTask {
        RequestEvent requestEvent;
        // String toTag;
        ServerTransaction serverTx;

        public MyTimerTask(RequestEvent requestEvent, ServerTransaction tx) {
            log.info("MyTimerTask ");
            this.requestEvent = requestEvent;
            // this.toTag = toTag;
            this.serverTx = tx;

        }

        public void run() {
            sendInviteOK(requestEvent, serverTx);
        }

    }


    public void processRequest(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        ServerTransaction serverTransactionId = requestEvent
                .getServerTransaction();

        log.info("\n\nRequest " + request.getMethod()
                + " received at " + protocolObjects.sipStack.getStackName()
                + " with server transaction id " + serverTransactionId);

        if (request.getMethod().equals(Request.INVITE)) {
            processInvite(requestEvent, serverTransactionId);
        } else if (request.getMethod().equals(Request.ACK)) {
            processAck(requestEvent, serverTransactionId);
        } else if (request.getMethod().equals(Request.BYE)) {
            processBye(requestEvent, serverTransactionId);
        } else if (request.getMethod().equals(Request.CANCEL)) {
            processCancel(requestEvent, serverTransactionId);
        }

    }

    public void processResponse(ResponseEvent responseEvent) {
    }

    /**
     * Process the ACK request. Send the bye and complete the call flow.
     */
    public void processAck(RequestEvent requestEvent,
                           ServerTransaction serverTransaction) {
        log.info("shootme: got an ACK! ");
        log.info("Dialog = " + requestEvent.getDialog());
        if (requestEvent.getDialog() != null) {
            log.info("Dialog State = " + requestEvent.getDialog().getState());
        } else {
            log.debug("No dialog !!");
        }

        this.ackSeen = true;
    }

    /**
     * Process the invite request.
     */
    public void processInvite(RequestEvent requestEvent,
                              ServerTransaction serverTransaction) {
        SipProvider sipProvider = (SipProvider) requestEvent.getSource();
        Request request = requestEvent.getRequest();
        try {
            log.info("shootme: got an Invite sending Trying");
            // log.info("shootme: " + request);

            ServerTransaction st = requestEvent.getServerTransaction();

            if (st == null) {
                log.info("null server tx -- getting a new one");
                st = sipProvider.getNewServerTransaction(request);
            }

            log.info("getNewServerTransaction : " + st);

            String txId = ((ViaHeader) request.getHeader(ViaHeader.NAME)).getBranch();
            this.serverTxTable.put(txId, st);

            // Create the 100 Trying response.
            Response response = protocolObjects.messageFactory.createResponse(Response.TRYING,
                    request);
            ListeningPoint lp = sipProvider.getListeningPoint(protocolObjects.transport);
            int myPort = lp.getPort();

            Address address = protocolObjects.addressFactory.createAddress("Shootme <sip:"
                    + myAddress + ":" + myPort + ">");

            // Add a random sleep to stagger the two OK's for the benifit of implementations
            // that may not be too good about handling re-entrancy.
            int timeToSleep = (int) (Math.random() * 1000);

            Thread.sleep(timeToSleep);

            st.sendResponse(response);

            Response ringingResponse = protocolObjects.messageFactory.createResponse(Response.RINGING,
                    request);
            ContactHeader contactHeader = protocolObjects.headerFactory.createContactHeader(address);
            response.addHeader(contactHeader);
            ToHeader toHeader = (ToHeader) ringingResponse.getHeader(ToHeader.NAME);
            String toTag = actAsNonRFC3261UAS ? null : new Integer((int) (Math.random() * 10000)).toString();
            if (!actAsNonRFC3261UAS) toHeader.setTag(toTag); // Application is supposed to set.
            ringingResponse.addHeader(contactHeader);
            st.sendResponse(ringingResponse);
            Dialog dialog = st.getDialog();
            dialog.setApplicationData(st);

            this.inviteSeen = true;

            new Timer().schedule(new MyTimerTask(requestEvent, st/*,toTag*/), 1000);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }
    }

    private void sendInviteOK(RequestEvent requestEvent, ServerTransaction inviteTid) {
        try {
            log.info("sendInviteOK: " + inviteTid);
            if (inviteTid.getState() != TransactionState.COMPLETED) {
                log.info("shootme: Dialog state before OK: "
                        + inviteTid.getDialog().getState());

                SipProvider sipProvider = (SipProvider) requestEvent.getSource();
                Request request = requestEvent.getRequest();
                Response okResponse = protocolObjects.messageFactory.createResponse(Response.OK,
                        request);
                ListeningPoint lp = sipProvider.getListeningPoint(protocolObjects.transport);
                int myPort = lp.getPort();

                Address address = protocolObjects.addressFactory.createAddress("Shootme <sip:"
                        + myAddress + ":" + myPort + ">");
                ContactHeader contactHeader = protocolObjects.headerFactory
                        .createContactHeader(address);
                okResponse.addHeader(contactHeader);
                inviteTid.sendResponse(okResponse);
                log.info("shootme: Dialog state after OK: "
                        + inviteTid.getDialog().getState());
                TestHarness.assertEquals(DialogState.CONFIRMED, inviteTid.getDialog().getState());
            } else {
                log.info("semdInviteOK: inviteTid = " + inviteTid + " state = " + inviteTid.getState());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Process the bye request.
     */
    public void processBye(RequestEvent requestEvent,
                           ServerTransaction serverTransactionId) {
        Request request = requestEvent.getRequest();
        try {
            log.info("shootme:  got a bye sending OK.");
            log.info("shootme:  dialog = " + requestEvent.getDialog());
            log.info("shootme:  dialogState = " + requestEvent.getDialog().getState());
            Response response = protocolObjects.messageFactory.createResponse(200, request);
            if (serverTransactionId != null) {
                serverTransactionId.sendResponse(response);
            }
            log.info("shootme:  dialogState = " + requestEvent.getDialog().getState());

            this.byeSeen = true;


        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);

        }
    }

    public void processCancel(RequestEvent requestEvent,
                              ServerTransaction serverTransactionId) {
        Request request = requestEvent.getRequest();
        SipProvider sipProvider = (SipProvider) requestEvent.getSource();
        try {
            log.info("shootme:  got a cancel. ");
            // Because this is not an In-dialog request, you will get a null server Tx id here.
            if (serverTransactionId == null) {
                serverTransactionId = sipProvider.getNewServerTransaction(request);
            }
            Response response = protocolObjects.messageFactory.createResponse(200, request);
            serverTransactionId.sendResponse(response);

            String serverTxId = ((ViaHeader) response.getHeader(ViaHeader.NAME)).getBranch();
            ServerTransaction serverTx = (ServerTransaction) this.serverTxTable.get(serverTxId);
            if (serverTx != null && (serverTx.getState().equals(TransactionState.TRYING) ||
                    serverTx.getState().equals(TransactionState.PROCEEDING))) {
                Request originalRequest = serverTx.getRequest();
                Response resp = protocolObjects.messageFactory.createResponse(Response.REQUEST_TERMINATED, originalRequest);
                serverTx.sendResponse(resp);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);

        }
    }

    public void processTimeout(TimeoutEvent timeoutEvent) {
        Transaction transaction;
        if (timeoutEvent.isServerTransaction()) {
            transaction = timeoutEvent.getServerTransaction();
        } else {
            transaction = timeoutEvent.getClientTransaction();
        }
        log.info("state = " + transaction.getState());
        log.info("dialog = " + transaction.getDialog());
        log.info("dialogState = "
                + transaction.getDialog().getState());
        log.info("Transaction Time out");
    }

    public SipProvider createProvider() {
        try {

            ListeningPoint lp = protocolObjects.sipStack.createListeningPoint(myAddress,
                    myPort, protocolObjects.transport);

            sipProvider = protocolObjects.sipStack.createSipProvider(lp);
            log.info("provider " + sipProvider);
            log.info("sipStack = " + protocolObjects.sipStack);
            sipProvider.setAutomaticDialogSupportEnabled(true);
            return sipProvider;
        } catch (Exception ex) {
            log.error("Exception", ex);
            TestHarness.fail(unexpectedException);
            return null;

        }

    }

    public Shootme(int myPort, ProtocolObjects protocolObjects) {
        this.myPort = myPort;
        this.protocolObjects = protocolObjects;
    }


    public void processIOException(IOExceptionEvent exceptionEvent) {
        log.info("IOException");

    }

    public void processTransactionTerminated(
            TransactionTerminatedEvent transactionTerminatedEvent) {
        log.info("Transaction terminated event recieved");

    }

    public void processDialogTerminated(
            DialogTerminatedEvent dialogTerminatedEvent) {
        log.info("Dialog terminated event recieved");

    }

    public void checkState() {
        TestHarness.assertTrue("Should see invite", inviteSeen);

    }

}
