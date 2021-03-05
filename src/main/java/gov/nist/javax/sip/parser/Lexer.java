/*
 * Conditions Of Use
 *
 * This software was developed by employees of the National Institute of
 * Standards and Technology (NIST), an agency of the Federal Government.
 * Pursuant to title 15 Untied States Code Section 105, works of NIST
 * employees are not subject to copyright protection in the United States
 * and are considered to be in the public domain.  As a result, a formal
 * license is not needed to use the software.
 *
 * This software is provided by NIST as a service and is expressly
 * provided "AS IS."  NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
 * OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
 * AND DATA ACCURACY.  NIST does not warrant or make any representations
 * regarding the use of the software or the results thereof, including but
 * not limited to the correctness, accuracy, reliability or usefulness of
 * the software.
 *
 * Permission to use this software is contingent upon your acceptance
 * of the terms of this agreement
 *
 * .
 *
 */
package gov.nist.javax.sip.parser;

import co.ecg.jain_sip.core.ri.LexerCore;
import co.ecg.jain_sip.sip.ri.header.extensions.JoinHeader;
import co.ecg.jain_sip.sip.ri.header.extensions.MinSEHeader;
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

import java.util.concurrent.ConcurrentHashMap;

import co.ecg.jain_sip.sip.header.AcceptEncodingHeader;
import co.ecg.jain_sip.sip.header.AcceptHeader;
import co.ecg.jain_sip.sip.header.AcceptLanguageHeader;
import co.ecg.jain_sip.sip.header.AlertInfoHeader;
import co.ecg.jain_sip.sip.header.AllowEventsHeader;
import co.ecg.jain_sip.sip.header.AllowHeader;
import co.ecg.jain_sip.sip.header.AuthenticationInfoHeader;
import co.ecg.jain_sip.sip.header.AuthorizationHeader;
import co.ecg.jain_sip.sip.header.CSeqHeader;
import co.ecg.jain_sip.sip.header.CallIdHeader;
import co.ecg.jain_sip.sip.header.CallInfoHeader;
import co.ecg.jain_sip.sip.header.ContactHeader;
import co.ecg.jain_sip.sip.header.ContentDispositionHeader;
import co.ecg.jain_sip.sip.header.ContentEncodingHeader;
import co.ecg.jain_sip.sip.header.ContentLanguageHeader;
import co.ecg.jain_sip.sip.header.ContentLengthHeader;
import co.ecg.jain_sip.sip.header.ContentTypeHeader;
import co.ecg.jain_sip.sip.header.DateHeader;
import co.ecg.jain_sip.sip.header.ErrorInfoHeader;
import co.ecg.jain_sip.sip.header.EventHeader;
import co.ecg.jain_sip.sip.header.ExpiresHeader;
import co.ecg.jain_sip.sip.header.FromHeader;
import co.ecg.jain_sip.sip.header.InReplyToHeader;
import co.ecg.jain_sip.sip.header.MaxForwardsHeader;
import co.ecg.jain_sip.sip.header.MimeVersionHeader;
import co.ecg.jain_sip.sip.header.MinExpiresHeader;
import co.ecg.jain_sip.sip.header.OrganizationHeader;
import co.ecg.jain_sip.sip.header.PriorityHeader;
import co.ecg.jain_sip.sip.header.ProxyAuthenticateHeader;
import co.ecg.jain_sip.sip.header.ProxyAuthorizationHeader;
import co.ecg.jain_sip.sip.header.ProxyRequireHeader;
import co.ecg.jain_sip.sip.header.RAckHeader;
import co.ecg.jain_sip.sip.header.RSeqHeader;
import co.ecg.jain_sip.sip.header.ReasonHeader;
import co.ecg.jain_sip.sip.header.RecordRouteHeader;
import co.ecg.jain_sip.sip.header.ReferToHeader;
import co.ecg.jain_sip.sip.header.ReplyToHeader;
import co.ecg.jain_sip.sip.header.RequireHeader;
import co.ecg.jain_sip.sip.header.RetryAfterHeader;
import co.ecg.jain_sip.sip.header.RouteHeader;
import co.ecg.jain_sip.sip.header.SIPETagHeader;
import co.ecg.jain_sip.sip.header.SIPIfMatchHeader;
import co.ecg.jain_sip.sip.header.ServerHeader;
import co.ecg.jain_sip.sip.header.SubjectHeader;
import co.ecg.jain_sip.sip.header.SubscriptionStateHeader;
import co.ecg.jain_sip.sip.header.SupportedHeader;
import co.ecg.jain_sip.sip.header.TimeStampHeader;
import co.ecg.jain_sip.sip.header.ToHeader;
import co.ecg.jain_sip.sip.header.UnsupportedHeader;
import co.ecg.jain_sip.sip.header.UserAgentHeader;
import co.ecg.jain_sip.sip.header.ViaHeader;
import co.ecg.jain_sip.sip.header.WWWAuthenticateHeader;
import co.ecg.jain_sip.sip.header.WarningHeader;

