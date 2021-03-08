package co.ecg.jain_sip.tck.msgflow.callflows.refer;

import java.util.ArrayList;

import co.ecg.jain_sip.sip.*;
import co.ecg.jain_sip.sip.address.Address;
import co.ecg.jain_sip.sip.address.AddressFactory;
import co.ecg.jain_sip.sip.address.SipURI;
import co.ecg.jain_sip.sip.header.CSeqHeader;
import co.ecg.jain_sip.sip.header.CallIdHeader;
import co.ecg.jain_sip.sip.header.ContactHeader;
import co.ecg.jain_sip.sip.header.FromHeader;
import co.ecg.jain_sip.sip.header.HeaderFactory;
import co.ecg.jain_sip.sip.header.MaxForwardsHeader;
import co.ecg.jain_sip.sip.header.ReferToHeader;
import co.ecg.jain_sip.sip.header.SubscriptionStateHeader;
import co.ecg.jain_sip.sip.header.ToHeader;
import co.ecg.jain_sip.sip.header.ViaHeader;
import co.ecg.jain_sip.sip.message.MessageFactory;
import co.ecg.jain_sip.sip.message.Request;
import co.ecg.jain_sip.sip.message.Response;

import co.ecg.jain_sip.tck.TestHarness;
import co.ecg.jain_sip.tck.msgflow.callflows.ProtocolObjects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;

/**
 * This example shows an out-of-dialog REFER scenario:
 * <p>
 * referer sends REFER to referee, with Refer-To set to Shootme
 * referee sends INVITE to Shootme, and NOTIFYs to referer about call progress
 *
 * @author Jeroen van Bemmel
 * @author Ivelin Ivanov
 */
@Slf4j
public class Referrer implements SipListener {

    private SipProvider sipProvider;

    private AddressFactory addressFactory;

    private MessageFactory messageFactory;

    private HeaderFactory headerFactory;

    private SipStack sipStack;

    private ContactHeader contactHeader;

    private String transport;

    public static final int myPort = 5080;

    public int count;//< Number of NOTIFYs

    private ClientTransaction subscribeTid;

    private ListeningPoint listeningPoint;

    public Referrer(ProtocolObjects protObjects) {
        addressFactory = protObjects.addressFactory;
        messageFactory = protObjects.messageFactory;
        headerFactory = protObjects.headerFactory;
        sipStack = protObjects.sipStack;
        transport = protObjects.transport;
    }

    public void processRequest(RequestEvent requestReceivedEvent) {
        Request request = requestReceivedEvent.getRequest();
        ServerTransaction serverTransactionId = requestReceivedEvent
                .getServerTransaction();
        String viaBranch = ((ViaHeader) (request.getHeaders(ViaHeader.NAME).next())).getParameter("branch");

        log.info("\n\nRequest " + request.getMethod() + " received at "
                + sipStack.getStackName() + " with server transaction id "
                + serverTransactionId +
                " branch ID = " + viaBranch);
        //log.info( request );
        if (request.getMethod().equals(Request.NOTIFY)) {
            processNotify(requestReceivedEvent, serverTransactionId);
        } else if (request.getMethod().equals(Request.INVITE)) {
            processInvite(requestReceivedEvent);
        } else if (request.getMethod().equals(Request.ACK)) {
            processAck(requestReceivedEvent);
        } else if (request.getMethod().equals(Request.BYE)) {
            processBye(requestReceivedEvent);
        } else {
            TestHarness.fail("Unexpected request type:" + request.getMethod());
        }

    }

    private void processNotify(RequestEvent requestEvent,
                               ServerTransaction serverTransactionId) {
        SipProvider provider = (SipProvider) requestEvent.getSource();
        Request notify = requestEvent.getRequest();
        if (notify.getMethod().equals("NOTIFY")) try {
            log.info("referer:  got a NOTIFY count  " + ++this.count + ":\n" + notify);
            if (serverTransactionId == null) {
                log.info("referer:  null TID.");
                serverTransactionId = provider.getNewServerTransaction(notify);
            }
            Dialog dialog = serverTransactionId.getDialog();
            log.info("Dialog = " + dialog);

            TestHarness.assertTrue("Dialog should not be null", dialog != null);
            log.info("Dialog State = " + dialog.getState());

            Response response = messageFactory.createResponse(200, notify);
            // SHOULD add a Contact
            ContactHeader contact = (ContactHeader) SerializationUtils.clone(contactHeader);
            ((SipURI) contact.getAddress().getURI()).setParameter("id", "sub");
            response.addHeader(contact);
            log.info("Transaction State = " + serverTransactionId.getState());
            serverTransactionId.sendResponse(response);
            log.info("Dialog State = " + dialog.getState());
            SubscriptionStateHeader subscriptionState = (SubscriptionStateHeader) notify
                    .getHeader(SubscriptionStateHeader.NAME);

            // Subscription is terminated?
            String state = subscriptionState.getState();
            if (state.equalsIgnoreCase(SubscriptionStateHeader.TERMINATED)) {
                dialog.delete();
            } else {
                log.info("Referer: state now " + state);
            }
        } catch (Exception ex) {
            TestHarness.fail("Failed processing notify, because of " + ex);

        }
        else {
            TestHarness.fail("Unexpected request type");
        }
    }

