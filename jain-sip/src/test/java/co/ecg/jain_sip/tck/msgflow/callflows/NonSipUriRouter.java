package co.ecg.jain_sip.tck.msgflow.callflows;

import java.util.LinkedList;
import java.util.ListIterator;

import co.ecg.jain_sip.sip.SipException;
import co.ecg.jain_sip.sip.SipStack;
import co.ecg.jain_sip.sip.address.Hop;
import co.ecg.jain_sip.sip.address.Router;
import co.ecg.jain_sip.sip.address.URI;
import co.ecg.jain_sip.sip.message.Request;

import junit.framework.TestCase;

/**
 * This is a Router for NON-SIP URI requests.
 * @author M. Ranganathan
 *
 */
public class NonSipUriRouter implements Router {

    int myPort = 5070;

    public static boolean routerWasConsulted = false;

    class HopImpl implements Hop {

        public String getHost() {

            return "127.0.0.1";
        }

        public int getPort() {

            return myPort;
        }

        public String getTransport() {

            return "udp";
        }

    }
    public NonSipUriRouter( SipStack sipStack, String outboundProxy) {


    }

    public void setMyPort ( int myPort ) {
        this.myPort = myPort;
    }

    public Hop getOutboundProxy() {

        return new HopImpl();
    }

    public ListIterator getNextHops(Request request) {
        URI uri = request.getRequestURI();
        ScenarioHarness.assertTrue("Expected to be consulted only for tel uri",uri.getScheme().equals("tel"));
        LinkedList llist = new LinkedList();
        llist.add(new HopImpl());
        return llist.listIterator();
    }

    public Hop getNextHop(Request request) throws SipException {
        routerWasConsulted = true;
        URI uri = request.getRequestURI();
        ScenarioHarness.assertTrue("expected to be consulted only for tel uri",uri.getScheme().equals("tel"));
        return new HopImpl();
    }

}
