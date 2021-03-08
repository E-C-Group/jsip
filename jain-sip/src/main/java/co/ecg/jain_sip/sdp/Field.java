/*
 * Field.java
 *
 * Created on December 18, 2001, 10:42 AM
 */

package co.ecg.jain_sip.sdp;

import java.io.*;

/**
 * A Field represents a single line of information within a SDP
 * session description.
 *
 * @author deruelle
 * @version 1.0
 */
public interface Field extends Serializable {

    /**
     * Returns the type character for the field.
     *
     * @return the type character for the field.
     */
    public char getTypeChar();
}

