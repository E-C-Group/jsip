package co.ecg.jain_sip.sip.ri.parser.ims;
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

import java.text.ParseException;

import co.ecg.jain_sip.sip.ri.header.SIPHeader;
import co.ecg.jain_sip.sip.ri.header.ims.PUserDatabase;
import co.ecg.jain_sip.sip.ri.parser.Lexer;
import co.ecg.jain_sip.sip.ri.parser.ParametersParser;
import co.ecg.jain_sip.sip.ri.parser.TokenTypes;

/**
 * @author aayush.bhatnagar
 * Rancore Technologies Pvt Ltd, Mumbai India.
 * <p>
 * This is the parser for the P-user-database header.
 * The syntax for the P-user-database header as per
 * RFC 4457 is given below:
 * <p>
 * P-User-Database = "P-User-Database" HCOLON database
 * *( SEMI generic-param )
 * database        = LAQUOT DiameterURI RAQUOT
 * <p>
 * Eg: P-User-Database: <aaa://host.example.com;transport=tcp>
 */
public class PUserDatabaseParser extends ParametersParser implements TokenTypes {

    /**
     * @param databaseName
     */
    public PUserDatabaseParser(String databaseName) {
        super(databaseName);
    }

    /**
     * @param lexer
     */
    public PUserDatabaseParser(Lexer lexer) {
        super(lexer);
    }

    public SIPHeader parse() throws ParseException {


        dbg_enter("PUserDatabase.parse");

        try {
            this.lexer.match(TokenTypes.P_USER_DATABASE);
            this.lexer.SPorHT();
            this.lexer.match(':');
            this.lexer.SPorHT();

            PUserDatabase userDatabase = new PUserDatabase();
            this.parseheader(userDatabase);

            return userDatabase;
        } finally {
            dbg_leave("PUserDatabase.parse");
        }
    }

    private void parseheader(PUserDatabase userDatabase) throws ParseException {
        StringBuilder dbname = new StringBuilder();
        this.lexer.match(LESS_THAN);

        while (this.lexer.hasMoreChars()) {
            char next = this.lexer.getNextChar();
            if (next != '>' && next != '\n') {
                dbname.append(next);
            }

        }
        userDatabase.setDatabaseName(dbname.toString());
        super.parse(userDatabase);

    }
}
