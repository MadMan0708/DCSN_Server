/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author UP711643
 */
public class ServerCommands {

    private static final Logger LOG = Logger.getLogger(Server.class.getName());
    private Server server;

    public ServerCommands(Server server) {
        this.server = server;
    }

    public static String[] parseCommand(String params) {
        return params.split("\\s+");
    }

    public static boolean checkParamNum(int expected, String[] params) {
        if (expected == params.length) {
            return true;
        } else {
            return false;
        }
    }

    public void start(String[] params) {
        if (checkParamNum(0, params)) {
            server.startListening();
        } else {
            LOG.log(Level.INFO, "Command has no parameters");
        }
    }

    public void getInfo(String[] params) {
        if (checkParamNum(0, params)) {
            server.printPort();
            server.printBaseDir();
        } else {
            LOG.log(Level.INFO, "Command has no parameters");
        }
    }

    public void setBaseDir(String[] params) {
        if (checkParamNum(1, params)) {
            server.setBaseDir(params[0]);
        } else {
            LOG.log(Level.INFO, "Expected parameters: 1");
            LOG.log(Level.INFO, "1: new base dir");
        }
    }

    public void getBaseDir(String[] params) {
        if (checkParamNum(0, params)) {
            server.printBaseDir();
        } else {
            LOG.log(Level.INFO, "Command has no parameters");
        }
    }

    public void getPort(String[] params) {
        if (checkParamNum(0, params)) {
            server.printPort();
        } else {
            LOG.log(Level.INFO, "Command has no parameters");
        }
    }

    public void stop(String[] params) {
        if (checkParamNum(0, params)) {
            server.stopListening();
        } else {
            LOG.log(Level.INFO, "Command has no parameters");
        }
    }

    public void exit(String[] params) {
        if (checkParamNum(0, params)) {
            server.exitServer();
        } else {
            LOG.log(Level.INFO, "Command has no parameters");
        }
    }
}
