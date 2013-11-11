/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import cz.cuni.mff.bc.server.misc.GConsole;
import cz.cuni.mff.bc.server.misc.IConsole;
import cz.cuni.mff.bc.server.misc.PropertiesManager;
import cz.cuni.mff.bc.server.logging.CustomFormater;
import cz.cuni.mff.bc.server.logging.CustomHandler;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import org.cojen.dirmi.Environment;
import org.cojen.dirmi.SessionAcceptor;

/**
 *
 * @author Aku
 */
public class Server implements IConsole {

    private static String basedir;
    private static PropertiesManager propManager;
    private static final int numThreads = 100;
    private Environment env;
    private SessionAcceptor sesAcceptor;
    private static ArrayList<String> activeConnections;
    private static TaskManager taskManager;
    private IServerImpl remoteMethods;
    private Handler logHandler;
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Server.class.getName());

    public Server() {
        logHandler = new CustomHandler(new File("server.log"));
        logHandler.setFormatter(new CustomFormater());
        logHandler.setLevel(Level.ALL);

        activeConnections = new ArrayList<>();
        taskManager = new TaskManager(logHandler);
        remoteMethods = new IServerImpl(logHandler);

        propManager = new PropertiesManager("server.config.properties", logHandler);
        LOG.addHandler(logHandler);
    }

    public void initialize() {
        basedir = propManager.getProperty("basedir");
    }

    public static TaskManager getTaskManager() {
        return taskManager;
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
            LOG.log(Level.WARNING, "Proceeding command: {0}", e.getMessage());
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
                LOG.log(Level.INFO, "Server is listening for incoming sessions");
                break;
            }
            case "setBaseDir": {
                try {
                    File f = new File(cmd[1]);
                    if (!setBaseDir(f.getCanonicalPath())) {
                        throw new IOException();
                    }
                    propManager.setProperty("basedir", f.getAbsolutePath());
                    LOG.log(Level.INFO, "Basedir is now set to: {0}", f.getAbsolutePath());
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Path {0} is not correct path", cmd[1]);
                }
                break;
            }
            case "getBaseDir": {
                LOG.log(Level.INFO, "Basedir is set to: {0}", propManager.getProperty("basedir"));
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
            LOG.log(Level.INFO, "Server base dir has to be set before starting the server");
        } else {
            try {
                checkFolders();
                env = new Environment(numThreads);
                sesAcceptor = env.newSessionAcceptor(1099);
                sesAcceptor.accept(new CustomSessionListener(remoteMethods, sesAcceptor, logHandler));

            } catch (IOException e) {
                LOG.log(Level.WARNING, "Starting server: {0}", e.getMessage());
            }
        }
    }

    private void stopListening() {
        try {
            if (env != null) {
                env.close();
            }
            LOG.log(Level.INFO, "Server succesfully stopped.");
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Stopping server: {0}", e.getMessage());
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