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
package co.ecg.jain_sip.sip.ri.stack;

import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;

/**
 * Contributed by Alexander Saveliev, Avistar Communications for Issue http://java.net/jira/browse/JSIP-430
 * Allows to choose between direct vs non direct buffers
 */
@Slf4j
public class ByteBufferFactory {

    private static ByteBufferFactory instance = new ByteBufferFactory();

    private boolean useDirect = true;

    public static ByteBufferFactory getInstance() {
        return instance;
    }

    public ByteBuffer allocateDirect(int capacity) {
        log.trace("Allocating direct buffer " + capacity);
        return useDirect ? ByteBuffer.allocateDirect(capacity) : ByteBuffer.allocate(capacity);
    }

    public ByteBuffer allocate(int capacity) {
        log.trace("Allocating buffer " + capacity);
        return ByteBuffer.allocate(capacity);
    }

    public void setUseDirect(boolean useDirect) {
        log.trace("Direct buffers are " + (useDirect ? "enabled" : "disabled"));
        this.useDirect = useDirect;
    }
}
