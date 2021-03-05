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

import java.text.ParseException;

import co.ecg.jain_sip.core.ri.Token;
import co.ecg.jain_sip.sip.*;
import co.ecg.jain_sip.sip.ri.header.RAck;
import co.ecg.jain_sip.sip.ri.header.SIPHeader;
import co.ecg.jain_sip.sip.ri.header.SIPHeaderNames;

/**
 * Parser for RAck header.
 *
 * @version 1.2 $Revision: 1.8 $ $Date: 2009-07-17 18:58:02 $
 *
 * @author Olivier Deruelle   <br/>
 * @author M. Ranganathan   <br/>
 *
 *
 *
 * @version 1.0
 */
public class RAckParser extends HeaderParser {

    /**
     * Creates a new instance of RAckParser
     * @param rack the header to parse
     */
    public RAckParser(String rack) {
        super(rack);
    }

    /**
     * Constructor
     * @param lexer the lexer to use to parse the header
     */
    protected RAckParser(Lexer lexer) {
        super(lexer);
    }

    /**
     * parse the String message
     * @return SIPHeader (RAck object)
     * @throws ParseException if the message does not respect the spec.
     */
    public SIPHeader parse() throws ParseException {


            dbg_enter("RAckParser.parse");
        RAck rack = new RAck();
        try {
            headerName(TokenTypes.RACK);

            rack.setHeaderName(SIPHeaderNames.RACK);

            try {
                String number = this.lexer.number();
                rack.setRSequenceNumber(Long.parseLong(number));
                this.lexer.SPorHT();
                number = this.lexer.number();
                rack.setCSequenceNumber(Long.parseLong(number));
                this.lexer.SPorHT();
                this.lexer.match(TokenTypes.ID);
                Token token = lexer.getNextToken();
                rack.setMethod(token.getTokenValue());

            } catch (InvalidArgumentException ex) {
                throw createParseException(ex.getMessage());
            }
            this.lexer.SPorHT();
            this.lexer.match('\n');

            return rack;
        } finally {

                dbg_leave("RAckParser.parse");
        }
    }

    /** Test program
    public static void main(String args[]) throws ParseException {
    String r[] = {
            "RAck: 776656 1 INVITE\n"
            };

    for (int i = 0; i < r.length; i++ ) {
        RAckParser parser =
          new RAckParser(r[i]);
        RAck ra= (RAck) parser.parse();
        System.out.println("encoded = " + ra.encode());
    }
    }
     */
}
/*
 * $Log: not supported by cvs2svn $
 * Revision 1.7  2006/08/15 21:44:50  mranga
 * Issue number:
 * Obtained from:
 * Submitted by:  mranga
 * Reviewed by:   mranga
 * Incorporating the latest API changes from Phelim
 * CVS: ----------------------------------------------------------------------
 * CVS: Issue number:
 * CVS:   If this change addresses one or more issues,
 * CVS:   then enter the issue number(s) here.
 * CVS: Obtained from:
 * CVS:   If this change has been taken from another system,
 * CVS:   then name the system in this line, otherwise delete it.
 * CVS: Submitted by:
 * CVS:   If this code has been contributed to the project by someone else; i.e.,
 * CVS:   they sent us a patch or a set of diffs, then include their name/email
 * CVS:   address here. If this is your work then delete this line.
 * CVS: Reviewed by:
 * CVS:   If we are doing pre-commit code reviews and someone else has
 * CVS:   reviewed your changes, include their name(s) here.
 * CVS:   If you have not had it reviewed then delete this line.
 *
 * Revision 1.6  2006/07/13 09:02:24  mranga
 * Issue number:
 * Obtained from:
 * Submitted by:  jeroen van bemmel
 * Reviewed by:   mranga
 * Moved some changes from co.ecg.jain-sip-1.2 to java.net
 *
 * CVS: ----------------------------------------------------------------------
 * CVS: Issue number:
 * CVS:   If this change addresses one or more issues,
 * CVS:   then enter the issue number(s) here.
 * CVS: Obtained from:
 * CVS:   If this change has been taken from another system,
 * CVS:   then name the system in this line, otherwise delete it.
 * CVS: Submitted by:
 * CVS:   If this code has been contributed to the project by someone else; i.e.,
 * CVS:   they sent us a patch or a set of diffs, then include their name/email
 * CVS:   address here. If this is your work then delete this line.
 * CVS: Reviewed by:
 * CVS:   If we are doing pre-commit code reviews and someone else has
 * CVS:   reviewed your changes, include their name(s) here.
 * CVS:   If you have not had it reviewed then delete this line.
 *
 * Revision 1.4  2006/06/19 06:47:27  mranga
 * javadoc fixups
 *
 * Revision 1.3  2006/06/16 15:26:28  mranga
 * Added NIST disclaimer to all public domain files. Clean up some javadoc. Fixed a leak
 *
 * Revision 1.2  2006/05/24 06:21:43  mranga
 * change to use the long setter
 *
 * Revision 1.1.1.1  2005/10/04 17:12:35  mranga
 *
 * Import
 *
 *
 * Revision 1.4  2004/01/22 13:26:31  sverker
 * Issue number:
 * Obtained from:
 * Submitted by:  sverker
 * Reviewed by:   mranga
 *
 * Major reformat of code to conform with style guide. Resolved compiler and javadoc warnings. Added CVS tags.
 *
 * CVS: ----------------------------------------------------------------------
 * CVS: Issue number:
 * CVS:   If this change addresses one or more issues,
 * CVS:   then enter the issue number(s) here.
 * CVS: Obtained from:
 * CVS:   If this change has been taken from another system,
 * CVS:   then name the system in this line, otherwise delete it.
 * CVS: Submitted by:
 * CVS:   If this code has been contributed to the project by someone else; i.e.,
 * CVS:   they sent us a patch or a set of diffs, then include their name/email
 * CVS:   address here. If this is your work then delete this line.
 * CVS: Reviewed by:
 * CVS:   If we are doing pre-commit code reviews and someone else has
 * CVS:   reviewed your changes, include their name(s) here.
 * CVS:   If you have not had it reviewed then delete this line.
 *
 */
