package test.unit.gov.nist.javax.sip.parser.ims;
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
import co.ecg.jain_sip.sip.ri.header.HeaderFactoryImpl;
import co.ecg.jain_sip.sip.ri.header.ims.PPreferredServiceHeader;
import co.ecg.jain_sip.sip.ri.parser.ims.PPreferredServiceParser;
import test.unit.gov.nist.javax.sip.parser.ParserTestCase;
/**
 *
 * @author aayush.bhatnagar
 *
 */
public class PPreferredServiceParserTest extends ParserTestCase{

    @Override
    public void testParser() {

        System.out.println("***********************************************");
        System.out.println(" parsie parsie......");
        System.out.println("***********************************************");

        String p_pref_serv[] ={"P-Preferred-Service: urn:urn-7:3gpp-service.videogaming.service1\n",
                               "P-Preferred-Service: urn:urn-7:3gpp-application.chatterboi.service2\n",
                               "P-Preferred-Service: urn:urn-7:3gpp-service.photoshare.collage.privacyenabled.service3\n",
                               "P-Preferred-Service: urn:urn-7:3gpp-application.calltracerservice\n",
                               "P-Preferred-Service: urn:urn-7:3gpp-service.missedcallalertservice\n",
                               "P-Preferred-Service: urn:urn-7:3gpp-service.ims.icsi.mmtel.gsma.ipcall\n"};

        super.testParser(PPreferredServiceParser.class, p_pref_serv);

        System.out.println("***********************************************");
        System.out.println("Let us now test the usage of this header from the");
        System.out.println("perspective of the application");
        System.out.println("***********************************************");


        HeaderFactoryImpl himpl = new HeaderFactoryImpl();

        PPreferredServiceHeader ppsh = himpl.createPPreferredServiceHeader();

        // This is a 3gpp-service type:
        ppsh.setSubserviceIdentifiers("chatroom.presenceenabled.photoshareservice");

        System.out.println("The encoded header is-----> "+ppsh.toString());
        System.out.println("The sub-service defined is---->"+ppsh.getSubserviceIdentifiers());

    }

}
