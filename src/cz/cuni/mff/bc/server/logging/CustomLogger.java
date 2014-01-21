/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server.logging;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Custom logger
 *
 * @author Jakub Hava
 */
public class CustomLogger {

    FileLogger fileLogger;

    public CustomLogger(String fileName) {

        fileLogger = new FileLogger(new File(fileName));
    }

    public void log(String message) {
        Date date = new Date();
        fileLogger.log(date.toString() + " : " + message);
    }
}
