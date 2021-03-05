package test.unit.gov.nist.javax.sip.stack.reinvitechallenge;

import co.ecg.jain_sip.sip.ClientTransaction;



import co.ecg.jain_sip.sip.ri.clientauthutils.AccountManager;
import co.ecg.jain_sip.sip.ri.clientauthutils.UserCredentials;

public class AccountManagerImpl implements AccountManager {

	
	public UserCredentials getCredentials(
			ClientTransaction challengedTransaction, String realm) {
			return new UserCredentialsImpl();
	}

}
