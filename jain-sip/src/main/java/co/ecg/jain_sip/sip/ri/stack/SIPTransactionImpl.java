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

import co.ecg.jain_sip.sip.ri.message.SIPMessage;
import co.ecg.jain_sip.sip.ri.message.SIPRequest;
import co.ecg.jain_sip.core.ri.InternalErrorHandler;
import co.ecg.jain_sip.sip.ri.SIPConstants;
import co.ecg.jain_sip.sip.ri.SipProviderImpl;
import co.ecg.jain_sip.sip.ri.SipStackImpl;
import co.ecg.jain_sip.sip.ri.address.AddressFactoryImpl;
import co.ecg.jain_sip.sip.ri.header.Via;
import co.ecg.jain_sip.sip.ri.message.SIPMessage;
import co.ecg.jain_sip.sip.ri.message.SIPRequest;
import co.ecg.jain_sip.sip.ri.message.SIPResponse;
import co.ecg.jain_sip.sip.ri.stack.SIPClientTransactionImpl.ExpiresTimerTask;

import java.io.IOException;
import java.net.InetAddress;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLPeerUnverifiedException;

import co.ecg.jain_sip.sip.Dialog;
import co.ecg.jain_sip.sip.IOExceptionEvent;
import co.ecg.jain_sip.sip.TransactionState;
import co.ecg.jain_sip.sip.address.SipURI;
import co.ecg.jain_sip.sip.message.Request;
import co.ecg.jain_sip.sip.message.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;

/*
 * Modifications for TLS Support added by Daniel J. Martinez Manzano
 * <dani@dif.um.es> Bug fixes by Jeroen van Bemmel (JvB) and others.
 */

/**
 * Abstract class to support both client and server transactions. Provides an
 * encapsulation of a message channel, handles timer events, and creation of the
 * Via header for a message.
 *
 * @author Jeff Keyser
 * @author M. Ranganathan
 * @version 1.2 $Revision: 1.100 $ $Date: 2010-12-02 22:04:13 $
 */
@Slf4j
public abstract class SIPTransactionImpl implements SIPTransaction {


    // Contribution on http://java.net/jira/browse/JSIP-417 from Alexander Saveliev
    private static final Pattern EXTRACT_CN = Pattern.compile(".*CN\\s*=\\s*([\\w*\\.\\-_]+).*");

    protected boolean toListener; // Flag to indicate that the listener gets

    // to see the event.

    protected int baseTimerInterval = SIPTransactionStack.BASE_TIMER_INTERVAL;
    /**
     * 5 sec Maximum duration a message will remain in the network
     */
    protected int T4 = 5000 / baseTimerInterval;

    /**
     * The maximum retransmit interval for non-INVITE requests and INVITE
     * responses
     */
    protected int T2 = 4000 / baseTimerInterval;
    protected int timerI = T4;

    protected int timerK = T4;

    protected int timerD = 32000 / baseTimerInterval;

    // Proposed feature for next release.
    protected transient Object applicationData;

    protected SIPResponse lastResponse;

    // private SIPDialog dialog;

    protected boolean isMapped;

    private transient TransactionSemaphore semaphore;

    // protected boolean eventPending; // indicate that an event is pending
    // here.

    protected String transactionId; // Transaction Id.

    // Audit tag used by the SIP Stack audit
    protected long auditTag = 0;

    // Parent stack for this transaction
    protected transient SIPTransactionStack sipStack;

    // Original request that is being handled by this transaction
    protected SIPRequest originalRequest;
    //jeand we nullify the originalRequest fast to save on mem and help GC
    // so we keep only those data instead
    protected byte[] originalRequestBytes;
    protected long originalRequestCSeqNumber;
    protected String originalRequestBranch;
    protected boolean originalRequestHasPort;


    // Underlying channel being used to send messages for this transaction
    protected transient MessageChannel encapsulatedChannel;

    protected AtomicBoolean transactionTimerStarted = new AtomicBoolean(false);

    // Transaction branch ID
    private String branch;

    // Method of the Request used to create the transaction.
    private String method;

    // Current transaction state
    private int currentState = -1;

    // Number of ticks the retransmission timer was set to last
    private transient int retransmissionTimerLastTickCount;

    // Number of ticks before the message is retransmitted
    private transient int retransmissionTimerTicksLeft;

    // Number of ticks before the transaction times out
    protected int timeoutTimerTicksLeft;

    // List of event listeners for this transaction
    private transient Set<SIPTransactionEventListener> eventListeners;


    // Counter for caching of connections.
    // Connection lingers for collectionTime
    // after the Transaction goes to terminated state.
    protected int collectionTime;

    private boolean terminatedEventDelivered;

    // aggressive flag to optimize eagerly
    private boolean releaseReferences;

    // caching flags
    private Boolean inviteTransaction = null;
    private Boolean dialogCreatingTransaction = null;

    // caching fork id
    private String forkId = null;
    protected String mergeId = null;

    public ExpiresTimerTask expiresTimerTask;
    // http://java.net/jira/browse/JSIP-420
    private MaxTxLifeTimeListener maxTxLifeTimeListener;

    /**
     * @see SIPTransaction#getBranchId()
     */
    @Override
    public String getBranchId() {
        return this.branch;
    }

    // [Issue 284] https://jain-sip.dev.java.net/issues/show_bug.cgi?id=284
    // JAIN SIP drops 200 OK due to race condition
    // Wrapper that uses a semaphore for non reentrant listener
    // and a lock for reetrant listener to avoid race conditions 
    // when 2 responses 180/200 OK arrives at the same time
    class TransactionSemaphore {

        private static final long serialVersionUID = -1634100711669020804L;
        Semaphore sem = null;
        ReentrantLock lock = null;

        public TransactionSemaphore() {
            if (((SipStackImpl) sipStack).isReEntrantListener()) {
                lock = new ReentrantLock();
            } else {
                sem = new Semaphore(1, true);
            }
        }

