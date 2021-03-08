package co.ecg.jain_sip.tck.msgflow.callflows.redirect;

import co.ecg.jain_sip.sip.SipListener;
import co.ecg.jain_sip.sip.SipProvider;


import co.ecg.jain_sip.tck.msgflow.callflows.ScenarioHarness;
import lombok.extern.slf4j.Slf4j;

/**
 * @author M. Ranganathan
 */
@Slf4j
public abstract class AbstractRedirectTestCase extends ScenarioHarness implements
        SipListener {


    protected Shootist shootist;
    protected Shootme shootme;


    // private Appender appender;

    public AbstractRedirectTestCase() {

        super("redirect", true);


    }

    public void setUp() {
        try {
            super.setUp();

            log.info("RedirectTest: setup()");
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
            log.error("unexpected exception", ex);
            fail("unexpected exception ");
        }

    }


}
