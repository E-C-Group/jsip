package co.ecg.jain_sip.sip.stack.forkedinvitedialogtimeout;

import co.ecg.jain_sip.sip.*;
import co.ecg.jain_sip.sip.address.Address;
import co.ecg.jain_sip.sip.address.AddressFactory;
import co.ecg.jain_sip.sip.address.SipURI;
import co.ecg.jain_sip.sip.header.*;
import co.ecg.jain_sip.sip.message.MessageFactory;
import co.ecg.jain_sip.sip.message.Request;
import co.ecg.jain_sip.sip.message.Response;

import java.util.Hashtable;
import java.util.Iterator;

import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;


/**
 * A very simple forking proxy server.
 * 
 * @author M. Ranganathan
 * 
 */
@Slf4j
public class Proxy implements SipListener {

    // private ServerTransaction st;

    private SipProvider inviteServerTxProvider;

    private Hashtable clientTxTable = new Hashtable();

    private static String host = "127.0.0.1";

    private int port = 5070;

    private SipProvider sipProvider;

    private static String unexpectedException = "Unexpected exception";

    private static AddressFactory addressFactory;

    private static MessageFactory messageFactory;

    private static HeaderFactory headerFactory;

    private static String transport = "udp";

    private SipStack sipStack;

    private int ntargets;
    
    
    private void sendTo(ServerTransaction st, Request request, int targetPort) throws Exception {
        Request newRequest = (Request) SerializationUtils.clone(request);
        
        SipURI sipUri = addressFactory.createSipURI("UA1", "127.0.0.1");
        sipUri.setPort(targetPort);
        sipUri.setLrParam();
        Address address = addressFactory.createAddress("client1", sipUri);
        RouteHeader rheader = headerFactory.createRouteHeader(address);

        newRequest.addFirst(rheader);
        ViaHeader viaHeader = headerFactory.createViaHeader(host, this.port, transport, null);
        newRequest.addFirst(viaHeader);
        ClientTransaction ct1 = sipProvider.getNewClientTransaction(newRequest);
        sipUri = addressFactory.createSipURI("proxy", "127.0.0.1");
        address = addressFactory.createAddress("proxy", sipUri);
        sipUri.setPort(5070);
        sipUri.setLrParam();
        RecordRouteHeader recordRoute = headerFactory.createRecordRouteHeader(address);
        newRequest.addHeader(recordRoute);
        ct1.setApplicationData(st);
        this.clientTxTable.put(new Integer(targetPort), ct1);
        ct1.sendRequest();
    }

    public void processRequest(RequestEvent requestEvent) {
        try {
            Request request = requestEvent.getRequest();
            SipProvider sipProvider = (SipProvider) requestEvent.getSource();
            this.inviteServerTxProvider = sipProvider;
            if (request.getMethod().equals(Request.INVITE)) {

                ListeningPoint lp = sipProvider.getListeningPoint(transport);
                String host = lp.getIPAddress();
                int port = lp.getPort();

                ServerTransaction st = null;
                if (requestEvent.getServerTransaction() == null) {
                    st = sipProvider.getNewServerTransaction(request);

                }
                
                for ( int i = 0; i < ntargets; i++ ) {
                    this.sendTo(st,request,5080 + i);
                }

             
               

            } else {
                // Remove the topmost route header
                // The route header will make sure it gets to the right place.
                log.info("proxy: Got a request " + request.getMethod());
                Request newRequest = (Request) SerializationUtils.clone(request);
                newRequest.removeFirst(RouteHeader.NAME);
                sipProvider.sendRequest(newRequest);
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
        log.error("Timeout occured");
        TestCase.fail("unexpected event");
    }

    public void processIOException(IOExceptionEvent exceptionEvent) {
        log.info("IOException occured");
        TestCase.fail("unexpected exception io exception");
    }

    public SipProvider createSipProvider() {
        try {
            ListeningPoint listeningPoint = sipStack.createListeningPoint(host, port, transport);

            sipProvider = sipStack.createSipProvider(listeningPoint);
            return sipProvider;
        } catch (Exception ex) {
            log.error(unexpectedException, ex);
            TestCase.fail(unexpectedException);
            return null;
        }

    }

    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
        log.info("Transaction terminated event occured -- cleaning up");
        if (!transactionTerminatedEvent.isServerTransaction()) {
            ClientTransaction ct = transactionTerminatedEvent.getClientTransaction();
            for (Iterator it = this.clientTxTable.values().iterator(); it.hasNext();) {
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

    public Proxy(int myPort, int ntargets) {
        this.port = myPort;
        this.ntargets = ntargets;
        SipObjects sipObjects = new SipObjects(myPort, "proxy","off");
        addressFactory = sipObjects.addressFactory;
        messageFactory = sipObjects.messageFactory;
        headerFactory = sipObjects.headerFactory;
        this.sipStack = sipObjects.sipStack;
  
    }

    public void stop() {
       this.sipStack.stop();
    }

}
