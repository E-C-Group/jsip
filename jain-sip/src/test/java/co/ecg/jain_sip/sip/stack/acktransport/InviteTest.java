/**
 *
 */
package co.ecg.jain_sip.sip.stack.acktransport;

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


    protected HashSet<Shootme> shootme = new HashSet<Shootme>();


    private Proxy proxy;

    // private Appender appender;

    public InviteTest() {

        super("");

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

    public void testSendInvite() throws Exception {
        try {

            Shootme shootmeUa = new Shootme(5080, true, 4000, 4000);
            SipProvider shootmeProvider = shootmeUa.createProvider();
            shootmeProvider.addSipListener(shootmeUa);


            this.proxy = new Proxy(5070);
            SipProvider provider = proxy.createSipProvider("tcp");
            provider.addSipListener(proxy);
            provider = proxy.createSipProvider("udp");
            provider.addSipListener(proxy);


            Shootist shootist = new Shootist(6050, 5070);
            SipProvider shootistProvider = shootist.createSipProvider();
            shootistProvider.addSipListener(shootist);
            //  shootistProvider = shootist.createSipProvider("udp");
            //  shootistProvider.addSipListener(shootist);

            shootist.sendInvite(1);

            Thread.sleep(30000);

            shootmeUa.checkState();
            shootist.checkState();
            shootist.stop();
            shootmeUa.stop();
            proxy.stop();
        } finally {

        }
    }

}
