package test.unit.gov.nist.javax.sip.stack;



import co.ecg.jain_sip.sip.ri.ListeningPointImpl;
import co.ecg.jain_sip.sip.ri.stack.NioMessageProcessorFactory;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;

import co.ecg.jain_sip.sip.ClientTransaction;
import co.ecg.jain_sip.sip.DialogTerminatedEvent;
import co.ecg.jain_sip.sip.IOExceptionEvent;
import co.ecg.jain_sip.sip.ListeningPoint;
import co.ecg.jain_sip.sip.RequestEvent;
import co.ecg.jain_sip.sip.ResponseEvent;
import co.ecg.jain_sip.sip.SipFactory;
import co.ecg.jain_sip.sip.SipListener;
import co.ecg.jain_sip.sip.SipProvider;
import co.ecg.jain_sip.sip.SipStack;
import co.ecg.jain_sip.sip.TimeoutEvent;
import co.ecg.jain_sip.sip.TransactionTerminatedEvent;
import co.ecg.jain_sip.sip.address.Address;
import co.ecg.jain_sip.sip.address.AddressFactory;
import co.ecg.jain_sip.sip.address.URI;
import co.ecg.jain_sip.sip.header.CSeqHeader;
import co.ecg.jain_sip.sip.header.CallIdHeader;
import co.ecg.jain_sip.sip.header.ContactHeader;
import co.ecg.jain_sip.sip.header.FromHeader;
import co.ecg.jain_sip.sip.header.HeaderFactory;
import co.ecg.jain_sip.sip.header.MaxForwardsHeader;
import co.ecg.jain_sip.sip.header.ToHeader;
import co.ecg.jain_sip.sip.message.MessageFactory;
import co.ecg.jain_sip.sip.message.Request;

import test.tck.msgflow.callflows.ScenarioHarness;

import junit.framework.Assert;

public class MultipleContactsTest extends ScenarioHarness {

	
    public MultipleContactsTest() {
    	super("MultipleContactsTest",true);
	}


	public final int SERVER_PORT = 5600;

    public final int CLIENT_PORT = 6500;
    
    protected String testProtocol = "udp";

	public HeaderFactory headerFactory;

	public MessageFactory messageFactory;

	public AddressFactory addressFactory;

	public String host;
    	
	public void setUp() throws Exception {
		
	}
	
	public void testMultipleContacts() throws Exception {
		Server server = new Server();
		Client client = new Client();
		
		client.sendMultipleContactRegistration();
		Thread.sleep(1000);
		
		Request serverLastRequestReceived = server.getLastRequestReceived();
		assertNotNull(serverLastRequestReceived);
		
		ListIterator<ContactHeader> iterator = serverLastRequestReceived.getHeaders(ContactHeader.NAME);
		assertTrue(iterator.hasNext());
		String first = iterator.next().getAddress().getURI().toString();
		String second = iterator.next().getAddress().getURI().toString();		
		
		assertEquals("sip:here@somewhereelse:5080", second);
		assertEquals("sip:here@somewhere:5070", first);		
	}
	
	
	public class Server extends SipAdapter {
        protected SipStack sipStack;

        protected SipFactory sipFactory = null;

        protected SipProvider provider = null;

        private Request lastRequestReceived;
        
