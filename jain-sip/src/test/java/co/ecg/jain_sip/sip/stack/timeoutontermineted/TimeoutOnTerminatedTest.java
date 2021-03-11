/**
 *
 */
package co.ecg.jain_sip.sip.stack.timeoutontermineted;

import co.ecg.jain_sip.sip.SipProvider;
import junit.framework.TestCase;

import lombok.extern.slf4j.Slf4j;


/**
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 *
 */
@Slf4j
public class TimeoutOnTerminatedTest extends TestCase {

    protected Shootist shootist;

    protected Shootme shootme;

    public TimeoutOnTerminatedTest() {

        super("timeoutontermineted");

    }

    @Override
    public void setUp() {

        try {
            super.setUp();
            shootist = new Shootist(5060, 5080);
            SipProvider shootistProvider = shootist.createSipProvider();
            shootistProvider.addSipListener(shootist);

            shootme = new Shootme(5080, 1000);
            SipProvider shootmeProvider = shootme.createProvider();
            shootmeProvider.addSipListener(shootme);

            log.debug("setup completed");

        } catch (Exception ex) {
            fail("unexpected exception ");
        }
    }

    @Override
    public void tearDown() {
        try {
            Thread.sleep(60000);

            this.shootist.checkState();

            this.shootme.checkState();

            this.shootist.stop();

            this.shootme.stop();

        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("unexpected exception", ex);
            fail("unexpected exception ");
        }
    }

    public void testInvite() throws Exception {
        this.shootist.sendInvite();

    }

}
