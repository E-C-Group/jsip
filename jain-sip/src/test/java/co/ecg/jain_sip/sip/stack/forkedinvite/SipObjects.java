package co.ecg.jain_sip.sip.stack.forkedinvite;

import co.ecg.jain_sip.sip.SipException;
import co.ecg.jain_sip.sip.SipFactory;
import co.ecg.jain_sip.sip.SipStack;
import co.ecg.jain_sip.sip.address.AddressFactory;
import co.ecg.jain_sip.sip.header.HeaderFactory;
import co.ecg.jain_sip.sip.message.MessageFactory;
import co.ecg.jain_sip.sip.ri.stack.NioMessageProcessorFactory;

import java.util.Properties;


public class SipObjects {
    SipStack sipStack;
    HeaderFactory headerFactory;
    AddressFactory addressFactory;
    MessageFactory messageFactory;

    public SipObjects(int myPort, String stackName, String automaticDialog) {
        SipFactory sipFactory = SipFactory.getInstance();
        sipFactory.resetFactory();
        sipFactory.setPathName("co.ecg.jain_sip");
        Properties properties = new Properties();
        String stackname = stackName + myPort;
        properties.setProperty("javax.sip.STACK_NAME", stackname);

        // The following properties are specific to nist-sip
        // and are not necessarily part of any other jain-sip
        // implementation.

        properties.setProperty("javax.sip.AUTOMATIC_DIALOG_SUPPORT", automaticDialog);

       /* properties.setProperty("gov.nist.javax.sip.LOG_FACTORY", SipFoundryLogRecordFactory.class
                .getName()); */

        properties.setProperty("gov.nist.javax.sip.MAX_FORK_TIME_SECONDS", "12");

        if (System.getProperty("enableNIO") != null && System.getProperty("enableNIO").equalsIgnoreCase("true")) {
            properties.setProperty("gov.nist.javax.sip.MESSAGE_PROCESSOR_FACTORY", NioMessageProcessorFactory.class.getName());
        }
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
            throw new RuntimeException(ex);
        }
    }
}
