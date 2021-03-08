package co.ecg.jain_sip.tck.msgflow.callflows.forkedinvite;

import java.util.Hashtable;
import java.util.Iterator;

import co.ecg.jain_sip.sip.ClientTransaction;
import co.ecg.jain_sip.sip.DialogTerminatedEvent;
import co.ecg.jain_sip.sip.IOExceptionEvent;
import co.ecg.jain_sip.sip.ListeningPoint;
import co.ecg.jain_sip.sip.RequestEvent;
import co.ecg.jain_sip.sip.ResponseEvent;
import co.ecg.jain_sip.sip.ServerTransaction;
import co.ecg.jain_sip.sip.SipListener;
import co.ecg.jain_sip.sip.SipProvider;
import co.ecg.jain_sip.sip.TimeoutEvent;
import co.ecg.jain_sip.sip.TransactionTerminatedEvent;
import co.ecg.jain_sip.sip.address.Address;
import co.ecg.jain_sip.sip.address.SipURI;
import co.ecg.jain_sip.sip.header.CSeqHeader;
import co.ecg.jain_sip.sip.header.RecordRouteHeader;
import co.ecg.jain_sip.sip.header.RouteHeader;
import co.ecg.jain_sip.sip.header.ViaHeader;
import co.ecg.jain_sip.sip.message.Request;
import co.ecg.jain_sip.sip.message.Response;

import co.ecg.jain_sip.tck.TestHarness;
import co.ecg.jain_sip.tck.msgflow.callflows.ProtocolObjects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;

/**
 * A very simple forking proxy server.
 *
 * @author M. Ranganathan
 *
 */
@Slf4j
public class Proxy extends TestHarness implements SipListener {

    // private ServerTransaction st;

    private SipProvider inviteServerTxProvider;

    private Hashtable clientTxTable = new Hashtable();

    private static String host = "127.0.0.1";

    private int port = 5070;

    private SipProvider sipProvider;

    private static String unexpectedException = "Unexpected exception";

    private ProtocolObjects protocolObjects;

