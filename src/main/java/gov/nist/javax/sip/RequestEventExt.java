package gov.nist.javax.sip;

import co.ecg.jain_sip.sip.Dialog;
import co.ecg.jain_sip.sip.RequestEvent;
import co.ecg.jain_sip.sip.ServerTransaction;
import co.ecg.jain_sip.sip.message.Request;


/**
 * Extension of the RequestEvent.
 * 
 * 
 */


public class RequestEventExt extends RequestEvent {
    private String remoteIpAddress;
    
    private int    remotePort;
    
    public RequestEventExt(Object source, ServerTransaction serverTransaction, Dialog dialog, Request request) {
        super(source,serverTransaction,dialog,request);
    }

    public void setRemoteIpAddress(String remoteIpAddress) {
        this.remoteIpAddress = remoteIpAddress;
    }

    public String getRemoteIpAddress() {
        return remoteIpAddress;
    }

    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }

    public int getRemotePort() {
        return remotePort;
    }
}
