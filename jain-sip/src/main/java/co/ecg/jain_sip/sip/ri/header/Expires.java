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
/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package co.ecg.jain_sip.sip.ri.header;

import co.ecg.jain_sip.sip.*;
import co.ecg.jain_sip.sip.header.ExpiresHeader;

/**
 * Expires SIP Header.
 *
 * @version 1.2 $Revision: 1.7 $ $Date: 2010-05-06 14:07:46 $
 * @since 1.1
 *
 * @author M. Ranganathan   <br/>
 *
 *
 */
public class Expires
    extends SIPHeader
    implements ExpiresHeader {

    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 3134344915465784267L;

    /** expires field
     */
    protected long expires;

    /** default constructor
     */
    public Expires() {
        super(NAME);
    }

    /**
     * Return canonical form.
     * @return String
     */
    public String encodeBody() {
        return encodeBody(new StringBuilder()).toString();
    }

    protected StringBuilder encodeBody(StringBuilder buffer) {
        return buffer.append(expires);
    }

    /**
     * Gets the expires value of the ExpiresHeader. This expires value is
     *
     * relative time.
     *
     *
     *
     * @return the expires value of the ExpiresHeader.
     *
     *
     */
    public long getExpiresLong() {
        return expires;
    }
    
    /**
     * Gets the expires value of the ExpiresHeader as type int. This expires value is relative time.
     * Values larger than Integer.MAX_VALUE will be returned as Integer.MAX_VALUE.
     * @return the expires value of the ExpiresHeader.
     */
    public int getExpires() {
        int ret;
        if (expires > Integer.MAX_VALUE) {
            ret = Integer.MAX_VALUE;
        } else {
            ret = (int) expires;
        }
        return ret;
    }

    /**
     * Sets the relative expires value of the ExpiresHeader.
     * The expires value MUST be greater or equal than zero and MUST be
     * less than 2**32.
     *
     * @param expires - the new expires value of this ExpiresHeader
     *
     * @throws InvalidArgumentException if supplied value is less than zero.
     *
     *
     */
    public void setExpires(long expires) throws InvalidArgumentException {
        if (expires < 0 || expires > 4294967295L)
            throw new InvalidArgumentException("bad argument " + expires);
        this.expires = expires;
    }
    
    /**
     * Sets the relative expires value of the ExpiresHeader.
     * The expires value MUST be greater or equal than zero and MUST be
     * less than 2**31.
     *
     * @param expires - the new expires value of this ExpiresHeader
     *
     * @throws InvalidArgumentException if supplied value is less than zero.
     *
     *
     */
    public void setExpires(int expires) throws InvalidArgumentException {
        setExpires((long) expires);
    }
}
