package co.ecg.jain_sip.sip.parser.extensions;

import co.ecg.jain_sip.sip.parser.ParserTestCase;
import co.ecg.jain_sip.sip.ri.parser.extensions.JoinParser;

public class JoinParserTest extends ParserTestCase {

    public void testParser() {
        String to[] =
        {   "Join: 12345th5z8z\n",
            "Join: 12345th5z8z;to-tag=tozght6-45;from-tag=fromzght789-337-2\n",
        };

        super.testParser(JoinParser.class,to);


    }

}
