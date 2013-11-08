/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import java.io.IOException;

/**
 *
 * @author Aku
 */
public class ServerMain {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException{
        final Server server = new Server();
         server.startGUIConsole();
        server.initialize();
        if (System.console() != null) {
            server.startClassicConsole();
        }
    }
}
