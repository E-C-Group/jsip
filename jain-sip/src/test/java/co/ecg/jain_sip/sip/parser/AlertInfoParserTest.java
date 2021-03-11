package co.ecg.jain_sip.sip.parser;

import co.ecg.jain_sip.sip.ri.parser.AlertInfoParser;

public class AlertInfoParserTest extends ParserTestCase {
    public void testParser() {
        String r[] = {
                "Alert-Info: <http://wwww.example.com/alice/photo.jpg> ;purpose=icon,"
                        + "<http://www.example.com/alice/> ;purpose=info\n",
                "Alert-Info: PolycomSpecialBug;param=blah\n",
                "Alert-Info: <http://wwww.example.com/alice/photo1.jpg>\n"};
        super.testParser(AlertInfoParser.class, r);
    }


}
