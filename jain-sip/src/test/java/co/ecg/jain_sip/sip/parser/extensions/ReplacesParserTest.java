package co.ecg.jain_sip.sip.parser.extensions;

import co.ecg.jain_sip.sip.parser.ParserTestCase;
import co.ecg.jain_sip.sip.ri.parser.extensions.ReplacesParser;

public class ReplacesParserTest extends ParserTestCase {

    public void testParser() {
        String to[] =
                {"Replaces: 12345th5z8z\n",
                        "Replaces: 12345th5z8z;to-tag=tozght6-45;from-tag=fromzght789-337-2\n",
                };

        super.testParser(ReplacesParser.class, to);


    }

}
