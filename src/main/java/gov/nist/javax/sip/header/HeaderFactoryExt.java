package gov.nist.javax.sip.header;

import java.text.ParseException;

import co.ecg.jain_sip.sip.ri.header.extensions.JoinHeader;
import co.ecg.jain_sip.sip.ri.header.extensions.ReferencesHeader;
import co.ecg.jain_sip.sip.ri.header.extensions.ReferredByHeader;
import co.ecg.jain_sip.sip.ri.header.extensions.ReplacesHeader;
import co.ecg.jain_sip.sip.ri.header.extensions.SessionExpiresHeader;
import co.ecg.jain_sip.sip.ri.header.ims.PAccessNetworkInfoHeader;
import co.ecg.jain_sip.sip.ri.header.ims.PAssertedIdentityHeader;
import co.ecg.jain_sip.sip.ri.header.ims.PAssertedServiceHeader;
import co.ecg.jain_sip.sip.ri.header.ims.PAssociatedURIHeader;
import co.ecg.jain_sip.sip.ri.header.ims.PCalledPartyIDHeader;
import co.ecg.jain_sip.sip.ri.header.ims.PChargingFunctionAddressesHeader;
import co.ecg.jain_sip.sip.ri.header.ims.PChargingVectorHeader;
import co.ecg.jain_sip.sip.ri.header.ims.PMediaAuthorizationHeader;
import co.ecg.jain_sip.sip.ri.header.ims.PPreferredIdentityHeader;
import co.ecg.jain_sip.sip.ri.header.ims.PPreferredServiceHeader;
import co.ecg.jain_sip.sip.ri.header.ims.PProfileKeyHeader;
import co.ecg.jain_sip.sip.ri.header.ims.PServedUserHeader;
import co.ecg.jain_sip.sip.ri.header.ims.PUserDatabaseHeader;
import co.ecg.jain_sip.sip.ri.header.ims.PVisitedNetworkIDHeader;
import co.ecg.jain_sip.sip.ri.header.ims.PathHeader;
import co.ecg.jain_sip.sip.ri.header.ims.PrivacyHeader;
import co.ecg.jain_sip.sip.ri.header.ims.SecurityClientHeader;
import co.ecg.jain_sip.sip.ri.header.ims.SecurityServerHeader;
import co.ecg.jain_sip.sip.ri.header.ims.SecurityVerifyHeader;
import co.ecg.jain_sip.sip.ri.header.ims.ServiceRouteHeader;

import co.ecg.jain_sip.sip.InvalidArgumentException;
import co.ecg.jain_sip.sip.address.Address;
import co.ecg.jain_sip.sip.header.Header;
import co.ecg.jain_sip.sip.header.HeaderFactory;

/**
 * Header factory extensions. These will be included in the next release of
 * JAIN-SIP.
 * 
 * @since 2.0
 *
 */
public interface HeaderFactoryExt extends HeaderFactory {
    
    /**
     * Create a RequestLine from a String
     * @throws ParseException 
     */
    public SipRequestLine createRequestLine(String requestLine) throws ParseException;
    
    
    /**
     * Create a StatusLine from a String.
     */
    public SipStatusLine createStatusLine(String statusLine) throws ParseException;
    
    
    /**
     * Create a ReferredBy Header.
     *
     * @param address --
     *            address for the header.
     *
     */
    public ReferredByHeader createReferredByHeader(Address address);

    /**
     *
     * Create a Replaces header with a call Id, to and from tag.
     *
     * @param callId -
     *            the call id to use.
     * @param toTag -
     *            the to tag to use.
     * @param fromTag -
     *            the fromTag to use.
     *
     */
    public ReplacesHeader createReplacesHeader(String callId, String toTag,
            String fromTag) throws ParseException;

    /**
     * creates a P-Access-Network-Info header.
     *
     * @return newly created P-Access-Network-Info header
     */
    public PAccessNetworkInfoHeader createPAccessNetworkInfoHeader();

    /**
     * P-Asserted-Identity header
     *
     * @param address -
     *            Address
     * @return newly created P-Asserted-Identity header
     * @throws ParseException
     * @throws NullPointerException
     */
    public PAssertedIdentityHeader createPAssertedIdentityHeader(Address address)
            throws NullPointerException, ParseException;

    /**
     * Creates a new P-Associated-URI header based on the supplied address
     *
     * @param assocURI -
     *            Address
     * @return newly created P-Associated-URI header
     * @throws NullPointerException
     *             if the supplied address is null
     * @throws ParseException
     */
    public PAssociatedURIHeader createPAssociatedURIHeader(Address assocURI);

