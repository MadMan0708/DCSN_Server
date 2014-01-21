/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server.logging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Custom logger
 *
 * @author Jakub Hava
 */
public class CustomLogger {

    File logFile;

    public CustomLogger(String fileName) {

        logFile = new File(fileName);
    }

    public void log(String message, Object... args) {
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)))) {
            Date date = new Date();
            pw.printf(date.toString() + " : " + message + "\n", args);
        } catch (IOException e) {
            System.err.println("Can not write message to log file:" + logFile.getAbsolutePath());
        }
    }
}
