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
/**
 *
 */
package co.ecg.jain_sip.tck.msgflow.callflows.sctp;

import java.util.EventObject;

import co.ecg.jain_sip.sip.DialogTerminatedEvent;
import co.ecg.jain_sip.sip.IOExceptionEvent;
import co.ecg.jain_sip.sip.RequestEvent;
import co.ecg.jain_sip.sip.ResponseEvent;
import co.ecg.jain_sip.sip.SipListener;
import co.ecg.jain_sip.sip.SipProvider;
import co.ecg.jain_sip.sip.TimeoutEvent;
import co.ecg.jain_sip.sip.TransactionTerminatedEvent;

import co.ecg.jain_sip.tck.msgflow.callflows.ScenarioHarness;
import lombok.extern.slf4j.Slf4j;

/**
 * @author M. Ranganathan
 *
 * JvB: copied from TLS test
 * Note: SCTP only works with Java7 on Linux
 */
@Slf4j
public class SctpTest extends ScenarioHarness implements SipListener {


    protected Shootist shootist;

    private Shootme shootme;


    private SipListener getSipListener(EventObject sipEvent) {
        SipProvider source = (SipProvider) sipEvent.getSource();
        SipListener listener = (SipListener) providerTable.get(source);
        assertTrue(listener != null);
        return listener;
    }

    public SctpTest() {
        super("SctpTest", true);
    }

    public void setUp() {

        try {
            this.transport = "sctp";

            super.setUp();
            shootist = new Shootist(getRiProtocolObjects());
            SipProvider shootistProvider = shootist.createSipProvider();
            providerTable.put(shootistProvider, shootist);

            shootme = new Shootme(getTiProtocolObjects());
            SipProvider shootmeProvider = shootme.createSipProvider();
            providerTable.put(shootmeProvider, shootme);
            shootistProvider.addSipListener(this);
            shootmeProvider.addSipListener(this);

            getRiProtocolObjects().start();
            if (getTiProtocolObjects() != getRiProtocolObjects())
                getTiProtocolObjects().start();
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("unexpected exception ");
        }
    }

    public void testSendInvite() {
        this.shootist.sendInvite();
    }

    public void tearDown() {
        try {
            Thread.sleep(4000); // needs some time
            this.shootist.checkState();
            this.shootme.checkState();
            getTiProtocolObjects().destroy();
            if (getTiProtocolObjects() != getRiProtocolObjects())
                getRiProtocolObjects().destroy();
            Thread.sleep(1000);
            this.providerTable.clear();

            logTestCompleted();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void processRequest(RequestEvent requestEvent) {
        getSipListener(requestEvent).processRequest(requestEvent);

    }

    public void processResponse(ResponseEvent responseEvent) {
        getSipListener(responseEvent).processResponse(responseEvent);

    }

    public void processTimeout(TimeoutEvent timeoutEvent) {
        getSipListener(timeoutEvent).processTimeout(timeoutEvent);
    }

    public void processIOException(IOExceptionEvent exceptionEvent) {
        fail("unexpected exception");

    }

    public void processTransactionTerminated(
            TransactionTerminatedEvent transactionTerminatedEvent) {
        getSipListener(transactionTerminatedEvent)
                .processTransactionTerminated(transactionTerminatedEvent);

    }

    public void processDialogTerminated(
            DialogTerminatedEvent dialogTerminatedEvent) {
        getSipListener(dialogTerminatedEvent).processDialogTerminated(
                dialogTerminatedEvent);

    }

}
