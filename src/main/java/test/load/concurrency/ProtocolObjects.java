
package test.load.concurrency;

import java.util.Properties;
import co.ecg.jain_sip.sip.SipException;
import co.ecg.jain_sip.sip.SipFactory;
import co.ecg.jain_sip.sip.SipStack;
import co.ecg.jain_sip.sip.address.AddressFactory;
import co.ecg.jain_sip.sip.header.HeaderFactory;
import co.ecg.jain_sip.sip.message.MessageFactory;

/**
 * @author M. Ranganathan
 *
 */
public class ProtocolObjects {
    static  AddressFactory addressFactory;

    static MessageFactory messageFactory;

    static HeaderFactory headerFactory;

    static SipStack sipStack;

    static int logLevel  = 0;

    public static String logFileDirectory = "";

    public static String transport = "tcp";



    static void init(String stackname, boolean autoDialog)
    {
        SipFactory sipFactory = null;

        sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");
        Properties properties = new Properties();
        properties.setProperty("javax.sip.STACK_NAME", stackname);

        // The following properties are specific to nist-sip
        // and are not necessarily part of any other co.ecg.jain-sip
        // implementation.
        properties.setProperty("gov.nist.javax.sip.DEBUG_LOG",
            logFileDirectory +  stackname + "debuglog.txt");
        properties.setProperty("gov.nist.javax.sip.SERVER_LOG",
                logFileDirectory + stackname + "log.txt");

        properties.setProperty("javax.sip.AUTOMATIC_DIALOG_SUPPORT",
                    (autoDialog? "on": "off"));

        properties.setProperty("gov.nist.javax.sip.THREAD_POOL_SIZE", "8");
        properties.setProperty("gov.nist.javax.sip.REENTRANT_LISTENER", "true");

        // Set to 0 in your production code for max speed.
        // You need 16 for logging traces. 32 for debug + traces.
        // Your code will limp at 32 but it is best for debugging.
        properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", new Integer(logLevel).toString());

        try {
            // Create SipStack object
            sipStack = sipFactory.createSipStack(properties);

            System.out.println("createSipStack " + sipStack);
        } catch (Exception e) {
            // could not find
            // gov.nist.jain.protocol.ip.sip.SipStackImpl
            // in the classpath
            e.printStackTrace();
            System.err.println(e.getMessage());
            throw new RuntimeException("Stack failed to initialize");
        }

        try {
            headerFactory = sipFactory.createHeaderFactory();
            addressFactory = sipFactory.createAddressFactory();
            messageFactory = sipFactory.createMessageFactory();
        } catch (SipException ex) {
            ex.printStackTrace();
            throw new RuntimeException ( ex);
        }
    }

    public static void destroy() {
        sipStack.stop();
    }

    public static void start() throws Exception  {
        sipStack.start();

    }
}
