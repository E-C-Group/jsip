package co.ecg.jain_sip.sip.stack;

import co.ecg.jain_sip.sip.*;
import co.ecg.jain_sip.sip.address.Address;
import co.ecg.jain_sip.sip.address.AddressFactory;
import co.ecg.jain_sip.sip.header.CSeqHeader;
import co.ecg.jain_sip.sip.header.ContactHeader;
import co.ecg.jain_sip.sip.header.HeaderFactory;
import co.ecg.jain_sip.sip.header.ToHeader;
import co.ecg.jain_sip.sip.message.MessageFactory;
import co.ecg.jain_sip.sip.message.Request;
import co.ecg.jain_sip.sip.message.Response;
import co.ecg.jain_sip.sip.ri.message.MessageFactoryImpl;
import co.ecg.jain_sip.sip.ri.stack.NioMessageProcessorFactory;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Properties;

import junit.framework.TestCase;

public class DeliverRequestEventWithBadHeaderTest extends TestCase {
    public class Shootme implements SipListener {

        private AddressFactory addressFactory;

        private MessageFactory messageFactory;

        private HeaderFactory headerFactory;

        private SipStack sipStack;

        private static final String myAddress = "127.0.0.1";

        private static final int myPort = 5070;

        private Dialog dialog;

        private boolean sawInvite;

        public static final boolean callerSendsBye = true;

