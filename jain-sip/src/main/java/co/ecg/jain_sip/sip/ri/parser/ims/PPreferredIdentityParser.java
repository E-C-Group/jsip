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
/*******************************************
 * PRODUCT OF PT INOVACAO - EST DEPARTMENT *
 *******************************************/

package co.ecg.jain_sip.sip.ri.parser.ims;

import java.text.ParseException;

import co.ecg.jain_sip.sip.ri.parser.Lexer;
import co.ecg.jain_sip.sip.ri.parser.TokenTypes;
import co.ecg.jain_sip.sip.ri.header.SIPHeader;
import co.ecg.jain_sip.sip.ri.header.ims.PPreferredIdentity;

import co.ecg.jain_sip.sip.ri.parser.AddressParametersParser;

/**
 * P-Preferred-Identity header parser.
 *
 * @author ALEXANDRE MIGUEL SILVA SANTOS
 */

public class PPreferredIdentityParser
    //extends AddressHeaderParser
    extends AddressParametersParser
    implements TokenTypes {

    public PPreferredIdentityParser(String preferredIdentity) {
        super(preferredIdentity);

    }


    protected PPreferredIdentityParser(Lexer lexer) {
        super(lexer);

    }

    public SIPHeader parse() throws ParseException {


            dbg_enter("PreferredIdentityParser.parse");

        try {
            this.lexer.match(TokenTypes.P_PREFERRED_IDENTITY);
            this.lexer.SPorHT();
            this.lexer.match(':');
            this.lexer.SPorHT();

            PPreferredIdentity p = new PPreferredIdentity();
            super.parse( p );
            return p;
        } finally {

                dbg_leave("PreferredIdentityParser.parse");
            }


    }

}
