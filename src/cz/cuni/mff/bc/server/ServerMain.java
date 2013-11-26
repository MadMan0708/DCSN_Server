/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import java.io.IOException;
import java.nio.file.Paths;

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
        
        switch (args.length) {
            case 0:
                server.startGUIConsole();
                server.startClassicConsole();
                server.initialize();
                break;
            case 1:
                if (args[0].equals("nogui")) {
                    server.startClassicConsole();
                    server.initialize();
                } 
                else{
                    System.err.println("Incorrect parameter");
                }
                break;
            default:
                System.err.println("Wrong number of parameters");
                break;
        }
        
    }
}
