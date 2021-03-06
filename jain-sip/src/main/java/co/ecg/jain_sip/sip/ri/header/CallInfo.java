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

import co.ecg.jain_sip.sip.address.URI;
import co.ecg.jain_sip.sip.header.CallInfoHeader;
import co.ecg.jain_sip.sip.ri.address.GenericURI;

import java.text.ParseException;

/**
 * CallInfo SIPHeader.
 *
 * @author "M. Ranganathan"  <br/>
 * @version 1.2 $Revision: 1.8 $ $Date: 2010-05-06 14:07:48 $
 * @since 1.1
 */
public final class CallInfo
        extends ParametersHeader
        implements CallInfoHeader {

    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = -8179246487696752928L;

    protected GenericURI info;

    /**
     * Default constructor
     */
    public CallInfo() {
        super(CALL_INFO);
    }

    /**
     * Return canonical representation.
     *
     * @return String
     */
    public String encodeBody() {
        return encodeBody(new StringBuilder()).toString();
    }

    protected StringBuilder encodeBody(StringBuilder buffer) {
        buffer.append(LESS_THAN);
        info.encode(buffer);
        buffer.append(GREATER_THAN);

        if (parameters != null && !parameters.isEmpty()) {
            buffer.append(SEMICOLON);
            parameters.encode(buffer);
        }

        return buffer;
    }

    /**
     * get the purpose field
     *
     * @return String
     */
    public String getPurpose() {
        return this.getParameter("purpose");
    }

    /**
     * get the URI field
     *
     * @return URI
     */
    public URI getInfo() {
        return info;
    }

    /**
     * set the purpose field
     *
     * @param purpose is the purpose field.
     */
    public void setPurpose(String purpose) {
        if (purpose == null)
            throw new NullPointerException("null arg");
        try {
            this.setParameter("purpose", purpose);
        } catch (ParseException ex) {
        }
    }

    /**
     * set the URI field
     *
     * @param info is the URI to set.
     */
    public void setInfo(URI info) {
        this.info = (GenericURI) info;
    }
}
