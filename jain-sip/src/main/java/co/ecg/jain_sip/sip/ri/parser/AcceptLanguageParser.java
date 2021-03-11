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
/*
 * AcceptLanguageParser.java
 *
 * Created on June 10, 2002, 3:31 PM
 */

package co.ecg.jain_sip.sip.ri.parser;

import co.ecg.jain_sip.core.ri.Token;
import co.ecg.jain_sip.sip.ri.header.AcceptLanguage;
import co.ecg.jain_sip.sip.ri.header.AcceptLanguageList;

import co.ecg.jain_sip.sip.*;
import co.ecg.jain_sip.sip.ri.header.SIPHeader;

import java.text.ParseException;


/**
 * Parser for Accept Language Headers.
 * <p>
 * Accept Language body.
 * <pre>
 *
 * Accept-Language = "Accept-Language" ":"
 *                         1#( language-range [ ";" "q" "=" qvalue ] )
 *       language-range  = ( ( 1*8ALPHA *( "-" 1*8ALPHA ) ) | "*" )
 *
 * HTTP RFC 2616 Section 14.4
 * </pre>
 * <p>
 * Accept-Language: da, en-gb;q=0.8, en;q=0.7
 *
 * @author Olivier Deruelle
 * @version 1.2 $Revision: 1.8 $ $Date: 2009-07-17 18:57:56 $
 * @see AcceptLanguageList
 */
public class AcceptLanguageParser extends HeaderParser {

    /**
     * Constructor
     *
     * @param acceptLanguage AcceptLanguage message to parse
     */
    public AcceptLanguageParser(String acceptLanguage) {
        super(acceptLanguage);
    }

    /**
     * Constructor
     *
     * @param lexer Lexer to set
     */
    protected AcceptLanguageParser(Lexer lexer) {
        super(lexer);
    }

    /**
     * parse the String message
     *
     * @return SIPHeader (AcceptLanguage object)
     * @throws ParseException if the message does not respect the spec.
     */
    public SIPHeader parse() throws ParseException {
        AcceptLanguageList acceptLanguageList = new AcceptLanguageList();

        dbg_enter("AcceptLanguageParser.parse");

        try {
            headerName(TokenTypes.ACCEPT_LANGUAGE);
            do {
                AcceptLanguage acceptLanguage = new AcceptLanguage();
                // acceptLanguage.setHeaderName(SIPHeaderNames.ACCEPT_LANGUAGE);
                this.lexer.SPorHT();
                if (lexer.startsId()) {
                    // Content-Coding:
                    Token value = lexer.match(TokenTypes.ID);    // e.g. "en-gb" or '*'
                    acceptLanguage.setLanguageRange(value.getTokenValue());
                    this.lexer.SPorHT();
                    while (lexer.lookAhead(0) == ';') {
                        this.lexer.match(';');
                        this.lexer.SPorHT();
                        this.lexer.match('q');
                        this.lexer.SPorHT();
                        this.lexer.match('=');
                        this.lexer.SPorHT();
                        lexer.match(TokenTypes.ID);
                        value = lexer.getNextToken();
                        try {
                            float fl = Float.parseFloat(value.getTokenValue());
                            acceptLanguage.setQValue(fl);
                        } catch (NumberFormatException ex) {
                            throw createParseException(ex.getMessage());
                        } catch (InvalidArgumentException ex) {
                            throw createParseException(ex.getMessage());
                        }
                        this.lexer.SPorHT();
                    }
                }
                acceptLanguageList.add(acceptLanguage);
                if (lexer.lookAhead(0) == ',') {
                    this.lexer.match(',');
                    this.lexer.SPorHT();
                } else
                    break;
            } while (true);
        } finally {
            dbg_leave("AcceptLanguageParser.parse");
        }

        return acceptLanguageList;
    }
}
