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
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

/**
 * NIO implementation for TCP.
 *
 * @author mranga
 */
@Slf4j
public class NioTcpMessageProcessor extends ConnectionOrientedMessageProcessor {

    protected Selector selector;

    protected Thread selectorThread;
    protected NIOHandler nioHandler;

    protected ServerSocketChannel channel;

    // Cache the change request here, the selector thread will read it when it wakes up and execute the request
    private final List<ChangeRequest> changeRequests = new LinkedList<ChangeRequest>();

    // Data send over a socket is cached here before hand, the selector thread will take it later for physical send
    private final Map<SocketChannel, List<ByteBuffer>> pendingData =
            new WeakHashMap<SocketChannel, List<ByteBuffer>>();


    public static class ChangeRequest {
        public static final int REGISTER = 1;
        public static final int CHANGEOPS = 2;

        public SocketChannel socket;
        public int type;
        public int ops;

        public ChangeRequest(SocketChannel socket, int type, int ops) {
            this.socket = socket;
            this.type = type;
            this.ops = ops;
        }

        public String toString() {
            return socket + " type = " + type + " ops = " + ops;
        }
    }

//  Commented out as part of https://java.net/jira/browse/JSIP-504
//	public void assignChannelToDestination(HostPort targetHostPort, NioTcpMessageChannel channel) {
//		String key = MessageChannel.getKey(targetHostPort, transport);
//		this.messageChannels.put(key, channel);
//	}

