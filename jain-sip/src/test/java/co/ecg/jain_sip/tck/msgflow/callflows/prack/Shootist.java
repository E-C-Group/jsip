package co.ecg.jain_sip.tck.msgflow.callflows.prack;

import java.util.ArrayList;

import co.ecg.jain_sip.sip.*;
import co.ecg.jain_sip.sip.address.Address;
import co.ecg.jain_sip.sip.address.AddressFactory;
import co.ecg.jain_sip.sip.address.SipURI;
import co.ecg.jain_sip.sip.header.CSeqHeader;
import co.ecg.jain_sip.sip.header.CallIdHeader;
import co.ecg.jain_sip.sip.header.ContactHeader;
import co.ecg.jain_sip.sip.header.ContentTypeHeader;
import co.ecg.jain_sip.sip.header.FromHeader;
import co.ecg.jain_sip.sip.header.HeaderFactory;
import co.ecg.jain_sip.sip.header.MaxForwardsHeader;
import co.ecg.jain_sip.sip.header.RequireHeader;
import co.ecg.jain_sip.sip.header.ToHeader;
import co.ecg.jain_sip.sip.header.ViaHeader;
import co.ecg.jain_sip.sip.message.MessageFactory;
import co.ecg.jain_sip.sip.message.Request;
import co.ecg.jain_sip.sip.message.Response;

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
public class Shootist implements SipListener {

    private static SipProvider sipProvider;

    private static AddressFactory addressFactory;

    private static MessageFactory messageFactory;

    private static HeaderFactory headerFactory;

    private static SipStack sipStack;

    private ContactHeader contactHeader;

    private ClientTransaction inviteTid;

    private Dialog dialog;

    private String transport;

    private boolean prackTriggerReceived;
    private boolean prackConfirmed;

    public static final int myPort = 5070;

    private String toUser = "LittleGuy";

    public Shootist(ProtocolObjects protObjects) {
        addressFactory = protObjects.addressFactory;
        messageFactory = protObjects.messageFactory;
        headerFactory = protObjects.headerFactory;
        sipStack = protObjects.sipStack;
        transport = protObjects.transport;
    }

    public SipProvider createProvider() throws Exception {
        ListeningPoint lp = sipStack.createListeningPoint("127.0.0.1",
                myPort, transport);

        sipProvider = sipStack.createSipProvider(lp);
        log.info(transport + " SIP provider " + sipProvider);

        return sipProvider;
    }


    public void processRequest(RequestEvent requestReceivedEvent) {
        Request request = requestReceivedEvent.getRequest();
        ServerTransaction serverTransactionId = requestReceivedEvent
                .getServerTransaction();

        log.info("\n\nRequest " + request.getMethod()
                + " received at " + sipStack.getStackName()
                + " with server transaction id " + serverTransactionId);

        // We are the UAC so the only request we get is the BYE.
        if (request.getMethod().equals(Request.BYE))
            processBye(request, serverTransactionId);

    }

    public void processBye(Request request,
            ServerTransaction serverTransactionId) {
        try {
            log.info("shootist:  got a bye .");
            if (serverTransactionId == null) {
                log.info("shootist:  null TID.");
                return;
            }
            Dialog dialog = serverTransactionId.getDialog();
            log.info("Dialog State = " + dialog.getState());
            Response response = messageFactory.createResponse(200, request);
            serverTransactionId.sendResponse(response);
            log.info("shootist:  Sending OK.");
            log.info("Dialog State = " + dialog.getState());

        } catch (Exception ex) {
            TestHarness.fail(ex.getMessage());
            System.exit(0);

        }
    }

    public void processResponse(ResponseEvent responseReceivedEvent) {
        log.info("Got a response");
        Response response = (Response) responseReceivedEvent.getResponse();
        ClientTransaction tid = responseReceivedEvent.getClientTransaction();
        CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);

        log.info("Response received : Status Code = "
                + response.getStatusCode() + " " + cseq);

        if (cseq.getMethod() == Request.PRACK) {
            prackConfirmed = true;
        }

        if (tid == null) {
            log.info("Stray response -- dropping ");
            return;
        }
        log.info("transaction state is " + tid.getState());
        log.info("Dialog = " + tid.getDialog());
        log.info("Dialog State is " + tid.getDialog().getState());
        SipProvider provider = (SipProvider) responseReceivedEvent.getSource();
        dialog = tid.getDialog();