    private void processInvite(RequestEvent re) {
        SipProvider provider = (SipProvider) re.getSource();
        ServerTransaction st = re.getServerTransaction();
        try {
            if (st == null) st = provider.getNewServerTransaction(re.getRequest());
            Response r = messageFactory.createResponse(100, re.getRequest());
            st.sendResponse(r);
            r = messageFactory.createResponse(180, re.getRequest());
            r.addHeader((ContactHeader) SerializationUtils.clone(contactHeader));
            ((ToHeader) r.getHeader("To")).setTag("inv_res");
            st.sendResponse(r);
            Thread.sleep(500);
            r = messageFactory.createResponse(200, re.getRequest());
            r.addHeader(SerializationUtils.clone(contactHeader));
            ((ToHeader) r.getHeader("To")).setTag("inv_res");
            st.sendResponse(r);
        } catch (Throwable t) {
            t.printStackTrace();
            TestHarness.fail("Throwable:" + t.getLocalizedMessage());
        }
    }

    private void processAck(RequestEvent re) {
        // ignore it, Referee sends BYE right after
    }

    private void processBye(RequestEvent re) {
        try {
            re.getServerTransaction().sendResponse(
                    messageFactory.createResponse(200, re.getRequest()));
        } catch (Throwable t) {
            t.printStackTrace();
            TestHarness.fail("Throwable:" + t.getLocalizedMessage());
        }
    }

    public void processResponse(ResponseEvent responseReceivedEvent) {
        Response response = (Response) responseReceivedEvent.getResponse();
        Transaction tid = responseReceivedEvent.getClientTransaction();

        log.info("Got a response:" + response.getStatusCode()
                + ':' + response.getHeader(CSeqHeader.NAME));

        log.info("Response received with client transaction id " + tid
                + ": " + response.getStatusCode());
        if (tid == null) {
            log.info("Stray response -- dropping ");
            return;
        }
        log.info("transaction state is " + tid.getState());
        log.info("Dialog = " + tid.getDialog());
        if (tid.getDialog() != null)
            log.info("Dialog State is " + tid.getDialog().getState());

    }

    public SipProvider createProvider() throws Exception {

        this.listeningPoint = sipStack.createListeningPoint("127.0.0.1", myPort,
                transport);
        sipProvider = sipStack.createSipProvider(listeningPoint);
        return sipProvider;

    }

    public void sendRefer() {

        try {

            String fromName = "BigGuy";
            String fromSipAddress = "here.com";
            String fromDisplayName = "The Master Blaster";

            String toSipAddress = "127.0.0.1";
            String toUser = "referee";
            String toDisplayName = "Referee";

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
                    toSipAddress);
            requestURI.setPort(Referee.myPort);// referee
            requestURI.setTransportParam(transport);

            // Create ViaHeaders

            ArrayList viaHeaders = new ArrayList();
            int port = sipProvider.getListeningPoint(transport).getPort();
            ViaHeader viaHeader = headerFactory.createViaHeader("127.0.0.1",
                    port, transport, null);

            // add via headers
            viaHeaders.add(viaHeader);

            // Create a new CallId header
            CallIdHeader callIdHeader = sipProvider.getNewCallId();
            // JvB: Make sure that the implementation matches the messagefactory
            callIdHeader = headerFactory.createCallIdHeader(callIdHeader.getCallId());


            // Create a new Cseq header
            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1L,
                    Request.REFER);

            // Create a new MaxForwardsHeader
            MaxForwardsHeader maxForwards = headerFactory
                    .createMaxForwardsHeader(70);

            // Create the request.
            Request request = messageFactory.createRequest(requestURI,
                    Request.REFER, callIdHeader, cSeqHeader, fromHeader,
                    toHeader, viaHeaders, maxForwards);
            // Create contact headers
            String host = listeningPoint.getIPAddress();

            SipURI contactUrl = addressFactory.createSipURI(fromName, host);
            contactUrl.setPort(listeningPoint.getPort());

            // Create the contact name address.
            SipURI contactURI = addressFactory.createSipURI(fromName, host);
            contactURI.setTransportParam(transport);
            contactURI.setPort(sipProvider.getListeningPoint(transport).getPort());

            Address contactAddress = addressFactory.createAddress(contactURI);

            // Add the contact address.
            contactAddress.setDisplayName(fromName);

            contactHeader = headerFactory.createContactHeader(contactAddress);
            request.addHeader(contactHeader);

            // Create the client transaction.
            subscribeTid = sipProvider.getNewClientTransaction(request);

            // REFER has an implicit "refer" event
            // Create an event header for the subscription.
            // EventHeader eventHeader = headerFactory.createEventHeader("foo");
            // eventHeader.setEventId("foo");
            // request.addHeader(eventHeader);

            // Make the INVITE come back to this listener!
            ReferToHeader referTo = headerFactory.createReferToHeader(
                    addressFactory.createAddress("<sip:127.0.0.1:" + myPort + ";transport=" + transport + ">")
            );
            request.addHeader(referTo);

            log.info("Refer Dialog = " + subscribeTid.getDialog());

            // send the request out.
            subscribeTid.sendRequest();
        } catch (Throwable ex) {
            TestHarness.fail("Referrer failed sending Subscribe request, because of " + ex);
        }
    }

    public void processIOException(IOExceptionEvent exceptionEvent) {
        log.info("io exception event received");
        TestHarness.fail("IOException unexpected");
    }

    public void processTransactionTerminated(
            TransactionTerminatedEvent tte) {
        log.info("transaction terminated:" + tte);

    }

    public void processDialogTerminated(
            DialogTerminatedEvent dialogTerminatedEvent) {
        log.info("dialog terminated event received");
    }

    public void processTimeout(TimeoutEvent timeoutEvent) {
        log.info("Transaction Time out");
    }
}
