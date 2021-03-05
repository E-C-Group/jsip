/*
* Conditions Of Use
*
* This software was developed by employees of the National Institute of
* Standards and Technology (NIST), an agency of the Federal Government,
* and others.
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

/************************************************************************************************
 * PRODUCT OF PT INOVACAO - EST DEPARTMENT and Telecommunications Institute (Aveiro, Portugal)  *
 ************************************************************************************************/


package co.ecg.jain_sip.sip.ri.header.ims;



import gov.nist.javax.sip.header.SIPHeaderList;
import gov.nist.javax.sip.header.ims.SecurityClient;


/**
 * List of Security-Client headers.
 *
 * @author Miguel Freitas (IT) PT-Inovacao
 */


public class SecurityClientList extends SIPHeaderList<SecurityClient>
{


    private static final long serialVersionUID = 3094231003329176217L;


    public SecurityClientList()
    {
        super(SecurityClient.class, SecurityClientHeader.NAME);
    }


    public Object clone() {
        SecurityClientList retval = new SecurityClientList();
        return retval.clonehlist(this.hlist);
    }

}


