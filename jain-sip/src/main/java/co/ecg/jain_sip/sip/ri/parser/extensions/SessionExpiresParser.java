package co.ecg.jain_sip.sip.ri.parser.extensions;

import java.text.ParseException;

import co.ecg.jain_sip.sip.*;
import co.ecg.jain_sip.sip.ri.header.SIPHeader;
import co.ecg.jain_sip.sip.ri.header.extensions.SessionExpires;
import co.ecg.jain_sip.sip.ri.parser.Lexer;
import co.ecg.jain_sip.sip.ri.parser.ParametersParser;
import co.ecg.jain_sip.sip.ri.parser.TokenTypes;

/**
 * Parser for SIP Session Expires Header.
 */
public class SessionExpiresParser extends ParametersParser {

    /**
     * protected constructor.
     *
     * @param text is the text of the header to parse
     */
    public SessionExpiresParser(String text) {
        super(text);
    }

    /**
     * constructor.
     *
     * @param lexer is the lexer passed in from the enclosing parser.
     */
    protected SessionExpiresParser(Lexer lexer) {
        super(lexer);
    }

    /**
     * Parse the header.
     */
    public SIPHeader parse() throws ParseException {
        SessionExpires se = new SessionExpires();

        dbg_enter("parse");

        try {
            headerName(TokenTypes.SESSIONEXPIRES_TO);

            String nextId = lexer.getNextId();

            try {
                int delta = Integer.parseInt(nextId);
                se.setExpires(delta);
            } catch (NumberFormatException ex) {
                throw createParseException("bad integer format");
            } catch (InvalidArgumentException ex) {
                throw createParseException(ex.getMessage());
            }
            // May have parameters...
            this.lexer.SPorHT();
            super.parse(se);
            return se;

        } finally {
            dbg_leave("parse");
        }

    }

    public static void main(String args[]) throws ParseException {
        String to[] =
                {"Session-Expires: 30\n",
                        "Session-Expires: 45;refresher=uac\n",
                };

        for (int i = 0; i < to.length; i++) {
            SessionExpiresParser tp = new SessionExpiresParser(to[i]);
            SessionExpires t = (SessionExpires) tp.parse();
            System.out.println("encoded = " + t.encode());
            System.out.println("\ntime=" + t.getExpires());
            if (t.getParameter("refresher") != null)
                System.out.println("refresher=" + t.getParameter("refresher"));

        }
    }


}

