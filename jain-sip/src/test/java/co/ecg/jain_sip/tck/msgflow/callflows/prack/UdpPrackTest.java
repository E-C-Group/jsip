package co.ecg.jain_sip.tck.msgflow.callflows.prack;

public class UdpPrackTest extends AbstractPrackTestCase {
    boolean myFlag;
    public void setUp() throws Exception {
        testedImplFlag = !myFlag;
        myFlag = !testedImplFlag;
        super.transport = "udp";
        super.setUp();
    }

    public void testPrack() {
        this.shootist.sendInvite();

    }

    public void testPrack2() {
        this.shootist.sendInvite();
    }
}
