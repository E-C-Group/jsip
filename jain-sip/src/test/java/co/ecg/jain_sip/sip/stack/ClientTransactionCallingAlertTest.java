package co.ecg.jain_sip.sip.stack;

import co.ecg.jain_sip.sip.*;
import co.ecg.jain_sip.sip.address.Address;
import co.ecg.jain_sip.sip.address.AddressFactory;
import co.ecg.jain_sip.sip.address.SipURI;
import co.ecg.jain_sip.sip.header.*;
import co.ecg.jain_sip.sip.message.MessageFactory;
import co.ecg.jain_sip.sip.message.Request;
import co.ecg.jain_sip.sip.message.Response;
import co.ecg.jain_sip.sip.ri.ClientTransactionExt;
import co.ecg.jain_sip.sip.ri.stack.NioMessageProcessorFactory;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Properties;
import java.util.TimerTask;


import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientTransactionCallingAlertTest extends TestCase {
    public static final boolean callerSendsBye = true;


    class Shootist implements SipListener {

        private SipProvider sipProvider;

        private AddressFactory addressFactory;

        private MessageFactory messageFactory;

        private HeaderFactory headerFactory;

        private SipStack sipStack;

        private ContactHeader contactHeader;

        private ListeningPoint udpListeningPoint;

        private ClientTransaction inviteTid;

        private Dialog dialog;

        private long startTime = System.currentTimeMillis();

        private boolean byeTaskRunning;

        class ByeTask extends TimerTask {
            Dialog dialog;

            public ByeTask(Dialog dialog) {
                this.dialog = dialog;
            }

            public void run() {
                try {
                    Request byeRequest = this.dialog.createRequest(Request.BYE);
                    ClientTransaction ct = sipProvider
                            .getNewClientTransaction(byeRequest);
                    log.info("15s are over: sending BYE now");
                    dialog.sendRequest(ct);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.exit(0);
                }

            }

        }

        private static final String usageString = "java "
                + "examples.shootist.Shootist \n"
                + ">>>> is your class path set to the root?";

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
            else {
                try {
                    serverTransactionId.sendResponse(messageFactory
                            .createResponse(202, request));
                } catch (Exception e) {
                    fail("Unexpected exception");
                    log.error("Unexpected exception", e);
                }
            }

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
                ex.printStackTrace();
                System.exit(0);

            }
        }

        // Save the created ACK request, to respond to retransmitted 2xx
        private Request ackRequest;

        private boolean timeoutSeen;

        public void processResponse(ResponseEvent responseReceivedEvent) {
            log.info("Response seen " + responseReceivedEvent.getResponse().getStatusCode());

        }


        // ***************************************************************** END

        public void processTimeout(TimeoutEvent timeoutEvent) {

            log.info("Transaction Time out");
            if (timeoutEvent.getTimeout() == Timeout.RETRANSMIT) {
                this.timeoutSeen = true;
            }

            assertEquals("ClientTxState must be Calling ", timeoutEvent.getClientTransaction().getState(), TransactionState.CALLING);
        }

        public void init() {
            SipFactory sipFactory = null;
            sipStack = null;
            sipFactory = SipFactory.getInstance();
            sipFactory.setPathName("co.ecg.jain_sip");
            Properties properties = new Properties();
            // If you want to try TCP transport change the following to
            String transport = "udp";
            String peerHostPort = "127.0.0.1:5070";
            properties.setProperty("javax.sip.OUTBOUND_PROXY", peerHostPort
                    + "/" + transport);
            // If you want to use UDP then uncomment this.
            properties.setProperty("javax.sip.STACK_NAME", "shootist");

            // The following properties are specific to nist-sip
            // and are not necessarily part of any other jain-sip
            // implementation.
            // You can set a max message size for tcp transport to
            // guard against denial of service attack.
            properties.setProperty("gov.nist.javax.sip.DEBUG_LOG",
                    "logs/" + this.getClass().getName() + ".shootistdebug.txt");
            properties.setProperty("gov.nist.javax.sip.SERVER_LOG",
                    "logs/shootistlog.txt");

            // Drop the client connection after we are done with the
            // transaction.
            properties.setProperty(
                    "gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS", "false");
            // Set to 0 (or NONE) in your production code for max speed.
            // You need 16 (or TRACE) for logging traces. 32 (or DEBUG) for
            // debug + traces.
            // Your code will limp at 32 but it is best for debugging.


            if (System.getProperty("enableNIO") != null && System.getProperty("enableNIO").equalsIgnoreCase("true")) {
                log.info("\nNIO Enabled\n");
                properties.setProperty("gov.nist.javax.sip.MESSAGE_PROCESSOR_FACTORY", NioMessageProcessorFactory.class.getName());
            }

            try {
                // Create SipStack object
                sipStack = sipFactory.createSipStack(properties);
                log.info("createSipStack " + sipStack);
            } catch (PeerUnavailableException e) {
                // could not find
                // gov.nist.jain.protocol.ip.sip.SipStackImpl
                // in the classpath
                e.printStackTrace();
                System.err.println(e.getMessage());
                System.exit(0);
            }

            try {
                headerFactory = sipFactory.createHeaderFactory();
                addressFactory = sipFactory.createAddressFactory();
                messageFactory = sipFactory.createMessageFactory();
                udpListeningPoint = sipStack.createListeningPoint("127.0.0.1",
                        5060, "udp");
                sipProvider = sipStack.createSipProvider(udpListeningPoint);
                Shootist listener = this;
                sipProvider.addSipListener(listener);

                String fromName = "BigGuy";
                String fromSipAddress = "here.com";
                String fromDisplayName = "The Master Blaster";

                String toSipAddress = "there.com";
                String toUser = "LittleGuy";
                String toDisplayName = "The Little Blister";

                // create >From Header
                SipURI fromAddress = addressFactory.createSipURI(fromName,
                        fromSipAddress);

                Address fromNameAddress = addressFactory
                        .createAddress(fromAddress);
                fromNameAddress.setDisplayName(fromDisplayName);
                FromHeader fromHeader = headerFactory.createFromHeader(
                        fromNameAddress, "12345");

                // create To Header
                SipURI toAddress = addressFactory.createSipURI(toUser,
                        toSipAddress);
                Address toNameAddress = addressFactory.createAddress(toAddress);
                toNameAddress.setDisplayName(toDisplayName);
                ToHeader toHeader = headerFactory.createToHeader(toNameAddress,
                        null);

                // create Request URI
                SipURI requestURI = addressFactory.createSipURI(toUser,
                        peerHostPort);

                // Create ViaHeaders

                ArrayList viaHeaders = new ArrayList();
                String ipAddress = udpListeningPoint.getIPAddress();
                ViaHeader viaHeader = headerFactory.createViaHeader(ipAddress,
                        sipProvider.getListeningPoint(transport).getPort(),
                        transport, null);

                // add via headers
                viaHeaders.add(viaHeader);

                // Create ContentTypeHeader
                ContentTypeHeader contentTypeHeader = headerFactory
                        .createContentTypeHeader("application", "sdp");

                // Create a new CallId header
                CallIdHeader callIdHeader = sipProvider.getNewCallId();

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
                contactUrl.setPort(udpListeningPoint.getPort());
                contactUrl.setLrParam();

                // Create the contact name address.
                SipURI contactURI = addressFactory.createSipURI(fromName, host);
                contactURI.setPort(sipProvider.getListeningPoint(transport)
                        .getPort());

                Address contactAddress = addressFactory
                        .createAddress(contactURI);

                // Add the contact address.
                contactAddress.setDisplayName(fromName);

                contactHeader = headerFactory
                        .createContactHeader(contactAddress);
                request.addHeader(contactHeader);

                // You can add extension headers of your own making
                // to the outgoing SIP request.
                // Add the extension header.
                Header extensionHeader = headerFactory.createHeader(
                        "My-Header", "my header value");
                request.addHeader(extensionHeader);

                String sdpData = "v=0\r\n"
                        + "o=4855 13760799956958020 13760799956958020"
                        + " IN IP4  129.6.55.78\r\n"
                        + "s=mysession session\r\n" + "p=+46 8 52018010\r\n"
                        + "c=IN IP4  129.6.55.78\r\n" + "t=0 0\r\n"
                        + "m=audio 6022 RTP/AVP 0 4 18\r\n"
                        + "a=rtpmap:0 PCMU/8000\r\n"
                        + "a=rtpmap:4 G723/8000\r\n"
                        + "a=rtpmap:18 G729A/8000\r\n" + "a=ptime:20\r\n";
                byte[] contents = sdpData.getBytes();

                request.setContent(contents, contentTypeHeader);
                // You can add as many extension headers as you
                // want.

                extensionHeader = headerFactory.createHeader("My-Other-Header",
                        "my new header value ");
                request.addHeader(extensionHeader);

                Header callInfoHeader = headerFactory.createHeader("Call-Info",
                        "<http://www.antd.nist.gov>");
                request.addHeader(callInfoHeader);

                // Non regression test for https://java.net/jira/browse/JSIP-460
                ReasonHeader reasonHeader = headerFactory.createReasonHeader("SIP", 200, "Call Completed Elsewhere");
                request.addHeader(reasonHeader);
                reasonHeader = headerFactory.createReasonHeader("Q.850", 16, "Terminated");
                request.addHeader(reasonHeader);
                // https://java.net/jira/browse/JSIP-479
                reasonHeader = headerFactory.createReasonHeader("Q.777", 12, null);
                request.addHeader(reasonHeader);

                ListIterator<Header> listIt = request.getHeaders(ReasonHeader.NAME);
                int i = 0;
                while (listIt.hasNext()) {
                    listIt.next();
                    i++;
                }
                assertEquals(3, i);
                // non regression test for https://java.net/jira/browse/JSIP-467
                request.setExpires(headerFactory.createExpiresHeader(3600000));
                // Create the client transaction.
                inviteTid = sipProvider.getNewClientTransaction(request);

                ((ClientTransactionExt) inviteTid).alertIfStillInCallingStateBy(2);

                // send the request out.
                inviteTid.sendRequest();

                dialog = inviteTid.getDialog();

            } catch (Exception ex) {
                log.error("Unexpected exception", ex);
                fail("Unexpected exception ");
            }
        }

        public void processIOException(IOExceptionEvent exceptionEvent) {
            log.error("IOException happened for "
                    + exceptionEvent.getHost() + " port = "
                    + exceptionEvent.getPort());

        }

        public void processTransactionTerminated(
                TransactionTerminatedEvent transactionTerminatedEvent) {
            if (transactionTerminatedEvent.getServerTransaction() != null)
                log.info("Shootist: Transaction terminated event recieved on transaction : " +
                        transactionTerminatedEvent.getServerTransaction());
            else
                log.info("Shootist : Transaction terminated event recieved on transaction : " +
                        transactionTerminatedEvent.getClientTransaction());
        }

        public void processDialogTerminated(
                DialogTerminatedEvent dialogTerminatedEvent) {
            log.info("dialogTerminatedEvent");

        }

        public void terminate() {
            sipStack.stop();
        }
    }

    public class Shootme implements SipListener {

        private AddressFactory addressFactory;

        private MessageFactory messageFactory;

        private HeaderFactory headerFactory;

        private SipStack sipStack;

        private static final String myAddress = "127.0.0.1";

        private static final int myPort = 5070;


        private Response okResponse;

        private Request inviteRequest;

        private Dialog dialog;


        protected static final String usageString = "java "
                + "examples.shootist.Shootist \n"
                + ">>>> is your class path set to the root?";

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
            }

        }

        public void processResponse(ResponseEvent responseEvent) {
            fail("Unexpected event");
        }

        /**
         * Process the ACK request. Send the bye and complete the call flow.
         */
        public void processAck(RequestEvent requestEvent,
                               ServerTransaction serverTransaction) {
            try {
                log.info("shootme: got an ACK! ");
                log.info("Dialog State = " + dialog.getState());
                SipProvider provider = (SipProvider) requestEvent.getSource();
                if (!callerSendsBye) {
                    Request byeRequest = dialog.createRequest(Request.BYE);
                    ClientTransaction ct = provider
                            .getNewClientTransaction(byeRequest);
                    dialog.sendRequest(ct);
                }
            } catch (Exception ex) {
                log.error("Unexpected exception", ex);
                fail("unexpected exception");
            }

        }

        /**
         * Process the invite request.
         */
        public void processInvite(RequestEvent requestEvent,
                                  ServerTransaction serverTransaction) {
            SipProvider sipProvider = (SipProvider) requestEvent.getSource();
            Request request = requestEvent.getRequest();
            this.inviteRequest = request;
        }


        /**
         * Process the bye request.
         */
        public void processBye(RequestEvent requestEvent,
                               ServerTransaction serverTransactionId) {
            SipProvider sipProvider = (SipProvider) requestEvent.getSource();
            Request request = requestEvent.getRequest();
            Dialog dialog = requestEvent.getDialog();
            log.info("shootme: local party = " + dialog.getLocalParty());
            try {
                log.info("shootme:  got a bye sending OK.");
                Response response = messageFactory.createResponse(200, request);
                serverTransactionId.sendResponse(response);
                log.info("shootme: Dialog State is "
                        + serverTransactionId.getDialog().getState());

            } catch (Exception ex) {
                log.error("UNexpected exception", ex);
                fail("UNexpected exception");

            }
        }


        public void processTimeout(TimeoutEvent timeoutEvent) {
            Transaction transaction;
            if (timeoutEvent.isServerTransaction()) {
                transaction = timeoutEvent.getServerTransaction();
            } else {
                transaction = timeoutEvent.getClientTransaction();
            }
            log.info("Shootme: Transaction Time out : " + transaction);
        }

        public void init() {
            SipFactory sipFactory = null;
            sipStack = null;
            sipFactory = SipFactory.getInstance();
            sipFactory.setPathName("co.ecg.jain_sip");
            Properties properties = new Properties();
            properties.setProperty("javax.sip.STACK_NAME", "shootme");
            // You need 16 for logging traces. 32 for debug + traces.
            // Your code will limp at 32 but it is best for debugging.

            properties.setProperty("gov.nist.javax.sip.DEBUG_LOG",
                    "logs/" + this.getClass().getName() + ".shootmedebug.txt");
            properties.setProperty("gov.nist.javax.sip.SERVER_LOG",
                    "shootmelog.txt");
            if (System.getProperty("enableNIO") != null && System.getProperty("enableNIO").equalsIgnoreCase("true")) {
                log.info("\nNIO Enabled\n");
                properties.setProperty("gov.nist.javax.sip.MESSAGE_PROCESSOR_FACTORY", NioMessageProcessorFactory.class.getName());
            }
            try {
                // Create SipStack object
                sipStack = sipFactory.createSipStack(properties);
                log.info("sipStack = " + sipStack);
            } catch (PeerUnavailableException e) {
                // could not find
                // gov.nist.jain.protocol.ip.sip.SipStackImpl
                // in the classpath
                e.printStackTrace();
                System.err.println(e.getMessage());
                if (e.getCause() != null)
                    e.getCause().printStackTrace();
                log.error("Unexpected error creating stack", e);
                fail("Unexpected error");
            }

            try {
                headerFactory = sipFactory.createHeaderFactory();
                addressFactory = sipFactory.createAddressFactory();
                messageFactory = sipFactory.createMessageFactory();
                ListeningPoint lp = sipStack.createListeningPoint("127.0.0.1",
                        myPort, "udp");

                Shootme listener = this;

                SipProvider sipProvider = sipStack.createSipProvider(lp);
                log.info("udp provider " + sipProvider);
                sipProvider.addSipListener(listener);

            } catch (Exception ex) {
                fail("Unexpected exception");
            }

        }

        public void terminate() {
            this.sipStack.stop();
        }

        public void processIOException(IOExceptionEvent exceptionEvent) {
            log.info("IOException");

        }

        public void processTransactionTerminated(
                TransactionTerminatedEvent transactionTerminatedEvent) {
            if (transactionTerminatedEvent.isServerTransaction())
                log.info("Transaction terminated event recieved"
                        + transactionTerminatedEvent.getServerTransaction());
            else
                log.info("Transaction terminated "
                        + transactionTerminatedEvent.getClientTransaction());

        }

        public void processDialogTerminated(
                DialogTerminatedEvent dialogTerminatedEvent) {
            log.info("Shootme: Dialog terminated event recieved");
            Dialog d = dialogTerminatedEvent.getDialog();
            log.info("Local Party = " + d.getLocalParty());

        }

    }

    private Shootist shootist;
    private Shootme shootme;

    public void setUp() {
        this.shootme = new Shootme();
        this.shootist = new Shootist();


    }

    public void tearDown() {
        shootist.terminate();
        shootme.terminate();
    }

    public void testRetransmit() {
        this.shootme.init();
        this.shootist.init();
        try {
            Thread.sleep(2000);
        } catch (Exception ex) {

        }
        assertTrue("Timeout must be seen", shootist.timeoutSeen);
    }
}
