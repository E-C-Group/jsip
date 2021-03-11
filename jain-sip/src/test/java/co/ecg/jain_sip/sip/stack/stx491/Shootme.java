/*
 * Conditions Of Use
 *
 * This software was developed by employees of the National Institute of
 * Standards and Technology (NIST), and others.
 * This software is has been contributed to the public domain.
 * As a result, a formal license is not needed to use the software.
 *
 * This software is provided "AS IS."
 * NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
 * OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
 * AND DATA ACCURACY.  NIST does not warrant or make any representations
 * regarding the use of the software or the results thereof, including but
 * not limited to the correctness, accuracy, reliability or usefulness of
 * the software.
 *
 *
 */
package co.ecg.jain_sip.sip.stack.stx491;

import co.ecg.jain_sip.sip.*;
import co.ecg.jain_sip.sip.address.Address;
import co.ecg.jain_sip.sip.address.SipURI;
import co.ecg.jain_sip.sip.header.*;
import co.ecg.jain_sip.sip.message.Request;
import co.ecg.jain_sip.sip.message.Response;
import co.ecg.jain_sip.tck.msgflow.callflows.ProtocolObjects;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is a UAC template.
 *
 * @author M. Ranganathan
 */
@Slf4j
public class Shootme implements SipListener {


    private ProtocolObjects protocolObjects;


    // To run on two machines change these to suit.
    public static final String myAddress = "127.0.0.1";

    public static final int myPort = 5070;

    private ServerTransaction inviteTid;

    private Dialog dialog;

    private boolean okRecieved;


    private boolean requestPendingSeen;

    class ApplicationData {
        protected int ackCount;
    }