    private SocketChannel initiateConnection(InetSocketAddress address, int timeout) throws IOException {

        // We use blocking outbound connect just because it's pure pain to deal with http://stackoverflow.com/questions/204186/java-nio-select-returns-without-selected-keys-why
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(true);


        log.debug("Init connect " + address);
        socketChannel.socket().connect(address, timeout);
        socketChannel.configureBlocking(false);

        log.debug("Blocking set to false now " + address);

        synchronized (this.changeRequests) {
            changeRequests.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER, SelectionKey.OP_READ));
        }
        selector.wakeup();
        return socketChannel;
    }

    public SocketChannel blockingConnect(InetSocketAddress address, int timeout) throws IOException {
        return initiateConnection(address, timeout);
    }

    public void send(SocketChannel socket, byte[] data) {

        log.debug("Sending data " + data.length + " bytes on socket " + socket);

        synchronized (this.changeRequests) {
            this.changeRequests.add(new ChangeRequest(socket, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));

            synchronized (this.pendingData) {
                List<ByteBuffer> queue = this.pendingData.get(socket);
                if (queue == null) {
                    queue = new ArrayList<ByteBuffer>();
                    this.pendingData.put(socket, queue);
                }
                queue.add(ByteBuffer.wrap(data));
            }
        }

        log.debug("Waking up selector thread");
        this.selector.wakeup();
    }

    // This will be our selector thread, only one thread for all sockets. If you want to understand the overall design decisions read this first http://rox-xmlrpc.sourceforge.net/niotut/
    class ProcessorTask implements Runnable {

        public ProcessorTask() {
        }

        public void read(SelectionKey selectionKey) {
            // read it.
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
            final NioTcpMessageChannel nioTcpMessageChannel = NioTcpMessageChannel.getMessageChannel(socketChannel);

            log.debug("Got something on nioTcpMessageChannel " + nioTcpMessageChannel + " socket " + socketChannel);
            if (nioTcpMessageChannel == null) {

                log.debug("Dead socketChannel" + socketChannel + " socket " + socketChannel.socket().getInetAddress() + ":" + socketChannel.socket().getPort());
                selectionKey.cancel();
                // https://java.net/jira/browse/JSIP-475 remove the socket from the hashmap
                pendingData.remove(socketChannel);
                return;
            }

            nioTcpMessageChannel.readChannel();

        }

        public void write(SelectionKey selectionKey) {
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

            final NioTcpMessageChannel nioTcpMessageChannel = NioTcpMessageChannel.getMessageChannel(socketChannel);

            log.debug("Need to write something on nioTcpMessageChannel " + nioTcpMessageChannel + " socket " + socketChannel);
            if (nioTcpMessageChannel == null) {

                log.debug("Dead socketChannel" + socketChannel + " socket " + socketChannel.socket().getInetAddress() + ":" + socketChannel.socket().getPort());
                selectionKey.cancel();
                // https://java.net/jira/browse/JSIP-475 remove the socket from the hashmap
                pendingData.remove(socketChannel);
                return;
            }

            synchronized (pendingData) {
                List<ByteBuffer> queue = pendingData.get(socketChannel);

                log.debug("Queued items for writing " + queue.size());
                while (!queue.isEmpty()) {
                    ByteBuffer buf = queue.get(0);

                    try {
                        socketChannel.write(buf);
                    } catch (IOException e) {

                        log.debug("Dead socketChannel" + socketChannel + " socket " + socketChannel.socket().getInetAddress() + ":" + socketChannel.socket().getPort() + " : error message " + e.getMessage());
                        nioTcpMessageChannel.close();
                        // Shall we perform a retry mechanism in case the remote host connection was closed due to a TCP RST ?
                        // https://java.net/jira/browse/JSIP-475 in the meanwhile remove the data from the hashmap
                        queue.remove(0);
                        pendingData.remove(socketChannel);
                        return;
                    }

                    int remain = buf.remaining();

                    if (remain > 0) {
                        // ... or the socket's buffer fills up

                        log.debug("Socket buffer filled and more is remaining" + queue.size() + " remain = " + remain);
                        break;
                    }
                    queue.remove(0);
                }

                if (queue.isEmpty()) {

                    log.debug("We wrote away all data. Setting READ interest. Queue is emtpy now size =" + queue.size());
                    selectionKey.interestOps(SelectionKey.OP_READ);
                }
            }

            log.debug("Done writing");
        }

        public void connect(SelectionKey selectionKey) throws IOException {
            // Ignoring the advice from http://rox-xmlrpc.sourceforge.net/niotut/ because it leads to spinning on my machine
            throw new IOException("We should use blocking connect, we must never reach here");
        	/*SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        	  
        	try {
        		socketChannel.finishConnect();
        	} catch (IOException e) {
        		selectionKey.cancel();
        		log.error("Cant connect", e);
        		return;
        	}
            synchronized (socketChannel) {
            	log.debug("Notifying to wake up the blocking connect");
            	socketChannel.notify();
            }

            // Register an interest in writing on this channel
            selectionKey.interestOps(SelectionKey.OP_WRITE);
            */
        }

        public void accept(SelectionKey selectionKey) throws IOException {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
            SocketChannel client;
            client = serverSocketChannel.accept();

            if (sipStack.isTcpNoDelayEnabled) {
                client.setOption(StandardSocketOptions.TCP_NODELAY, true);
            }

            client.configureBlocking(false);

            log.debug("got a new connection! " + client);

            // No need for MAX SOCKET CHANNELS check here because this can be configured at OS level

            createMessageChannel(NioTcpMessageProcessor.this, client);


            log.debug("Adding to selector " + client);
            client.register(selector, SelectionKey.OP_READ);

        }

        @Override
        public void run() {
            while (true) {

                log.debug("Selector thread cycle begin...");

                synchronized (changeRequests) {
                    for (ChangeRequest change : changeRequests) {

                        log.debug("ChangeRequest " + change + " selector = " + selector);
                        try {
                            switch (change.type) {
                                case ChangeRequest.CHANGEOPS:
                                    SelectionKey key = change.socket.keyFor(selector);
                                    if (key == null || !key.isValid()) continue;
                                    key.interestOps(change.ops);

                                    log.debug("Change opts " + change + " selector = " + selector + " key = " + key + " blocking=" + change.socket.isBlocking());

                                    break;
                                case ChangeRequest.REGISTER:
                                    try {


                                        log.debug("NIO register " + change + " selector = " + selector + " blocking=" + change.socket.isBlocking());


                                        change.socket.register(selector, change.ops);
                                    } catch (ClosedChannelException e) {
                                        log.warn("Socket closed before register ops " + change.socket);
                                    }
                                    break;
                            }
                        } catch (Exception e) {
                            log.error("Problem setting changes", e);
                        }
                    }
                    changeRequests.clear();
                }
                try {

                    log.debug("Before select");

                    if (!selector.isOpen()) {

                        log.info("Selector is closed ");

                        return;
                    } else {
                        selector.select();

                        log.debug("After select");

                    }
                } catch (IOException e) {
                    log.error("problem in select", e);
                    break;
                } catch (CancelledKeyException cke) {

                    log.info("Looks like remote side closed a connection");

                }
                try {
                    if (selector.selectedKeys() == null) {

                        log.debug("null selectedKeys ");

                        continue;
                    }

                    Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                    while (it.hasNext()) {
                        SelectionKey selectionKey = it.next();
                        try {
                            it.remove();

                            log.debug("We got selkey " + selectionKey);

                            if (!selectionKey.isValid()) {

                                log.debug("Invalid key found " + selectionKey);

                            } else if (selectionKey.isAcceptable()) {

                                log.debug("Accept " + selectionKey);

                                accept(selectionKey);
                            } else if (selectionKey.isReadable()) {

                                log.debug("Read " + selectionKey);

                                read(selectionKey);

                            } else if (selectionKey.isWritable()) {

                                log.debug("Write " + selectionKey);

                                write(selectionKey);
                            } else if (selectionKey.isConnectable()) {

                                log.debug("Connect " + selectionKey);

                                connect(selectionKey);
                            }
                        } catch (Exception e) {
                            log.error("Problem processing selection key event", e);
                            //NioTcpMessageChannel.get(selectionKey.channel());
                        }
                    }

                } catch (ClosedSelectorException ex) {

                    log.info("Selector is closed");

                    return;
                } catch (Exception ex) {
                    log.error("Problem in the selector loop", ex);
                }
            }
        }
    }

    public NioTcpMessageChannel createMessageChannel(NioTcpMessageProcessor nioTcpMessageProcessor, SocketChannel client) throws IOException {
        return NioTcpMessageChannel.create(NioTcpMessageProcessor.this, client);
    }

    public NioTcpMessageProcessor(InetAddress ipAddress, SIPTransactionStack sipStack, int port) {
        super(ipAddress, port, "TCP", sipStack);
        nioHandler = new NIOHandler(sipStack, this);
    }

    @Override
    public MessageChannel createMessageChannel(HostPort targetHostPort) throws IOException {

        log.debug("NioTcpMessageProcessor::createMessageChannel: " + targetHostPort);

        try {
            String key = MessageChannel.getKey(targetHostPort, transport);
            if (messageChannels.get(key) != null) {
                return this.messageChannels.get(key);
            } else {
                NioTcpMessageChannel retval = new NioTcpMessageChannel(targetHostPort.getInetAddress(),
                        targetHostPort.getPort(), sipStack, this);


                //	retval.getSocketChannel().register(selector, SelectionKey.OP_READ);
                synchronized (messageChannels) {
                    this.messageChannels.put(key, retval);
                }
                retval.isCached = true;

                log.debug("key " + key);
                log.debug("Creating " + retval);

                selector.wakeup();
                return retval;

            }
        } finally {
            log.debug("MessageChannel::createMessageChannel - exit");
        }
    }

    @Override
    public MessageChannel createMessageChannel(InetAddress targetHost, int port) throws IOException {
        String key = MessageChannel.getKey(targetHost, port, transport);
        if (messageChannels.get(key) != null) {
            return this.messageChannels.get(key);
        } else {
            NioTcpMessageChannel retval = new NioTcpMessageChannel(targetHost, port, sipStack, this);

            selector.wakeup();
            //           retval.getSocketChannel().register(selector, SelectionKey.OP_READ);
            this.messageChannels.put(key, retval);
            retval.isCached = true;

            log.debug("key " + key);
            log.debug("Creating " + retval);

            return retval;
        }

    }

    // https://java.net/jira/browse/JSIP-475
    @Override
    protected synchronized void remove(
            ConnectionOrientedMessageChannel messageChannel) {

        log.debug(Thread.currentThread() + " removing " + ((NioTcpMessageChannel) messageChannel).getSocketChannel() + " from processor " + getIpAddress() + ":" + getPort() + "/" + getTransport());

        pendingData.remove(((NioTcpMessageChannel) messageChannel).getSocketChannel());
        super.remove(messageChannel);
    }

    @Override
    public int getDefaultTargetPort() {
        return 5060;
    }

    @Override
    public boolean isSecure() {
        return false;
    }


    @Override
    public void start() throws IOException {
        selector = Selector.open();
        channel = ServerSocketChannel.open();
        channel.configureBlocking(false);
        InetSocketAddress isa = new InetSocketAddress(super.getIpAddress(), super.getPort());
        channel.socket().bind(isa);
        channel.register(selector, SelectionKey.OP_ACCEPT);
        selectorThread = new Thread(createProcessorTask());
        selectorThread.start();
        selectorThread.setName("NioSelector-" + getTransport() + '-' + getIpAddress().getHostAddress() + '/' + getPort());
    }

    protected ProcessorTask createProcessorTask() {
        return new ProcessorTask();
    }

    @Override
    public void stop() {
        try {
            nioHandler.stop();
            if (selector.isOpen()) {
                selector.close();
            }
        } catch (Exception ex) {
            log.error("Problem closing channel ", ex);
        }
        try {
            channel.close();
        } catch (Exception ex) {
            log.error("Problem closing channel ", ex);
        }
    }

}