        public Server() {
            try {
                final Properties defaultProperties = new Properties();
                host = "127.0.0.1";

                defaultProperties.setProperty("javax.sip.STACK_NAME", "server");
                defaultProperties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "DEBUG");
                defaultProperties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "server_debug_NoAutoDialogTest.txt");
                defaultProperties.setProperty("gov.nist.javax.sip.SERVER_LOG", "server_log_NoAutoDialogTest.txt");
                defaultProperties.setProperty("gov.nist.javax.sip.READ_TIMEOUT", "1000");
                defaultProperties.setProperty("gov.nist.javax.sip.CACHE_SERVER_CONNECTIONS",
                        "false");
                if(System.getProperty("enableNIO") != null && System.getProperty("enableNIO").equalsIgnoreCase("true")) {
                	defaultProperties.setProperty("gov.nist.javax.sip.MESSAGE_PROCESSOR_FACTORY", NioMessageProcessorFactory.class.getName());
                }
                this.sipFactory = SipFactory.getInstance();
                this.sipFactory.setPathName("gov.nist");
                this.sipStack = this.sipFactory.createSipStack(defaultProperties);
                this.sipStack.start();
                ListeningPoint lp = this.sipStack.createListeningPoint(host, SERVER_PORT, testProtocol);
                this.provider = this.sipStack.createSipProvider(lp);;
                this.provider.addSipListener(this);
            } catch (Exception e) {
                e.printStackTrace();
                Assert.fail("unexpected exception ");
            }

        }

        public void stop() {
            this.sipStack.stop();
        }

        public void processRequest(RequestEvent requestEvent) {
           lastRequestReceived = requestEvent.getRequest();
        }

		public Request getLastRequestReceived() {
			return lastRequestReceived;
		}                
    }

    public class Client extends SipAdapter {

        private SipFactory sipFactory;
        private SipStack sipStack;
        private SipProvider provider;
        private boolean o_sentInvite, o_received180, o_sentCancel, o_receiver200Cancel,
                o_inviteTxTerm, o_dialogTerinated;

        public Client() {
            try {
                final Properties defaultProperties = new Properties();
                String host = "127.0.0.1";
                defaultProperties.setProperty("javax.sip.STACK_NAME", "client");
                defaultProperties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "DEBUG");
                defaultProperties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "client_debug.txt");
                defaultProperties.setProperty("gov.nist.javax.sip.SERVER_LOG", "client_log.txt");
                defaultProperties.setProperty("gov.nist.javax.sip.READ_TIMEOUT", "1000");
                defaultProperties.setProperty("gov.nist.javax.sip.CACHE_SERVER_CONNECTIONS","false");
                if(System.getProperty("enableNIO") != null && System.getProperty("enableNIO").equalsIgnoreCase("true")) {
                	defaultProperties.setProperty("gov.nist.javax.sip.MESSAGE_PROCESSOR_FACTORY", NioMessageProcessorFactory.class.getName());
                }
                this.sipFactory = SipFactory.getInstance();
                this.sipFactory.setPathName("gov.nist");
                this.sipStack = this.sipFactory.createSipStack(defaultProperties);
                this.sipStack.start();
                ListeningPoint lp = this.sipStack.createListeningPoint(host, CLIENT_PORT, testProtocol);
                this.provider = this.sipStack.createSipProvider(lp);
                headerFactory = this.sipFactory.createHeaderFactory();
                messageFactory = this.sipFactory.createMessageFactory();
                addressFactory = this.sipFactory.createAddressFactory();
                this.provider.addSipListener(this);
            } catch (Exception e) {
                e.printStackTrace();
                Assert.fail("unexpected exception ");
            }
        }
        
        public void sendMultipleContactRegistration() throws Exception {
        	Address fromAddress = addressFactory.createAddress("here@somewhere:5070");
        	ContactHeader contactHeader1 = headerFactory.createContactHeader(addressFactory.createAddress("sip:here@somewhere:5070"));
    		ContactHeader contactHeader2 = headerFactory.createContactHeader(addressFactory.createAddress("sip:here@somewhereelse:5080"));
    		
    		
			CallIdHeader callId = provider.getNewCallId();			
			CSeqHeader cSeq = headerFactory.createCSeqHeader(1l, Request.REGISTER);
			FromHeader from = headerFactory.createFromHeader(fromAddress, "1234");
			ToHeader to = headerFactory.createToHeader(addressFactory.createAddress("server@"+host+":"+SERVER_PORT), null);
			List via = Arrays.asList(((ListeningPointImpl)provider.getListeningPoint(testProtocol)).getViaHeader());			
			MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(10);
    		
    		URI requestURI = addressFactory.createURI("sip:test@"+host+":"+SERVER_PORT);
    		Request request = messageFactory.createRequest(requestURI, Request.REGISTER, callId, cSeq, from, to, via, maxForwards);
    		    		
    		request.setRequestURI(requestURI);
    		request.addHeader(contactHeader1);
    		request.addHeader(contactHeader2);
    		ClientTransaction ctx = this.provider.getNewClientTransaction(request);
    		ctx.sendRequest();
        }
    }


	private static class SipAdapter implements SipListener {

		public void processDialogTerminated(DialogTerminatedEvent arg0) {}

		public void processIOException(IOExceptionEvent arg0) {}

		public void processRequest(RequestEvent arg0) {}

		public void processResponse(ResponseEvent arg0) {}

		public void processTimeout(TimeoutEvent arg0) {}
		
		public void processTransactionTerminated(TransactionTerminatedEvent arg0) {}
	}
}
