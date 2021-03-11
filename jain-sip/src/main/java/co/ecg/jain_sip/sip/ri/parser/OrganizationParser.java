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
package co.ecg.jain_sip.sip.ri.parser;

import co.ecg.jain_sip.sip.ri.header.Organization;
import co.ecg.jain_sip.sip.ri.header.SIPHeader;

import java.text.ParseException;

/**
 * Parser for Organization header.
 *
 * @author Olivier Deruelle   <br/>
 * @author M. Ranganathan   <br/>
 * @version 1.2 $Revision: 1.8 $ $Date: 2009-07-17 18:58:01 $
 */
public class OrganizationParser extends HeaderParser {

    /**
     * Creates a new instance of OrganizationParser
     *
     * @param organization the header to parse
     */
    public OrganizationParser(String organization) {
        super(organization);
    }

    /**
     * Constructor
     *
     * @param lexer the lexer to use to parse the header
     */
    protected OrganizationParser(Lexer lexer) {
        super(lexer);
    }

    /**
     * parse the String header
     *
     * @return SIPHeader (Organization object)
     * @throws ParseException if the message does not respect the spec.
     */
    public SIPHeader parse() throws ParseException {


        dbg_enter("OrganizationParser.parse");
        Organization organization = new Organization();
        try {
            headerName(TokenTypes.ORGANIZATION);
            String value = this.lexer.getRest();
            organization.setOrganization(value.trim());
            return organization;
        } finally {

            dbg_leave("OrganizationParser.parse");
        }
    }


}