        public boolean acquire() {
            try {
                if (((SipStackImpl) sipStack).isReEntrantListener()) {
                    lock.lock();
                } else {
                    sem.acquire();
                }
                return true;
            } catch (Exception ex) {
                log.error("Unexpected exception acquiring sem",
                        ex);
                InternalErrorHandler.handleException(ex);
                return false;
            }
        }

        public boolean tryAcquire() {
            try {
                if (((SipStackImpl) sipStack).isReEntrantListener()) {
                    return lock.tryLock(sipStack.maxListenerResponseTime, TimeUnit.SECONDS);
                } else {
                    return sem.tryAcquire(sipStack.maxListenerResponseTime, TimeUnit.SECONDS);
                }
            } catch (Exception ex) {
                log.error("Unexpected exception trying acquiring sem",
                        ex);
                InternalErrorHandler.handleException(ex);
                return false;
            }
        }

        public void release() {
            try {
                if (((SipStackImpl) sipStack).isReEntrantListener()) {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                } else {
                    sem.release();
                }
            } catch (Exception ex) {
                log.error("Unexpected exception releasing sem",
                        ex);
            }
        }
    }

    /**
     * The linger timer is used to remove the transaction from the transaction
     * table after it goes into terminated state. This allows connection caching
     * and also takes care of race conditins.
     */
    class LingerTimer extends SIPStackTimerTask {

        public LingerTimer() {
            if (log.isDebugEnabled()) {
                SIPTransaction sipTransaction = SIPTransactionImpl.this;
                log.debug("LingerTimer : "
                        + sipTransaction.getTransactionId());
            }

        }

        public void runTask() {
            cleanUp();
        }
    }

    /**
     * http://java.net/jira/browse/JSIP-420
     * This timer task will terminate the transaction after a configurable time
     */
    class MaxTxLifeTimeListener extends SIPStackTimerTask {
        SIPTransaction sipTransaction = SIPTransactionImpl.this;

        public void runTask() {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Fired MaxTxLifeTimeListener for tx " + sipTransaction + " , tx id " + sipTransaction.getTransactionId() + " , state " + sipTransaction.getState());
                }

                raiseErrorEvent(SIPTransactionErrorEvent.TIMEOUT_ERROR);

