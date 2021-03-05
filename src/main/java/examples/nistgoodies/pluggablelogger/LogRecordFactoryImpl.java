package examples.nistgoodies.pluggablelogger;

import co.ecg.jain_sip.sip.ri.LogRecord;
import co.ecg.jain_sip.sip.ri.LogRecordFactory;

public class LogRecordFactoryImpl implements LogRecordFactory {

    public LogRecord createLogRecord(String message, String source,
            String destination, long timeStamp, boolean isSender,
            String firstLine, String tid, String callId, long timestampVal) {

        return new LogRecordImpl(message,source,destination,timeStamp,isSender,firstLine,tid,callId,timestampVal);
    }

}
