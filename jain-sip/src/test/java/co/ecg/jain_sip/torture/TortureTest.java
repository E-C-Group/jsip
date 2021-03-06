/*
* Conditions Of Use
*
* This software was developed by employees of the National Institute of
* Standards and Technology (NIST), and others.
* This software is has been contributed to the public domain.
* As a result, a formal license is not needed to use the software.
*
* This software is provided "AS IS."
* NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
* OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
* AND DATA ACCURACY.  NIST does not warrant or make any representations
* regarding the use of the software or the results thereof, including but
* not limited to the correctness, accuracy, reliability or usefulness of
* the software.
*
*
*/
package co.ecg.jain_sip.torture;

import junit.framework.TestCase;

public class TortureTest extends TestCase {


    public void setUp () {

    }

    public void testParser( ) throws Exception {
        Torture torture = new Torture();

        torture.doTests();
        if (torture.failureFlag) {
            fail(torture.failureReason);
        } else {
            System.out.println("Torture tests passed!");
        }
    }

}
