package co.ecg.jain_sip.sip.stack;

import co.ecg.jain_sip.sip.*;
import co.ecg.jain_sip.sip.address.Address;
import co.ecg.jain_sip.sip.address.AddressFactory;
import co.ecg.jain_sip.sip.address.SipURI;
import co.ecg.jain_sip.sip.header.*;
import co.ecg.jain_sip.sip.message.MessageFactory;
import co.ecg.jain_sip.sip.message.Request;
import co.ecg.jain_sip.sip.message.Response;
import co.ecg.jain_sip.sip.ri.stack.NioMessageProcessorFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Properties;

import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;

@Slf4j
public class DeliverNotifyBefore202Test extends TestCase {
    private static AddressFactory addressFactory;

    private static MessageFactory messageFactory;

    private static HeaderFactory headerFactory;
    private static SipFactory sipFactory;

    private Notifier notifier;
    private Subscriber subscriber;


    static {
        try {
            sipFactory = SipFactory.getInstance();
            sipFactory.setPathName("co.ecg.jain_sip");

            headerFactory = sipFactory.createHeaderFactory();
            addressFactory = sipFactory.createAddressFactory();
            messageFactory = sipFactory.createMessageFactory();

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    /**
     * The Subscriber.
     */

    class Subscriber implements SipListener {

        private SipProvider udpProvider;

        private SipStack sipStack;

        private ContactHeader contactHeader;

        private int notifierPort;

        private String transport = "udp";

        private int count;


        private ClientTransaction subscribeTid;

        private ListeningPoint listeningPoint;

        private int port;

        private boolean notifySeen;

        public void processRequest(RequestEvent requestReceivedEvent) {
            Request request = requestReceivedEvent.getRequest();
            ServerTransaction serverTransactionId = requestReceivedEvent.getServerTransaction();
            String viaBranch = ((ViaHeader) (request.getHeaders(ViaHeader.NAME).next()))
                    .getParameter("branch");

            log.info("\n\nRequest " + request.getMethod() + " received at "
                    + sipStack.getStackName() + " with server transaction id "
                    + serverTransactionId + " branch ID = " + viaBranch);

            if (request.getMethod().equals(Request.NOTIFY))
                processNotify(requestReceivedEvent, serverTransactionId);

        }

        public synchronized void processNotify(RequestEvent requestEvent,
                                               ServerTransaction serverTransactionId) {
            SipProvider provider = (SipProvider) requestEvent.getSource();
            Request notify = requestEvent.getRequest();
            try {
                log.info("subscriber:  got a notify count  " + this.count++);
                if (serverTransactionId == null) {
                    log.info("subscriber:  null TID.");
                    serverTransactionId = provider.getNewServerTransaction(notify);
                }
                Dialog dialog = serverTransactionId.getDialog();
                log.info("Dialog = " + dialog);

                if (dialog != null) {
                    log.info("Dialog State = " + dialog.getState());
                }

                Response response = messageFactory.createResponse(200, notify);
                // SHOULD add a Contact
                ContactHeader contact = SerializationUtils.clone(contactHeader);
                ((SipURI) contact.getAddress().getURI()).setParameter("id", "sub");
                response.addHeader(contact);
                log.info("Transaction State = " + serverTransactionId.getState());
                serverTransactionId.sendResponse(response);
                if (dialog != null) {
                    log.info("Dialog State = " + dialog.getState());
                }
                SubscriptionStateHeader subscriptionState = (SubscriptionStateHeader) notify
                        .getHeader(SubscriptionStateHeader.NAME);

                // Subscription is terminated?
                String state = subscriptionState.getState();
                if (state.equalsIgnoreCase(SubscriptionStateHeader.TERMINATED)) {
                    dialog.delete();
                } else {
                    log.info("Subscriber: state now " + state);
                }
                this.notifySeen = true;

            } catch (Exception ex) {
                ex.printStackTrace();
                log.error("Unexpected exception", ex);
                fail("Unexpected exception");

            }
        }

        public void processResponse(ResponseEvent responseReceivedEvent) {
            log.info("Got a response");
            Response response = (Response) responseReceivedEvent.getResponse();
            Transaction tid = responseReceivedEvent.getClientTransaction();

            log.info("Response received with client transaction id " + tid + ":\n"
                    + response.getStatusCode());
            if (tid == null) {
                log.info("Stray response -- dropping ");
                return;
            }
            log.info("transaction state is " + tid.getState());
            log.info("Dialog = " + tid.getDialog());
            if (tid.getDialog() != null)
                log.info("Dialog State is " + tid.getDialog().getState());

        }

        public void createProvider() throws Exception {

            this.listeningPoint = sipStack.createListeningPoint("127.0.0.1", port, transport);
            udpProvider = sipStack.createSipProvider(listeningPoint);

        }

        public void sendSubscribe() {

            try {

                String fromName = "BigGuy";
                String fromSipAddress = "here.com";
                String fromDisplayName = "The Master Blaster";

                String toSipAddress = "there.com";
                String toUser = "LittleGuy";
                String toDisplayName = "The Little Blister";

                // create >From Header
                SipURI fromAddress = addressFactory.createSipURI(fromName, fromSipAddress);

                Address fromNameAddress = addressFactory.createAddress(fromAddress);
                fromNameAddress.setDisplayName(fromDisplayName);
                FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress, "12345");

                // create To Header
                SipURI toAddress = addressFactory.createSipURI(toUser, toSipAddress);
                Address toNameAddress = addressFactory.createAddress(toAddress);
                toNameAddress.setDisplayName(toDisplayName);
                ToHeader toHeader = headerFactory.createToHeader(toNameAddress, null);

                // create Request URI
                SipURI requestURI = addressFactory.createSipURI(toUser, toSipAddress);

                // Create ViaHeaders

                ArrayList viaHeaders = new ArrayList();
                int port = udpProvider.getListeningPoint(transport).getPort();
                ViaHeader viaHeader = headerFactory.createViaHeader("127.0.0.1", port, transport,
                        null);

                // add via headers
                viaHeaders.add(viaHeader);

                // Create a new CallId header
                CallIdHeader callIdHeader = udpProvider.getNewCallId();

                // Create a new Cseq header
                CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1L, Request.SUBSCRIBE);

                // Create a new MaxForwardsHeader
                MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);

                // Create the request.
                Request request = messageFactory.createRequest(requestURI, Request.SUBSCRIBE,
                        callIdHeader, cSeqHeader, fromHeader, toHeader, viaHeaders, maxForwards);
                // Create contact headers
                String host = listeningPoint.getIPAddress();

                SipURI contactUrl = addressFactory.createSipURI(fromName, host);
                contactUrl.setPort(listeningPoint.getPort());

                // Create the contact name address.
                SipURI contactURI = addressFactory.createSipURI(fromName, host);
                contactURI.setTransportParam(transport);
                contactURI.setPort(udpProvider.getListeningPoint(transport).getPort());

                Address contactAddress = addressFactory.createAddress(contactURI);

                // Add the contact address.
                contactAddress.setDisplayName(fromName);

                contactHeader = headerFactory.createContactHeader(contactAddress);
                request.addHeader(contactHeader);

                // JvB: To test forked SUBSCRIBEs, send it via the Forker
                // Note: BIG Gotcha: Need to do this before creating the
                // ClientTransaction!

                RouteHeader route = headerFactory.createRouteHeader(addressFactory
                        .createAddress("<sip:127.0.0.1:" + notifierPort + ";transport="
                                + transport + ";lr>"));
                request.addHeader(route);
                // JvB end added

                // Create the client transaction.
                subscribeTid = udpProvider.getNewClientTransaction(request);

                // Create an event header for the subscription.
                EventHeader eventHeader = headerFactory.createEventHeader("foo");
                eventHeader.setEventId("foo");
                request.addHeader(eventHeader);

                log.info("Subscribe Dialog = " + subscribeTid.getDialog());


                // send the request out.
                subscribeTid.sendRequest();

            } catch (Throwable ex) {
                log.info(ex.getMessage());
                ex.printStackTrace();
                fail("Unexpected exception");
            }
        }

