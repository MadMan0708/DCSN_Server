/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import cz.cuni.mff.bc.server.strategies.StrategiesList;
import cz.cuni.mff.bc.misc.PropertiesManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads and stores server parameters
 *
 * @author Jakub Hava
 */
public class ServerParams {

    private static PropertiesManager propManager;
    private StrategiesList strategy;
    private int taskLimit;
    private int port;
    private Path basedir;
    private static final Logger LOG = Logger.getLogger(Server.class.getName());

    /**
     * Constructor
     *
     * @param logHandler logging handler
     */
    public ServerParams(Handler logHandler) {
        propManager = new PropertiesManager("server.config.properties", logHandler);
    }

    private void setDefaultPort() {
        setPort(1099);
    }

    private void setDefaultStrategy() {
        setStrategy(StrategiesList.HIGHEST_PRIORITY_FIRST);
    }
    
    private void setDefaultTaskLimit(){
        setTaskLimit(100);
    }
    /**
     * Gets the port
     *
     * @return port
     */
    public int getPort() {
        return port;
    }
    
    /**
     *Gets the task limit for absolute planning for finishing projects
     * @return the task limit for absolute planning for finishing projects
     */
    public int getTaskLimit(){
        return taskLimit;
    }

    /**
     * Sets the port
     *
     * @param port port
     * @throws IllegalArgumentException
     */
    public void setPort(int port) throws IllegalArgumentException {
        if (validatePort(port)) {
            this.port = port;
            LOG.log(Level.INFO, "Server port is now set to: {0}", port);
            propManager.setProperty("port", port + "");
        } else {
            throw new IllegalArgumentException();
        }
    }

    private boolean validatePort(int port) {
        if (port >= 1 && port <= 65535) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sets the server strategy
     *
     * @param strategy server strategy
     */
    public void setStrategy(StrategiesList strategy) {
        this.strategy = strategy;
        LOG.log(Level.INFO, "Server strategy has been set to: {0}", strategy.toString());
        propManager.setProperty("strategy", strategy.toString());
    }
    
    /**
     *Sets the task limit for planning for finishing projects
     * @param taskLimit Task limit for planning for finishing projects
     */
    public void setTaskLimit(int taskLimit) throws IllegalArgumentException{
        if(taskLimit>0){
        this.taskLimit = taskLimit;
        LOG.log(Level.INFO, "Task limit for absolute planning for finishing projects has been set to: {0}", taskLimit);
        propManager.setProperty("tasklimit", taskLimit+"");
        }else{
            throw new IllegalArgumentException();
        }
    }

    /**
     * Gets the server strategy
     *
     * @return server strategy
     */
    public StrategiesList getStrategy() {
        return strategy;
    }

    /**
     * Sets the base directory
     *
     * @param dir path to the base directory
     * @return
     */
    public boolean setBaseDir(String dir) {
        File f = new File(dir);
        if ((f.exists() && f.isDirectory()) || f.mkdirs()) {
            try {
                basedir = f.getCanonicalFile().toPath().toAbsolutePath();
                LOG.log(Level.INFO, "Basedir is now set to: {0}", basedir);
                propManager.setProperty("basedir", basedir.toString());
                return true;
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Path {0} is not correct path", dir);
                return false;
            }

        } else {
            LOG.log(Level.WARNING, "Path {0} is not correct path", dir);
            return false;
        }
    }

    /**
     * Gets server base directory
     *
     * @return base directory
     */
    public Path getBaseDir() {
        return basedir;
    }

    /**
     * Initialises parameters
     */
    public void initialiseParameters() {
        if (propManager.getProperty("basedir") == null) {
            setBaseDir(System.getProperty("user.home") + File.separator + "DCSN_base");
        } else {
            setBaseDir(propManager.getProperty("basedir"));
        }

        if (propManager.getProperty("strategy") == null) {
            setDefaultStrategy();
        } else {
            switch (propManager.getProperty("strategy")) {
                case "HIGHEST_PRIORITY_FIRST":
                    setStrategy(StrategiesList.HIGHEST_PRIORITY_FIRST);
                    break;
                case "MAXIMAL_THROUGHPUT":
                    setStrategy(StrategiesList.MAXIMAL_THROUGHPUT);
                    break;
                default:
                    setDefaultStrategy();
            }
        }

        if(propManager.getProperty("tasklimit") == null){
            setDefaultTaskLimit();
        }else{
              int taskLimit = Integer.parseInt(propManager.getProperty("tasklimit"));
            try {
                setTaskLimit(taskLimit);
            } catch (IllegalArgumentException e) {
                LOG.log(Level.WARNING, "INITIALIZING: Task limit for planning of finishing projects has to be positive integer");
                setDefaultTaskLimit();
            }
        }
        
        if (propManager.getProperty("port") == null) {
            setDefaultPort();
        } else {
            int tmpPort = Integer.parseInt(propManager.getProperty("port"));
            try {
                setPort(tmpPort);
            } catch (IllegalArgumentException e) {
                LOG.log(Level.WARNING, "INITIALIZING: Port number has to be between 1 - 65535");
                setDefaultPort();
            }
        }
    }
}
