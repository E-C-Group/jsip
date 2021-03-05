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
 * File Name     : ContentLengthHeader.java
 * Author        : Phelim O'Doherty
 * <p>
 * HISTORY
 * Version   Date      Author              Comments
 * 1.1     08/10/2002  Phelim O'Doherty
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */
package co.ecg.jain_sip.sip.header;

import co.ecg.jain_sip.sip.InvalidArgumentException;

/**
 * The Content-Length header field indicates the size of the message-body, in
 * decimal number of octets, sent to the recipient. Applications SHOULD use
 * this field to indicate the size of the message-body to be transferred,
 * regardless of the media type of the entity.  If a stream-based protocol
 * (such as TCP) is used as transport, the header field MUST be used.
 * <p>
 * The size of the message-body does not include the CRLF separating header
 * fields and body.  Any Content-Length greater than or equal to zero is a
 * valid value.  If no body is present in a message, then the Content-Length
 * header field value MUST be set to zero.
 *
 * @author BEA Systems, NIST
 * @version 1.2
 * @see ContentDispositionHeader
 * @see ContentTypeHeader
 * @see ContentEncodingHeader
 * @see ContentLanguageHeader
 */


public interface ContentLengthHeader extends Header {


    /**
     * Set content-length of ContentLengthHeader. The content-length must be
     * greater than or equal to zero.
     *
     * @param contentLength the content-length of the message body
     *                      <p>
     *                      as a decimal number of octets.
     * @throws InvalidArgumentException if contentLength is less than zero.
     */

    public void setContentLength(int contentLength) throws InvalidArgumentException;


    /**
     * Gets content-length of the message body.
     *
     * @return content-length of the message body as a decimal number of octets.
     */

    public int getContentLength();


    /**
     * Name of ContentLengthHeader
     */

    public final static String NAME = "Content-Length";

}

