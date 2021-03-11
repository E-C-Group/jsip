package co.ecg.jain_sip.sip.stack.tls;

import co.ecg.jain_sip.sip.SipException;
import co.ecg.jain_sip.sip.SipStack;
import co.ecg.jain_sip.sip.address.Hop;
import co.ecg.jain_sip.sip.address.Router;
import co.ecg.jain_sip.sip.message.Request;

import java.util.*;

public class MyRouter implements Router {
    protected SipStack myStack;
    protected HopImpl defaultRoute;

    public MyRouter(SipStack sipStack, String nextHop) {

        this.myStack = sipStack;
        this.defaultRoute = new HopImpl(nextHop);
    }

    /**
     * Always send requests to the default route location.
     */
    public ListIterator getNextHops(Request sipRequest) {
        LinkedList ll = null;
        if (defaultRoute != null) {
            if (ll == null)
                ll = new LinkedList();
            ll.add(defaultRoute);
            return ll.listIterator();
        } else
            return null;
    }

    public Hop getOutboundProxy() {
        return this.defaultRoute;
    }

    public Hop getNextHop(Request request) throws SipException {
        return this.defaultRoute;
    }

}
