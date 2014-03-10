/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import cz.cuni.mff.bc.server.strategies.StrategiesList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contains console commands
 *
 * @author Jakub Hava
 */
public class ServerCommands {

    private Server server;
    private static final Logger LOG = Logger.getLogger(Server.class.getName());

    /**
     * Constructor
     *
     * @param server server
     */
    public ServerCommands(Server server) {
        this.server = server;
    }

    /**
     * Splits the parameters into the array
     *
     * @param params string of parameters
     * @return parameters split in the array
     */
    public static String[] parseCommand(String params) {
        return params.split("\\s+");
    }

    /**
     * Checks if the number of parameters is correct
     *
     * @param expected expected number of parameters
     * @param params array of parameters
     * @return true if number of parameters is correct, false otherwise
     */
    public static boolean checkParamNum(int expected, String[] params) {
        if (expected == params.length) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sets the port
     *
     * @param params array of parameters
     */
    public void setPort(String[] params) {
        if (checkParamNum(1, params)) {
            try {
                server.getServerParams().setPort(Integer.parseInt(params[0]));
            } catch (IllegalArgumentException e) {
                LOG.log(Level.WARNING, "Port number has to be integer between 1 - 65535");
            }
        } else {
            LOG.log(Level.INFO, "Expected parameters: 1");
            LOG.log(Level.INFO, "1: Server new port");
        }
    }

    /**
     * Gets server port
     *
     * @param params array of parameters
     */
    public void getPort(String[] params) {
        if (checkParamNum(0, params)) {
            LOG.log(Level.INFO, "Server port is set to : {0}", server.getServerParams().getPort());
        } else {
            LOG.log(Level.INFO, "Command has no parameters");
        }
    }

    /**
     * Sets the task limit for planning for finishing projects
     *
     * @param params array of parameters
     */
    public void setTaskLimit(String[] params) {
        if (checkParamNum(1, params)) {
            try {
                server.getServerParams().setTaskLimit(Integer.parseInt(params[0]));
            } catch (NumberFormatException e) {
                LOG.log(Level.WARNING, "Task limit for planning of finishing projects has to be positive integer");
            }
        } else {
            LOG.log(Level.INFO, "Expected parameters: 1");
            LOG.log(Level.INFO, "1: Task limit for planning of finishing projects");
        }
    }

      /**
     * Gets the task limit for planning for finishing projects
     *
     * @param params array of parameters
     */
    public void getTaskLimit(String[] params) {
        if (checkParamNum(0, params)) {
                 LOG.log(Level.INFO, "Task limit for planning of finishing projects is set to : {0}", server.getServerParams().getTaskLimit());        
        } else {
            LOG.log(Level.INFO, "Command has no parameter");
        }
    }
    /**
     * Sets the strategy
     *
     * @param params array of parameters
     */
    public void setStrategy(String[] params) {
        if (checkParamNum(1, params)) {
            boolean ok = false;
            switch (params[0]) {
                case "priority":
                    ok = true;
                    server.getServerParams().setStrategy(StrategiesList.HIGHEST_PRIORITY_FIRST);
                    break;
                case "max-throughput":
                    ok = true;
                    server.getServerParams().setStrategy(StrategiesList.MAXIMAL_THROUGHPUT);
                    break;
            }
            if (ok) {
                LOG.log(Level.INFO, "Server strategy is set to: {0}", server.getServerParams().getStrategy());
            } else {
                LOG.log(Level.WARNING, "Strategy options are only: priority and max-throughput");
            }
        } else {
            LOG.log(Level.INFO, "Expected parameters: 1");
            LOG.log(Level.INFO, "1: Server strategy - priority or max-throughput");
        }
    }

    /**
     * Gets the strategy
     *
     * @param params array of parameters
     */
    public void getStrategy(String[] params) {
        if (checkParamNum(0, params)) {
            LOG.log(Level.INFO, "Server strategy is set to: {0}", server.getServerParams().getStrategy().toString());
        } else {
            LOG.log(Level.INFO, "Command has no parameters");
        }
    }

    /**
     * Prints information about server
     *
     * @param params array of parameters
     */
    public void getInfo(String[] params) {
        if (checkParamNum(0, params)) {
            getPort(params);
            getBaseDir(params);
            getStrategy(params);
        } else {
            LOG.log(Level.INFO, "Command has no parameters");
        }
    }

    /**
     * Sets the base directory
     *
     * @param params array of parameters
     */
    public void setBaseDir(String[] params) {
        if (checkParamNum(1, params)) {
            server.getServerParams().setBaseDir(params[0]);
        } else {
            LOG.log(Level.INFO, "Expected parameters: 1");
            LOG.log(Level.INFO, "1: new base dir");
        }
    }

    /**
     * Gets the base directory
     *
     * @param params array of parameters
     */
    public void getBaseDir(String[] params) {
        if (checkParamNum(0, params)) {
            LOG.log(Level.INFO, "Base dir is set to : {0}", server.getServerParams().getBaseDir());
        } else {
            LOG.log(Level.INFO, "Command has no parameters");
        }
    }

    /**
     * Starts listening for incoming sessions
     *
     * @param params array of parameters
     */
    public void start(String[] params) {
        if (checkParamNum(1, params)) {
            setPort(params);
            server.startListening();
        } else if (checkParamNum(0, params)) {
            server.startListening();
        } else {
            LOG.log(Level.INFO, "Expected parameters: 0 or 1");
            LOG.log(Level.INFO, "1: Server new port");
        }
    }

    /**
     * Stops listening for incoming sessions
     *
     * @param params array of parameters
     */
    public void stop(String[] params) {
        if (checkParamNum(0, params)) {
            server.stopListening();
        } else {
            LOG.log(Level.INFO, "Command has no parameters");
        }
    }

    /**
     * Exits the server
     *
     * @param params array of parameters
     */
    public void exit(String[] params) {
        if (checkParamNum(0, params)) {
            server.exitServer();
        } else {
            LOG.log(Level.INFO, "Command has no parameters");
        }
    }
}
