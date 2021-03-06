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

import lombok.extern.slf4j.Slf4j;

import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * @author jean.deruelle@gmail.com
 */
@Slf4j
public class SocketTimeoutAuditor extends SIPStackTimerTask {

    long nioSocketMaxIdleTime;

    public SocketTimeoutAuditor(long nioSocketMaxIdleTime) {
        this.nioSocketMaxIdleTime = nioSocketMaxIdleTime;
    }

    public void runTask() {
        try {
            // Reworked the method for https://java.net/jira/browse/JSIP-471

            log.debug("keys to check for inactivity removal " + NioTcpMessageChannel.channelMap.keySet());

            Iterator<Entry<SocketChannel, NioTcpMessageChannel>> entriesIterator = NioTcpMessageChannel.channelMap.entrySet().iterator();
            while (entriesIterator.hasNext()) {
                Entry<SocketChannel, NioTcpMessageChannel> entry = entriesIterator.next();
                SocketChannel socketChannel = entry.getKey();
                NioTcpMessageChannel messageChannel = entry.getValue();
                if (System.currentTimeMillis() - messageChannel.getLastActivityTimestamp() > nioSocketMaxIdleTime) {

                    log.debug("Will remove socket " + messageChannel.key + " lastActivity="
                            + messageChannel.getLastActivityTimestamp() + " current= " +
                            System.currentTimeMillis() + " socketChannel = "
                            + socketChannel);

                    messageChannel.close();
                    entriesIterator = NioTcpMessageChannel.channelMap.entrySet().iterator();
                } else {

                    log.debug("don't remove socket " + messageChannel.key + " as lastActivity="
                            + messageChannel.getLastActivityTimestamp() + " and current= " +
                            System.currentTimeMillis() + " socketChannel = "
                            + socketChannel);

                }
            }
        } catch (Exception anything) {

        }
    }
}
