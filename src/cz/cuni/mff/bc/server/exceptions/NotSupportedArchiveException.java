/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server.exceptions;

/**
 *
 * @author Jakub
 */
public class NotSupportedArchiveException extends Exception {

    public NotSupportedArchiveException() {
    }

    public NotSupportedArchiveException(String message) {
        super(message);
    }

    public NotSupportedArchiveException(Throwable cause) {
        super(cause);
    }

    public NotSupportedArchiveException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
