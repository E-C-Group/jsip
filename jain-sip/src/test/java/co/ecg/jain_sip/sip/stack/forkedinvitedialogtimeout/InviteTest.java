/**
 *
 */
package co.ecg.jain_sip.sip.stack.forkedinvitedialogtimeout;

import java.util.HashSet;

import co.ecg.jain_sip.sip.SipProvider;
import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;

/**
 * @author M. Ranganathan
 *
 */
@Slf4j
public class InviteTest extends TestCase {

    protected Shootist shootist;

    private static int forkCount = 2;

    protected HashSet<Shootme> shootme = new HashSet<Shootme>();

    private Proxy proxy;

    // private Appender appender;

    public InviteTest() {

        super("forkedInviteTest");

    }

    public void setUp() {

        try {
            super.setUp();


        } catch (Exception ex) {
            fail("unexpected exception ");
        }
    }

    public void tearDown() {
        try {

        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("unexpected exception", ex);
            fail("unexpected exception ");
        }
    }

    public void testSendInviteDialogTimeoutEventSeen() throws Exception {
        try {
            shootist = new Shootist(6050, 5070, "on", true);
            SipProvider shootistProvider = shootist.createSipProvider();
            shootistProvider.addSipListener(shootist);

            Shootme shootmeUa = new Shootme(5080, true, 4000, 4000);
            SipProvider shootmeProvider = shootmeUa.createProvider();
            shootmeProvider.addSipListener(shootmeUa);
            this.shootme.add(shootmeUa);

            shootmeUa = new Shootme(5081, true, 5000, 4000);
            shootmeProvider = shootmeUa.createProvider();
            shootmeProvider.addSipListener(shootmeUa);
            this.shootme.add(shootmeUa);

            this.proxy = new Proxy(5070, forkCount);
            SipProvider provider = proxy.createSipProvider();
            provider.addSipListener(proxy);
            log.debug("setup completed");

            this.shootist.sendInvite(forkCount);

            Thread.sleep(69000);
            this.shootist.checkStateForDialogTimeoutEvent();
            int ackCount = 0;
            for (Shootme shootme : this.shootme) {
                shootme.checkState();
                if (shootme.isAckSeen()) {
                    ackCount++;
                }
            }
            assertEquals("ACK count must be exactly 1", 1, ackCount);
        } finally {
            this.shootist.stop();
            for (Shootme shootme : this.shootme) {
                shootme.stop();
            }
            if (this.proxy != null) {
                this.proxy.stop();
            }
        }
    }


    public void testSendInviteEarlyDialogTimeoutEventSeen() throws Exception {
        try {
            shootist = new Shootist(6050, 5070, "on", true);
            SipProvider shootistProvider = shootist.createSipProvider();
            shootistProvider.addSipListener(shootist);

            Shootme shootmeUa = new Shootme(5080, true, 4000, 4000);
            SipProvider shootmeProvider = shootmeUa.createProvider();
            shootmeProvider.addSipListener(shootmeUa);
            this.shootme.add(shootmeUa);

            // Never send an OK. Just send RINGING.
            shootmeUa = new Shootme(5081, true, 5000, -1);
            shootmeProvider = shootmeUa.createProvider();
            shootmeProvider.addSipListener(shootmeUa);
            this.shootme.add(shootmeUa);

            this.proxy = new Proxy(5070, forkCount);
            SipProvider provider = proxy.createSipProvider();
            provider.addSipListener(proxy);
            log.debug("setup completed");

            this.shootist.sendInvite(forkCount);

            Thread.sleep(185 * 1000);
            this.shootist.checkStateForDialogTimeoutEvent();
            int ackCount = 0;
            for (Shootme shootme : this.shootme) {
                shootme.checkState();
                if (shootme.isAckSeen()) {
                    ackCount++;
                }
            }
            assertEquals("ACK count must be exactly 1", 1, ackCount);
        } finally {
            this.shootist.stop();
            for (Shootme shootme : this.shootme) {
                shootme.stop();
            }
            if (this.proxy != null) {
                this.proxy.stop();
            }
        }
    }
}
