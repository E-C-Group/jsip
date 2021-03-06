package co.ecg.jain_sip.tck.msgflow.callflows.prack;

import co.ecg.jain_sip.sip.*;
import co.ecg.jain_sip.sip.address.*;
import co.ecg.jain_sip.sip.header.*;
import co.ecg.jain_sip.sip.message.*;

import co.ecg.jain_sip.tck.TestHarness;
import co.ecg.jain_sip.tck.msgflow.callflows.ProtocolObjects;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is a UAC template. Shootist is the guy that shoots and shootme is
 * the guy that gets shot.
 *
 * @author M. Ranganathan
 */
@Slf4j
public class Shootme implements SipListener {

    protected static final int PRACK_CODE = 183;

    private static SipProvider sipProvider;

    private static AddressFactory addressFactory;

    private static MessageFactory messageFactory;

    private static HeaderFactory headerFactory;

    private static SipStack sipStack;

    private static final String myAddress = "127.0.0.1";

    protected ServerTransaction inviteTid;

    private Request inviteRequest;

    private Dialog dialog;

    private String toTag;

    private String transport;

    private boolean prackRequestReceived;

    private boolean inviteReceived;

    public static final int myPort = 5080;

    public Shootme(ProtocolObjects protObjects) {
        addressFactory = protObjects.addressFactory;
        messageFactory = protObjects.messageFactory;
        headerFactory = protObjects.headerFactory;
        sipStack = protObjects.sipStack;
        transport = protObjects.transport;
    }

    public void processRequest(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        ServerTransaction serverTransactionId = requestEvent
                .getServerTransaction();

        log.info("\n\nRequest " + request.getMethod() + " received at "
                + sipStack.getStackName() + " with server transaction id "
                + serverTransactionId);

        if (request.getMethod().equals(Request.INVITE)) {
            processInvite(requestEvent, serverTransactionId);
        } else if (request.getMethod().equals(Request.ACK)) {
            processAck(requestEvent, serverTransactionId);
        } else if (request.getMethod().equals(Request.BYE)) {
            processBye(requestEvent, serverTransactionId);
        } else if (request.getMethod().equals(Request.PRACK)) {
            processPrack(requestEvent, serverTransactionId);
        }

    }

    private void processPrack(RequestEvent requestEvent,
            ServerTransaction serverTransactionId) {
        prackRequestReceived = true;
        try {
            log.info("shootme: got an PRACK! ");
            log.info("Dialog State = " + dialog.getState());

            /**
             * JvB: First, send 200 OK for PRACK
             */
            Request prack = requestEvent.getRequest();
            Response prackOk = messageFactory.createResponse(200, prack);
            serverTransactionId.sendResponse(prackOk);

            /**
             * Send a 200 OK response to complete the 3 way handshake for the
             * INIVTE.
             */
            Response response = messageFactory.createResponse(200,
                    inviteRequest);
            ToHeader to = (ToHeader) response.getHeader(ToHeader.NAME);
            to.setTag(this.toTag);
            Address address = addressFactory.createAddress("Shootme <sip:"
                    + myAddress + ":" + myPort + ">");
            ContactHeader contactHeader = headerFactory
                    .createContactHeader(address);
            response.addHeader(contactHeader);
            inviteTid.sendResponse(response);
        } catch (Exception ex) {
            TestHarness.fail(ex.getMessage());
        }
    }

    public void processResponse(ResponseEvent responseEvent) {
    }

    /**
     * Process the ACK request. Send the bye and complete the call flow.
     */
    public void processAck(RequestEvent requestEvent,
            ServerTransaction serverTransaction) {

        try {
            log.info("shootme: got an ACK! Sending  a BYE");
            log.info("Dialog State = " + dialog.getState());

            // JvB: there should not be a transaction for ACKs; requestEvent
            // can be used to get it instead
            // Dialog dialog = serverTransaction.getDialog();
            Dialog dialog = requestEvent.getDialog();

            SipProvider provider = (SipProvider) requestEvent.getSource();
            Request byeRequest = dialog.createRequest(Request.BYE);
            ClientTransaction ct = provider.getNewClientTransaction(byeRequest);
            dialog.sendRequest(ct);
        } catch (Exception ex) {
            ex.printStackTrace();
            TestHarness.fail(ex.getMessage());
        }

    }

    /**
     * Process the invite request.
     */
    public void processInvite(RequestEvent requestEvent,
            ServerTransaction serverTransaction) {
        inviteReceived = true;
        SipProvider sipProvider = (SipProvider) requestEvent.getSource();
        Request request = requestEvent.getRequest();
        try {
            log.info("shootme: got an Invite sending Trying");
            // log.info("shootme: " + request);
            Response response = messageFactory.createResponse(Response.TRYING,
                    request);
            ServerTransaction st = requestEvent.getServerTransaction();

            if (st == null) {
                st = sipProvider.getNewServerTransaction(request);
            }
            dialog = st.getDialog();

            st.sendResponse(response);

            // reliable provisional response.

            Response okResponse = messageFactory.createResponse(PRACK_CODE,
                    request);
            ToHeader toHeader = (ToHeader) okResponse.getHeader(ToHeader.NAME);
            this.toTag = "4321";
            toHeader.setTag(toTag); // Application is supposed to set.
            this.inviteTid = st;
            this.inviteRequest = request;

            log.info("sending reliable provisional response.");

            RequireHeader requireHeader = headerFactory
                    .createRequireHeader("100rel");
            okResponse.addHeader(requireHeader);
            dialog.sendReliableProvisionalResponse(okResponse);
        } catch (Exception ex) {
          ex.printStackTrace();
            TestHarness.fail(ex.getMessage());

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
            Response response = messageFactory.createResponse(200, request);
            serverTransactionId.sendResponse(response);
            log.info("Dialog State is "
                    + serverTransactionId.getDialog().getState());

        } catch (Exception ex) {
            TestHarness.fail(ex.getMessage());
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
        log.info("dialogState = " + transaction.getDialog().getState());
        log.info("Transaction Time out");
    }

    public SipProvider createProvider() throws Exception {
        ListeningPoint lp = sipStack.createListeningPoint("127.0.0.1", myPort,
                transport);

        sipProvider = sipStack.createSipProvider(lp);
        log.info(transport + " SIP provider " + sipProvider);

        return sipProvider;
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
        TestHarness.assertTrue(prackRequestReceived && inviteReceived);
    }

}
