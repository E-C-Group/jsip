package co.ecg.jain_sip.sip.parser.extensions;


import co.ecg.jain_sip.sip.parser.ParserTestCase;
import co.ecg.jain_sip.sip.ri.parser.extensions.MinSEParser;

public class MinSEParserTest extends ParserTestCase {

    public void testParser() {

        String to[] =
                {"Min-SE: 30\n",
                        "Min-SE: 45;some-param=somevalue\n",
                };

        super.testParser(MinSEParser.class, to);

    }

}
