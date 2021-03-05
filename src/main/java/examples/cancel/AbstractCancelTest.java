/**
 *
 */
package examples.cancel;

import java.util.EventObject;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import co.ecg.jain_sip.sip.DialogTerminatedEvent;
import co.ecg.jain_sip.sip.IOExceptionEvent;
import co.ecg.jain_sip.sip.RequestEvent;
import co.ecg.jain_sip.sip.ResponseEvent;
import co.ecg.jain_sip.sip.SipListener;
import co.ecg.jain_sip.sip.SipProvider;
import co.ecg.jain_sip.sip.TimeoutEvent;
import co.ecg.jain_sip.sip.TransactionTerminatedEvent;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.helpers.NullEnumeration;

import junit.framework.TestCase;

/**
 * @author M. Ranganathan
 *
 */
public abstract class AbstractCancelTest extends TestCase implements SipListener {

    private Hashtable providerTable;

    protected Shootist shootist;

    private static Logger logger = Logger.getLogger(AbstractCancelTest.class);

    static {
        if (logger.getAllAppenders() instanceof NullEnumeration )
            PropertyConfigurator.configure("log4j.properties");



    }

    //private Appender appender;

    private SipListener getSipListener(EventObject sipEvent) {
        SipProvider source = (SipProvider) sipEvent.getSource();
        SipListener listener = (SipListener) providerTable.get(source);
        assertTrue(listener != null);
        return listener;
    }

    public AbstractCancelTest() {

        try {
            ProtocolObjects.logFileDirectory = "logs/";
            ProtocolObjects.init("canceltest");
            providerTable = new Hashtable();
            shootist = new Shootist();
            SipProvider shootistProvider = shootist.createSipProvider();
            providerTable.put(shootistProvider, shootist);
            Shootme shootme = new Shootme();
            SipProvider shootmeProvider = shootme.createProvider();
            providerTable.put(shootmeProvider, shootme);
            shootistProvider.addSipListener(this);
            shootmeProvider.addSipListener(this);
            ProtocolObjects.start();
        } catch (Exception ex) {
            fail("unexpected exception ");
        }
    }

    public void setUp() {

            try {
                //appender = new ConsoleAppender(new SimpleLayout());
                //logger.addAppender(appender);

            } catch (Exception ex) {
                throw new RuntimeException("Unexpected error initializing logging",
                        ex);
            }


    }

    public void tearDown() {

        ProtocolObjects.destroy();
        //logger.removeAppender(appender);

    }





    public void processRequest(RequestEvent requestEvent) {
        getSipListener(requestEvent).processRequest(requestEvent);

    }

    public void processResponse(ResponseEvent responseEvent) {
        getSipListener(responseEvent).processResponse(responseEvent);

    }

    public void processTimeout(TimeoutEvent timeoutEvent) {
        getSipListener(timeoutEvent).processTimeout(timeoutEvent);
    }

    public void processIOException(IOExceptionEvent exceptionEvent) {
        fail("unexpected exception");

    }

    public void processTransactionTerminated(
            TransactionTerminatedEvent transactionTerminatedEvent) {
        getSipListener(transactionTerminatedEvent)
                .processTransactionTerminated(transactionTerminatedEvent);

    }

    public void processDialogTerminated(
            DialogTerminatedEvent dialogTerminatedEvent) {
        getSipListener(dialogTerminatedEvent).processDialogTerminated(
                dialogTerminatedEvent);

    }

}
