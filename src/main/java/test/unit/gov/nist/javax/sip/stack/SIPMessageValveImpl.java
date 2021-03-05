package test.unit.gov.nist.javax.sip.stack;
import co.ecg.jain_sip.sip.ri.SipStackImpl;
import co.ecg.jain_sip.sip.ri.message.SIPMessage;
import co.ecg.jain_sip.sip.ri.message.SIPRequest;
import co.ecg.jain_sip.sip.ri.stack.MessageChannel;
import co.ecg.jain_sip.sip.ri.stack.SIPMessageValve;

import java.io.IOException;

import co.ecg.jain_sip.sip.SipStack;
import co.ecg.jain_sip.sip.message.Response;

    public class SIPMessageValveImpl implements SIPMessageValve {
    	public static int lastResponseCode;
    	public static boolean inited;
    	public static boolean destroyed;

    	public boolean processRequest(SIPRequest request, MessageChannel messageChannel) {
    		try {
    			sendResponse(messageChannel, createErrorResponse(request, 603));
    			return false;
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    		return false;
    	}
    	
    	/**
    	 * Demonstrating how stateless response is created and sent
    	 * @param request
    	 * @param code
    	 * @return
    	 */
    	public SIPMessage createErrorResponse(SIPRequest request, int code) {
    		return request.createResponse(code);
    	}
    	
    	public void sendResponse(MessageChannel channel, SIPMessage response) throws IOException {
    		channel.sendMessage(response);
    	}

		public boolean processResponse(Response response,
				MessageChannel messageChannel) {
			lastResponseCode = response.getStatusCode();
			return true;
		}

		public void destroy() {
			destroyed = true;
		}

		public void init(SipStack stack) {
			SipStackImpl impl = (SipStackImpl) stack;
			impl.getConfigurationProperties().getProperty("keee");
			impl.getActiveClientTransactionCount();
			inited = true;
		}
    	
    	
    }
