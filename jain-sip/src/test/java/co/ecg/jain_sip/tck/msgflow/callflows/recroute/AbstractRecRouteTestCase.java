package co.ecg.jain_sip.tck.msgflow.callflows.recroute;

import java.util.Hashtable;

import co.ecg.jain_sip.sip.SipListener;
import co.ecg.jain_sip.sip.SipProvider;


import co.ecg.jain_sip.tck.msgflow.callflows.ScenarioHarness;
import lombok.extern.slf4j.Slf4j;

/**
 * @author M. Ranganathan
 * @author Jeroen van Bemmel
 */
@Slf4j
public class AbstractRecRouteTestCase extends ScenarioHarness implements
        SipListener {


    protected Shootist shootist;


    protected Shootme shootme;

    private Proxy proxy;


    // private Appender appender;

    public AbstractRecRouteTestCase() {

        super("TCPRecRouteTest", true);

        try {
            providerTable = new Hashtable();

        } catch (Exception ex) {
            log.error("unexpected exception", ex);
            fail("unexpected exception ");
        }
    }

    public void setUp() {

        try {
            super.setUp(false);
            shootist = new Shootist(5060, 5070, getTiProtocolObjects());
            SipProvider shootistProvider = shootist.createSipProvider();
            providerTable.put(shootistProvider, shootist);
            shootistProvider.addSipListener(this);

            this.shootme = new Shootme(5080, getTiProtocolObjects());
            SipProvider shootmeProvider = shootme.createProvider();
            providerTable.put(shootmeProvider, shootme);
            shootmeProvider.addSipListener(this);

            this.proxy = new Proxy(5070, getRiProtocolObjects());
            SipProvider provider = proxy.createSipProvider();
            //provider.setAutomaticDialogSupportEnabled(false);
            providerTable.put(provider, proxy);
            provider.addSipListener(this);

            getTiProtocolObjects().start();
            if (getTiProtocolObjects() != getRiProtocolObjects())
                getRiProtocolObjects().start();
        } catch (Exception ex) {
            log.error("Unexpected exception", ex);
            fail("unexpected exception ");
        }
    }


    public void tearDown() {
        try {
            Thread.sleep(5000);
            this.shootist.checkState();
            this.shootme.checkState();
            this.proxy.checkState();
            super.tearDown();
            Thread.sleep(4000);
            this.providerTable.clear();

            super.logTestCompleted();
        } catch (Exception ex) {
            log.error("unexpected exception", ex);
            fail("unexpected exception ");
        }
    }


}
