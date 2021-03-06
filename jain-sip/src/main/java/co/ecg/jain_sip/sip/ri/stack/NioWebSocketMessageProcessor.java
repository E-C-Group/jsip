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


import co.ecg.jain_sip.core.ri.HostPort;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.SocketChannel;

@Slf4j
public class NioWebSocketMessageProcessor extends NioTcpMessageProcessor {

    public NioWebSocketMessageProcessor(InetAddress ipAddress,
                                        SIPTransactionStack sipStack, int port) {
        super(ipAddress, sipStack, port);
        transport = "WS"; // by default its WS, can be overriden if there is TLS acclereator
    }

    @Override
    public NioTcpMessageChannel createMessageChannel(NioTcpMessageProcessor nioTcpMessageProcessor, SocketChannel client) throws IOException {
        return NioWebSocketMessageChannel.create(this, client);
    }

    @Override
    public MessageChannel createMessageChannel(HostPort targetHostPort) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug(":createMessageChannel: " + targetHostPort);
        }
        try {
            String key = MessageChannel.getKey(targetHostPort, transport);
            if (messageChannels.get(key) != null) {
                return this.messageChannels.get(key);
            } else {
                NioWebSocketMessageChannel retval = new NioWebSocketMessageChannel(targetHostPort.getInetAddress(),
                        targetHostPort.getPort(), sipStack, this);


                //	retval.getSocketChannel().register(selector, SelectionKey.OP_READ);
                synchronized (messageChannels) {
                    this.messageChannels.put(key, retval);
                }
                retval.isCached = true;
                if (log.isDebugEnabled()) {
                    log.debug("key " + key);
                    log.debug("Creating " + retval);
                }
                selector.wakeup();
                return retval;

            }
        } finally {
            if (log.isDebugEnabled()) {
                log.debug("MessageChannel::createMessageChannel - exit");
            }
        }
    }

    @Override
    public MessageChannel createMessageChannel(InetAddress targetHost, int port) throws IOException {
        String key = MessageChannel.getKey(targetHost, port, transport);
        if (messageChannels.get(key) != null) {
            return this.messageChannels.get(key);
        } else {
            NioWebSocketMessageChannel retval = new NioWebSocketMessageChannel(targetHost, port, sipStack, this);

            selector.wakeup();
            //           retval.getSocketChannel().register(selector, SelectionKey.OP_READ);
            this.messageChannels.put(key, retval);
            retval.isCached = true;
            if (log.isDebugEnabled()) {
                log.debug("key " + key);
                log.debug("Creating " + retval);
            }
            return retval;
        }

    }

}
