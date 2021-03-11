package co.ecg.jain_sip.sip.stack.reinvitechallenge;

import co.ecg.jain_sip.sip.ri.clientauthutils.UserCredentials;

public class UserCredentialsImpl implements UserCredentials {
	
	public String getPassword() {
		return "password";
	}

	
	public String getSipDomain() {
		return "127.0.0.1";
	}

	
	public String getUserName() {		
		return "user";
	}

}
