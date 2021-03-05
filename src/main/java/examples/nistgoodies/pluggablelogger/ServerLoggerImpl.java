package examples.nistgoodies.pluggablelogger;

import java.util.Properties;

import co.ecg.jain_sip.sip.SipStack;
import co.ecg.jain_sip.sip.header.TimeStampHeader;

import examples.nistgoodies.configlogger.LogRecordFactoryImpl;

import co.ecg.jain_sip.core.ri.ServerLogger;
import co.ecg.jain_sip.sip.ri.LogRecord;
import co.ecg.jain_sip.sip.ri.LogRecordFactory;
import co.ecg.jain_sip.sip.ri.header.CallID;
import co.ecg.jain_sip.sip.ri.message.SIPMessage;
import co.ecg.jain_sip.sip.ri.stack.SIPTransactionStack;

public class ServerLoggerImpl implements ServerLogger {
   
    private SIPTransactionStack sipStack;

    private LogRecordFactory logRecordFactory;
    
    
    public ServerLoggerImpl() {
        this.logRecordFactory = new LogRecordFactoryImpl();
    }

    public void closeLogFile() {
    
    }

    public void logException(Exception exception) {
        sipStack.getStackLogger().logStackTrace();
    }

    public void logMessage(SIPMessage message, String source, String destination, boolean isSender, long timeStamp) {
        String firstLine = message.getFirstLine();
        String tid = message.getTransactionId();
        String callId = message.getCallId().getCallId();
        
        LogRecord logRecord = logRecordFactory.createLogRecord(message.encode(), source, destination, timeStamp, isSender, firstLine, tid, callId, 
                0);
        sipStack.getStackLogger().logInfo(logRecord.toString());
        
    }

    public void logMessage(SIPMessage message, String from, String to, String status, boolean sender) {
        logMessage(message, from, to, status, sender, System.currentTimeMillis());
    }

    public void logMessage(SIPMessage message, String source, String destination, String status, boolean isSender,
            long timeStamp) {
        // TODO Auto-generated method stub
        CallID cid = (CallID) message.getCallId();
        String callId = null;
        if (cid != null)
            callId = cid.getCallId();
        String firstLine = message.getFirstLine().trim();
        String tid = message.getTransactionId();
        TimeStampHeader tshdr = (TimeStampHeader) message.getHeader(TimeStampHeader.NAME);
        long tsval = tshdr == null ? 0 : tshdr.getTime();
        LogRecord logRecord = logRecordFactory.createLogRecord(message.encode(), source, destination, timeStamp, isSender, firstLine, tid, callId, 
                tsval);
        sipStack.getStackLogger().logInfo(logRecord.toString());
     
    }

    public void setSipStack(SipStack sipStack) {
        this.sipStack = (SIPTransactionStack) sipStack;
       
    }

    public void setStackProperties(Properties properties) {
       
    }
    
   
}
