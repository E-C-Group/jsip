package co.ecg.jain_sip.sip.ri.parser.extensions;

import co.ecg.jain_sip.sip.ri.header.SIPHeader;
import co.ecg.jain_sip.sip.ri.header.extensions.Replaces;
import co.ecg.jain_sip.sip.ri.parser.Lexer;
import co.ecg.jain_sip.sip.ri.parser.ParametersParser;
import co.ecg.jain_sip.sip.ri.parser.TokenTypes;

import java.text.ParseException;

// Parser for Replaces Header (RFC3891)
// Extension by pmusgrave
//
// Replaces        = "Replaces" HCOLON callid *(SEMI replaces-param)
// replaces-param  = to-tag / from-tag / early-flag / generic-param
// to-tag          = "to-tag" EQUAL token
// from-tag        = "from-tag" EQUAL token
// early-flag      = "early-only"
//
// TODO Should run a test case on early-only
//

public class ReplacesParser extends ParametersParser {

    /**
     * Creates new CallIDParser
     *
     * @param callID message to parse
     */
    public ReplacesParser(String callID) {
        super(callID);
    }

    /**
     * Constructor
     *
     * @param lexer Lexer to set
     */
    protected ReplacesParser(Lexer lexer) {
        super(lexer);
    }

    /**
     * parse the String message
     *
     * @return SIPHeader (CallID object)
     * @throws ParseException if the message does not respect the spec.
     */
    public SIPHeader parse() throws ParseException {

        dbg_enter("parse");

        try {
            headerName(TokenTypes.REPLACES_TO);

            Replaces replaces = new Replaces();
            this.lexer.SPorHT();
            String callId = lexer.byteStringNoSemicolon();
            this.lexer.SPorHT();
            super.parse(replaces);
            replaces.setCallId(callId);
            return replaces;
        } finally {
            dbg_leave("parse");
        }
    }

    public static void main(String args[]) throws ParseException {
        String to[] =
                {"Replaces: 12345th5z8z\n",
                        "Replaces: 12345th5z8z;to-tag=tozght6-45;from-tag=fromzght789-337-2\n",
                };

        for (int i = 0; i < to.length; i++) {
            ReplacesParser tp = new ReplacesParser(to[i]);
            Replaces t = (Replaces) tp.parse();
            System.out.println("Parsing => " + to[i]);
            System.out.print("encoded = " + t.encode() + "==> ");
            System.out.println("callId " + t.getCallId() + " from-tag=" + t.getFromTag()
                    + " to-tag=" + t.getToTag());

        }
    }

}
