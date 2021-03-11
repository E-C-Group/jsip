package co.ecg.jain_sip.sip.stack.dialog.b2bua;

import co.ecg.jain_sip.sip.*;
import co.ecg.jain_sip.sip.address.Address;
import co.ecg.jain_sip.sip.address.AddressFactory;
import co.ecg.jain_sip.sip.address.SipURI;
import co.ecg.jain_sip.sip.header.*;
import co.ecg.jain_sip.sip.message.MessageFactory;
import co.ecg.jain_sip.sip.message.Request;
import co.ecg.jain_sip.sip.message.Response;
import co.ecg.jain_sip.sip.ri.SipStackImpl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import co.ecg.jain_sip.tck.msgflow.callflows.ProtocolObjects;
import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is a UAC template. Shootist is the guy that shoots and shootme is
 * the guy that gets shot.
 *
 * @author M. Ranganathan
 */
@Slf4j
public class Shootist implements SipListener {

    private ContactHeader contactHeader;

    private ClientTransaction inviteTid;


    private SipProvider sipProvider;

    private String host = "127.0.0.1";

    private int port;

    private String peerHost = "127.0.0.1";

    private int peerPort;

    private ListeningPoint listeningPoint;

    private static String unexpectedException = "Unexpected exception ";

    private Dialog originalDialog;

    private HashSet<Dialog> forkedDialogs = new HashSet<Dialog>();

    private Dialog ackedDialog;

    private SipStack sipStack;

    private HashSet<Dialog> canceledDialog = new HashSet<Dialog>();

    private boolean byeResponseSeen;

    private int counter;

    private static HeaderFactory headerFactory;

    private static MessageFactory messageFactory;

    private static AddressFactory addressFactory;

    private static final String transport = "udp";

    static boolean callerSendsBye = true;

    private boolean byeSent;

    private Timer timer = new Timer();

    private boolean inviteOkSeen;


    class SendBye extends TimerTask {

        private Dialog dialog;

        public SendBye(Dialog dialog) {
            this.dialog = dialog;
        }

        @Override
        public void run() {
            try {
                TestCase.assertEquals("Dialog state must be confirmed",
                        DialogState.CONFIRMED, dialog.getState());


                Request byeRequest = dialog.createRequest(Request.BYE);
                ClientTransaction ctx = sipProvider.getNewClientTransaction(byeRequest);
                dialog.sendRequest(ctx);
            } catch (Exception ex) {
                TestCase.fail("Unexpected exception");
            }

        }

    }


    public Shootist(int myPort, int proxyPort) throws Exception {


        this.port = myPort;

        ProtocolObjects sipObjects = new ProtocolObjects("shootist", "co.ecg.jain_sip", "udp", true, false, false);
        addressFactory = sipObjects.addressFactory;
        messageFactory = sipObjects.messageFactory;
        headerFactory = sipObjects.headerFactory;
        this.sipStack = sipObjects.sipStack;

        this.peerPort = proxyPort;
        this.createSipProvider();
        this.sipProvider.addSipListener(this);


    }

