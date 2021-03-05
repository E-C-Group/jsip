package test.unit.gov.nist.javax.sip.stack.uasreinvite;

import co.ecg.jain_sip.sip.ri.SipStackImpl;

import java.util.EventObject;

import co.ecg.jain_sip.sip.DialogTerminatedEvent;
import co.ecg.jain_sip.sip.IOExceptionEvent;
import co.ecg.jain_sip.sip.RequestEvent;
import co.ecg.jain_sip.sip.ResponseEvent;
import co.ecg.jain_sip.sip.SipListener;
import co.ecg.jain_sip.sip.SipProvider;
import co.ecg.jain_sip.sip.TimeoutEvent;
import co.ecg.jain_sip.sip.TransactionTerminatedEvent;

import org.apache.log4j.Logger;

import test.tck.msgflow.callflows.ScenarioHarness;

public class ReInviteAllowInterleavingTest extends ScenarioHarness implements SipListener {

    protected Shootist shootist;

    private Shootme shootme;

    private static Logger logger = Logger.getLogger("test.tck");

    static {
        if (!logger.isAttached(console))
            logger.addAppender(console);
    }

    private SipListener getSipListener(EventObject sipEvent) {
        SipProvider source = (SipProvider) sipEvent.getSource();
        SipListener listener = (SipListener) providerTable.get(source);
        assertTrue(listener != null);
        return listener;
    }

    public ReInviteAllowInterleavingTest() {
        super("reinvitetest", true);
    }

    public void setUp() {

        try {
            this.transport = "udp";

            super.setUp();
            
            shootist = new Shootist(getRiProtocolObjects());
            
            SipProvider shootistProvider = shootist.createSipProvider();
            providerTable.put(shootistProvider, shootist);

            shootme = new Shootme(getTiProtocolObjects());
            SipProvider shootmeProvider = shootme.createSipProvider();
            providerTable.put(shootmeProvider, shootme);
            shootistProvider.addSipListener(this);
            shootmeProvider.addSipListener(this);
            ((SipStackImpl)getTiProtocolObjects().sipStack).setIsBackToBackUserAgent(false);
            ((SipStackImpl)getRiProtocolObjects().sipStack).setIsBackToBackUserAgent(false);

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
            Thread.sleep(4000);
            this.shootist.checkState();
            this.shootme.checkState();
            super.tearDown();
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
