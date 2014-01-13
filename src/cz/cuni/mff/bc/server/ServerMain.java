/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import java.io.IOException;

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
}
