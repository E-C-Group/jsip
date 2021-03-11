package co.ecg.jain_sip.sip.stack.acktransport;


import co.ecg.jain_sip.sip.*;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;


import co.ecg.jain_sip.sip.address.Address;
import co.ecg.jain_sip.sip.address.AddressFactory;
import co.ecg.jain_sip.sip.address.SipURI;
import co.ecg.jain_sip.sip.header.*;
import co.ecg.jain_sip.sip.message.MessageFactory;
import co.ecg.jain_sip.sip.message.Request;
import co.ecg.jain_sip.sip.message.Response;
import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;

/**
 * A very simple forking proxy server.
 *
 * @author M. Ranganathan
 */
@Slf4j
public class Proxy implements SipListener {

    // private ServerTransaction st;

    private SipProvider inviteServerTxProvider;

    private Hashtable clientTxTable = new Hashtable();

    private static String host = "127.0.0.1";

    private int port = 5070;

    private HashMap<String, SipProvider> sipProviders = new HashMap<String, SipProvider>();

    private static String unexpectedException = "Unexpected exception";

    private static AddressFactory addressFactory;

    private static MessageFactory messageFactory;

    private static HeaderFactory headerFactory;

    private SipStack sipStack;

    private void sendTo(ServerTransaction st, Request request, int targetPort) throws Exception {
        Request newRequest = SerializationUtils.clone(request);

        SipURI sipUri = addressFactory.createSipURI("UA1", "127.0.0.1");
        sipUri.setPort(targetPort);
        sipUri.setLrParam();
        sipUri.setTransportParam(Shootme.transport);
        Address address = addressFactory.createAddress("client1", sipUri);
        RouteHeader rheader = headerFactory.createRouteHeader(address);

        newRequest.addFirst(rheader);
        ViaHeader viaHeader = headerFactory.createViaHeader(host, this.port, "udp", null);
        newRequest.addFirst(viaHeader);
        ClientTransaction ct1 = sipProviders.get(Shootme.transport).getNewClientTransaction(newRequest);
        sipUri = addressFactory.createSipURI("proxy", "127.0.0.1");
        address = addressFactory.createAddress("proxy", sipUri);
        sipUri.setPort(5070);
        sipUri.setLrParam();
        RecordRouteHeader recordRoute = headerFactory.createRecordRouteHeader(address);
        newRequest.addHeader(recordRoute);
        ct1.setApplicationData(st);
        ct1.sendRequest();
    }

    public void processRequest(RequestEvent requestEvent) {
        try {
            Request request = requestEvent.getRequest();
            SipProvider sipProvider = (SipProvider) requestEvent.getSource();
            this.inviteServerTxProvider = sipProvider;

            if (request.getMethod().equals(Request.INVITE)) {
                // Attach a record route header with NO transport here.
                ServerTransaction st = null;
                if (requestEvent.getServerTransaction() == null) {
                    st = sipProvider.getNewServerTransaction(request);

                }
                this.sendTo(st, request, 5080);


            } else {
                // Remove the topmost route header
                // The route header will make sure it gets to the right place.
                log.info("proxy: Got a request " + request.getMethod());
                Request newRequest = SerializationUtils.clone(request);
                newRequest.removeFirst(RouteHeader.NAME);
                String transport = ((SipURI) newRequest.getRequestURI()).getTransportParam();
                sipProviders.get(transport).sendRequest(newRequest);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }

    }

    public void processResponse(ResponseEvent responseEvent) {
        try {
            Response response = responseEvent.getResponse();
            CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
            log.info("ClientTxID = " + responseEvent.getClientTransaction() + " client tx id "
                    + ((ViaHeader) response.getHeader(ViaHeader.NAME)).getBranch()
                    + " CSeq header = " + response.getHeader(CSeqHeader.NAME) + " status code = "
                    + response.getStatusCode());

            // JvB: stateful proxy MUST NOT forward 100 Trying
            if (response.getStatusCode() == 100)
                return;

            if (cseq.getMethod().equals(Request.INVITE)) {
                ClientTransaction ct = responseEvent.getClientTransaction();
                if (ct != null) {
                    ServerTransaction st = (ServerTransaction) ct.getApplicationData();

                    // Strip the topmost via header
                    Response newResponse = SerializationUtils.clone(response);
                    newResponse.removeFirst(ViaHeader.NAME);
                    // The server tx goes to the terminated state.

                    st.sendResponse(newResponse);
                } else {
                    // Client tx has already terminated but the UA is
                    // retransmitting
                    // just forward the response statelessly.
                    // Strip the topmost via header

                    Response newResponse = SerializationUtils.clone(response);
                    newResponse.removeFirst(ViaHeader.NAME);
                    // Send the retransmission statelessly
                    this.inviteServerTxProvider.sendResponse(newResponse);
                }
            } else {
                // this is the OK for the cancel.
                log.info("Got a non-invite response " + response);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            TestCase.fail("unexpected exception");
        }
    }

    public void processTimeout(TimeoutEvent timeoutEvent) {
        log.error("Timeout occurred");
        TestCase.fail("unexpected event");
    }

    public void processIOException(IOExceptionEvent exceptionEvent) {
        log.info("IOException occurred");
        TestCase.fail("unexpected exception io exception");
    }

    public SipProvider createSipProvider(String transport) {
        try {
            ListeningPoint listeningPoint = sipStack.createListeningPoint(host, port, transport.toUpperCase());

            SipProvider sipProvider = sipStack.createSipProvider(listeningPoint);
            this.sipProviders.put(transport, sipProvider);
            return sipProvider;
        } catch (Exception ex) {
            log.error(unexpectedException, ex);
            ex.printStackTrace();
            TestCase.fail(unexpectedException);
            return null;
        }

    }

    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
        log.info("Transaction terminated event occured -- cleaning up");
        if (!transactionTerminatedEvent.isServerTransaction()) {
            ClientTransaction ct = transactionTerminatedEvent.getClientTransaction();
            for (Iterator it = this.clientTxTable.values().iterator(); it.hasNext(); ) {
                if (it.next().equals(ct)) {
                    it.remove();
                }
            }
        } else {
            log.info("Server tx terminated! ");
        }
    }

    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
        TestCase.fail("unexpected event");
    }

    public Proxy(int myPort) {
        this.port = myPort;
        SipObjects sipObjects = new SipObjects(myPort, "proxy", "off");
        addressFactory = sipObjects.addressFactory;
        messageFactory = sipObjects.messageFactory;
        headerFactory = sipObjects.headerFactory;
        this.sipStack = sipObjects.sipStack;

    }

    public void stop() {
        this.sipStack.stop();
    }

}