        public void processRequest(RequestEvent requestEvent) {
            Request request = requestEvent.getRequest();
            ServerTransaction serverTransactionId = requestEvent
                    .getServerTransaction();

            System.out.println("\n\nRequest " + request.getMethod()
                    + " received at " + sipStack.getStackName()
                    + " with server transaction id " + serverTransactionId);

            if (request.getMethod().equals(Request.INVITE)) {
                processInvite(requestEvent, serverTransactionId);
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
                System.out.println("shootme: got an ACK! ");
                System.out.println("Dialog State = " + dialog.getState());
                SipProvider provider = (SipProvider) requestEvent.getSource();
                if (!callerSendsBye) {
                    Request byeRequest = dialog.createRequest(Request.BYE);
                    ClientTransaction ct = provider
                            .getNewClientTransaction(byeRequest);
                    dialog.sendRequest(ct);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }

        /**
         * Process the invite request.
         */
        public void processInvite(RequestEvent requestEvent,
                                  ServerTransaction serverTransaction) {
            SipProvider sipProvider = (SipProvider) requestEvent.getSource();
            Request request = requestEvent.getRequest();
            this.sawInvite = true;
            try {

                Response okResponse = messageFactory.createResponse(
                        Response.OK, request);
                Address address = addressFactory.createAddress("Shootme <sip:"
                        + myAddress + ":" + myPort + ">");
                ContactHeader contactHeader = headerFactory
                        .createContactHeader(address);
                ToHeader toHeader = (ToHeader) okResponse
                        .getHeader(ToHeader.NAME);
                toHeader.setTag("4321"); // Application is supposed to set.
                okResponse.addHeader(contactHeader);
                sipProvider.sendResponse(okResponse); // Send it through the
                // Provider.

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
            System.out.println("state = " + transaction.getState());
            System.out.println("dialog = " + transaction.getDialog());
            System.out.println("dialogState = "
                    + transaction.getDialog().getState());
            System.out.println("Transaction Time out");
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
                    "shootmedebug.txt");
            properties.setProperty("gov.nist.javax.sip.SERVER_LOG",
                    "shootmelog.txt");
            if (System.getProperty("enableNIO") != null && System.getProperty("enableNIO").equalsIgnoreCase("true")) {
                properties.setProperty("gov.nist.javax.sip.MESSAGE_PROCESSOR_FACTORY", NioMessageProcessorFactory.class.getName());
            }
            try {
                // Create SipStack object
                sipStack = sipFactory.createSipStack(properties);
                System.out.println("sipStack = " + sipStack);
            } catch (PeerUnavailableException e) {
                // could not find
                // gov.nist.jain.protocol.ip.sip.SipStackImpl
                // in the classpath
                e.printStackTrace();
                System.err.println(e.getMessage());
                if (e.getCause() != null)
                    e.getCause().printStackTrace();
                System.exit(0);
            }

            try {
                headerFactory = sipFactory.createHeaderFactory();
                addressFactory = sipFactory.createAddressFactory();
                messageFactory = sipFactory.createMessageFactory();
                ((MessageFactoryImpl) messageFactory).setTest(true);
                ListeningPoint lp = sipStack.createListeningPoint("127.0.0.1",
                        myPort, "udp");

                Shootme listener = this;

                SipProvider sipProvider = sipStack.createSipProvider(lp);
                System.out.println("udp provider " + sipProvider);
                sipProvider.addSipListener(listener);

            } catch (Exception ex) {
                ex.printStackTrace();
                fail("Unexpected exception");
            }

        }

        public void processIOException(IOExceptionEvent exceptionEvent) {
            fail("IOException");

        }

        public void processTransactionTerminated(
                TransactionTerminatedEvent transactionTerminatedEvent) {
            if (transactionTerminatedEvent.isServerTransaction())
                System.out.println("Transaction terminated event recieved"
                        + transactionTerminatedEvent.getServerTransaction());
            else
                System.out.println("Transaction terminated "
                        + transactionTerminatedEvent.getClientTransaction());

        }

        public void processDialogTerminated(
                DialogTerminatedEvent dialogTerminatedEvent) {
            Dialog d = dialogTerminatedEvent.getDialog();
            System.out.println("Local Party = " + d.getLocalParty());

        }

        public void terminate() {
            this.sipStack.stop();
        }

    }

    public class Shootist implements SipListener {

        private SipProvider sipProvider;

        private AddressFactory addressFactory;

        private MessageFactory messageFactory;

        private HeaderFactory headerFactory;

        private SipStack sipStack;

        private ContactHeader contactHeader;

        private ListeningPoint udpListeningPoint;

        private Dialog dialog;

        private boolean timeoutRecieved;

        boolean sawOk;

        public void processRequest(RequestEvent requestReceivedEvent) {


        }

        public void processResponse(ResponseEvent responseReceivedEvent) {
            try {
                if (responseReceivedEvent.getResponse().getStatusCode() == Response.OK) {
                    this.sawOk = true;
                    Dialog dialog = responseReceivedEvent.getDialog();
                    long cseq = ((CSeqHeader) responseReceivedEvent.getResponse()
                            .getHeader(CSeqHeader.NAME)).getSeqNumber();
                    Request ack = dialog.createAck(cseq);
                    dialog.sendAck(ack);
                }
            } catch (Exception ex) {
                fail("Unexpected exception");

            }

        }

        public void processTimeout(TimeoutEvent timeoutEvent) {

            System.out.println("Got a timeout "
                    + timeoutEvent.getClientTransaction());

            this.timeoutRecieved = true;
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
                    "shootistdebug.txt");
            properties.setProperty("gov.nist.javax.sip.SERVER_LOG",
                    "shootistlog.txt");

            // Drop the client connection after we are done with the
            // transaction.
            properties.setProperty(
                    "gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS", "false");
            // Set to 0 (or NONE) in your production code for max speed.
            // You need 16 (or TRACE) for logging traces. 32 (or DEBUG) for
            // debug + traces.
            // Your code will limp at 32 but it is best for debugging.

            if (System.getProperty("enableNIO") != null && System.getProperty("enableNIO").equalsIgnoreCase("true")) {
                properties.setProperty("gov.nist.javax.sip.MESSAGE_PROCESSOR_FACTORY", NioMessageProcessorFactory.class.getName());
            }
            try {
                // Create SipStack object
                sipStack = sipFactory.createSipStack(properties);
                System.out.println("createSipStack " + sipStack);
            } catch (PeerUnavailableException e) {
                // could not find
                // gov.nist.jain.protocol.ip.sip.SipStackImpl
                // in the classpath
                e.printStackTrace();
                System.err.println(e.getMessage());
                fail("Problem with setup");
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

                String badRequest = "INVITE sip:LittleGuy@127.0.0.1:5070 SIP/2.0\r\n"
                        + "Call-ID: 7a3cd620346e3fa199cb397fe6b6fc16@127.0.0.1\r\n"
                        + "CSeq: 1 INVITE\r\n"
                        + "From: \"The Master Blaster\" <sip:BigGuy@here.com>;tag=12345\r\n"
                        + "To: \"The Little Blister\" <sip:LittleGuy@there.com>\r\n"
                        + "Via: SIP/2.0/UDP 127.0.0.1:5060\r\n"
                        + "Max-Forwards: 70\r\n"
                        + "Date: 123 GMT 789\r\n"
                        + "Contact: <127.0.0.1:5060>\r\n"
                        + "Content-Length: 0\r\n\r\n";

                System.out.println("Parsing message \n" + badRequest);
                boolean sawParseException = false;
                Request request = null;
                try {
                    request = messageFactory.createRequest(badRequest);
                } catch (ParseException ex) {
                    sawParseException = true;
                }

                assertTrue("Should not see a parse exception ",
                        !sawParseException);

                assertTrue("Should see unparsed headder", request.getUnrecognizedHeaders().hasNext() &&
                        ((String) request.getUnrecognizedHeaders().next()).equals("Date: 123 GMT 789"));

                ClientTransaction inviteTid = sipProvider
                        .getNewClientTransaction(request);

                // send the request out.
                inviteTid.sendRequest();

                dialog = inviteTid.getDialog();

            } catch (Exception ex) {
                ex.printStackTrace();
                fail("cannot create or send initial invite");
            }
        }

        public void processIOException(IOExceptionEvent exceptionEvent) {
            System.out.println("IOException happened for "
                    + exceptionEvent.getHost() + " port = "
                    + exceptionEvent.getPort());

        }

        public void processTransactionTerminated(
                TransactionTerminatedEvent transactionTerminatedEvent) {
            System.out.println("Transaction terminated event recieved");
        }

        public void processDialogTerminated(
                DialogTerminatedEvent dialogTerminatedEvent) {
            System.out.println("dialogTerminatedEvent");

        }

        public void terminate() {
            this.sipStack.stop();
        }
    }

    private Shootme shootme;
    private Shootist shootist;

    public void setUp() {
        this.shootme = new Shootme();
        this.shootist = new Shootist();

    }

    public void tearDown() {
        shootist.terminate();
        shootme.terminate();
    }

    public void testSendInviteWithBadHeader() {
        this.shootme.init();
        this.shootist.init();
        try {
            Thread.sleep(5000);
        } catch (Exception ex) {

        }
        assertTrue("Should see reuqest at shootme ", shootme.sawInvite);
        assertTrue("Should see response at shootist", shootist.sawOk);
    }

}