    public Shootme(ProtocolObjects protocolObjects) {
        this.protocolObjects = protocolObjects;
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
        }

    }

    /**
     * Process the ACK request. Send the bye and complete the call flow.
     */
    public void processAck(RequestEvent requestEvent,
                           ServerTransaction serverTransaction) {
        log.info("shootme: got an ACK "
                + requestEvent.getRequest());
    }

    /**
     * Process the invite request.
     */
    public void processInvite(RequestEvent requestEvent,
                              ServerTransaction serverTransaction) {
        SipProvider sipProvider = (SipProvider) requestEvent.getSource();
        Request request = requestEvent.getRequest();
        log.info("Got an INVITE  " + request);
        try {
            log.info("shootme: got an Invite sending OK");
            // log.info("shootme: " + request);
            Response response = protocolObjects.messageFactory.createResponse(180, request);
            ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
            toHeader.setTag("4321");
            Address address = protocolObjects.addressFactory.createAddress("Shootme <sip:"
                    + myAddress + ":" + myPort + ">");
            ContactHeader contactHeader = protocolObjects.headerFactory
                    .createContactHeader(address);
            response.setHeader(contactHeader);
            ServerTransaction st = requestEvent.getServerTransaction();

            if (st == null) {
                st = sipProvider.getNewServerTransaction(request);
                log.info("Server transaction created!" + request);

                log.info("Dialog = " + st.getDialog());
                if (st.getDialog().getApplicationData() == null) {
                    st.getDialog().setApplicationData(new ApplicationData());
                }
            } else {
                // If Server transaction is not null, then
                // this is a re-invite.
                log.info("This is a RE INVITE ");
                ReInviteTest.assertSame("Dialog mismatch ", st.getDialog(), this.dialog);
            }

            // Thread.sleep(5000);
            log.info("got a server tranasaction " + st);
            byte[] content = request.getRawContent();
            if (content != null) {
                log.info(" content = " + new String(content));
                ContentTypeHeader contentTypeHeader = protocolObjects.headerFactory
                        .createContentTypeHeader("application", "sdp");
                log.info("response = " + response);
                response.setContent(content, contentTypeHeader);
            }
            dialog = st.getDialog();

            if (dialog != null) {
                log.info("Dialog " + dialog);
                log.info("Dialog state " + dialog.getState());
            }
            st.sendResponse(response);
            response = protocolObjects.messageFactory.createResponse(200, request);
            toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
            toHeader.setTag("4321");
            // Application is supposed to set.
            response.setHeader(contactHeader);
            st.sendResponse(response);
            log.info("TxState after sendResponse = " + st.getState());
            this.inviteTid = st;
            this.sendReInvite(sipProvider);
        } catch (Exception ex) {
            String s = "unexpected exception";

            log.error(s, ex);
            ReInviteTest.fail(s);
        }
    }

    public void sendReInvite(SipProvider sipProvider) throws Exception {
        Request inviteRequest = dialog.createRequest(Request.INVITE);
        ((SipURI) inviteRequest.getRequestURI()).removeParameter("transport");
        MaxForwardsHeader mf = protocolObjects.headerFactory.createMaxForwardsHeader(10);
        inviteRequest.addHeader(mf);
        ((ViaHeader) inviteRequest.getHeader(ViaHeader.NAME))
                .setTransport(protocolObjects.transport);
        Address address = protocolObjects.addressFactory.createAddress("Shootme <sip:"
                + myAddress + ":" + myPort + ">");
        ContactHeader contactHeader = protocolObjects.headerFactory
                .createContactHeader(address);
        inviteRequest.addHeader(contactHeader);
        ClientTransaction ct = sipProvider
                .getNewClientTransaction(inviteRequest);
        dialog.sendRequest(ct);
    }

    /**
     * Process the bye request.
     */
    public void processBye(RequestEvent requestEvent,
                           ServerTransaction serverTransactionId) {

        SipProvider sipProvider = (SipProvider) requestEvent.getSource();
        Request request = requestEvent.getRequest();
        try {
            log.info("shootme:  got a bye sending OK.");
            Response response = protocolObjects.messageFactory.createResponse(200, request);
            if (serverTransactionId != null) {
                serverTransactionId.sendResponse(response);
                log.info("Dialog State is "
                        + serverTransactionId.getDialog().getState());
            } else {
                log.info("null server tx.");
                // sipProvider.sendResponse(response);
            }

        } catch (Exception ex) {
            String s = "Unexpected exception";
            log.error(s, ex);
            ReInviteTest.fail(s);

        }
    }

    public void processResponse(ResponseEvent responseReceivedEvent) {
        log.info("Got a response");
        Response response = (Response) responseReceivedEvent.getResponse();
        Transaction tid = responseReceivedEvent.getClientTransaction();

        log.info("Response received with client transaction id "
                + tid + ":\n" + response);
        try {
            if (response.getStatusCode() == Response.REQUEST_PENDING) {
                this.requestPendingSeen = true;
            }
            if (tid != null) {
                Dialog dialog = tid.getDialog();
                log.info("Dalog State = " + dialog.getState());
            }
        } catch (Exception ex) {

            String s = "Unexpected exception";

            log.error(s, ex);
            ReInviteTest.fail(s);
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


    public SipProvider createSipProvider() throws Exception {
        ListeningPoint lp = protocolObjects.sipStack.createListeningPoint(myAddress,
                myPort, protocolObjects.transport);


        SipProvider sipProvider = protocolObjects.sipStack.createSipProvider(lp);
        return sipProvider;
    }


    public static void main(String args[]) throws Exception {
        ProtocolObjects protocolObjects = new ProtocolObjects("shootme", "co.ecg.jain_sip", "udp", true, false, false);

        Shootme shootme = new Shootme(protocolObjects);
        shootme.createSipProvider().addSipListener(shootme);

    }

    public void checkState() {
        ReInviteTest.assertTrue("Should NOT see request pending", !this.requestPendingSeen);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.sip.SipListener#processIOException(javax.sip.IOExceptionEvent)
     */
    public void processIOException(IOExceptionEvent exceptionEvent) {
        log.error("An IO Exception was detected : "
                + exceptionEvent.getHost());

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.sip.SipListener#processTransactionTerminated(javax.sip.TransactionTerminatedEvent)
     */
    public void processTransactionTerminated(
            TransactionTerminatedEvent transactionTerminatedEvent) {
        log.info("Tx terminated event ");

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.sip.SipListener#processDialogTerminated(javax.sip.DialogTerminatedEvent)
     */
    public void processDialogTerminated(
            DialogTerminatedEvent dialogTerminatedEvent) {
        log.info("Dialog terminated event detected ");

    }

}