/**
 * Lexer class for the parser.
 *
 * @version 1.2
 *
 * @author M. Ranganathan <br/>
 *
 *
 */
public class Lexer extends LexerCore {
    /**
     * get the header name of the line
     *
     * @return the header name (stuff before the :) bug fix submitted by
     *         zvali@dev.java.net
     */
    public static String getHeaderName(String line) {
        if (line == null)
            return null;
        String headerName = null;
        try {
            int begin = line.indexOf(":");
            headerName = null;
            if (begin >= 1)
                headerName = line.substring(0, begin).trim();
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
        return headerName;
    }

    public Lexer(String lexerName, String buffer) {
        super(lexerName, buffer);
        this.selectLexer(lexerName);
    }

    /**
     * get the header value of the line
     *
     * @return String
     */
    public static String getHeaderValue(String line) {
        if (line == null)
            return null;
        String headerValue = null;
        try {
            int begin = line.indexOf(":");
            headerValue = line.substring(begin + 1);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
        return headerValue;
    }

    public void selectLexer(String lexerName) {
        // Synchronization Bug fix by Robert Rosen.
    	ConcurrentHashMap<String, Integer> lexer = lexerTables.get(lexerName);
        this.currentLexerName = lexerName;
        if (lexer == null) {
        	ConcurrentHashMap<String, Integer> newLexer  = new ConcurrentHashMap<String, Integer>();
            // Temporarily set newLexer as current, so addKeyword populate it
            currentLexer = newLexer;
//          addLexer(lexerName);
            if (lexerName.equals("method_keywordLexer")) {
                addKeyword(TokenNames.REGISTER, TokenTypes.REGISTER);
                addKeyword(TokenNames.ACK, TokenTypes.ACK);
                addKeyword(TokenNames.OPTIONS, TokenTypes.OPTIONS);
                addKeyword(TokenNames.BYE, TokenTypes.BYE);
                addKeyword(TokenNames.INVITE, TokenTypes.INVITE);
                addKeyword(TokenNames.SIP, TokenTypes.SIP);
                addKeyword(TokenNames.SIPS, TokenTypes.SIPS);
                addKeyword(TokenNames.SUBSCRIBE, TokenTypes.SUBSCRIBE);
                addKeyword(TokenNames.NOTIFY, TokenTypes.NOTIFY);
                addKeyword(TokenNames.MESSAGE, TokenTypes.MESSAGE);

                // JvB: added to support RFC3903
                addKeyword(TokenNames.PUBLISH, TokenTypes.PUBLISH);

            } else if (lexerName.equals("command_keywordLexer")) {
                addKeyword(ErrorInfoHeader.NAME,
                        TokenTypes.ERROR_INFO);
                addKeyword(AllowEventsHeader.NAME,
                        TokenTypes.ALLOW_EVENTS);
                addKeyword(AuthenticationInfoHeader.NAME,
                        TokenTypes.AUTHENTICATION_INFO);
                addKeyword(EventHeader.NAME, TokenTypes.EVENT);
                addKeyword(MinExpiresHeader.NAME,
                        TokenTypes.MIN_EXPIRES);
                addKeyword(RSeqHeader.NAME, TokenTypes.RSEQ);
                addKeyword(RAckHeader.NAME, TokenTypes.RACK);
                addKeyword(ReasonHeader.NAME,
                        TokenTypes.REASON);
                addKeyword(ReplyToHeader.NAME,
                        TokenTypes.REPLY_TO);
                addKeyword(SubscriptionStateHeader.NAME,
                        TokenTypes.SUBSCRIPTION_STATE);
                addKeyword(TimeStampHeader.NAME,
                        TokenTypes.TIMESTAMP);
                addKeyword(InReplyToHeader.NAME,
                        TokenTypes.IN_REPLY_TO);
                addKeyword(MimeVersionHeader.NAME,
                        TokenTypes.MIME_VERSION);
                addKeyword(AlertInfoHeader.NAME,
                        TokenTypes.ALERT_INFO);
                addKeyword(FromHeader.NAME, TokenTypes.FROM);
                addKeyword(ToHeader.NAME, TokenTypes.TO);
                addKeyword(ReferToHeader.NAME,
                        TokenTypes.REFER_TO);
                addKeyword(ViaHeader.NAME, TokenTypes.VIA);
                addKeyword(UserAgentHeader.NAME,
                        TokenTypes.USER_AGENT);
                addKeyword(ServerHeader.NAME,
                        TokenTypes.SERVER);
                addKeyword(AcceptEncodingHeader.NAME,
                        TokenTypes.ACCEPT_ENCODING);
                addKeyword(AcceptHeader.NAME,
                        TokenTypes.ACCEPT);
                addKeyword(AllowHeader.NAME, TokenTypes.ALLOW);
                addKeyword(RouteHeader.NAME, TokenTypes.ROUTE);
                addKeyword(AuthorizationHeader.NAME,
                        TokenTypes.AUTHORIZATION);
                addKeyword(ProxyAuthorizationHeader.NAME,
                        TokenTypes.PROXY_AUTHORIZATION);
                addKeyword(RetryAfterHeader.NAME,
                        TokenTypes.RETRY_AFTER);
                addKeyword(ProxyRequireHeader.NAME,
                        TokenTypes.PROXY_REQUIRE);
                addKeyword(ContentLanguageHeader.NAME,
                        TokenTypes.CONTENT_LANGUAGE);
                addKeyword(UnsupportedHeader.NAME,
                        TokenTypes.UNSUPPORTED);
                addKeyword(SupportedHeader.NAME,
                        TokenTypes.SUPPORTED);
                addKeyword(WarningHeader.NAME,
                        TokenTypes.WARNING);
                addKeyword(MaxForwardsHeader.NAME,
                        TokenTypes.MAX_FORWARDS);
                addKeyword(DateHeader.NAME, TokenTypes.DATE);
                addKeyword(PriorityHeader.NAME,
                        TokenTypes.PRIORITY);
                addKeyword(ProxyAuthenticateHeader.NAME,
                        TokenTypes.PROXY_AUTHENTICATE);
                addKeyword(ContentEncodingHeader.NAME,
                        TokenTypes.CONTENT_ENCODING);
                addKeyword(ContentLengthHeader.NAME,
                        TokenTypes.CONTENT_LENGTH);
                addKeyword(SubjectHeader.NAME,
                        TokenTypes.SUBJECT);
                addKeyword(ContentTypeHeader.NAME,
                        TokenTypes.CONTENT_TYPE);
                addKeyword(ContactHeader.NAME,
                        TokenTypes.CONTACT);
                addKeyword(CallIdHeader.NAME,
                        TokenTypes.CALL_ID);
                addKeyword(RequireHeader.NAME,
                        TokenTypes.REQUIRE);
                addKeyword(ExpiresHeader.NAME,
                        TokenTypes.EXPIRES);
                addKeyword(RecordRouteHeader.NAME,
                        TokenTypes.RECORD_ROUTE);
                addKeyword(OrganizationHeader.NAME,
                        TokenTypes.ORGANIZATION);
                addKeyword(CSeqHeader.NAME, TokenTypes.CSEQ);
                addKeyword(AcceptLanguageHeader.NAME,
                        TokenTypes.ACCEPT_LANGUAGE);
                addKeyword(WWWAuthenticateHeader.NAME,
                        TokenTypes.WWW_AUTHENTICATE);
                addKeyword(CallInfoHeader.NAME,
                        TokenTypes.CALL_INFO);
                addKeyword(ContentDispositionHeader.NAME,
                        TokenTypes.CONTENT_DISPOSITION);
                // And now the dreaded short forms....
                addKeyword(TokenNames.K, TokenTypes.SUPPORTED);
                addKeyword(TokenNames.C,
                        TokenTypes.CONTENT_TYPE);
                addKeyword(TokenNames.E,
                        TokenTypes.CONTENT_ENCODING);
                addKeyword(TokenNames.F, TokenTypes.FROM);
                addKeyword(TokenNames.I, TokenTypes.CALL_ID);
                addKeyword(TokenNames.M, TokenTypes.CONTACT);
                addKeyword(TokenNames.L,
                        TokenTypes.CONTENT_LENGTH);
                addKeyword(TokenNames.S, TokenTypes.SUBJECT);
                addKeyword(TokenNames.T, TokenTypes.TO);
                addKeyword(TokenNames.U,
                        TokenTypes.ALLOW_EVENTS); // JvB: added
                addKeyword(TokenNames.V, TokenTypes.VIA);
                addKeyword(TokenNames.R, TokenTypes.REFER_TO);
                addKeyword(TokenNames.O, TokenTypes.EVENT); // Bug
                                                                            // fix
                                                                            // by
                                                                            // Mario
                                                                            // Mantak
                addKeyword(TokenNames.X, TokenTypes.SESSIONEXPIRES_TO); // Bug fix by Jozef Saniga
                
                // JvB: added to support RFC3903
                addKeyword(SIPETagHeader.NAME,
                        TokenTypes.SIP_ETAG);
                addKeyword(SIPIfMatchHeader.NAME,
                        TokenTypes.SIP_IF_MATCH);

                // pmusgrave: Add RFC4028 and ReferredBy
                addKeyword(SessionExpiresHeader.NAME,
                        TokenTypes.SESSIONEXPIRES_TO);
                addKeyword(MinSEHeader.NAME,
                        TokenTypes.MINSE_TO);
                addKeyword(ReferredByHeader.NAME,
                        TokenTypes.REFERREDBY_TO);

                // pmusgrave RFC3891
                addKeyword(ReplacesHeader.NAME,
                        TokenTypes.REPLACES_TO);
                //jean deruelle RFC3911
                addKeyword(JoinHeader.NAME,
                        TokenTypes.JOIN_TO);

                // IMS Headers
                addKeyword(PathHeader.NAME, TokenTypes.PATH);
                addKeyword(ServiceRouteHeader.NAME,
                        TokenTypes.SERVICE_ROUTE);
                addKeyword(PAssertedIdentityHeader.NAME,
                        TokenTypes.P_ASSERTED_IDENTITY);
                addKeyword(PPreferredIdentityHeader.NAME,
                        TokenTypes.P_PREFERRED_IDENTITY);
                addKeyword(PrivacyHeader.NAME,
                        TokenTypes.PRIVACY);

                // issued by Miguel Freitas
                addKeyword(PCalledPartyIDHeader.NAME,
                        TokenTypes.P_CALLED_PARTY_ID);
                addKeyword(PAssociatedURIHeader.NAME,
                        TokenTypes.P_ASSOCIATED_URI);
                addKeyword(PVisitedNetworkIDHeader.NAME,
                        TokenTypes.P_VISITED_NETWORK_ID);
                addKeyword(PChargingFunctionAddressesHeader.NAME,
                        TokenTypes.P_CHARGING_FUNCTION_ADDRESSES);
                addKeyword(PChargingVectorHeader.NAME,
                        TokenTypes.P_VECTOR_CHARGING);
                addKeyword(PAccessNetworkInfoHeader.NAME,
                        TokenTypes.P_ACCESS_NETWORK_INFO);
                addKeyword(PMediaAuthorizationHeader.NAME,
                        TokenTypes.P_MEDIA_AUTHORIZATION);

                addKeyword(SecurityServerHeader.NAME,
                        TokenTypes.SECURITY_SERVER);
                addKeyword(SecurityVerifyHeader.NAME,
                        TokenTypes.SECURITY_VERIFY);
                addKeyword(SecurityClientHeader.NAME,
                        TokenTypes.SECURITY_CLIENT);

                // added by aayush@rancore
                addKeyword(PUserDatabaseHeader.NAME,
                        TokenTypes.P_USER_DATABASE);

                // added by aayush@rancore
                addKeyword(PProfileKeyHeader.NAME,
                        TokenTypes.P_PROFILE_KEY);

                // added by aayush@rancore
                addKeyword(PServedUserHeader.NAME,
                        TokenTypes.P_SERVED_USER);

                // added by aayush@rancore
                addKeyword(PPreferredServiceHeader.NAME,
                        TokenTypes.P_PREFERRED_SERVICE);

                // added by aayush@rancore
                addKeyword(PAssertedServiceHeader.NAME,
                        TokenTypes.P_ASSERTED_SERVICE);
                
                // added References header
                addKeyword(ReferencesHeader.NAME,TokenTypes.REFERENCES);

                // end //


            } else if (lexerName.equals("status_lineLexer")) {
                addKeyword(TokenNames.SIP, TokenTypes.SIP);
            } else if (lexerName.equals("request_lineLexer")) {
                addKeyword(TokenNames.SIP, TokenTypes.SIP);
            } else if (lexerName.equals("sip_urlLexer")) {
                addKeyword(TokenNames.TEL, TokenTypes.TEL);
                addKeyword(TokenNames.SIP, TokenTypes.SIP);
                addKeyword(TokenNames.SIPS, TokenTypes.SIPS);
            }

            // Now newLexer is completely initialized, let's check if somebody
            // have put lexer in table
            lexer = lexerTables.putIfAbsent(lexerName, newLexer);
            if (lexer == null) {
                // put succeeded, use new value
                lexer = newLexer;                    
            }
            currentLexer = lexer;
        } else {
        	currentLexer = lexer;
        }
    }
}
