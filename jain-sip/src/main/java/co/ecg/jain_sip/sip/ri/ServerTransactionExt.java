package co.ecg.jain_sip.sip.ri;

import javax.sip.ServerTransaction;


public interface ServerTransactionExt extends ServerTransaction, TransactionExt {
    /**
     * Return the canceled Invite transaction corresponding to an
     * incoming CANCEL server transaction.
     *
     * @return -- the canceled Invite transaction.
     *
     */
    public ServerTransaction getCanceledInviteTransaction();
}
