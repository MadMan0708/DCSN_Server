/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import cz.cuni.mff.bc.server.logging.CustomFormater;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main entry point of the server side
 *
 * @author Jakub Hava
 */
public class ServerMain {

    /**
     * Main method
     *
     * @param args the command line arguments
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        final Server server = new Server();
        getAndSetConsoleHandler();
        switch (args.length) {
            case 0:
                server.startGUIConsole();
                server.startClassicConsole();
                server.initialise();
                break;
            case 1:
                if (args[0].equals("nogui")) {
                    server.startClassicConsole();
                    server.initialise();
                } else {
                    System.err.println("Incorrect parameter");
                }
                break;
            default:
                System.err.println("Wrong number of parameters");
                break;
        }
    }

    /**
     * Gets console handler and sets the logging level
     *
     * @return console handler
     */
    public static ConsoleHandler getAndSetConsoleHandler() {
        ConsoleHandler consoleHandler = null;
        Handler[] handlers = Logger.getLogger("").getHandlers();
        for (Handler handler : handlers) {
            if (handler instanceof ConsoleHandler) {
                handler.setLevel(Level.INFO);
                handler.setFormatter(new CustomFormater());
                consoleHandler = (ConsoleHandler) handler;
                break;
            }
        }
        return consoleHandler;
    }
}