                SIPStackTimerTask myTimer = new LingerTimer();
                sipStack.getTimer().schedule(myTimer,
                        SIPTransactionStack.CONNECTION_LINGER_TIME * 1000);
                maxTxLifeTimeListener = null;

            } catch (Exception ex) {
                log.error("unexpected exception", ex);
            }
        }
    }

    /**
     * Transaction constructor.
     *
     * @param newParentStack         Parent stack for this transaction.
     * @param newEncapsulatedChannel Underlying channel for this transaction.
     */
    protected SIPTransactionImpl(SIPTransactionStack newParentStack,
                                 MessageChannel newEncapsulatedChannel) {

        sipStack = newParentStack;
        this.semaphore = new TransactionSemaphore();

        encapsulatedChannel = newEncapsulatedChannel;

        if (this.isReliable()) {
            encapsulatedChannel.useCount++;
            if (log.isDebugEnabled())
                log.debug("use count for encapsulated channel"
                        + this
                        + " "
                        + encapsulatedChannel.useCount);
        }

        this.currentState = -1;

        disableRetransmissionTimer();
        disableTimeoutTimer();
        eventListeners = new CopyOnWriteArraySet<SIPTransactionEventListener>();

        // Always add the parent stack as a listener
        // of this transaction
        addEventListener(newParentStack);

        releaseReferences = sipStack.isAggressiveCleanup();
    }

    /**
     * @see SIPTransaction#cleanUp()
     */
    @Override
    public abstract void cleanUp();

    /**
     * @see SIPTransaction#setOriginalRequest(SIPRequest)
     */
    @Override
    public void setOriginalRequest(SIPRequest newOriginalRequest) {

        // Branch value of topmost Via header
        String newBranch;

        final String newTransactionId = newOriginalRequest.getTransactionId();
        if (this.originalRequest != null
                && (!this.originalRequest.getTransactionId().equals(
                newTransactionId))) {
            sipStack.removeTransactionHash(this);
        }
        // This will be cleared later.

        this.originalRequest = newOriginalRequest;
        this.originalRequestCSeqNumber = newOriginalRequest.getCSeq().getSeqNumber();
        final Via topmostVia = newOriginalRequest.getTopmostVia();
        this.originalRequestBranch = topmostVia.getBranch();
        this.originalRequestHasPort = topmostVia.hasPort();
        int originalRequestViaPort = topmostVia.getPort();

        if (originalRequestViaPort == -1) {
            if (topmostVia.getTransport().equalsIgnoreCase("TLS")) {
                originalRequestViaPort = 5061;
            } else {
                originalRequestViaPort = 5060;
            }
        }

        // just cache the control information so the
        // original request can be released later.
        this.method = newOriginalRequest.getMethod();

        this.transactionId = newTransactionId;

        originalRequest.setTransaction(this);

        // If the message has an explicit branch value set,
        newBranch = topmostVia.getBranch();
        if (newBranch != null) {
            if (log.isDebugEnabled())
                log.debug("Setting Branch id : " + newBranch);

            // Override the default branch with the one
            // set by the message
            setBranch(newBranch);

        } else {
            if (log.isDebugEnabled())
                log.debug("Branch id is null - compute TID!"
                        + newOriginalRequest.encode());
            setBranch(newTransactionId);
        }
    }

    /**
     * @see SIPTransaction#getOriginalRequest()
     */
    @Override
    public SIPRequest getOriginalRequest() {
        return this.originalRequest;
    }

    /**
     * @see SIPTransaction#getRequest()
     */
    @Override
    public Request getRequest() {
        if (isReleaseReferences() && originalRequest == null && originalRequestBytes != null) {
            if (log.isWarnEnabled()) {
                log.warn("reparsing original request " + originalRequestBytes + " since it was eagerly cleaned up, but beware this is not efficient with the aggressive flag set !");
            }
            try {
                originalRequest = (SIPRequest) sipStack.getMessageParserFactory().createMessageParser(sipStack).parseSIPMessage(originalRequestBytes, true, false, null);
//                originalRequestBytes = null;
            } catch (ParseException e) {
                log.error("message " + originalRequestBytes + " could not be reparsed !");
            }
        }
        return (Request) originalRequest;
    }

    /**
     * @see SIPTransaction#isDialogCreatingTransaction()
     */
    @Override
    public boolean isDialogCreatingTransaction() {
        if (dialogCreatingTransaction == null) {
            dialogCreatingTransaction = Boolean.valueOf(isInviteTransaction() || getMethod().equals(Request.SUBSCRIBE) || getMethod().equals(Request.REFER));
        }
        return dialogCreatingTransaction.booleanValue();
    }

    /**
     * @see SIPTransaction#isInviteTransaction()
     */
    @Override
    public boolean isInviteTransaction() {
        if (inviteTransaction == null) {
            inviteTransaction = Boolean.valueOf(getMethod().equals(Request.INVITE));
        }
        return inviteTransaction.booleanValue();
    }

    /**
     * @see SIPTransaction#isCancelTransaction()
     */
    @Override
    public boolean isCancelTransaction() {
        return getMethod().equals(Request.CANCEL);
    }

    /**
     * @see SIPTransaction#isByeTransaction()
     */
    @Override
    public boolean isByeTransaction() {
        return getMethod().equals(Request.BYE);
    }

    /**
     * @see SIPTransaction#getMessageChannel()
     */
    @Override
    public MessageChannel getMessageChannel() {
        return encapsulatedChannel;
    }

    /**
     * @see SIPTransaction#setBranch(String)
     */
    @Override
    public void setBranch(String newBranch) {
        branch = newBranch;
    }

    /**
     * @see SIPTransaction#getBranch()
     */
    @Override
    public String getBranch() {
        if (this.branch == null) {
            this.branch = originalRequestBranch;
        }
        return branch;
    }

    /**
     * @see SIPTransaction#getMethod()
     */
    @Override
    public String getMethod() {
        return this.method;
    }

    /**
     * @see SIPTransaction#getCSeq()
     */
    @Override
    public long getCSeq() {
        return this.originalRequestCSeqNumber;
    }

    /**
     * @see SIPTransaction#setState(int)
     */
    @Override
    public void setState(int newState) {
        // PATCH submitted by sribeyron
        if (currentState == TransactionState._COMPLETED) {
            if (newState != TransactionState._TERMINATED
                    && newState != TransactionState._CONFIRMED)
                newState = TransactionState._COMPLETED;
        }
        if (currentState == TransactionState._CONFIRMED) {
            if (newState != TransactionState._TERMINATED)
                newState = TransactionState._CONFIRMED;
        }
        if (currentState != TransactionState._TERMINATED) {
            currentState = newState;
        } else
            newState = currentState;
        // END OF PATCH

        if (newState == TransactionState._COMPLETED) {
            enableTimeoutTimer(TIMER_H); // timer H must be started around now
        }

        if (log.isDebugEnabled()) {
            log.debug("Transaction:setState " + newState
                    + " " + this + " branchID = " + this.getBranch()
                    + " isClient = " + (this instanceof SIPClientTransaction));
        }
    }

    /**
     * @see SIPTransaction#getInternalState()
     */
    @Override
    public int getInternalState() {
        return this.currentState;
    }

    /**
     * @see SIPTransaction#getState()
     */
    @Override
    public TransactionState getState() {
        if (currentState < 0) {
            return null;
        }
        return TransactionState.getObject(this.currentState);
    }

    /**
     * Enables retransmission timer events for this transaction to begin in one
     * tick.
     */
    protected void enableRetransmissionTimer() {
        enableRetransmissionTimer(1);
    }

    /**
     * Enables retransmission timer events for this transaction to begin after
     * the number of ticks passed to this routine.
     *
     * @param tickCount Number of ticks before the next retransmission timer event
     *                  occurs.
     */
    protected void enableRetransmissionTimer(int tickCount) {
        // For INVITE Client transactions, double interval each time
        if (isInviteTransaction() && (this instanceof SIPClientTransaction)) {
            retransmissionTimerTicksLeft = tickCount;
        } else {
            // non-INVITE transactions and 3xx-6xx responses are capped at T2
            retransmissionTimerTicksLeft = Math.min(tickCount,
                    getTimerT2());
        }
        retransmissionTimerLastTickCount = retransmissionTimerTicksLeft;
    }


    /**
     * @see SIPTransaction#disableRetransmissionTimer()
     */
    @Override
    public void disableRetransmissionTimer() {
        retransmissionTimerTicksLeft = -1;
    }

    /**
     * Enables a timeout event to occur for this transaction after the number of
     * ticks passed to this method.
     *
     * @param tickCount Number of ticks before this transaction times out.
     */
    protected void enableTimeoutTimer(int tickCount) {
        if (log.isDebugEnabled())
            log.debug("enableTimeoutTimer " + this
                    + " tickCount " + tickCount + " currentTickCount = "
                    + timeoutTimerTicksLeft);

        timeoutTimerTicksLeft = tickCount;
    }


    /**
     * @see SIPTransaction#disableTimeoutTimer()
     */
    @Override
    public void disableTimeoutTimer() {
        if (log.isDebugEnabled()) log.debug("disableTimeoutTimer " + this);
        timeoutTimerTicksLeft = -1;
    }


    /**
     * @see SIPTransaction#fireTimer()
     */
    @Override
    public void fireTimer() {
        // If the timeout timer is enabled,

        if (timeoutTimerTicksLeft != -1) {
            // Count down the timer, and if it has run out,
            if (--timeoutTimerTicksLeft == 0) {
                fireTimeoutTimer();
            }
        }

        // If the retransmission timer is enabled,
        if (retransmissionTimerTicksLeft != -1) {
            // Count down the timer, and if it has run out,
            if (--retransmissionTimerTicksLeft == 0) {
                // Enable this timer to fire again after
                // twice the original time
                enableRetransmissionTimer(retransmissionTimerLastTickCount * 2);
                // Fire the timeout timer
                fireRetransmissionTimer();
            }
        }
    }

    /**
     * @see SIPTransaction#isTerminated()
     */
    @Override
    public boolean isTerminated() {
        return currentState == TransactionState._TERMINATED;
    }

    /**
     * @see SIPTransaction#getHost()
     */
    @Override
    public String getHost() {
        return encapsulatedChannel.getHost();
    }

    /**
     * @see SIPTransaction#getKey()
     */
    @Override
    public String getKey() {
        return encapsulatedChannel.getKey();
    }

    /**
     * @see SIPTransaction#getPort()
     */
    @Override
    public int getPort() {
        return encapsulatedChannel.getPort();
    }

    /**
     * @see SIPTransaction#getSIPStack()
     */
    @Override
    public SIPTransactionStack getSIPStack() {
        return (SIPTransactionStack) sipStack;
    }

    /**
     * @see SIPTransaction#getPeerAddress()
     */
    @Override
    public String getPeerAddress() {
        return this.encapsulatedChannel.getPeerAddress();
    }

    /**
     * @see SIPTransaction#getPeerPort()
     */
    @Override
    public int getPeerPort() {
        return this.encapsulatedChannel.getPeerPort();
    }

    // @@@ hagai

    /**
     * @see SIPTransaction#getPeerPacketSourcePort()
     */
    @Override
    public int getPeerPacketSourcePort() {
        return this.encapsulatedChannel.getPeerPacketSourcePort();
    }

    /**
     * @see SIPTransaction#getPeerPacketSourceAddress()
     */
    @Override
    public InetAddress getPeerPacketSourceAddress() {
        return this.encapsulatedChannel.getPeerPacketSourceAddress();
    }

    public InetAddress getPeerInetAddress() {
        return this.encapsulatedChannel.getPeerInetAddress();
    }

    public String getPeerProtocol() {
        return this.encapsulatedChannel.getPeerProtocol();
    }

    /**
     * @see SIPTransaction#getTransport()
     */
    @Override
    public String getTransport() {
        return encapsulatedChannel.getTransport();
    }

    /**
     * @see SIPTransaction#isReliable()
     */
    @Override
    public boolean isReliable() {
        return encapsulatedChannel.isReliable();
    }

    /**
     * @see SIPTransaction#getViaHeader()
     */
    @Override
    public Via getViaHeader() {
        // Via header of the encapulated channel
        Via channelViaHeader;

        // Add the branch parameter to the underlying
        // channel's Via header
        channelViaHeader = encapsulatedChannel.getViaHeader();
        try {
            channelViaHeader.setBranch(branch);
        } catch (ParseException ex) {
        }
        return channelViaHeader;

    }

    /**
     * @see SIPTransaction#sendMessage(SIPMessage)
     */
    @Override
    public void sendMessage(final SIPMessage messageToSend) throws IOException {
        // Use the peer address, port and transport
        // that was specified when the transaction was
        // created. Bug was noted by Bruce Evangelder
        // soleo communications.
        try {
            final RawMessageChannel channel = (RawMessageChannel) encapsulatedChannel;
            for (MessageProcessor messageProcessor : sipStack
                    .getMessageProcessors()) {
                boolean addrmatch = messageProcessor.getIpAddress().getHostAddress().toString().equals(this.getPeerAddress());
                if (addrmatch
                        && messageProcessor.getPort() == this.getPeerPort()
                        && messageProcessor.getTransport().equalsIgnoreCase(
                        this.getPeerProtocol())) {
                    if (channel instanceof TCPMessageChannel) {
                        try {

                            Runnable processMessageTask = new Runnable() {

                                public void run() {
                                    try {
                                        ((TCPMessageChannel) channel)
                                                .processMessage(SerializationUtils.clone(messageToSend), getPeerInetAddress());
                                    } catch (Exception ex) {

                                        if (log.isErrorEnabled()) {
                                            log.error("Error self routing TCP message cause by: ", ex);
                                        }
                                    }
                                }
                            };
                            getSIPStack().getSelfRoutingThreadpoolExecutor().execute(processMessageTask);

                        } catch (Exception e) {

                            log.error("Error passing message in self routing TCP", e);

                        }
                        if (log.isDebugEnabled())
                            log.debug("Self routing message TCP");

                        return;
                    }
                    if (channel instanceof TLSMessageChannel) {
                        try {

                            Runnable processMessageTask = new Runnable() {

                                public void run() {
                                    try {
                                        ((TLSMessageChannel) channel)
                                                .processMessage(SerializationUtils.clone(messageToSend), getPeerInetAddress());
                                    } catch (Exception ex) {
                                        if (log.isErrorEnabled()) {
                                            log.error("Error self routing TLS message cause by: ", ex);
                                        }
                                    }
                                }
                            };
                            getSIPStack().getSelfRoutingThreadpoolExecutor().execute(processMessageTask);

                        } catch (Exception e) {
                            log.error("Error passing message in TLS self routing", e);
                        }
                        if (log.isDebugEnabled())
                            log.debug("Self routing message TLS");
                        return;
                    }
                    if (channel instanceof RawMessageChannel) {
                        try {

                            Runnable processMessageTask = new Runnable() {

                                public void run() {
                                    try {
                                        channel.processMessage(SerializationUtils.clone(messageToSend));
                                    } catch (Exception ex) {
                                        if (log.isErrorEnabled()) {
                                            log.error("Error self routing message cause by: ", ex);
                                        }
                                    }
                                }
                            };
                            getSIPStack().getSelfRoutingThreadpoolExecutor().execute(processMessageTask);
                        } catch (Exception e) {
                            log.error("Error passing message in self routing", e);
                        }
                        if (log.isDebugEnabled())
                            log.debug("Self routing message");
                        return;
                    }

                }
            }
            encapsulatedChannel.sendMessage(messageToSend,
                    this.getPeerInetAddress(), this.getPeerPort());
        } finally {
            this.startTransactionTimer();
        }
    }

    /**
     * Parse the byte array as a message, process it through the transaction,
     * and send it to the SIP peer. This is just a placeholder method -- calling
     * it will result in an IO exception.
     *
     * @param messageBytes    Bytes of the message to send.
     * @param receiverAddress Address of the target peer.
     * @param receiverPort    Network port of the target peer.
     * @throws IOException If called.
     */
    public void sendMessage(byte[] messageBytes,
                            InetAddress receiverAddress, int receiverPort, boolean retry)
            throws IOException {
        throw new IOException(
                "Cannot send unparsed message through Transaction Channel!");
    }

    /**
     * @see SIPTransaction#addEventListener(SIPTransactionEventListener)
     */
    @Override
    public void addEventListener(SIPTransactionEventListener newListener) {
        eventListeners.add(newListener);
    }

    /**
     * @see SIPTransaction#removeEventListener(SIPTransactionEventListener)
     */
    @Override
    public void removeEventListener(SIPTransactionEventListener oldListener) {
        eventListeners.remove(oldListener);
    }

    /**
     * @see SIPTransaction#raiseErrorEvent(int)
     */
    @Override
    public void raiseErrorEvent(int errorEventID) {

        // Error event to send to all listeners
        SIPTransactionErrorEvent newErrorEvent;
        // Iterator through the list of listeners
        Iterator<SIPTransactionEventListener> listenerIterator;
        // Next listener in the list
        SIPTransactionEventListener nextListener;

        // Create the error event
        newErrorEvent = new SIPTransactionErrorEvent(this, errorEventID);

        // Loop through all listeners of this transaction
        synchronized (eventListeners) {
            listenerIterator = eventListeners.iterator();
            while (listenerIterator.hasNext()) {
                // Send the event to the next listener
                nextListener = (SIPTransactionEventListener) listenerIterator
                        .next();
                nextListener.transactionErrorEvent(newErrorEvent);
            }
        }
        // Clear the event listeners after propagating the error.
        // Retransmit notifications are just an alert to the
        // application (they are not an error).
        if (errorEventID != SIPTransactionErrorEvent.TIMEOUT_RETRANSMIT) {
            eventListeners.clear();

            // Errors always terminate a transaction
            this.setState(TransactionState._TERMINATED);

            if (this instanceof SIPServerTransaction && this.isByeTransaction()
                    && this.getDialog() != null)
                ((SIPDialog) this.getDialog())
                        .setState(SIPDialog.TERMINATED_STATE);
        }
    }

    /**
     * @see SIPTransaction#isServerTransaction()
     */
    @Override
    public boolean isServerTransaction() {
        return this instanceof SIPServerTransaction;
    }

    /**
     * @see SIPTransaction#getDialog()
     */
    @Override
    public abstract Dialog getDialog();

    /**
     * @see SIPTransaction#setDialog(SIPDialog, String)
     */
    @Override
    public abstract void setDialog(SIPDialog sipDialog, String dialogId);

    /**
     * @see SIPTransaction#getRetransmitTimer()
     */
    @Override
    public int getRetransmitTimer() {
        return SIPTransactionStack.BASE_TIMER_INTERVAL;
    }

    /**
     * @see SIPTransaction#getViaHost()
     */
    @Override
    public String getViaHost() {
        return this.getViaHeader().getHost();

    }

    /**
     * @see SIPTransaction#getLastResponse()
     */
    @Override
    public SIPResponse getLastResponse() {
        return this.lastResponse;
    }

    /**
     * @see SIPTransaction#getResponse()
     */
    @Override
    public Response getResponse() {
        return (Response) this.lastResponse;
    }

    /**
     * @see SIPTransaction#getTransactionId()
     */
    @Override
    public String getTransactionId() {
        return this.transactionId;
    }

    /**
     * @see SIPTransaction#hashCode()
     */
    @Override
    public int hashCode() {
        if (this.transactionId == null)
            return -1;
        else
            return this.transactionId.hashCode();
    }

    /**
     * @see SIPTransaction#getViaPort()
     */
    @Override
    public int getViaPort() {
        return this.getViaHeader().getPort();
    }

    /**
     * @see SIPTransaction#doesCancelMatchTransaction(SIPRequest)
     */
    @Override
    public boolean doesCancelMatchTransaction(SIPRequest requestToTest) {

        // List of Via headers in the message to test
//        ViaList viaHeaders;
        // Topmost Via header in the list
        Via topViaHeader;
        // Branch code in the topmost Via header
        String messageBranch;
        // Flags whether the select message is part of this transaction
        boolean transactionMatches;

        transactionMatches = false;
        final SIPRequest origRequest = getOriginalRequest();
        if (origRequest == null
                || this.getMethod().equals(Request.CANCEL))
            return false;
        // Get the topmost Via header and its branch parameter
        topViaHeader = requestToTest.getTopmostVia();
        if (topViaHeader != null) {

//            topViaHeader = (Via) viaHeaders.getFirst();
            messageBranch = topViaHeader.getBranch();
            if (messageBranch != null) {

                // If the branch parameter exists but
                // does not start with the magic cookie,
                if (!messageBranch.toLowerCase().startsWith(SIPConstants.BRANCH_MAGIC_COOKIE_LOWER_CASE)) {

                    // Flags this as old
                    // (RFC2543-compatible) client
                    // version
                    messageBranch = null;

                }

            }

            // If a new branch parameter exists,
            if (messageBranch != null && this.getBranch() != null) {

                // If the branch equals the branch in
                // this message,
                if (getBranch().equalsIgnoreCase(messageBranch)
                        && topViaHeader.getSentBy().equals(
                        origRequest.getTopmostVia().getSentBy())) {
                    transactionMatches = true;
                    if (log.isDebugEnabled())
                        log.debug("returning  true");
                }

            } else {
                // If this is an RFC2543-compliant message,
                // If RequestURI, To tag, From tag,
                // CallID, CSeq number, and top Via
                // headers are the same,
                if (log.isDebugEnabled())
                    log.debug("testing against "
                            + origRequest);

                if (origRequest.getRequestURI().equals(
                        requestToTest.getRequestURI())
                        && origRequest.getTo().equals(
                        requestToTest.getTo())
                        && origRequest.getFrom().equals(
                        requestToTest.getFrom())
                        && origRequest.getCallId().getCallId().equals(
                        requestToTest.getCallId().getCallId())
                        && origRequest.getCSeq().getSeqNumber() == requestToTest
                        .getCSeq().getSeqNumber()
                        && topViaHeader.equals(origRequest.getTopmostVia())) {

                    transactionMatches = true;
                }

            }

        }

        return transactionMatches;
    }

    /**
     * @see SIPTransaction#setRetransmitTimer(int)
     */
    @Override
    public void setRetransmitTimer(int retransmitTimer) {

        if (retransmitTimer <= 0)
            throw new IllegalArgumentException(
                    "Retransmit timer must be positive!");
        if (this.transactionTimerStarted.get())
            throw new IllegalStateException(
                    "Transaction timer is already started");
        baseTimerInterval = retransmitTimer;
        // Commented out for Issue 303 since those timers are configured separately now
//      T4 = 5000 / BASE_TIMER_INTERVAL;
//      T2 = 4000 / BASE_TIMER_INTERVAL;
//      TIMER_D = 32000 / BASE_TIMER_INTERVAL;

    }

    /**
     * @see SIPTransaction#close()
     */
    @Override
    public void close() {
        this.encapsulatedChannel.close();
        if (log.isDebugEnabled())
            log.debug("Closing " + this.encapsulatedChannel);

    }

    /**
     * @see SIPTransaction#isSecure()
     */
    @Override
    public boolean isSecure() {
        return encapsulatedChannel.isSecure();
    }

    /**
     * @see SIPTransaction#getMessageProcessor()
     */
    @Override
    public MessageProcessor getMessageProcessor() {
        return this.encapsulatedChannel.getMessageProcessor();
    }

    /**
     * @see SIPTransaction#setApplicationData(Object)
     */

    @Override
    public void setApplicationData(Object applicationData) {
        this.applicationData = applicationData;
    }

    /**
     * @see SIPTransaction#getApplicationData()
     */
    @Override
    public Object getApplicationData() {
        return this.applicationData;
    }

    /**
     * @see SIPTransaction#setEncapsulatedChannel(MessageChannel)
     */
    @Override
    public void setEncapsulatedChannel(MessageChannel messageChannel) {
        this.encapsulatedChannel = messageChannel;
        if (this instanceof SIPClientTransaction) {
            this.encapsulatedChannel.setEncapsulatedClientTransaction((SIPClientTransaction) this);
        }
    }

    /**
     * @see SIPTransaction#getSipProvider()
     */
    @Override
    public SipProviderImpl getSipProvider() {

        return this.getMessageProcessor().getListeningPoint().getProvider();
    }

    /**
     * @see SIPTransaction#raiseIOExceptionEvent()
     */
    @Override
    public void raiseIOExceptionEvent() {
        setState(TransactionState._TERMINATED);
        String host = getPeerAddress();
        int port = getPeerPort();
        String transport = getTransport();
        IOExceptionEvent exceptionEvent = new IOExceptionEvent(this, host,
                port, transport);
        getSipProvider().handleEvent(exceptionEvent, this);
    }

    /**
     * @see SIPTransaction#acquireSem()
     */
    @Override
    public boolean acquireSem() {
        boolean retval = false;
        if (log.isDebugEnabled()) {
            log.debug("acquireSem [[[[" + this);
        }
        if (this.sipStack.maxListenerResponseTime == -1) {
            retval = this.semaphore.acquire();
        } else {
            retval = this.semaphore.tryAcquire();
        }
        if (log.isDebugEnabled())
            log.debug(
                    "acquireSem() returning : " + retval);
        return retval;
    }


    /**
     * @see SIPTransaction#releaseSem()
     */
    @Override
    public void releaseSem() {
        try {

            this.toListener = false;
            this.semRelease();

        } catch (Exception ex) {
            log.error("Unexpected exception releasing sem",
                    ex);

        }

    }

    public void semRelease() {
        if (log.isDebugEnabled()) {
            log.debug("semRelease ]]]]" + this);
        }
        this.semaphore.release();
    }

    /**
     * @see SIPTransaction#passToListener()
     */

    @Override
    public boolean passToListener() {
        return toListener;
    }

    /**
     * @see SIPTransaction#setPassToListener()
     */
    @Override
    public void setPassToListener() {
        if (log.isDebugEnabled()) {
            log.debug("setPassToListener()");
        }
        this.toListener = true;

    }


    /**
     * @see SIPTransaction#testAndSetTransactionTerminatedEvent()
     */
    @Override
    public synchronized boolean testAndSetTransactionTerminatedEvent() {
        boolean retval = !this.terminatedEventDelivered;
        this.terminatedEventDelivered = true;
        return retval;
    }

    /**
     * @see SIPTransaction#getCipherSuite()
     */
    @Override
    public String getCipherSuite() throws UnsupportedOperationException {
        if (this.getMessageChannel() instanceof TLSMessageChannel) {
            if (((TLSMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener() == null)
                return null;
            else if (((TLSMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener().getHandshakeCompletedEvent() == null)
                return null;
            else
                return ((TLSMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener().getHandshakeCompletedEvent().getCipherSuite();
        } // Added for https://java.net/jira/browse/JSIP-483
        else if (this.getMessageChannel() instanceof NioTlsMessageChannel) {
            if (((NioTlsMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener() == null)
                return null;
            else
                return ((NioTlsMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener().getCipherSuite();
        } else throw new UnsupportedOperationException("Not a TLS channel");

    }


    /**
     * @see SIPTransaction#getLocalCertificates()
     */
    @Override
    public Certificate[] getLocalCertificates() throws UnsupportedOperationException {
        if (this.getMessageChannel() instanceof TLSMessageChannel) {
            if (((TLSMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener() == null)
                return null;
            else if (((TLSMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener().getHandshakeCompletedEvent() == null)
                return null;
            else
                return ((TLSMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener().getHandshakeCompletedEvent().getLocalCertificates();
        } // Added for https://java.net/jira/browse/JSIP-483
        else if (this.getMessageChannel() instanceof NioTlsMessageChannel) {
            if (((NioTlsMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener() == null)
                return null;
            else
                return ((NioTlsMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener().getLocalCertificates();
        } else throw new UnsupportedOperationException("Not a TLS channel");
    }


    /**
     * @see SIPTransaction#getPeerCertificates()
     */
    @Override
    public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
        if (this.getMessageChannel() instanceof TLSMessageChannel) {
            if (((TLSMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener() == null)
                return null;
            else if (((TLSMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener().getHandshakeCompletedEvent() == null)
                return null;
            else
                return ((TLSMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener().getHandshakeCompletedEvent().getPeerCertificates();
        } // Added for https://java.net/jira/browse/JSIP-483
        else if (this.getMessageChannel() instanceof NioTlsMessageChannel) {
            if (((NioTlsMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener() == null)
                return null;
            else
                return ((NioTlsMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener().getPeerCertificates();
        } else throw new UnsupportedOperationException("Not a TLS channel");

    }

    /**
     * @see SIPTransaction#extractCertIdentities()
     */
    @Override
    public List<String> extractCertIdentities() throws SSLPeerUnverifiedException {
        if (this.getMessageChannel() instanceof TLSMessageChannel || this.getMessageChannel() instanceof NioTlsMessageChannel) {
            List<String> certIdentities = new ArrayList<String>();
            Certificate[] certs = getPeerCertificates();
            if (certs == null) {
                if (log.isDebugEnabled()) {
                    log.debug("No certificates available");
                }
                return certIdentities;
            }
            for (Certificate cert : certs) {
                X509Certificate x509cert = (X509Certificate) cert;
                Collection<List<?>> subjAltNames = null;
                try {
                    subjAltNames = x509cert.getSubjectAlternativeNames();
                } catch (CertificateParsingException ex) {

                    log.error("Error parsing TLS certificate", ex);

                }
                // subjAltName types are defined in rfc2459
                final Integer dnsNameType = 2;
                final Integer uriNameType = 6;
                if (subjAltNames != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("found subjAltNames: " + subjAltNames);
                    }
                    // First look for a URI in the subjectAltName field
                    for (List<?> altName : subjAltNames) {
                        // 0th position is the alt name type
                        // 1st position is the alt name data
                        if (altName.get(0).equals(uriNameType)) {
                            SipURI altNameUri;
                            try {
                                altNameUri = new AddressFactoryImpl().createSipURI((String) altName.get(1));
                                // only sip URIs are allowed
                                if (!"sip".equals(altNameUri.getScheme()))
                                    continue;
                                // user certificates are not allowed
                                if (altNameUri.getUser() != null)
                                    continue;
                                String altHostName = altNameUri.getHost();
                                if (log.isDebugEnabled()) {
                                    log.debug(
                                            "found uri " + altName.get(1) + ", hostName " + altHostName);
                                }
                                certIdentities.add(altHostName);
                            } catch (ParseException e) {
                                log.error("certificate contains invalid uri: " + altName.get(1));
                            }
                        }

                    }
                    // DNS An implementation MUST accept a domain name system
                    // identifier as a SIP domain identity if and only if no other
                    // identity is found that matches the "sip" URI type described
                    // above.
                    if (certIdentities.isEmpty()) {
                        for (List<?> altName : subjAltNames) {
                            if (altName.get(0).equals(dnsNameType)) {
                                if (log.isDebugEnabled()) {
                                    log.debug("found dns " + altName.get(1));
                                }
                                certIdentities.add(altName.get(1).toString());
                            }
                        }
                    }
                } else {
                    // If and only if the subjectAltName does not appear in the
                    // certificate, the implementation MAY examine the CN field of the
                    // certificate. If a valid DNS name is found there, the
                    // implementation MAY accept this value as a SIP domain identity.
                    String dname = x509cert.getSubjectDN().getName();
                    String cname = "";
                    try {
                        Matcher matcher = EXTRACT_CN.matcher(dname);
                        if (matcher.matches()) {
                            cname = matcher.group(1);
                            if (log.isDebugEnabled()) {
                                log.debug("found CN: " + cname + " from DN: " + dname);
                            }
                            certIdentities.add(cname);
                        }
                    } catch (Exception ex) {
                        log.error("exception while extracting CN", ex);
                    }
                }
            }
            return certIdentities;
        } else
            throw new UnsupportedOperationException("Not a TLS channel");
    }


    /**
     * @see SIPTransaction#isMessagePartOfTransaction(SIPMessage)
     */
    @Override
    public abstract boolean isMessagePartOfTransaction(SIPMessage messageToTest);

    /*
     * (non-Javadoc)
     * @see gov.nist.javax.sip.DialogExt#isReleaseReferences()
     */

    /**
     * @see SIPTransaction#isReleaseReferences()
     */
    @Override
    public boolean isReleaseReferences() {
        return releaseReferences;
    }

    /*
     * (non-Javadoc)
     * @see gov.nist.javax.sip.DialogExt#setReleaseReferences(boolean)
     */

    /**
     * @see SIPTransaction#setReleaseReferences(boolean)
     */
    @Override
    public void setReleaseReferences(boolean releaseReferences) {
        this.releaseReferences = releaseReferences;
    }


    /**
     * @see SIPTransaction#getTimerD()
     */
    @Override
    public int getTimerD() {
        return timerD;
    }

    /**
     * @see SIPTransaction#getTimerT2()
     */
    @Override
    public int getTimerT2() {
        return T2;
    }

    /**
     * @see SIPTransaction#getTimerT4()
     */
    @Override
    public int getTimerT4() {
        return T4;
    }

    /**
     * @see SIPTransaction#setTimerD(int)
     */
    @Override
    public void setTimerD(int interval) {
        if (interval < 32000) {
            throw new IllegalArgumentException("To be RFC 3261 compliant, the value of Timer D should be at least 32s");
        }
        timerD = interval / baseTimerInterval;
    }


    /**
     * @see SIPTransaction#setTimerT2(int)
     */
    @Override
    public void setTimerT2(int interval) {
        T2 = interval / baseTimerInterval;
    }


    /**
     * @see SIPTransaction#setTimerT4(int)
     */
    @Override
    public void setTimerT4(int interval) {
        T4 = interval / baseTimerInterval;
        timerI = T4;
        timerK = T4;
    }

    /**
     * @see SIPTransaction#getBaseTimerInterval()
     */
    @Override
    public int getBaseTimerInterval() {
        return this.baseTimerInterval;
    }

    /**
     * @see SIPTransaction#getT4()
     */
    @Override
    public int getT4() {
        return this.T4;
    }

    /**
     * @see SIPTransaction#getT2()
     */
    @Override
    public int getT2() {
        return this.T2;
    }

    /**
     * @see SIPTransaction#getTimerI()
     */
    @Override
    public int getTimerI() {
        return this.timerI;
    }

    /**
     * @see SIPTransaction#getTimerK()
     */
    @Override
    public int getTimerK() {
        return this.timerK;
    }


    /**
     * @see SIPTransaction#setForkId(String)
     */
    @Override
    public void setForkId(String forkId) {
        this.forkId = forkId;
    }

    /**
     * @see SIPTransaction#getForkId()
     */
    @Override
    public String getForkId() {
        return forkId;
    }

    /**
     * @see SIPTransaction#scheduleMaxTxLifeTimeTimer()
     */
    @Override
    public void scheduleMaxTxLifeTimeTimer() {
        if (maxTxLifeTimeListener == null && this.getMethod().equalsIgnoreCase(Request.INVITE) && sipStack.getMaxTxLifetimeInvite() > 0) {
            if (log.isDebugEnabled()) {
                log.debug("Scheduling MaxTxLifeTimeListener for tx " + this + " , tx id " + this.getTransactionId() + " , state " + this.getState());
            }
            maxTxLifeTimeListener = new MaxTxLifeTimeListener();
            sipStack.getTimer().schedule(maxTxLifeTimeListener,
                    sipStack.getMaxTxLifetimeInvite() * 1000);
        }

        if (maxTxLifeTimeListener == null && !this.getMethod().equalsIgnoreCase(Request.INVITE) && sipStack.getMaxTxLifetimeNonInvite() > 0) {
            if (log.isDebugEnabled()) {
                log.debug("Scheduling MaxTxLifeTimeListener for tx " + this + " , tx id " + this.getTransactionId() + " , state " + this.getState());
            }
            maxTxLifeTimeListener = new MaxTxLifeTimeListener();
            sipStack.getTimer().schedule(maxTxLifeTimeListener,
                    sipStack.getMaxTxLifetimeNonInvite() * 1000);
        }
    }

    /**
     * @see SIPTransaction#cancelMaxTxLifeTimeTimer()
     */
    @Override
    public void cancelMaxTxLifeTimeTimer() {
        if (maxTxLifeTimeListener != null) {
            if (log.isDebugEnabled()) {
                log.debug("Cancelling MaxTxLifeTimeListener for tx " + this + " , tx id " + this.getTransactionId() + " , state " + this.getState());
            }
            sipStack.getTimer().cancel(maxTxLifeTimeListener);
            maxTxLifeTimeListener = null;
        }
    }

    /**
     * @see SIPTransaction#getMergeId()
     */
    @Override
    public String getMergeId() {
        if (mergeId == null) {
            return ((SIPRequest) getRequest()).getMergeId();
        }
        return mergeId;
    }

    /**
     * @see SIPTransaction#getAuditTag()
     */
    @Override
    public long getAuditTag() {
        return auditTag;
    }

    /**
     * @see SIPTransaction#setAuditTag(long)
     */
    @Override
    public void setAuditTag(long auditTag) {
        this.auditTag = auditTag;
    }

    /**
     * @see SIPServerTransaction#isTransactionMapped()
     */
    @Override
    public boolean isTransactionMapped() {
        return this.isMapped;
    }

    /**
     * @see SIPServerTransaction#setTransactionMapped(boolean)
     */
    @Override
    public void setTransactionMapped(boolean transactionMapped) {
        isMapped = transactionMapped;
    }

    /**
     * @see SIPTransaction#setCollectionTime(int)
     */
    @Override
    public void setCollectionTime(int collectionTime) {
        this.collectionTime = collectionTime;
    }
}
