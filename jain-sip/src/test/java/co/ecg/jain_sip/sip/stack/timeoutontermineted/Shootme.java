package co.ecg.jain_sip.sip.stack.timeoutontermineted;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import co.ecg.jain_sip.sip.*;
import co.ecg.jain_sip.sip.address.Address;
import co.ecg.jain_sip.sip.address.AddressFactory;
import co.ecg.jain_sip.sip.header.ContactHeader;
import co.ecg.jain_sip.sip.header.HeaderFactory;
import co.ecg.jain_sip.sip.header.ToHeader;
import co.ecg.jain_sip.sip.message.MessageFactory;
import co.ecg.jain_sip.sip.message.Request;
import co.ecg.jain_sip.sip.message.Response;
import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;


/**
 * This class is a UAC template. Shootist is the guy that shoots and shootme is
 * the guy that gets shot.
 *
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 */
@Slf4j
public class Shootme implements SipListener {

    private static final String myAddress = "127.0.0.1";


    private SipProvider sipProvider;

    private final int myPort;

    private static String unexpectedException = "Unexpected exception ";

    private boolean inviteSeen;

    private final SipStack sipStack;

    private static AddressFactory addressFactory;

    private static MessageFactory messageFactory;

    private static HeaderFactory headerFactory;

    private static final String transport = "udp";

    private static Timer timer = new Timer();
    private boolean seen_txTerm, seen_txTimeout, seen_dte;

    private ServerTransaction inviteTid;

    private Dialog inviteDialog;

    private final int delay;

    class MyTimerTask extends TimerTask {
        RequestEvent requestEvent;
        String toTag;
        ServerTransaction serverTx;

        public MyTimerTask(RequestEvent requestEvent, ServerTransaction tx, String toTag) {
            log.info("MyTimerTask ");
            this.requestEvent = requestEvent;
            this.toTag = toTag;
            this.serverTx = tx;

        }

        @Override
        public void run() {
            sendInviteOK(requestEvent, serverTx, toTag);
        }

    }

    public void processRequest(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        ServerTransaction serverTransactionId = requestEvent.getServerTransaction();

        log.info("\n\nRequest " + request.getMethod() + " received at " + sipStack.getStackName() + " with server transaction id " + serverTransactionId);

        if (request.getMethod().equals(Request.INVITE)) {
            processInvite(requestEvent, serverTransactionId);
        }

    }

    public void processResponse(ResponseEvent responseEvent) {
    }


    /**
     * Process the invite request.
     */
    public void processInvite(RequestEvent requestEvent, ServerTransaction serverTransaction) {
        SipProvider sipProvider = (SipProvider) requestEvent.getSource();
        Request request = requestEvent.getRequest();
        try {
            log.info("shootme: got an Invite sending Trying");
            // log.info("shootme: " + request);

            ServerTransaction st = requestEvent.getServerTransaction();
            inviteTid = st;
            if (st == null) {
                log.info("null server tx -- getting a new one");
                st = sipProvider.getNewServerTransaction(request);
            }

            log.info("getNewServerTransaction : " + st);


            // Create the 100 Trying response.
            Response response = messageFactory.createResponse(Response.TRYING, request);
            ListeningPoint lp = sipProvider.getListeningPoint(transport);
            int myPort = lp.getPort();

            Address address = addressFactory.createAddress("Shootme <sip:" + myAddress + ":" + myPort + ">");

            // Add a random sleep to stagger the two OK's for the benifit of
            // implementations
            // that may not be too good about handling re-entrancy.
            int timeToSleep = (int) (Math.random() * 1000);

            Thread.sleep(timeToSleep);

            st.sendResponse(response);


            ContactHeader contactHeader = headerFactory.createContactHeader(address);
            response.addHeader(contactHeader);

            String toTag = new Integer(new Random().nextInt()).toString();

            Dialog dialog = st.getDialog();
            inviteDialog = dialog;
            inviteDialog.terminateOnBye(true);
            dialog.setApplicationData(st);

            this.inviteSeen = true;

            timer.schedule(new MyTimerTask(requestEvent, st, toTag), this.delay);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }
    }

