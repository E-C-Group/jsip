package gov.nist.javax.sip.parser.extensions;

import co.ecg.jain_sip.core.ri.Token;
import co.ecg.jain_sip.sip.ri.header.Reason;
import co.ecg.jain_sip.sip.ri.header.ReasonList;
import co.ecg.jain_sip.sip.ri.header.SIPHeader;
import co.ecg.jain_sip.sip.ri.header.extensions.References;
import co.ecg.jain_sip.sip.ri.parser.Lexer;
import co.ecg.jain_sip.sip.ri.parser.ParametersParser;
import co.ecg.jain_sip.sip.ri.parser.TokenTypes;

import java.text.ParseException;

public class ReferencesParser extends ParametersParser {
    /**
     * Creates a new instance of ReferencesParser
     * @param references the header to parse
     */
    public ReferencesParser(String references) {
        super(references);
    }

    /**
     * Constructor
     * @param lexer the lexer to use to parse the header
     */
    protected ReferencesParser(Lexer lexer) {
        super(lexer);
    }
    
    /**
     * parse the String message
     * @return SIPHeader (ReasonParserList object)
     * @throws SIPParseException if the message does not respect the spec.
     */
    public SIPHeader parse() throws ParseException {
       
        if (debug)
            dbg_enter("ReasonParser.parse");

        try {
            headerName(TokenTypes.REFERENCES);
            References references= new References();
            this.lexer.SPorHT();
               
            String callId = lexer.byteStringNoSemicolon();
           
            references.setCallId(callId);
            super.parse(references);
            return references;
       } finally {
            if (debug)
                dbg_leave("ReferencesParser.parse");
        }

      
    }

}
