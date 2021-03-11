package co.ecg.jain_sip.tck.msgflow.callflows;

import java.util.EventObject;
import java.util.HashSet;
import java.util.Hashtable;

import co.ecg.jain_sip.sip.DialogTerminatedEvent;
import co.ecg.jain_sip.sip.IOExceptionEvent;
import co.ecg.jain_sip.sip.RequestEvent;
import co.ecg.jain_sip.sip.ResponseEvent;
import co.ecg.jain_sip.sip.SipListener;
import co.ecg.jain_sip.sip.SipProvider;
import co.ecg.jain_sip.sip.TimeoutEvent;
import co.ecg.jain_sip.sip.TransactionTerminatedEvent;

import co.ecg.jain_sip.tck.TckInternalError;
import co.ecg.jain_sip.tck.TestHarness;

public abstract class ScenarioHarness extends TestHarness {
    private HashSet<ProtocolObjects> tiProtocolObjects = new HashSet<ProtocolObjects>();

    private HashSet<ProtocolObjects> riProtocolObjects = new HashSet<ProtocolObjects>();

    protected String transport;

    protected Hashtable providerTable;

    // this flag determines whether the tested SIP Stack is shootist or shootme
    protected boolean testedImplFlag;

    public void setUp() throws Exception {
        if (testedImplFlag) {
            this.tiProtocolObjects.add(new ProtocolObjects("ti" + super.getName(), "co.ecg.jain_sip",
                    transport, true, false, false));

            this.addRiProtocolObjects(new ProtocolObjects("ri" + super.getName(), super
                    .getImplementationPath(), transport, true, false, false));
            /*
             * if (!getImplementationPath().equals("co.ecg.jain_sip")) this.riProtocolObjects = new
             * ProtocolObjects( super.getName(), super.getImplementationPath(), transport, true);
             * else this.riProtocolObjects = tiProtocolObjects;
             */

        } else {
            this.tiProtocolObjects.add(new ProtocolObjects("ti" + super.getName(),
                    getImplementationPath(), transport, true, false, false));
            this.addRiProtocolObjects(new ProtocolObjects("ri" + super.getName(), "co.ecg.jain_sip",
                    transport, true, false, false));

            /*
             * if (!getImplementationPath().equals("co.ecg.jain_sip")) this.riProtocolObjects = new
             * ProtocolObjects( super.getName(), super.getImplementationPath(), transport, true);
             * else this.riProtocolObjects = tiProtocolObjects;
             */

        }
    }

    public void setUp(int nri, int nti) throws Exception {
        if (testedImplFlag) {
            for (int i = 0; i < nti; i++) {
                this.tiProtocolObjects.add(new ProtocolObjects("ti" + super.getName() + i, "co.ecg.jain_sip",
                        transport, true, false, false));
            }
            for (int i = 0; i < nri; i++) {

                this.addRiProtocolObjects(new ProtocolObjects("ri" + super.getName() + i, super
                        .getImplementationPath(), transport, true, false, false));
            }
            /*
             * if (!getImplementationPath().equals("co.ecg.jain_sip")) this.riProtocolObjects = new
             * ProtocolObjects( super.getName(), super.getImplementationPath(), transport, true);
             * else this.riProtocolObjects = tiProtocolObjects;
             */

        } else {
            for (int i = 0; i < nti; i++) {
                this.tiProtocolObjects.add(new ProtocolObjects("ti" + super.getName() + i,
                        getImplementationPath(), transport, true, false, false));
            }
            for (int i = 0; i < nri; i++) {
                this.addRiProtocolObjects(new ProtocolObjects("ri" + super.getName() + i, "co.ecg.jain_sip",
                        transport, true, false, false));
            }

            /*
             * if (!getImplementationPath().equals("co.ecg.jain_sip")) this.riProtocolObjects = new
             * ProtocolObjects( super.getName(), super.getImplementationPath(), transport, true);
             * else this.riProtocolObjects = tiProtocolObjects;
             */

        }
    }

