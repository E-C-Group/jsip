package co.ecg.jain_sip.tck.msgflow.callflows.prack;

import co.ecg.jain_sip.sip.SipListener;
import co.ecg.jain_sip.sip.SipProvider;

import lombok.extern.slf4j.Slf4j;

import co.ecg.jain_sip.tck.msgflow.callflows.ScenarioHarness;

/**
 *
 * Implements common setup and tearDown sequence for PRACK tests
 *
 * @author M. Ranganathan
 * @author Ivelin Ivanov
 *
 */
@Slf4j
public abstract class AbstractPrackTestCase extends ScenarioHarness implements
        SipListener {


    protected Shootist shootist;

    protected Shootme shootme;

    public AbstractPrackTestCase() {
        super("prack", true);
    }

    public void setUp() throws Exception {
        try {
            super.setUp();

            log.info("PrackTest: setup()");
            shootist = new Shootist(getTiProtocolObjects());
            SipProvider shootistProvider = shootist.createProvider();
            providerTable.put(shootistProvider, shootist);

            shootme = new Shootme(getRiProtocolObjects());
            SipProvider shootmeProvider = shootme.createProvider();
            providerTable.put(shootmeProvider, shootme);

            shootistProvider.addSipListener(this);
            shootmeProvider.addSipListener(this);

            if (getTiProtocolObjects() != getRiProtocolObjects())
                getTiProtocolObjects().start();
            getRiProtocolObjects().start();
        } catch (Exception ex) {
            log.error("unexpected excecption ", ex);
            fail("unexpected exception");
        }
    }

    public void tearDown() throws Exception {
        try {
            Thread.sleep(2000);
            this.shootist.checkState();
            this.shootme.checkState();
            super.tearDown();
            Thread.sleep(1000);
            this.providerTable.clear();

            logTestCompleted();
        } catch (Exception ex) {
            log.error("unexpected exception", ex);
            fail("unexpected exception ");
        }
        super.tearDown();
    }




}
