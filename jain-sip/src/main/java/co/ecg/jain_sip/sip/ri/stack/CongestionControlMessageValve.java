/*
 * Conditions Of Use
 *
 * This software was developed by employees of the National Institute of
 * Standards and Technology (NIST), an agency of the Federal Government.
 * Pursuant to title 15 Untied States Code Section 105, works of NIST
 * employees are not subject to copyright protection in the United States
 * and are considered to be in the public domain.  As a result, a formal
 * license is not needed to use the software.
 *
 * This software is provided by NIST as a service and is expressly
 * provided "AS IS."  NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
 * OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
 * AND DATA ACCURACY.  NIST does not warrant or make any representations
 * regarding the use of the software or the results thereof, including but
 * not limited to the correctness, accuracy, reliability or usefulness of
 * the software.
 *
 * Permission to use this software is contingent upon your acceptance
 * of the terms of this agreement
 *
 * .
 *
 */
package co.ecg.jain_sip.sip.ri.stack;

import java.io.IOException;

import co.ecg.jain_sip.sip.ri.SipStackImpl;
import co.ecg.jain_sip.sip.ri.message.SIPRequest;
import co.ecg.jain_sip.sip.ri.message.SIPResponse;

import co.ecg.jain_sip.sip.SipStack;
import co.ecg.jain_sip.sip.message.Request;
import co.ecg.jain_sip.sip.message.Response;
import lombok.extern.slf4j.Slf4j;

/**
 * This is just a simple reusable congestion control valve JSIP apps can use to stop traffic when the number of
 * server transactions reaches the limit specified in gov.nist.javax.sip.MAX_SERVER_TRANSACTIONS without breaking
 * existing dialogs.
 * 
 * The drop policy is specified in DROP_RESPONSE_STATUS where "0" means silent drop and any positive number will be
 * interpreted as the status code of the error response that will be generated.
 * 
 * To enable this in your application you must specify this property:
 * gov.nist.javax.sip.SIP_MESSAGE_VALVE=gov.nist.javax.sip.stack.CongestionControlMessageValve
 * 
 * It is advised to extend this class to add your application-specific control conditions
 * 
 * @author vladimirralev
 *
 */
@Slf4j
public class CongestionControlMessageValve implements SIPMessageValve{

	protected SipStackImpl sipStack;
    // High water mark for ServerTransaction Table
    // after which requests are dropped.
    protected int serverTransactionTableHighwaterMark;
    protected int dropResponseStatus;
    
	public boolean processRequest(SIPRequest request,
			MessageChannel messageChannel) {
		String requestMethod = request.getMethod();
		
		// We should not attempt to drop these requests because they actually free resources
		// which is our goal in congested mode
		boolean undropableMethod = requestMethod.equals(Request.BYE) 
		|| requestMethod.equals(Request.ACK) 
		|| requestMethod.equals(Request.PRACK) 
		|| requestMethod.equals(Request.CANCEL);
		
		if(!undropableMethod) {
			if(serverTransactionTableHighwaterMark <= sipStack.getServerTransactionTableSize()) {
				// Allow directly any subsequent requests
				if(request.getToTag() != null) {
					return true;
				}
				if(dropResponseStatus>0) {
					SIPResponse response = request.createResponse(dropResponseStatus);
					try {
						messageChannel.sendMessage(response);
					} catch (IOException e) {
						log.error("Failed to send congestion control error response" + response, e);
					}
				}
				return false; // Do not pass this request to the pipeline
			}
		}
		return true; // OK, the processing of the request can continue
	}

	public boolean processResponse(Response response,
			MessageChannel messageChannel) {
		return true;
	}

	public void destroy() {
		log.info("Destorying the congestion control valve " + this);
		
	}

	public void init(SipStack stack) {
		sipStack = (SipStackImpl) stack;
		log.info("Initializing congestion control valve");
		String serverTransactionsString = sipStack.getConfigurationProperties().getProperty("gov.nist.javax.sip.MAX_SERVER_TRANSACTIONS", "10000");
		serverTransactionTableHighwaterMark = new Integer(serverTransactionsString);
		String dropResponseStatusString = sipStack.getConfigurationProperties().getProperty("DROP_RESPONSE_STATUS", "503");
		dropResponseStatus = new Integer(dropResponseStatusString);
	}

}
