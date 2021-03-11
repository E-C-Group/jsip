package co.ecg.jain_sip.sip.stack.forkedinvitedialogtimeout;

import co.ecg.jain_sip.sip.*;

import java.util.Hashtable;
import java.util.Properties;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import co.ecg.jain_sip.sip.address.Address;
import co.ecg.jain_sip.sip.address.AddressFactory;
import co.ecg.jain_sip.sip.header.ContactHeader;
import co.ecg.jain_sip.sip.header.HeaderFactory;
import co.ecg.jain_sip.sip.header.ToHeader;
import co.ecg.jain_sip.sip.header.ViaHeader;
import co.ecg.jain_sip.sip.message.MessageFactory;
import co.ecg.jain_sip.sip.message.Request;
import co.ecg.jain_sip.sip.message.Response;
import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;


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

    private boolean inviteSeen;


    private boolean byeSeen;

    private boolean ackSeen;

    private SipStack sipStack;

    private int ringingDelay;

    private int okDelay;

    private boolean sendRinging;

    private static AddressFactory addressFactory;

    private static MessageFactory messageFactory;

    private static HeaderFactory headerFactory;

    private static final String transport = "udp";

    private static Timer timer = new Timer();


    class MyTimerTask extends TimerTask {
        RequestEvent requestEvent;
        String toTag;
        ServerTransaction serverTx;

        public MyTimerTask(RequestEvent requestEvent, ServerTransaction tx, String toTag) {
            log.info("MyTimerTask ");
            this.requestEvent = requestEvent;
            this.toTag = toTag;
            this.serverTx = tx;

        }

        public void run() {
            sendInviteOK(requestEvent, serverTx, toTag);
        }

    }


    public void processRequest(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        ServerTransaction serverTransactionId = requestEvent
                .getServerTransaction();

        log.info("\n\nRequest " + request.getMethod()
                + " received at " + sipStack.getStackName()
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
        log.info("Dialog State = " + requestEvent.getDialog().getState());

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
            log.info("shootme: " + this.myPort + " got an Invite sending Trying");
            // log.info("shootme: " + request);

            ServerTransaction st = requestEvent.getServerTransaction();

            if (st == null) {
                log.info("null server tx -- getting a new one");
                st = sipProvider.getNewServerTransaction(request);
            }

            log.info("getNewServerTransaction : " + st);


            Address address = addressFactory.createAddress("Shootme <sip:"
                    + myAddress + ":" + myPort + ">");

            Response ringingResponse = messageFactory.createResponse(Response.RINGING,
                    request);
            ContactHeader contactHeader = headerFactory.createContactHeader(address);
            ringingResponse.setHeader(contactHeader);
            ToHeader toHeader = (ToHeader) ringingResponse.getHeader(ToHeader.NAME);
            String toTag = "shootme-" + myPort + "-" + new Integer(new Random().nextInt()).toString();
            toHeader.setTag(toTag);
            if (sendRinging) {
                Thread.sleep(this.ringingDelay / 2);
                st.sendResponse(ringingResponse);
            }
            Dialog dialog = st.getDialog();
            dialog.setApplicationData(st);

            this.inviteSeen = true;

            if (okDelay > 0) {
                timer.schedule(new MyTimerTask(requestEvent, st, toTag), this.okDelay);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }
    }

    private void sendInviteOK(RequestEvent requestEvent, ServerTransaction inviteTid, String toTag) {
        try {
            log.info("sendInviteOK: " + inviteTid);
            if (inviteTid.getState() != TransactionState.COMPLETED) {
                log.info("shootme: Dialog state before OK: "
                        + inviteTid.getDialog().getState());

                SipProvider sipProvider = (SipProvider) requestEvent.getSource();
                Request request = requestEvent.getRequest();
                Response okResponse = messageFactory.createResponse(Response.OK,
                        request);
                ListeningPoint lp = sipProvider.getListeningPoint(transport);
                int myPort = lp.getPort();


                ((ToHeader) okResponse.getHeader(ToHeader.NAME)).setTag(toTag);


                Address address = addressFactory.createAddress("Shootme <sip:"
                        + myAddress + ":" + myPort + ">");
                ContactHeader contactHeader = headerFactory
                        .createContactHeader(address);
                okResponse.addHeader(contactHeader);
                inviteTid.sendResponse(okResponse);
                log.info("shootme: Dialog state after OK: "
                        + inviteTid.getDialog().getState());
                TestCase.assertEquals(DialogState.CONFIRMED, inviteTid.getDialog().getState());
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
            Response response = messageFactory.createResponse(200, request);
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
            Response response = messageFactory.createResponse(200, request);
            serverTransactionId.sendResponse(response);

            String serverTxId = ((ViaHeader) response.getHeader(ViaHeader.NAME)).getBranch();
            ServerTransaction serverTx = (ServerTransaction) this.serverTxTable.get(serverTxId);
            if (serverTx != null && (serverTx.getState().equals(TransactionState.TRYING) ||
                    serverTx.getState().equals(TransactionState.PROCEEDING))) {
                Request originalRequest = serverTx.getRequest();
                Response resp = messageFactory.createResponse(Response.REQUEST_TERMINATED, originalRequest);
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

            ListeningPoint lp = sipStack.createListeningPoint(myAddress,
                    myPort, transport);

            sipProvider = sipStack.createSipProvider(lp);
            log.info("provider " + sipProvider);
            log.info("sipStack = " + sipStack);
            return sipProvider;
        } catch (Exception ex) {
            log.error("Exception", ex);
            TestCase.fail(unexpectedException);
            return null;

        }

    }

    public Shootme(int myPort, boolean sendRinging, int ringingDelay, int okDelay) {
        this.myPort = myPort;
        this.ringingDelay = ringingDelay;
        this.okDelay = okDelay;
        this.sendRinging = sendRinging;

        SipObjects sipObjects = new SipObjects(myPort, "shootme", "on");
        addressFactory = sipObjects.addressFactory;
        messageFactory = sipObjects.messageFactory;
        headerFactory = sipObjects.headerFactory;
        this.sipStack = sipObjects.sipStack;
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
        TestCase.assertTrue("Should see invite", inviteSeen);

        TestCase.assertTrue("Should see BYE", (!ackSeen) || byeSeen);

    }

    public boolean checkBye() {
        return this.byeSeen;
    }

    public void stop() {
        this.sipStack.stop();
    }


    /**
     * @return the ackSeen
     * <p>
     * Exactly one Dialog must get an ACK.
     */
    public boolean isAckSeen() {
        return ackSeen;
    }


}