        public void processIOException(IOExceptionEvent exceptionEvent) {
            log.info("io exception event recieved");
            fail("unexpected event -- IOException");
        }

        public void processTransactionTerminated(
                TransactionTerminatedEvent transactionTerminatedEvent) {
        }

        public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
            log.info("dialog terminated event    recieved");
        }

        public void processTimeout(TimeoutEvent timeoutEvent) {
            log.info("Transaction Time out");
            fail("Unexpected event -- timeout");
        }

        public Subscriber(int notifierPort, int port) throws Exception {
            this.notifierPort = notifierPort;
            this.port = port;

            Properties properties = new Properties();

            properties.setProperty("javax.sip.STACK_NAME", "subscriber" + port);
            // You need 16 for logging traces. 32 for debug + traces.
            // Your code will limp at 32 but it is best for debugging.

            properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "subscriberdebug_" + port
                    + ".txt");
            properties.setProperty("gov.nist.javax.sip.SERVER_LOG", "subscriberlog_" + port
                    + ".txt");
            if (System.getProperty("enableNIO") != null && System.getProperty("enableNIO").equalsIgnoreCase("true")) {
                log.info("\nNIO Enabled\n");
                properties.setProperty("gov.nist.javax.sip.MESSAGE_PROCESSOR_FACTORY", NioMessageProcessorFactory.class.getName());
            }
            // Create SipStack object
            sipStack = sipFactory.createSipStack(properties);
            log.info("sipStack = " + sipStack);
            this.createProvider();
            this.udpProvider.addSipListener(this);

        }

        public void tearDown() {
            this.sipStack.stop();
        }

        public boolean checkNotify() {
            return this.notifySeen;
        }
    }

    /**
     * The Notifier
     */
    class Notifier implements SipListener {

        private SipStack sipStack;

        private int port;

        private SipProvider udpProvider;

        private Dialog dialog;

        protected int notifyCount = 0;


        public void processRequest(RequestEvent requestEvent) {
            Request request = requestEvent.getRequest();
            ServerTransaction serverTransactionId = requestEvent.getServerTransaction();

            log.info("\n\nRequest " + request.getMethod() + " received at "
                    + sipStack.getStackName() + " with server transaction id "
                    + serverTransactionId + " and dialog id " + requestEvent.getDialog());

            if (request.getMethod().equals(Request.SUBSCRIBE)) {
                processSubscribe(requestEvent, serverTransactionId);
            }

        }

        /**
         * Process the invite request.
         */
        public void processSubscribe(RequestEvent requestEvent,
                                     ServerTransaction serverTransaction) {
            SipProvider sipProvider = (SipProvider) requestEvent.getSource();
            Request request = requestEvent.getRequest();
            try {
                log.info("notifier: got an Subscribe sending OK");
                log.info("notifier:  " + request);
                log.info("notifier : dialog = " + requestEvent.getDialog());
                EventHeader eventHeader = (EventHeader) request.getHeader(EventHeader.NAME);
                if (eventHeader == null) {
                    log.info("Cannot find event header.... dropping request.");
                    return;
                }

                // Always create a ServerTransaction, best as early as possible in the code
                Response response = null;
                ServerTransaction st = requestEvent.getServerTransaction();
                if (st == null) {
                    st = sipProvider.getNewServerTransaction(request);
                }

                // Check if it is an initial SUBSCRIBE or a refresh / unsubscribe
                boolean isInitial = requestEvent.getDialog() == null;
                if (isInitial) {
                    // JvB: need random tags to test forking
                    String toTag = Integer.toHexString((int) (Math.random() * Integer.MAX_VALUE));
                    response = messageFactory.createResponse(202, request);
                    ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);

                    // Sanity check: to header should not ahve a tag. Else the dialog
                    // should have matched
                    if (toHeader.getTag() != null) {
                        System.err
                                .println("####ERROR: To-tag!=null but no dialog match! My dialog="
                                        + dialog.getState());
                    }
                    toHeader.setTag(toTag); // Application is supposed to set.

                    this.dialog = st.getDialog();
                    // subscribe dialogs do not terminate on bye.
                    this.dialog.terminateOnBye(false);
                    if (dialog != null) {
                        log.info("Dialog " + dialog);
                        log.info("Dialog state " + dialog.getState());
                    }
                } else {
                    response = messageFactory.createResponse(200, request);
                }

                // Both 2xx response to SUBSCRIBE and NOTIFY need a Contact
                Address address = addressFactory.createAddress("Notifier <sip:127.0.0.1>");
                ((SipURI) address.getURI()).setPort(udpProvider.getListeningPoint("udp")
                        .getPort());
                ContactHeader contactHeader = headerFactory.createContactHeader(address);
                response.addHeader(contactHeader);

                // Expires header is mandatory in 2xx responses to SUBSCRIBE
                ExpiresHeader expires = (ExpiresHeader) request.getHeader(ExpiresHeader.NAME);
                if (expires == null) {
                    expires = headerFactory.createExpiresHeader(30); // rather short
                }
                response.addHeader(expires);

                /*
                 * NOTIFY requests MUST contain a "Subscription-State" header with a value of
                 * "active", "pending", or "terminated". The "active" value indicates that the
                 * subscription has been accepted and has been authorized (in most cases; see
                 * section 5.2.). The "pending" value indicates that the subscription has been
                 * received, but that policy information is insufficient to accept or deny the
                 * subscription at this time. The "terminated" value indicates that the
                 * subscription is not active.
                 */

                Address fromAddress = ((ToHeader) response.getHeader(ToHeader.NAME)).getAddress();
                String fromTag = ((ToHeader) response.getHeader(ToHeader.NAME)).getTag();
                FromHeader fromHeader = headerFactory.createFromHeader(fromAddress, fromTag);

                Address toAddress = ((FromHeader) response.getHeader(FromHeader.NAME)).getAddress();
                String toTag = ((FromHeader) response.getHeader(FromHeader.NAME)).getTag();
                ToHeader toHeader = headerFactory.createToHeader(toAddress, toTag);


                CallIdHeader callId = (CallIdHeader) response.getHeader(CallIdHeader.NAME);

                ContactHeader requestContact = (ContactHeader) request
                        .getHeader(ContactHeader.NAME);
                SipURI notifyRuri = (SipURI) requestContact.getAddress().getURI();
                CSeqHeader cSeq = headerFactory.createCSeqHeader(1L, Request.NOTIFY);
                String ipAddress = sipProvider.getListeningPoint("udp").getIPAddress();
                int port = sipProvider.getListeningPoint("udp").getPort();
                ViaHeader viaHeader = headerFactory.createViaHeader(ipAddress, port, "udp", null);
                LinkedList llist = new LinkedList<ViaHeader>();
                llist.add(viaHeader);

                MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);
                Request notifyRequest = messageFactory.createRequest(notifyRuri, Request.NOTIFY,
                        callId, cSeq, fromHeader, toHeader, llist, maxForwards);
                notifyRequest.addHeader(contactHeader);

                // Mark the contact header, to check that the remote contact is updated
                ((SipURI) contactHeader.getAddress().getURI()).setParameter("id", "not");

                // Initial state is pending, second time we assume terminated (Expires==0)
                SubscriptionStateHeader sstate = headerFactory
                        .createSubscriptionStateHeader(isInitial ? SubscriptionStateHeader.PENDING
                                : SubscriptionStateHeader.TERMINATED);

                // Need a reason for terminated
                if (sstate.getState().equalsIgnoreCase("terminated")) {
                    sstate.setReasonCode("deactivated");
                }

                notifyRequest.addHeader(sstate);
                notifyRequest.setHeader(eventHeader);
                notifyRequest.setHeader(contactHeader);
                // notifyRequest.setHeader(routeHeader);
                ClientTransaction ct = udpProvider.getNewClientTransaction(notifyRequest);

                /*
                 * We deliberately send the NOTIFY first before the 202 is sent.
                 */
                ct.sendRequest();
                log.info("NOTIFY Branch ID "
                        + ((ViaHeader) request.getHeader(ViaHeader.NAME)).getParameter("branch"));
                log.info("Dialog " + dialog);
                log.info("Dialog state after pending NOTIFY: " + dialog.getState());

                /*
                 * Now send the NOTIFY.
                 */
                st.sendResponse(response);

            } catch (Throwable ex) {
                ex.printStackTrace();
                // System.exit(0);
            }
        }

        public synchronized void processResponse(ResponseEvent responseReceivedEvent) {
            Response response = (Response) responseReceivedEvent.getResponse();
            Transaction tid = responseReceivedEvent.getClientTransaction();

            if (response.getStatusCode() != 200) {
                this.notifyCount--;
            } else {
                System.out.println("Notify Count = " + this.notifyCount);
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

        public void createProvider() {

            try {

                ListeningPoint lp = sipStack.createListeningPoint("127.0.0.1", this.port, "udp");

                this.udpProvider = sipStack.createSipProvider(lp);
                log.info("udp provider " + udpProvider);

            } catch (Exception ex) {
                log.info(ex.getMessage());
                ex.printStackTrace();

            }

        }

        public Notifier(int port) throws Exception {
            this.port = port;
            Properties properties = new Properties();

            properties.setProperty("javax.sip.STACK_NAME", "notifier" + port);
            // You need 16 for logging traces. 32 for debug + traces.
            // Your code will limp at 32 but it is best for debugging.

            properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "notifierdebug_" + port
                    + ".txt");
            properties.setProperty("gov.nist.javax.sip.SERVER_LOG", "notifierlog_" + port
                    + ".txt");
            if (System.getProperty("enableNIO") != null && System.getProperty("enableNIO").equalsIgnoreCase("true")) {
                log.info("\nNIO Enabled\n");
                properties.setProperty("gov.nist.javax.sip.MESSAGE_PROCESSOR_FACTORY", NioMessageProcessorFactory.class.getName());
            }
            // Create SipStack object
            sipStack = sipFactory.createSipStack(properties);
            log.info("sipStack = " + sipStack);
            this.createProvider();
            this.udpProvider.addSipListener(this);
        }

        public void processIOException(IOExceptionEvent exceptionEvent) {

        }

        public void processTransactionTerminated(
                TransactionTerminatedEvent transactionTerminatedEvent) {

        }

        public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
            // TODO Auto-generated method stub

        }

        public void tearDown() {
            this.sipStack.stop();
        }

    }

    public void setUp() throws Exception {
        this.notifier = new Notifier(5090);
        this.subscriber = new Subscriber(5090, 5092);

    }

    public void testDeliverNotifyBefore202() {
        this.subscriber.sendSubscribe();
        try {
            Thread.sleep(2000);
        } catch (Exception ex) {

        }
        if (!this.subscriber.checkNotify()) {
            fail("Notify not received");
        }
    }

    public void tearDown() {
        this.subscriber.tearDown();
        this.notifier.tearDown();
    }

}