    private void sendInviteOK(RequestEvent requestEvent, ServerTransaction inviteTid, String toTag) {
        try {
            log.info("sendInviteOK: " + inviteTid);
            if (inviteTid.getState() == TransactionState.PROCEEDING) {
                log.info("shootme: Dialog state before OK: " + inviteTid.getDialog().getState());
                System.err.println("shootme: Dialog state before OK: " + inviteTid.getDialog().getState());
                SipProvider sipProvider = (SipProvider) requestEvent.getSource();
                Request request = requestEvent.getRequest();
                Response okResponse = messageFactory.createResponse(Response.OK, request);
                ListeningPoint lp = sipProvider.getListeningPoint(transport);
                int myPort = lp.getPort();

                ((ToHeader) okResponse.getHeader(ToHeader.NAME)).setTag(toTag);

                Address address = addressFactory.createAddress("Shootme <sip:" + myAddress + ":" + myPort + ">");
                ContactHeader contactHeader = headerFactory.createContactHeader(address);
                okResponse.addHeader(contactHeader);
                inviteTid.sendResponse(okResponse);
                log.info("shootme: Dialog state after OK: " + inviteTid.getDialog().getState());
                TestCase.assertEquals(DialogState.CONFIRMED, inviteTid.getDialog().getState());
            } else {
                log.info("semdInviteOK: inviteTid = " + inviteTid + " state = " + inviteTid.getState());
                System.err.println("sentInviteOK: inviteTid = " + inviteTid + " state = " + inviteTid.getState());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void processIOException(IOExceptionEvent exceptionEvent) {
        log.error("IOException happened for " + exceptionEvent.getHost() + " port = " + exceptionEvent.getPort());
        TestCase.fail("Unexpected exception");

    }

    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
        log.info("[s]Transaction terminated event recieved: " + transactionTerminatedEvent.getServerTransaction());
        System.err.println("[s]Transaction terminated event recieved: " + transactionTerminatedEvent.getServerTransaction());
        //if (transactionTerminatedEvent.getClientTransaction() == inviteTid)
        seen_txTerm = true;
    }

    public void processTimeout(TimeoutEvent timeoutEvent) {
        log.info("[s]Transaction timedout event recieved: " + timeoutEvent.getServerTransaction());
        System.err.println("[s]Transaction timedout event recieved: " + timeoutEvent.getServerTransaction());
        //if (timeoutEvent.getClientTransaction() == inviteTid)
        seen_txTimeout = true;
    }

    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
        log.info("[s]Dialog terminated event recieved: " + dialogTerminatedEvent.getDialog());
        System.err.println("[s]Dialog terminated event recieved: " + dialogTerminatedEvent.getDialog());
        //if (dialogTerminatedEvent.getDialog() == inviteDialog)
        seen_dte = true;

    }

    public SipProvider createProvider() {
        try {

            ListeningPoint lp = sipStack.createListeningPoint(myAddress, myPort, transport);

            sipProvider = sipStack.createSipProvider(lp);
            log.info("provider " + sipProvider);
            log.info("sipStack = " + sipStack);
            return sipProvider;
        } catch (Exception ex) {
            log.error("Exception", ex);
            TestCase.fail(unexpectedException);
            return null;

        }

    }

    public Shootme(int myPort, int delay) {
        this.myPort = myPort;
        this.delay = delay;


        SipObjects sipObjects = new SipObjects(myPort, "shootme", "on");
        addressFactory = sipObjects.addressFactory;
        messageFactory = sipObjects.messageFactory;
        headerFactory = sipObjects.headerFactory;
        this.sipStack = sipObjects.sipStack;
    }

    public void checkState() {
        TestCase.assertTrue("INVTE must be observed", inviteSeen);
        TestCase.assertTrue("INVITE transaction should temrinate.", seen_txTerm);
        TestCase.assertFalse("INVITE transaction should not timeout.", seen_txTimeout);
        TestCase.assertTrue("INVITE dialog should die.", seen_dte);

    }

    public void stop() {
        this.sipStack.stop();
    }


}
