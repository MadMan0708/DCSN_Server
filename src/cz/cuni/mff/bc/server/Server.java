/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import cz.cuni.mff.bc.common.main.GConsole;
import cz.cuni.mff.bc.common.main.IConsole;
import cz.cuni.mff.bc.common.main.Logger;
import cz.cuni.mff.bc.common.main.PropertiesManager;
import cz.cuni.mff.bc.common.enums.ELoggerMessages;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import org.cojen.dirmi.Environment;
import org.cojen.dirmi.SessionAcceptor;

/**
 *
 * @author Aku
 */
public class Server implements IConsole {

    private static String basedir;
    private static Logger logger;
    private static PropertiesManager propManager;
    private static final int numThreads = 100;
    private Environment env;
    private SessionAcceptor sesAcceptor;
    private static ArrayList<String> activeConnections;
    private static TaskManager taskManager;
    private IServerImpl remoteMethods;

    public Server() {
        logger = new Logger("server.log");
        propManager = new PropertiesManager(logger, "server.config.properties");
        activeConnections = new ArrayList<>();
        taskManager = new TaskManager();
        remoteMethods = new IServerImpl();
    }

    public void initialize() {
        basedir = propManager.getProperty("basedir");
    }

    public static TaskManager getTaskManager() {
        return taskManager;
    }

    public static Logger getLogger() {
        return logger;
    }

    public static ArrayList<String> getActiveConnections() {
        return activeConnections;
    }

    private void checkFolders() {
        File base = new File(getBaseDir());
        if (base.exists() && base.isDirectory()) {
        } else {
            base.mkdir();
        }

        File uploaded = new File(getUploadedDir());
        if (uploaded.exists() && uploaded.isDirectory()) {
        } else {
            uploaded.mkdir();
        }

        File projects = new File(getProjectsDir());
        if (projects.exists() && projects.isDirectory()) {
        } else {
            projects.mkdir();
        }

    }

    public static String getUploadedDir() {
        return propManager.getProperty("basedir") + File.separator + "Uploaded";
    }

    public static String getProjectsDir() {
        return propManager.getProperty("basedir") + File.separator + "Projects";
    }

    public static String getBaseDir() {
        return propManager.getProperty("basedir");
    }

    @Override
    public void startGUIConsole() {
        GConsole con = new GConsole(this, "server");
        con.startConsole();

    }

    @Override
    public void startClassicConsole() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                System.out.print("Server>");
                proceedCommand(br.readLine());
            }
        } catch (IOException e) {
            logger.log("Proceeding command: " + e.getMessage(), ELoggerMessages.ERROR);
        }
    }

    private static boolean setBaseDir(String newBaseDir) {
        File f = new File(newBaseDir);
        if (f.exists() && f.isDirectory()) {
            basedir = newBaseDir;
            return true;
        } else {
            if (f.mkdirs()) {
                basedir = newBaseDir;
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public void proceedCommand(String command) {
        String[] cmd = parseCommand(command);
        switch (cmd[0]) {
            case "start": {
                startListening(numThreads);
                logger.log("Server is listening for incoming sessions");
                break;
            }
            case "setBaseDir": {
                try {
                    File f = new File(cmd[1]);
                    if (!setBaseDir(f.getCanonicalPath())) {
                        throw new IOException();
                    }
                    propManager.setProperty("basedir", f.getAbsolutePath());
                    logger.log("Basedir is now set to: " + f.getAbsolutePath());
                } catch (IOException e) {
                    logger.log("Path " + cmd[1] + " is not correct path");
                }
                break;
            }
            case "getBaseDir": {
                logger.log("Basedir is set to: " + propManager.getProperty("basedir"));
                break;
            }
            case "stop": {
                stopListening();
                break;
            }
            case "exit": {
                exitServer();
                break;
            }
        }
    }

    private void startListening(int numThreads) {
        if (basedir == null) {
            logger.log("Server base dir has to be set before starting the server", ELoggerMessages.ALERT);
        } else {
            try {
                checkFolders();
                env = new Environment(numThreads);
                sesAcceptor = env.newSessionAcceptor(1099);
                sesAcceptor.accept(new CustomSessionListener(remoteMethods, sesAcceptor));
                
            } catch (IOException e) {
                logger.log("Starting server: " + e.getMessage(), ELoggerMessages.ERROR);
            }
        }
    }

    private void stopListening() {
        try {
            if (env != null) {
                env.close();
            }
            logger.log("Server succesfully stopped.");
        } catch (IOException e) {
            logger.log("Stopping server: " + e.getMessage(), ELoggerMessages.ERROR);
        }
    }

    private String[] parseCommand(String cmd) {
        return cmd.split("\\s+");
    }

    private void exitServer() {
        stopListening();
        System.exit(0);
    }
}