/*
 * Conditions Of Use
 *
 * This software was developed by employees of the National Institute of
 * Standards and Technology (NIST), and others.
 * This software is has been contributed to the public domain.
 * As a result, a formal license is not needed to use the software.
 *
 * This software is provided "AS IS."
 * NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
 * OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
 * AND DATA ACCURACY.  NIST does not warrant or make any representations
 * regarding the use of the software or the results thereof, including but
 * not limited to the correctness, accuracy, reliability or usefulness of
 * the software.
 *
 *
 */
package co.ecg.jain_sip.tck.msgflow;

import java.util.TooManyListenersException;

import co.ecg.jain_sip.sip.ClientTransaction;
import co.ecg.jain_sip.sip.RequestEvent;
import co.ecg.jain_sip.sip.ServerTransaction;
import co.ecg.jain_sip.sip.SipException;
import co.ecg.jain_sip.sip.Timeout;
import co.ecg.jain_sip.sip.TimeoutEvent;
import co.ecg.jain_sip.sip.header.ContactHeader;
import co.ecg.jain_sip.sip.header.ToHeader;
import co.ecg.jain_sip.sip.message.Request;
import co.ecg.jain_sip.sip.message.Response;

import co.ecg.jain_sip.tck.TckInternalError;
import co.ecg.jain_sip.tck.TiUnexpectedError;
import lombok.extern.slf4j.Slf4j;

/**
 * Timeout test for invite client transactions -- test to see if an timeout
 * event is delivered to the listener if the RI refuses to send OK to the INVITE
 * Client Tx.
 *
 * @author M. Ranganathan
 */
@Slf4j
public class TransactionTimeoutEventTest extends MessageFlowHarness {

    public TransactionTimeoutEventTest(String name) {
        super(name, false); // disable auto-dialog for the RI, else ACKs get
        // filtered out
    }

    // ==================== tests ==============================

    /**
     * Test if the tx timeout is delivered.
     */
    public void testClientTransactionTimeout() {
        try {
            Request invite = createTiInviteRequest(null, null, null);
            ClientTransaction tran = null;
            try {
                eventCollector.collectRequestEvent(riSipProvider);

                tran = tiSipProvider.getNewClientTransaction(invite);
                tran.sendRequest();
            } catch (SipException ex) {
                throw new TiUnexpectedError(
                        "A SipExceptionOccurred while trying to send request!",
                        ex);
            } catch (TooManyListenersException ex) {
                throw new TckInternalError(
                        "Failed to regiest a SipListener with an RI SipProvider",
                        ex);
            }
            waitForMessage();
            RequestEvent inviteReceivedEvent = eventCollector
                    .extractCollectedRequestEvent();
            assertNotNull("RequestEvent", inviteReceivedEvent);
            try {
                eventCollector.collectTimeoutEvent(tiSipProvider);
            } catch (TooManyListenersException ex) {
                throw new TiUnexpectedError(
                        "Failed to register a SipListener with TI", ex);
            }
            waitForTimeout();
            TimeoutEvent timeoutEvent = eventCollector
                    .extractCollectedTimeoutEvent();
            assertNotNull("Timeout event", timeoutEvent);
            assertTrue("Timeout event type ", timeoutEvent.getTimeout().equals(
                    Timeout.TRANSACTION));
        } catch (Exception ex) {
            log.error("unexpected exception ", ex);
            ex.printStackTrace();
            fail("unexpected exception");
        }
    }


    public void testServerTransactionForTimeout() {
        try {
            Request invite = createRiInviteRequest(null, null, null);
            ClientTransaction tran = null;
            try {
                eventCollector.collectRequestEvent(tiSipProvider);
                tran = riSipProvider.getNewClientTransaction(invite);
                tran.sendRequest();
            } catch (SipException ex) {
                throw new TiUnexpectedError(
                        "A SipExceptionOccurred while trying to send request!",
                        ex);
            } catch (TooManyListenersException ex) {
                throw new TckInternalError(
                        "Failed to regiest a SipListener with an RI SipProvider",
                        ex);
            }
            waitForMessage();
            RequestEvent inviteReceivedEvent = eventCollector
                    .extractCollectedRequestEvent();
            assertNotNull("RequestEvent not seen at TI", inviteReceivedEvent);
            assertTrue("Server Transaction MUST be null", inviteReceivedEvent
                    .getServerTransaction() == null);
            ServerTransaction st = tiSipProvider
                    .getNewServerTransaction(inviteReceivedEvent.getRequest());
            Response response = tiMessageFactory.createResponse(Response.OK,
                    inviteReceivedEvent.getRequest());
            ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
            toHeader.setTag("123456");
            ContactHeader contact = super.createTiContact();
            response.setHeader(contact);
            st.sendResponse(response);

            eventCollector.collectTimeoutEvent(tiSipProvider);
            waitForTimeout();
            TimeoutEvent timeoutEvent = eventCollector
                    .extractCollectedTimeoutEvent();
            assertNull("Timeout event", timeoutEvent);
            //assertNotNull("Timeout event", timeoutEvent);
            //assertTrue("Timeout event type must be TRANSACTION ", timeoutEvent
            //.getTimeout().equals(Timeout.TRANSACTION));

        } catch (Exception ex) {
            log.error("unexpected exception ", ex);
            ex.printStackTrace();
            fail("unexpected exception");
        }
    }


    public void testServerTransactionForRetransmissionAlerts() {
        try {
            Request invite = createRiInviteRequest(null, null, null);
            ClientTransaction tran = null;
            tiSipProvider.setAutomaticDialogSupportEnabled(false);
            try {
                eventCollector.collectRequestEvent(tiSipProvider);
                tran = riSipProvider.getNewClientTransaction(invite);
                tran.sendRequest();
            } catch (SipException ex) {
                throw new TiUnexpectedError(
                        "A SipExceptionOccurred while trying to send request!",
                        ex);
            } catch (TooManyListenersException ex) {
                throw new TckInternalError(
                        "Failed to regiest a SipListener with an RI SipProvider",
                        ex);
            }
            waitForMessage();
            RequestEvent inviteReceivedEvent = eventCollector
                    .extractCollectedRequestEvent();
            assertNotNull("RequestEvent not seen at TI", inviteReceivedEvent);
            assertTrue("Server Transaction MUST be null", inviteReceivedEvent
                    .getServerTransaction() == null);
            ServerTransaction st = tiSipProvider
                    .getNewServerTransaction(inviteReceivedEvent.getRequest());
            Response response = tiMessageFactory.createResponse(Response.OK,
                    inviteReceivedEvent.getRequest());
            ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
            toHeader.setTag("1234567");
            ContactHeader contact = super.createTiContact();
            response.setHeader(contact);
            st.enableRetransmissionAlerts();
            st.sendResponse(response);
            eventCollector.collectTimeoutEvent(tiSipProvider);
            waitForTimeout();
            TimeoutEvent timeoutEvent = eventCollector
                    .extractCollectedTimeoutEvent();
            assertNotNull("Timeout event not found ", timeoutEvent);
            assertTrue("Timeout event type must be retransmit ", timeoutEvent
                    .getTimeout().equals(Timeout.RETRANSMIT));

        } catch (Exception ex) {
            log.error("unexpected exception ", ex);
            ex.printStackTrace();
            fail("unexpected exception");
        }
    }
}
