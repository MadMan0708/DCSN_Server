/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server.exceptions;

import java.io.IOException;

/**
 *
 * @author Jakub
 */
public class ExtractionException extends IOException {

    public ExtractionException() {
    }

    public ExtractionException(String message) {
        super(message);
    }

    public ExtractionException(Throwable cause) {
        super(cause);
    }

    public ExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