        try {
            if (response.getStatusCode() == Response.OK) {
                if (cseq.getMethod().equals(Request.INVITE)) {
                    Request ackRequest = dialog.createAck(((CSeqHeader) response.getHeader(CSeqHeader.NAME)).getSeqNumber());
                    log.info("Sending ACK");
                    dialog.sendAck(ackRequest);
                }

            } else if ( response.getStatusCode() == Shootme.PRACK_CODE) {
                prackTriggerReceived = true;
                RequireHeader requireHeader = (RequireHeader) response.getHeader(RequireHeader.NAME);
                if ( requireHeader.getOptionTag().equalsIgnoreCase("100rel")) {
                    Dialog dialog = tid.getDialog();
                    Request prackRequest = dialog.createPrack(response);
                    // create Request URI
                    SipURI requestURI = addressFactory.createSipURI(toUser,
                            "127.0.0.1:" + Shootme.myPort);
                    prackRequest.setRequestURI(requestURI);
                    ClientTransaction ct = provider.getNewClientTransaction(prackRequest);
                    dialog.sendRequest(ct);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            TestHarness.fail("Unexpected exception " + ex.getMessage());

        }

    }

    public void processTimeout(TimeoutEvent timeoutEvent) {
        log.info("Transaction Time out");
    }

    public void sendInvite() {
        try {
            String fromName = "BigGuy";
            String fromSipAddress = "here.com";
            String fromDisplayName = "The Master Blaster";

            String toSipAddress = "there.com";
            String toDisplayName = "The Little Blister";

            // create >From Header
            SipURI fromAddress = addressFactory.createSipURI(fromName,
                    fromSipAddress);

            Address fromNameAddress = addressFactory.createAddress(fromAddress);
            fromNameAddress.setDisplayName(fromDisplayName);
            FromHeader fromHeader = headerFactory.createFromHeader(
                    fromNameAddress, "12345");

            // create To Header
            SipURI toAddress = addressFactory
                    .createSipURI(toUser, toSipAddress);
            Address toNameAddress = addressFactory.createAddress(toAddress);
            toNameAddress.setDisplayName(toDisplayName);
            ToHeader toHeader = headerFactory.createToHeader(toNameAddress,
                    null);

            // create Request URI
            SipURI requestURI = addressFactory.createSipURI(toUser,
                    "127.0.0.1:" + Shootme.myPort);

            // Create ViaHeaders

            ArrayList viaHeaders = new ArrayList();
            ViaHeader viaHeader = headerFactory.createViaHeader("127.0.0.1",
                    sipProvider.getListeningPoint(transport).getPort(),
                    transport, null);

            // add via headers
            viaHeaders.add(viaHeader);

            // Create ContentTypeHeader
            ContentTypeHeader contentTypeHeader = headerFactory
                    .createContentTypeHeader("application", "sdp");

            // Create a new CallId header
            CallIdHeader callIdHeader = sipProvider.getNewCallId();
            // JvB: Make sure that the implementation matches the messagefactory
            callIdHeader = headerFactory.createCallIdHeader( callIdHeader.getCallId() );


            // Create a new Cseq header
            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1L,
                    Request.INVITE);

            // Create a new MaxForwardsHeader
            MaxForwardsHeader maxForwards = headerFactory
                    .createMaxForwardsHeader(70);

            // Create the request.
            Request request = messageFactory.createRequest(requestURI,
                    Request.INVITE, callIdHeader, cSeqHeader, fromHeader,
                    toHeader, viaHeaders, maxForwards);
            // Create contact headers
            String host = "127.0.0.1";

            SipURI contactUrl = addressFactory.createSipURI(fromName, host);
            ListeningPoint lp = sipProvider.getListeningPoint(transport);
            contactUrl.setPort(lp.getPort());

            // Create the contact name address.
            SipURI contactURI = addressFactory.createSipURI(fromName, host);
            contactURI.setPort(sipProvider.getListeningPoint(transport)
                    .getPort());

            Address contactAddress = addressFactory.createAddress(contactURI);

            // Add the contact address.
            contactAddress.setDisplayName(fromName);

            contactHeader = headerFactory.createContactHeader(contactAddress);
            request.addHeader(contactHeader);

            /*
             * When the UAC creates a new request, it can insist on reliable
             * delivery of provisional responses for that request. To do that,
             * it inserts a Require header field with the option tag 100rel into
             * the request.
             */

            RequireHeader requireHeader = headerFactory
                    .createRequireHeader("100rel");
            request.addHeader(requireHeader);
            // Create the client transaction.
            inviteTid = sipProvider.getNewClientTransaction(request);

            this.dialog = inviteTid.getDialog();

            // send the request out.
            inviteTid.sendRequest();


        } catch (Exception ex) {
            TestHarness.fail("sendInvite failed because of " + ex.getMessage());
        }
    }

    public void processIOException(IOExceptionEvent exceptionEvent) {
        log.info("IOException happened for "
                + exceptionEvent.getHost() + " port = "
                + exceptionEvent.getPort());

    }

    public void processTransactionTerminated(
            TransactionTerminatedEvent transactionTerminatedEvent) {
        log.info("Transaction terminated event recieved");
    }

    public void processDialogTerminated(
            DialogTerminatedEvent dialogTerminatedEvent) {
        log.info("dialogTerminatedEvent");
    }

    public void checkState() {
        TestHarness.assertTrue( this.prackTriggerReceived );
        TestHarness.assertTrue( prackConfirmed );
    }

}