    /**
     * P-Called-Party-ID header
     *
     * @param address -
     *            Address
     * @return newly created P-Called-Party-ID header
     * @throws NullPointerException
     * @throws ParseException
     */
    public PCalledPartyIDHeader createPCalledPartyIDHeader(Address address);

    /**
     * P-Charging-Function-Addresses header
     *
     * @return newly created P-Charging-Function-Addresses header
     */
    public PChargingFunctionAddressesHeader createPChargingFunctionAddressesHeader();

    /**
     * P-Charging-Vector header
     *
     * @param icid -
     *            icid string
     * @return newly created P-Charging-Vector header
     * @throws NullPointerException
     * @throws ParseException
     */
    public PChargingVectorHeader createChargingVectorHeader(String icid) throws ParseException;

     /**
     * P-Media-Authorization header
     * @param token - token string
     * @return newly created P-Media-Authorizarion header
     * @throws InvalidArgumentException
     * @throws ParseException
     */
    public PMediaAuthorizationHeader createPMediaAuthorizationHeader(String token)
        throws InvalidArgumentException, ParseException;

    /**
     * P-Preferred-Identity header
     * @param address - Address
     * @return newly created P-Preferred-Identity header
     * @throws NullPointerException
     */
    public PPreferredIdentityHeader createPPreferredIdentityHeader(Address address);

    /**
     * P-Visited-Network-ID header
     * @return newly created P-Visited-Network-ID header
     */
    public PVisitedNetworkIDHeader createPVisitedNetworkIDHeader();

    /**
     * PATH header
     * @param address - Address
     * @return newly created Path header
     * @throws NullPointerException
     * @throws ParseException
     */
    public PathHeader createPathHeader(Address address);

    /**
     * Privacy header
     * @param privacyType - privacy type string
     * @return newly created Privacy header
     * @throws NullPointerException
     */
    public PrivacyHeader createPrivacyHeader(String privacyType);


    /**
     * Service-Route header
     * @param address - Address
     * @return newly created Service-Route header
     * @throws NullPointerException
     */
    public ServiceRouteHeader createServiceRouteHeader(Address address);

    /**
     * Security-Server header
     * @return newly created Security-Server header
     */
    public SecurityServerHeader createSecurityServerHeader();

    /**
     * Security-Client header
     * @return newly created Security-Client header
     */
    public SecurityClientHeader createSecurityClientHeader();


    /**
     * Security-Verify header
     * @return newly created Security-Verify header
     */
    public SecurityVerifyHeader createSecurityVerifyHeader();


    /**
     * Creates a new SessionExpiresHeader based on the newly supplied expires value.
     *
     * @param expires - the new integer value of the expires.
     * @throws InvalidArgumentException if supplied expires is less
     * than zero.
     * @return the newly created SessionExpiresHeader object.
     *
     */
    public SessionExpiresHeader createSessionExpiresHeader(int expires) throws InvalidArgumentException ;

    /**
     *
     * Create a Join header with a call Id, to and from tag.
     *
     * @param callId -
     *            the call id to use.
     * @param toTag -
     *            the to tag to use.
     * @param fromTag -
     *            the fromTag to use.
     *
     */
    public JoinHeader createJoinHeader(String callId, String toTag,
            String fromTag) throws ParseException;

    /**
     * Create a P-User-Database header.
     * 
     * @return the newly created P-User-Database header
     * @param the database name, that may be an IP:port or a domain name.
     */
    public PUserDatabaseHeader createPUserDatabaseHeader(String databaseName);


    /**
     * Create a P-Profile-Key header.
     * 
     * @param address
     * @return The newly created P-Profile-Key header
     */
    public PProfileKeyHeader createPProfileKeyHeader(Address address);

    /**
     * Create a P-Served-User header.
     * 
     * @param address of the served user.
     * @return The newly created P-Served-User Header.
     */
    public PServedUserHeader createPServedUserHeader(Address address);

    /**
     * Create a P-Preferred-Service header.
     * 
     * @return The newly created P-Preferred-Service Header.
     */
    public PPreferredServiceHeader createPPreferredServiceHeader();

    /**
     * Create an AssertedService Header
     *
     * @return The newly created P-Asserted-Service Header.
     */
    public PAssertedServiceHeader createPAssertedServiceHeader();
    
    
    /**
     * Create a References header.
     * 
     * @param callId -- the referenced call Id.
     * @param rel -- the rel parameter of the references header.
     * 
     * @return the newly created References header.
     */
    public ReferencesHeader createReferencesHeader(String callId, String rel) throws ParseException;
    
    /**
     * Create a header from a string. The string is assumed to be in the 
     * name:value format. The trailing CRLF (if any ) will be stripped
     * before parsing this. The header should be a singleton.
     */
    public Header createHeader(String header) throws ParseException;

}
