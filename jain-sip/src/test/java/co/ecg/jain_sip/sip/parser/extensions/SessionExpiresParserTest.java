package co.ecg.jain_sip.sip.parser.extensions;


import co.ecg.jain_sip.sip.parser.ParserTestCase;
import co.ecg.jain_sip.sip.ri.parser.extensions.SessionExpiresParser;

public class SessionExpiresParserTest extends ParserTestCase {

    public void testParser() {
        String to[] =
                {"Session-Expires: 30\n",
                        "Session-Expires: 45;refresher=uac\n",
                };
        super.testParser(SessionExpiresParser.class, to);


    }

}
