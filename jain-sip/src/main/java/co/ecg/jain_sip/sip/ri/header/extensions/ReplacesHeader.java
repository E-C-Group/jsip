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
package co.ecg.jain_sip.sip.ri.header.extensions;



import java.text.ParseException;

import javax.sip.header.*;


/**
 * The From header field indicates the logical identity of the initiator

 * of the request, possibly the user's address-of-record. This may be different

 * from the initiator of the dialog.  Requests sent by the callee to the caller

 * use the callee's address in the From header field.

 * <p>

 * Like the To header field, it contains a URI and optionally a display name,

 * encapsulated in a {@link javax.sip.address.Address}.  It is used by SIP

 * elements to determine which processing rules to apply to a request (for

 * example, automatic call rejection). As such, it is very important that the

 * From URI not contain IP addresses or the FQDN of the host on which the UA is

 * running, since these are not logical names.

 * <p>

 * The From header field allows for a display name.  A UAC SHOULD use

 * the display name "Anonymous", along with a syntactically correct, but

 * otherwise meaningless URI (like sip:thisis@anonymous.invalid), if the

 * identity of the client is to remain hidden.

 * <p>

 * Usually, the value that populates the From header field in requests

 * generated by a particular UA is pre-provisioned by the user or by the

 * administrators of the user's local domain.  If a particular UA is used by

 * multiple users, it might have switchable profiles that include a URI

 * corresponding to the identity of the profiled user. Recipients of requests

 * can authenticate the originator of a request in order to ascertain that

 * they are who their From header field claims they are.

 * <p>

 * Two From header fields are equivalent if their URIs match, and their

 * parameters match. Extension parameters in one header field, not present in

 * the other are ignored for the purposes of comparison. This means that the

 * display name and presence or absence of angle brackets do not affect

 * matching.

 * <ul>

 * <li> The "Tag" parameter - is used in the To and From header fields of SIP

 * messages.  It serves as a general mechanism to identify a dialog, which is

 * the combination of the Call-ID along with two tags, one from each

 * participant in the dialog.  When a User Agent sends a request outside of a dialog,

 * it contains a From tag only, providing "half" of the dialog ID. The dialog

 * is completed from the response(s), each of which contributes the second half

 * in the To header field. When a tag is generated by a User Agent for insertion into

 * a request or response, it MUST be globally unique and cryptographically

 * random with at least 32 bits of randomness. Besides the requirement for

 * global uniqueness, the algorithm for generating a tag is implementation

 * specific.  Tags are helpful in fault tolerant systems, where a dialog is to

 * be recovered on an alternate server after a failure.  A UAS can select the

 * tag in such a way that a backup can recognize a request as part of a dialog

 * on the failed server, and therefore determine that it should attempt to

 * recover the dialog and any other state associated with it.

 * </ul>
 * For Example:<br>
 * <code>From: "Bob" sips:bob@biloxi.com ;tag=a48s<br>
 * From: sip:+12125551212@phone2net.com;tag=887s<br>
 * From: Anonymous sip:c8oqz84zk7z@privacy.org;tag=hyh8</code>
 *
 * @version 1.1
 * @author Sun Microsystems
 */
public interface ReplacesHeader extends Parameters, Header {



    /**

     * Sets the tag parameter of the FromHeader. The tag in the From field of a
     * request identifies the peer of the dialog. When a UA sends a request
     * outside of a dialog, it contains a From tag only, providing "half" of
     * the dialog Identifier.
     * <p>
     * The From Header MUST contain a new "tag" parameter, chosen by the UAC 
     * applicaton. Once the initial From "tag" is assigned it should not be 
     * manipulated by the application. That is on the client side for outbound 
     * requests the application is responsible for Tag assigmennment, after 
     * dialog establishment the stack will take care of Tag assignment.
     *
     * @param tag - the new tag of the FromHeader
     * @throws ParseException which signals that an error has been reached
     * unexpectedly while parsing the Tag value.
     */
    public void setToTag(String tag) throws ParseException;
    public void setFromTag(String tag) throws ParseException;





    /**

     * Gets the tag of FromHeader. The Tag parameter identified the Peer of the

     * dialogue and must always be present.

     *

     * @return the tag parameter of the FromHeader.

     */

    public String getToTag();
    public String getFromTag();


    /**

     * Sets the Call-Id of the CallIdHeader. The CallId parameter uniquely

     * identifies a serious of messages within a dialogue.

     *

     * @param callId - the string value of the Call-Id of this CallIdHeader.

     * @throws ParseException which signals that an error has been reached

     * unexpectedly while parsing the callId value.

     */

    public void setCallId(String callId) throws ParseException;



    /**

     * Returns the Call-Id of CallIdHeader. The CallId parameter uniquely

     * identifies a series of messages within a dialogue.

     *

     * @return the String value of the Call-Id of this CallIdHeader

     */

    public String getCallId();



    /**

     * Name of FromHeader

     */

    public final static String NAME = "Replaces";

}


