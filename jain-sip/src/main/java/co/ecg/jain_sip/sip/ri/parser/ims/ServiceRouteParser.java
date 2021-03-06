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
 * PRODUCT OF PT INOVAO - EST DEPARTMENT *
 *******************************************/

package co.ecg.jain_sip.sip.ri.parser.ims;

import java.text.ParseException;

import co.ecg.jain_sip.sip.ri.header.ims.ServiceRoute;
import co.ecg.jain_sip.sip.ri.header.ims.ServiceRouteList;
import co.ecg.jain_sip.sip.ri.header.SIPHeader;
import co.ecg.jain_sip.sip.ri.parser.AddressParametersParser;
import co.ecg.jain_sip.sip.ri.parser.Lexer;
import co.ecg.jain_sip.sip.ri.parser.TokenTypes;

/**
 * Service-Route header parser.
 *
 * @author ALEXANDRE MIGUEL SILVA SANTOS
 */

public class ServiceRouteParser extends AddressParametersParser {

    /**
     * Constructor
     */
    public ServiceRouteParser(String serviceRoute) {
        super(serviceRoute);

    }

    protected ServiceRouteParser(Lexer lexer) {
        super(lexer);

    }




    /**
     * parse the String message and generate the RecordRoute List Object
     * @return SIPHeader the RecordRoute List object
     * @throws ParseException if errors occur during the parsing
     */

    public SIPHeader parse() throws ParseException {
        ServiceRouteList serviceRouteList = new ServiceRouteList();


            dbg_enter("ServiceRouteParser.parse");

        try {
            this.lexer.match(TokenTypes.SERVICE_ROUTE);
            this.lexer.SPorHT();
            this.lexer.match(':');
            this.lexer.SPorHT();
            while (true) {
                ServiceRoute serviceRoute = new ServiceRoute();
                super.parse(serviceRoute);
                serviceRouteList.add(serviceRoute);
                this.lexer.SPorHT();
                if (lexer.lookAhead(0) == ',') {
                    this.lexer.match(',');
                    this.lexer.SPorHT();
                } else if (lexer.lookAhead(0) == '\n')
                    break;
                else
                    throw createParseException("unexpected char");
            }
            return serviceRouteList;
        } finally {

                dbg_leave("ServiceRouteParser.parse");
        }

    }

}
