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
/****************************************************************************
 * PRODUCT OF PT INOVACAO - EST DEPARTMENT and Aveiro University (Portugal) *
 ****************************************************************************/

package co.ecg.jain_sip.sip.ri.parser.ims;

import java.text.ParseException;

import co.ecg.jain_sip.core.ri.Token;
import co.ecg.jain_sip.sip.ri.address.GenericURI;
import co.ecg.jain_sip.sip.ri.header.ims.PAssociatedURI;
import co.ecg.jain_sip.sip.ri.header.ims.PAssociatedURIList;
import co.ecg.jain_sip.sip.ri.header.ims.SIPHeaderNamesIms;

import co.ecg.jain_sip.sip.ri.header.Allow;
import co.ecg.jain_sip.sip.ri.header.ErrorInfo;
import co.ecg.jain_sip.sip.ri.header.SIPHeader;
import co.ecg.jain_sip.sip.ri.header.SIPHeaderNames;
import co.ecg.jain_sip.sip.ri.parser.Lexer;
import co.ecg.jain_sip.sip.ri.parser.TokenTypes;
import co.ecg.jain_sip.sip.ri.parser.AddressParametersParser;
import co.ecg.jain_sip.sip.ri.parser.URLParser;

import co.ecg.jain_sip.sip.ri.parser.ParametersParser;
import co.ecg.jain_sip.sip.ri.parser.HeaderParser;


/**
 * P-Associated-URI header parser
 *
 * @author Miguel Freitas (IT) PT-Inovacao
 */

public class PAssociatedURIParser
    extends AddressParametersParser
{


    /**
     * Constructor
     * @param associatedURI content to set
     */
    public PAssociatedURIParser(String associatedURI)
    {
        super(associatedURI);
    }

    protected PAssociatedURIParser(Lexer lexer)
    {
        super(lexer);
    }


    public SIPHeader parse() throws ParseException
    {

            dbg_enter("PAssociatedURIParser.parse");

        PAssociatedURIList associatedURIList = new PAssociatedURIList();

        try {

            headerName(TokenTypes.P_ASSOCIATED_URI);

            PAssociatedURI associatedURI = new PAssociatedURI();
            associatedURI.setHeaderName(SIPHeaderNamesIms.P_ASSOCIATED_URI);

            super.parse(associatedURI);
            associatedURIList.add(associatedURI);

            this.lexer.SPorHT();
            while (lexer.lookAhead(0) == ',')
            {
                this.lexer.match(',');
                this.lexer.SPorHT();

                associatedURI = new PAssociatedURI();
                super.parse(associatedURI);
                associatedURIList.add(associatedURI);

                this.lexer.SPorHT();
            }
            this.lexer.SPorHT();
            this.lexer.match('\n');

            return associatedURIList;




        } finally {

                dbg_leave("PAssociatedURIParser.parse");
        }

    }





    /** Test program
    public static void main(String args[]) throws ParseException
    {
        String rou[] = {

                "P-Associated-URI: <sip:123qwe@ptinovacao.pt>\n",

                    "P-Associated-URI: <sip:testes1@ptinovacao.pt>,  " +
                                    "<sip:testes2@ptinovacao.pt> \n"
                    };

        for (int i = 0; i < rou.length; i++ ) {
            PAssociatedURIParser rp =
              new PAssociatedURIParser(rou[i]);
            PAssociatedURIList list = (PAssociatedURIList) rp.parse();
            System.out.println("encoded = " +list.encode());
        }
    }

    */

}