    public void processRequest(RequestEvent requestReceivedEvent) {
        Request request = requestReceivedEvent.getRequest();
        ServerTransaction serverTransactionId = requestReceivedEvent
                .getServerTransaction();

        log.info("\n\nRequest " + request.getMethod() + " received at "
                + sipStack.getStackName()
                + " with server transaction id " + serverTransactionId);

        // We are the UAC so the only request we get is the BYE.
        if (request.getMethod().equals(Request.BYE))
            processBye(request, serverTransactionId);
        else
            TestCase.fail("Unexpected request ! : " + request);

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
            Response response = messageFactory.createResponse(
                    200, request);
            serverTransactionId.sendResponse(response);
            log.info("shootist:  Sending OK.");
            log.info("Dialog State = " + dialog.getState());

        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);

        }
    }

    public synchronized void processResponse(ResponseEvent responseReceivedEvent) {
        log.info("Got a response");
        Response response = (Response) responseReceivedEvent.getResponse();
        ClientTransaction tid = responseReceivedEvent.getClientTransaction();
        CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);

        log.info("Response received : Status Code = "
                + response.getStatusCode() + " " + cseq);
        log.info("Response = " + response + " class=" + response.getClass());

        Dialog dialog = responseReceivedEvent.getDialog();
        TestCase.assertNotNull(dialog);

        if (tid != null)
            log.info("transaction state is " + tid.getState());
        else
            log.info("transaction = " + tid);

        log.info("Dialog = " + dialog);

        log.info("Dialog state is " + dialog.getState());

        try {
            if (response.getStatusCode() == Response.OK) {
                if (cseq.getMethod().equals(Request.INVITE)) {
                    this.inviteOkSeen = true;
                    TestCase.assertEquals(DialogState.CONFIRMED, dialog.getState());
                    Request ackRequest = dialog.createAck(cseq
                            .getSeqNumber());

                    TestCase.assertNotNull(ackRequest.getHeader(MaxForwardsHeader.NAME));

                    if (dialog == this.ackedDialog) {
                        dialog.sendAck(ackRequest);
                        return;
                    }
                    // Proxy will fork. I will accept the first dialog.
                    this.forkedDialogs.add(dialog);
                    if (responseReceivedEvent.getClientTransaction() != null) {
                        log.info("Sending ACK");
                        dialog.sendAck(ackRequest);
                        TestCase.assertTrue(
                                "Dialog state should be CONFIRMED", dialog
                                        .getState() == DialogState.CONFIRMED);

                        TestCase.assertTrue(this.ackedDialog == null ||
                                this.ackedDialog == dialog);
                        this.ackedDialog = dialog;

                        if (callerSendsBye) {
                            timer.schedule(new SendBye(ackedDialog), 1000);
                        }


                    } else {
                        this.canceledDialog.add(dialog);
                        // Send ACK to quench re-transmission
                        sipProvider.sendRequest(ackRequest);
                        // Kill the second dialog by sending a bye.
                        SipProvider sipProvider = (SipProvider) responseReceivedEvent
                                .getSource();

                        Request byeRequest = dialog.createRequest(Request.BYE);
                        ClientTransaction ct = sipProvider
                                .getNewClientTransaction(byeRequest);
                        dialog.sendRequest(ct);
                    }


                } else if (cseq.getMethod().equals(Request.BYE)) {

                    if (dialog == this.ackedDialog) {
                        this.byeResponseSeen = true;
                    }
                } else {
                    log.info("Response method = " + cseq.getMethod());
                }
            } else if (response.getStatusCode() == Response.RINGING) {
                //TestHarness.assertEquals( DialogState.EARLY, dialog.getState() );
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            // System.exit(0);
        }

    }

    public SipProvider createSipProvider() {
        try {
            listeningPoint = sipStack.createListeningPoint(
                    host, port, "udp");

            log.info("listening point = " + host + " port = " + port);
            log.info("listening point = " + listeningPoint);
            sipProvider = sipStack
                    .createSipProvider(listeningPoint);
            return sipProvider;
        } catch (Exception ex) {
            log.error(unexpectedException, ex);
            TestCase.fail(unexpectedException);
            return null;
        }

    }

    public void checkState() {

        TestCase.assertTrue("Should see BYE response for ACKED Dialog", this.byeResponseSeen);
        TestCase.assertTrue("InviteOK seen", this.inviteOkSeen);

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
            String toUser = "LittleGuy";
            String toDisplayName = "The Little Blister";

            // create >From Header
            SipURI fromAddress = addressFactory.createSipURI(
                    fromName, fromSipAddress);

            Address fromNameAddress = addressFactory
                    .createAddress(fromAddress);
            fromNameAddress.setDisplayName(fromDisplayName);
            FromHeader fromHeader = headerFactory
                    .createFromHeader(fromNameAddress, "12345");

            // create To Header
            SipURI toAddress = addressFactory.createSipURI(
                    toUser, toSipAddress);
            Address toNameAddress = addressFactory
                    .createAddress(toAddress);
            toNameAddress.setDisplayName(toDisplayName);
            ToHeader toHeader = headerFactory.createToHeader(
                    toNameAddress, null);

            // create Request URI
            String peerHostPort = peerHost + ":" + peerPort;
            SipURI requestURI = addressFactory.createSipURI(
                    toUser, peerHostPort);

            // Create ViaHeaders

            ArrayList viaHeaders = new ArrayList();
            ViaHeader viaHeader = headerFactory
                    .createViaHeader(host, sipProvider.getListeningPoint(
                            transport).getPort(),
                            transport, null);

            // add via headers
            viaHeaders.add(viaHeader);

            SipURI sipuri = addressFactory.createSipURI(null,
                    host);
            sipuri.setPort(peerPort);
            sipuri.setLrParam();

            RouteHeader routeHeader = headerFactory
                    .createRouteHeader(addressFactory
                            .createAddress(sipuri));

            // Create ContentTypeHeader
            ContentTypeHeader contentTypeHeader = headerFactory
                    .createContentTypeHeader("application", "sdp");

            // Create a new CallId header
            CallIdHeader callIdHeader = sipProvider.getNewCallId();
            // JvB: Make sure that the implementation matches the messagefactory
            callIdHeader = headerFactory
                    .createCallIdHeader(callIdHeader.getCallId());

            // Create a new Cseq header
            CSeqHeader cSeqHeader = headerFactory
                    .createCSeqHeader(1L, Request.INVITE);

            // Create a new MaxForwardsHeader
            MaxForwardsHeader maxForwards = headerFactory
                    .createMaxForwardsHeader(70);

            // Create the request.
            Request request = messageFactory.createRequest(
                    requestURI, Request.INVITE, callIdHeader, cSeqHeader,
                    fromHeader, toHeader, viaHeaders, maxForwards);
            // Create contact headers

            SipURI contactUrl = addressFactory.createSipURI(
                    fromName, host);
            contactUrl.setPort(listeningPoint.getPort());

            // Create the contact name address.
            SipURI contactURI = addressFactory.createSipURI(
                    fromName, host);
            contactURI.setPort(sipProvider.getListeningPoint(
                    transport).getPort());
            contactURI.setTransportParam(transport);

            Address contactAddress = addressFactory
                    .createAddress(contactURI);

            // Add the contact address.
            contactAddress.setDisplayName(fromName);

            contactHeader = headerFactory
                    .createContactHeader(contactAddress);
            request.addHeader(contactHeader);

            // Dont use the Outbound Proxy. Use Lr instead.
            request.setHeader(routeHeader);

            // Add the extension header.
            Header extensionHeader = headerFactory
                    .createHeader("My-Header", "my header value");
            request.addHeader(extensionHeader);

            String sdpData = "v=0\r\n"
                    + "o=4855 13760799956958020 13760799956958020"
                    + " IN IP4  129.6.55.78\r\n" + "s=mysession session\r\n"
                    + "p=+46 8 52018010\r\n" + "c=IN IP4  129.6.55.78\r\n"
                    + "t=0 0\r\n" + "m=audio 6022 RTP/AVP 0 4 18\r\n"
                    + "a=rtpmap:0 PCMU/8000\r\n" + "a=rtpmap:4 G723/8000\r\n"
                    + "a=rtpmap:18 G729A/8000\r\n" + "a=ptime:20\r\n";
            byte[] contents = sdpData.getBytes();

            request.setContent(contents, contentTypeHeader);

            extensionHeader = headerFactory.createHeader(
                    "My-Other-Header", "my new header value ");
            request.addHeader(extensionHeader);

            Header callInfoHeader = headerFactory.createHeader(
                    "Call-Info", "<http://www.antd.nist.gov>");
            request.addHeader(callInfoHeader);

            // Create the client transaction.
            inviteTid = sipProvider.getNewClientTransaction(request);
            Dialog dialog = inviteTid.getDialog();

            TestCase.assertTrue("Initial dialog state should be null",
                    dialog.getState() == null);

            // send the request out.
            inviteTid.sendRequest();

            this.originalDialog = dialog;

        } catch (Exception ex) {
            log.error(unexpectedException, ex);
            TestCase.fail(unexpectedException);

        }
    }

    public void processIOException(IOExceptionEvent exceptionEvent) {
        log.error("IOException happened for " + exceptionEvent.getHost()
                + " port = " + exceptionEvent.getPort());
        TestCase.fail("Unexpected exception");

    }

    public void processTransactionTerminated(
            TransactionTerminatedEvent transactionTerminatedEvent) {
        log.info("Transaction terminated event recieved");
    }

    public void processDialogTerminated(
            DialogTerminatedEvent dialogTerminatedEvent) {
        TestCase.assertTrue("DTE dialog must be one of those we canceled",
                this.canceledDialog.contains((Dialog) dialogTerminatedEvent.getDialog()));

    }

    public void stop() {
        this.sipStack.stop();
    }
}
