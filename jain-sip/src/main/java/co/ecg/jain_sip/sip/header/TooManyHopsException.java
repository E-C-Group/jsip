/**
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Unpublished - rights reserved under the Copyright Laws of the United States.
 * Copyright (C) 2003 Sun Microsystems, Inc. All rights reserved.
 * Copyright (C) 2005 BEA Systems, Inc. All rights reserved.
 * <p>
 * Use is subject to license terms.
 * <p>
 * This distribution may include materials developed by third parties.
 * <p>
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * <p>
 * Module Name   : JSIP Specification
 * File Name     : TooManyHopsException.java
 * Author        : Phelim O'Doherty
 * <p>
 * HISTORY
 * Version   Date      Author              Comments
 * 1.1     20/12/2002  Phelim O'Doherty    Initial version
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */
package co.ecg.jain_sip.sip.header;

import co.ecg.jain_sip.sip.SipException;

/**
 * This Exception is thrown when a user attempts decrement the Hop count when
 * the message as already reached its max number of forwards.
 *
 * @author BEA Systems, NIST
 * @version 1.2
 */
public class TooManyHopsException extends SipException {

    /**
     * Constructs a new <code>TooManyHopsException</code>
     */
    public TooManyHopsException() {
        super();
    }

    /**
     * Constructs a new <code>TooManyHopsException</code> with
     * the specified error message.
     *
     * @param  message the detail of the error message
     */
    public TooManyHopsException(String message) {
        super(message);
    }

    /**
     * Constructs a new <code>TooManyHopsException</code> with the
     * specified error message and specialized cause that triggered this error
     * condition.
     *
     * @param  message the detail of the error message
     * @param  cause  the specialized cause that triggered this exception
     */
    public TooManyHopsException(String message, Throwable cause) {
        super(message, cause);
    }


}

