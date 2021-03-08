/**
 *
 */
package co.ecg.jain_sip.tck.msgflow.callflows.forkedinvite;

import java.util.Hashtable;

import co.ecg.jain_sip.sip.SipListener;
import co.ecg.jain_sip.sip.SipProvider;

import co.ecg.jain_sip.tck.msgflow.callflows.ScenarioHarness;

import lombok.extern.slf4j.Slf4j;

/**
 * @author M. Ranganathan
 *
 */
@Slf4j
public class AbstractForkedInviteTestCase extends ScenarioHarness implements
        SipListener {

    protected Shootist shootist;

    protected Shootme shootme;

    private Shootme shootme2;


    // private Appender appender;

    public AbstractForkedInviteTestCase() {

        super("forkedInviteTest", true);

        try {
            providerTable = new Hashtable();

        } catch (Exception ex) {
            log.error("unexpected exception", ex);
            fail("unexpected exception ");
        }
    }

    public void setUp() {

        try {
            super.setUp(false, 1, 3);
            shootist = new Shootist(5060, 5070, super.getTiProtocolObjects(0));
            SipProvider shootistProvider = shootist.createSipProvider();
            providerTable.put(shootistProvider, shootist);

            this.shootme = new Shootme(5080, getTiProtocolObjects(1));
            SipProvider shootmeProvider = shootme.createProvider();
            providerTable.put(shootmeProvider, shootme);
            shootistProvider.addSipListener(this);
            shootmeProvider.addSipListener(this);

            this.shootme2 = new Shootme(5090, getTiProtocolObjects(2));
            shootmeProvider = shootme2.createProvider();
            providerTable.put(shootmeProvider, shootme2);
            shootistProvider.addSipListener(this);
            shootmeProvider.addSipListener(this);

            Proxy proxy = new Proxy(5070, getRiProtocolObjects(0));
            SipProvider provider = proxy.createSipProvider();
            provider.setAutomaticDialogSupportEnabled(false);
            providerTable.put(provider, proxy);
            provider.addSipListener(this);

            super.start();
        } catch (Exception ex) {
            System.out.println(ex.toString());
            fail("unexpected exception ");
        }
    }


    public void tearDown() {
        try {
            Thread.sleep(8000);
            this.shootist.checkState();
            this.shootme.checkState();
            this.shootme2.checkState();
            super.tearDown();
            Thread.sleep(2000);
            this.providerTable.clear();

            super.logTestCompleted();
        } catch (Exception ex) {
            log.error("unexpected exception", ex);
            fail("unexpected exception ");
        }
    }


}