    public synchronized void processRequest(RequestEvent requestEvent) {
        try {
            Request request = requestEvent.getRequest();
            SipProvider sipProvider = (SipProvider) requestEvent.getSource();
            this.inviteServerTxProvider = sipProvider;
            if (request.getMethod().equals(Request.INVITE)) {

                ListeningPoint lp = sipProvider
                        .getListeningPoint(protocolObjects.transport);
                String host = lp.getIPAddress();
                int port = lp.getPort();

                ServerTransaction st = null;
                if (requestEvent.getServerTransaction() == null) {
                    st = sipProvider.getNewServerTransaction(request);

                    Request newRequest = (Request) SerializationUtils.clone(request);
                    ((SipURI)newRequest.getRequestURI()).removePort();
                    SipURI sipUri = protocolObjects.addressFactory.createSipURI("UA1",
                            "127.0.0.1");
                    sipUri.setPort(5080);
                    sipUri.setLrParam();
                    Address address = protocolObjects.addressFactory.createAddress("client1",
                            sipUri);
                    RouteHeader rheader = protocolObjects.headerFactory
                            .createRouteHeader(address);

                    newRequest.setHeader(rheader);
                    ViaHeader viaHeader = protocolObjects.headerFactory.createViaHeader(host,
                            port, protocolObjects.transport, null);
                    newRequest.addFirst(viaHeader);
                    ClientTransaction ct1 = sipProvider.getNewClientTransaction(newRequest);
                    sipUri = protocolObjects.addressFactory.createSipURI("proxy", "127.0.0.1");
                    address = protocolObjects.addressFactory.createAddress("proxy", sipUri);
                    sipUri.setPort(5070);
                    sipUri.setLrParam();
                    RecordRouteHeader recordRoute = protocolObjects.headerFactory
                            .createRecordRouteHeader(address);
                    newRequest.addHeader(recordRoute);
                    ct1.setApplicationData(st);
                    this.clientTxTable.put(new Integer(5080), ct1);

                    newRequest = SerializationUtils.clone(request);
                    ((SipURI)newRequest.getRequestURI()).removePort();
                    sipUri = protocolObjects.addressFactory.createSipURI("UA2", "127.0.0.1");
                    sipUri.setLrParam();
                    sipUri.setPort(5090);
                    address = protocolObjects.addressFactory.createAddress("client2", sipUri);
                    rheader = protocolObjects.headerFactory.createRouteHeader(address);
                    newRequest.setHeader(rheader);
                    viaHeader = protocolObjects.headerFactory.createViaHeader(host, port,
                            protocolObjects.transport, null);
                    newRequest.addFirst(viaHeader);
                    sipUri = protocolObjects.addressFactory.createSipURI("proxy", "127.0.0.1");
                    sipUri.setPort(5070);
                    sipUri.setLrParam();
                    sipUri.setTransportParam(protocolObjects.transport);
                    address = protocolObjects.addressFactory.createAddress("proxy", sipUri);

                    recordRoute = protocolObjects.headerFactory.createRecordRouteHeader(address);

                    newRequest.addHeader(recordRoute);
                    ClientTransaction ct2 = sipProvider.getNewClientTransaction(newRequest);
                    ct2.setApplicationData(st);
                    this.clientTxTable.put(new Integer(5090), ct2);

                    // Send the requests out to the two listening points of the
                    // client.

                    ct2.sendRequest();
                    ct1.sendRequest();
                }

            } else {
                // Remove the topmost route header
                // The route header will make sure it gets to the right place.
                log.info("proxy: Got a request " + request);
                Request newRequest = SerializationUtils.clone(request);
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
            log.info("ClientTxID = "
                    + responseEvent.getClientTransaction()
                    + " client tx id "
                    + ((ViaHeader) response.getHeader(ViaHeader.NAME))
                            .getBranch() + " CSeq header = "
                    + response.getHeader(CSeqHeader.NAME) + " status code = "
                    + response.getStatusCode());

            // JvB: stateful proxy MUST NOT forward 100 Trying
            if (response.getStatusCode() == 100)
                return;

            if (cseq.getMethod().equals(Request.INVITE)) {
                ClientTransaction ct = responseEvent.getClientTransaction();
                if (ct != null) {
                    ServerTransaction st = (ServerTransaction) ct
                            .getApplicationData();

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

                    Response newResponse = (Response) SerializationUtils.clone(response);
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
            fail("unexpected exception");
        }
    }

    public void processTimeout(TimeoutEvent timeoutEvent) {
        log.error("Timeout occured");
        fail("unexpected event");
    }

    public void processIOException(IOExceptionEvent exceptionEvent) {
        log.info("IOException occured");
        fail("unexpected exception io exception");
    }

    public SipProvider createSipProvider() {
        try {
            ListeningPoint listeningPoint = protocolObjects.sipStack
                    .createListeningPoint(host, port, protocolObjects.transport);

            sipProvider = protocolObjects.sipStack
                    .createSipProvider(listeningPoint);
        //  sipProvider.setAutomaticDialogSupportEnabled(false);
            return sipProvider;
        } catch (Exception ex) {
            log.error(unexpectedException, ex);
            fail(unexpectedException);
            return null;
        }

    }

    public void processTransactionTerminated(
            TransactionTerminatedEvent transactionTerminatedEvent) {
        log.info("Transaction terminated event occured -- cleaning up");
        if (!transactionTerminatedEvent.isServerTransaction()) {
            ClientTransaction ct = transactionTerminatedEvent
                    .getClientTransaction();
            for (Iterator it = this.clientTxTable.values().iterator(); it
                    .hasNext();) {
                if (it.next().equals(ct)) {
                    it.remove();
                }
            }
        } else {
            log.info("Server tx terminated! ");
        }
    }

    public void processDialogTerminated(
            DialogTerminatedEvent dialogTerminatedEvent) {
        fail("unexpected event");
    }

    public Proxy(int myPort, ProtocolObjects protocolObjects) {
        this.port = myPort;
        this.protocolObjects = protocolObjects;
    }

}