    public void setUp(boolean riAutoDialog) throws Exception {
        if (testedImplFlag) {
            this.tiProtocolObjects.add(new ProtocolObjects("ti" + super.getName(), "co.ecg.jain_sip",
                    transport, true, false, false));

            this.addRiProtocolObjects(new ProtocolObjects("ri" + super.getName(), super
                    .getImplementationPath(), transport, riAutoDialog, false, false));
            /*
             * if (!getImplementationPath().equals("co.ecg.jain_sip")) this.riProtocolObjects = new
             * ProtocolObjects( super.getName(), super.getImplementationPath(), transport, true);
             * else this.riProtocolObjects = tiProtocolObjects;
             */

        } else {
            this.tiProtocolObjects.add(new ProtocolObjects("ti" + super.getName(),
                    getImplementationPath(), transport, true, false, false));
            this.addRiProtocolObjects(new ProtocolObjects("ri" + super.getName(), "co.ecg.jain_sip",
                    transport, riAutoDialog, false, false));

            /*
             * if (!getImplementationPath().equals("co.ecg.jain_sip")) this.riProtocolObjects = new
             * ProtocolObjects( super.getName(), super.getImplementationPath(), transport, true);
             * else this.riProtocolObjects = tiProtocolObjects;
             */

        }
    }

    public void setUp(boolean riAutoDialog, int nri, int nti) {
        if (testedImplFlag) {
            for (int i = 0; i < nti; i++) {
                this.tiProtocolObjects.add(new ProtocolObjects("ti" + super.getName() + i, "co.ecg.jain_sip",
                        transport, true, false, false));

            }
            for (int i = 0; i < nri; i++) {
                this.addRiProtocolObjects(new ProtocolObjects("ri" + super.getName(), super
                        .getImplementationPath(), transport, riAutoDialog, false, false));
            }

        } else {
            for (int i = 0; i < nti; i++) {
                this.tiProtocolObjects.add(new ProtocolObjects("ti" + super.getName() + i,
                        getImplementationPath(), transport, true, false, false));
            }
            for (int i = 0; i < nri; i++) {
                this.addRiProtocolObjects(new ProtocolObjects("ri" + super.getName(), "co.ecg.jain_sip",
                        transport, riAutoDialog, false, false));
            }

        }
    }

    private SipListener getSipListener(EventObject sipEvent) {
        SipProvider source = (SipProvider) sipEvent.getSource();
        SipListener listener = (SipListener) providerTable.get(source);
        if (listener == null)
            throw new TckInternalError("Unexpected null listener");
        return listener;
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

        System.out.println("IOException ");

    }

    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
        getSipListener(transactionTerminatedEvent).processTransactionTerminated(
                transactionTerminatedEvent);

    }

    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
        getSipListener(dialogTerminatedEvent).processDialogTerminated(dialogTerminatedEvent);

    }

    protected ScenarioHarness(String name, boolean autoDialog) {

        super(name, autoDialog);
        this.providerTable = new Hashtable();

    }

    protected ScenarioHarness(String name, boolean autoDialog, int nri, int nti) {
        super(name, autoDialog);
        this.providerTable = new Hashtable();

    }

    /**
     * @param riProtocolObjects the riProtocolObjects to set
     */
    protected void addRiProtocolObjects(ProtocolObjects riProtocolObjects) {
        this.riProtocolObjects.add(riProtocolObjects);
    }

    /**
     * @return the riProtocolObjects
     */
    protected ProtocolObjects getRiProtocolObjects() {
        return riProtocolObjects.iterator().next();
    }

    /**
     * @param tiProtocolObjects the tiProtocolObjects to set
     */
    protected void addTiProtocolObjects(
            ProtocolObjects tiProtocolObjects) {
        this.tiProtocolObjects.add(tiProtocolObjects);
    }

    /**
     * @return the tiProtocolObjects
     */
    protected ProtocolObjects getTiProtocolObjects() {
        return tiProtocolObjects.iterator().next();
    }

    public ProtocolObjects getTiProtocolObjects(int index) {
        return (ProtocolObjects) tiProtocolObjects.toArray()[index];
    }

    public ProtocolObjects getRiProtocolObjects(int index) {
        return (ProtocolObjects) riProtocolObjects.toArray()[index];
    }

    public void tearDown() throws Exception {
        for (ProtocolObjects protocolObjects : this.tiProtocolObjects) {
            protocolObjects.destroy();
        }
        for (ProtocolObjects protocolObjects : this.riProtocolObjects) {
            protocolObjects.destroy();
        }
    }

    public void start() throws Exception {
        for (ProtocolObjects protocolObjects : this.tiProtocolObjects) {
            protocolObjects.start();
        }
        for (ProtocolObjects protocolObjects : this.riProtocolObjects) {
            protocolObjects.start();
        }
    }

}