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
package co.ecg.jain_sip.core.ri.net;

import co.ecg.jain_sip.sip.ri.SipStackImpl;
import co.ecg.jain_sip.sip.ri.stack.ClientAuthType;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * extended implementation of a network layer that allows to define a private java
 * keystores/truststores
 *
 * @author f.reif
 * @version 1.2
 * @since 1.2
 *
 */
@Slf4j
public class SslNetworkLayer implements NetworkLayer {

    private SSLSocketFactory sslSocketFactory;

    private SSLServerSocketFactory sslServerSocketFactory;
    
 // Create a trust manager that does not validate certificate chains
    TrustManager[] trustAllCerts = new TrustManager[] { 
      new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
          return new X509Certificate[0]; 
        }
        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        	if (log.isDebugEnabled()) {
                log.debug(
                        "checkClientTrusted : Not validating certs " + certs + " authType " + authType);
            }
        }
        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        	if (log.isDebugEnabled()) {
                log.debug(
                        "checkServerTrusted : Not validating certs " + certs + " authType " + authType);
            }
        }
    }};

    public SslNetworkLayer(
    		SipStackImpl sipStack,
            String trustStoreFile,
            String keyStoreFile,
            char[] keyStorePassword,
            char[] trustStorePassword,
            String keyStoreType, String trustStoreType) throws GeneralSecurityException, FileNotFoundException, IOException
    {
        SSLContext sslContext;
        sslContext = SSLContext.getInstance("TLS");
        String algorithm = KeyManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(algorithm);
        KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(algorithm);
        SecureRandom secureRandom   = new SecureRandom();
        secureRandom.nextInt();
        KeyStore keyStore = KeyStore.getInstance(
             keyStoreType != null ? keyStoreType : KeyStore.getDefaultType());
        KeyStore trustStore = KeyStore.getInstance(
        		trustStoreType != null ? trustStoreType : KeyStore.getDefaultType());
        keyStore.load(new FileInputStream(keyStoreFile), keyStorePassword);
        trustStore.load(new FileInputStream(trustStoreFile), trustStorePassword);
        tmFactory.init(trustStore);
        kmFactory.init(keyStore, keyStorePassword);
        if(sipStack.getClientAuth() == ClientAuthType.DisabledAll) {
        	if (log.isDebugEnabled()) {
                log.debug(
                        "ClientAuth " + sipStack.getClientAuth()  +  " bypassing all cert validations");
            }
        	sslContext.init(null, trustAllCerts, secureRandom);
        } else {
        	if (log.isDebugEnabled()) {
                log.debug(
                        "ClientAuth " + sipStack.getClientAuth());
            }
        	sslContext.init(kmFactory.getKeyManagers(), tmFactory.getTrustManagers(), secureRandom);
        }
        sslServerSocketFactory = sslContext.getServerSocketFactory();        
        sslSocketFactory = sslContext.getSocketFactory();
    }

    public ServerSocket createServerSocket(int port, int backlog,
            InetAddress bindAddress) throws IOException {
        return new ServerSocket(port, backlog, bindAddress);
    }

    public Socket createSocket(InetAddress address, int port)
            throws IOException {
        return new Socket(address, port);
    }

    public DatagramSocket createDatagramSocket() throws SocketException {
        return new DatagramSocket();
    }

    public DatagramSocket createDatagramSocket(int port, InetAddress laddr)
            throws SocketException {
        return new DatagramSocket(port, laddr);
    }

    /* Added by Daniel J. Martinez Manzano <dani@dif.um.es> */
    public SSLServerSocket createSSLServerSocket(int port, int backlog,
            InetAddress bindAddress) throws IOException {
        return (SSLServerSocket) sslServerSocketFactory.createServerSocket(
                port, backlog, bindAddress);
    }

    /* Added by Daniel J. Martinez Manzano <dani@dif.um.es> */
    public SSLSocket createSSLSocket(InetAddress address, int port)
            throws IOException {
    	return createSSLSocket(address, port, null);
    }

    /* Added by Daniel J. Martinez Manzano <dani@dif.um.es> */
    public SSLSocket createSSLSocket(InetAddress address, int port,
            InetAddress myAddress) throws IOException {
    	SSLSocket sock = (SSLSocket) sslSocketFactory.createSocket();
    	if (myAddress != null) {
	    	// trying to bind to the correct ipaddress (in case of multiple vip addresses by example)
	    	// and let the JDK pick an ephemeral port
	    	sock.bind(new InetSocketAddress(myAddress, 0));
    	}
    	try {
    		sock.connect(new InetSocketAddress(address, port), 8000);
    	} catch (SocketTimeoutException e) {
    		throw new ConnectException("Socket timeout error (8sec)" + address + ":" + port);
    	}
    	return sock;
    }

    public Socket createSocket(InetAddress address, int port,
            InetAddress myAddress) throws IOException {
    	if (myAddress != null) {
        	Socket sock = new Socket();
        	// trying to bind to the correct ipaddress (in case of multiple vip addresses by example)
        	// and let the JDK pick an ephemeral port
        	sock.bind(new InetSocketAddress(myAddress, 0));
        	try {
	        	sock.connect(new InetSocketAddress(address, port), 8000);
	        } catch (SocketTimeoutException e) {
	        	throw new ConnectException("Socket timeout error (8sec)" + address + ":" + port);
	        }
        	return sock;
        }
        else {
        	Socket sock =  new Socket();
        	try {
        		sock.connect(new InetSocketAddress(address, port), 8000);
        	} catch (SocketTimeoutException e) {
        		throw new ConnectException("Socket timeout error (8sec)" + address + ":" + port);
        	}
        	return sock;
        }
    }

    /**
     * Creates a new Socket, binds it to myAddress:myPort and connects it to
     * address:port.
     *
     * @param address the InetAddress that we'd like to connect to.
     * @param port the port that we'd like to connect to
     * @param myAddress the address that we are supposed to bind on or null
     *        for the "any" address.
     * @param myPort the port that we are supposed to bind on or 0 for a random
     * one.
     *
     * @return a new Socket, bound on myAddress:myPort and connected to
     * address:port.
     * @throws IOException if binding or connecting the socket fail for a reason
     * (exception relayed from the correspoonding Socket methods)
     */
    public Socket createSocket(InetAddress address, int port,
                    InetAddress myAddress, int myPort)
        throws IOException
    {
    	if (myAddress != null) {
        	Socket sock = new Socket();
        	// trying to bind to the correct ipaddress (in case of multiple vip addresses by example)
        	// and let the JDK pick an ephemeral port    
        	sock.bind(new InetSocketAddress(myAddress, 0));
        	try {
	        	sock.connect(new InetSocketAddress(address, port), 8000);
	        } catch (SocketTimeoutException e) {
	        	throw new ConnectException("Socket timeout error (8sec)" + address + ":" + port);
	        }
        	return sock;
        }
        else {
        	Socket sock =  new Socket();
        	if(myPort != 0) {
        		sock.bind(new InetSocketAddress(port));
        	}
        	try {
        		sock.connect(new InetSocketAddress(address, port), 8000);
        	} catch (SocketTimeoutException e) {
        		throw new ConnectException("Socket timeout error (8sec)" + address + ":" + port);
        	}
        	return sock;
        }
//        if (myAddress != null)
//            return new Socket(address, port, myAddress, myPort);
//        else if (port != 0)
//        {
//            //myAddress is null (i.e. any)  but we have a port number
//            Socket sock = new Socket();
//            sock.bind(new InetSocketAddress(port));
//            sock.connect(new InetSocketAddress(address, port));
//            return sock;
//        }
//        else
//            return new Socket(address, port);
    }

	@Override
	public void setSipStack(SipStackImpl sipStackImpl) {}
}