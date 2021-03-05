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


import gov.nist.core.CommonLogger;
import gov.nist.core.HostPort;
import gov.nist.core.LogWriter;
import gov.nist.core.StackLogger;

import javax.net.ssl.SSLContext;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.SocketChannel;
import java.security.cert.CertificateException;

public class NioTlsWebSocketMessageProcessor extends NioWebSocketMessageProcessor {

    private static StackLogger logger = CommonLogger.getLogger(NioTlsWebSocketMessageProcessor.class);

    SSLContext sslServerCtx;
    SSLContext sslClientCtx;
    
    private static int MAX_WAIT_ATTEMPTS = 100;

	public NioTlsWebSocketMessageProcessor(InetAddress ipAddress,
			SIPTransactionStack sipStack, int port) {
		super(ipAddress, sipStack, port);
		transport = "WSS"; // by default it's WSS, can be overriden by TLS accelerator
		try {
			init();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public NioTcpMessageChannel createMessageChannel(NioTcpMessageProcessor nioTcpMessageProcessor, SocketChannel client) throws IOException {
		if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
    		log.debug("NioTlsWebSocketMessageProcessor::createMessageChannel: " + nioTcpMessageProcessor + " client " + client);
    	}
		return NioTlsWebSocketMessageChannel.create(NioTlsWebSocketMessageProcessor.this, client);		
    }
	
    @Override
    public synchronized MessageChannel createMessageChannel(HostPort targetHostPort) throws IOException {
    	if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
    		log.debug("NioTlsWebSocketMessageProcessor::createMessageChannel: " + targetHostPort);
    	}
    	NioTlsWebSocketMessageChannel retval = null;
    	try {
    		String key = MessageChannel.getKey(targetHostPort, transport);
			
    		if (messageChannels.get(key) != null) {
    			retval = (NioTlsWebSocketMessageChannel) this.messageChannels.get(key);
    			int wait;
    			for(wait=0; wait<=MAX_WAIT_ATTEMPTS; wait++) {
    				if(retval.readingHttp == true) {
    					try {
    						if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
    				    		log.debug("NioTlsWebSocketMessageProcessor::createMessageChannel: waiting for TLS/HTTP handshake");
    				    	}
							Thread.sleep(100);
						} catch (InterruptedException e) {}
    				} else {
    					if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
				    		log.debug("NioTlsWebSocketMessageProcessor::createMessageChannel: after handshake wait=" + wait);
				    	}
    					break;
    				}
    			}
    			if(wait == MAX_WAIT_ATTEMPTS) throw new IOException("Timed out waiting for TLS handshake");
    			return retval;
    		} else {
    			retval = new NioTlsWebSocketMessageChannel(targetHostPort.getInetAddress(),
    					targetHostPort.getPort(), sipStack, this);
    			
    		//	retval.getSocketChannel().register(selector, SelectionKey.OP_READ);
    			synchronized(messageChannels) {
    				this.messageChannels.put(key, retval);
    			}
    			retval.isCached = true;
    			if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
    				log.debug("key " + key);
    				log.debug("Creating " + retval);
    			}
    			selector.wakeup();
    			return retval;

    		}
    	} finally {
    		if(logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
    			log.debug("MessageChannel::createMessageChannel - exit " + retval);
    		}
    	}
    }

    @Override
    public synchronized MessageChannel createMessageChannel(InetAddress targetHost, int port) throws IOException {
        String key = MessageChannel.getKey(targetHost, port, transport);
        if (messageChannels.get(key) != null) {
            return this.messageChannels.get(key);
        } else {
        	NioTlsWebSocketMessageChannel retval = new NioTlsWebSocketMessageChannel(targetHost, port, sipStack, this);
            
            selector.wakeup();
 //           retval.getSocketChannel().register(selector, SelectionKey.OP_READ);
            this.messageChannels.put(key, retval);
            retval.isCached = true;
            if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
                log.debug("key " + key);
                log.debug("Creating " + retval);
            }
            return retval;
        }

    }
    
	public void init() throws Exception, CertificateException, FileNotFoundException, IOException {
		if(sipStack.securityManagerProvider.getKeyManagers(false) == null ||
				sipStack.securityManagerProvider.getTrustManagers(false) == null ||
                sipStack.securityManagerProvider.getTrustManagers(true) == null) {
			if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
                log.debug("TLS initialization failed due to NULL security config");
            }
			return; // The settings 
		}
			
        sslServerCtx = SSLContext.getInstance("TLS");
        sslClientCtx = SSLContext.getInstance("TLS");
        
        if(sipStack.getClientAuth() == ClientAuthType.DisabledAll) {
        	if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
                log.debug(
                        "ClientAuth " + sipStack.getClientAuth()  +  " bypassing all cert validations");
            }
        	sslServerCtx.init(sipStack.securityManagerProvider.getKeyManagers(false), NioTlsMessageProcessor.trustAllCerts, null);
        	sslClientCtx.init(sipStack.securityManagerProvider.getKeyManagers(true), NioTlsMessageProcessor.trustAllCerts, null);
        } else {
        	if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
                log.debug(
                        "ClientAuth " + sipStack.getClientAuth());
            }
        	 sslServerCtx.init(sipStack.securityManagerProvider.getKeyManagers(false), 
                     sipStack.securityManagerProvider.getTrustManagers(false),
                     null);
        	 sslClientCtx.init(sipStack.securityManagerProvider.getKeyManagers(true),
                     sipStack.securityManagerProvider.getTrustManagers(true),
                     null);

        }
    }

}
