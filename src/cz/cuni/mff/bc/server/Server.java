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
import cz.cuni.mff.bc.server.logging.FileLogger;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import org.cojen.dirmi.Environment;
import org.cojen.dirmi.Session;
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
    private static HashMap<String, Session> activeConnections;

    private static TaskManager taskManager;
    private IServerImpl remoteMethods;
    private CustomHandler logHandler;
    private final int port = 1099;
    private ServerCommands commands;
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Server.class.getName());

    public Server() {
        logHandler = new CustomHandler();
        logHandler.setFormatter(new CustomFormater());
        logHandler.setLevel(Level.ALL);
        logHandler.addLogTarget(new FileLogger(new File("server.log")));
        activeConnections = new HashMap<>();
        taskManager = new TaskManager();
        remoteMethods = new IServerImpl();

        propManager = new PropertiesManager("server.config.properties", logHandler);
        LOG.addHandler(logHandler);
        this.commands = new ServerCommands(this);
    }

    public void initialize() {
        if (propManager.getProperty("basedir") == null) {
            setBaseDir(System.getProperty("user.home") + File.separator + "DCSN_base");
        } else {
            setBaseDir(propManager.getProperty("basedir"));
        }
        startListening();
    }

    public static TaskManager getTaskManager() {
        return taskManager;
    }

    public static HashMap<String, Session> getActiveConnections() {
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
        GConsole con = new GConsole(this, "server", new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitServer();
            }
        });
        con.startConsole();
        logHandler.addLogTarget(con);
    }

    @Override
    public void startClassicConsole() {
        new Thread() {
            @Override
            public void run() {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
                    while (true) {
                        System.out.print("server>");
                        proceedCommand(br.readLine());
                    }
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Problem with reading command from console");
                }
            }
        }.start();
    }

    public boolean setBaseDir(String dir) {
        File f = new File(dir);
        if (f.exists() && f.isDirectory()) {
            basedir = f.getAbsolutePath();
            LOG.log(Level.INFO, "Basedir is now set to: {0}", basedir);
            propManager.setProperty("basedir", basedir);
            return true;
        } else {
            if (f.mkdirs()) {
                basedir = f.getAbsolutePath();
                LOG.log(Level.INFO, "Basedir is now set to: {0}", basedir);
                propManager.setProperty("basedir", basedir);
                return true;
            } else {
                LOG.log(Level.WARNING, "Path {0} is not correct path", dir);
                return false;
            }
        }
    }

    public void printPort() {
        LOG.log(Level.INFO, "Server port:{0}", port);
    }

    public void printBaseDir() {
        LOG.log(Level.INFO, "Basedir is set to: {0}", basedir);
    }

    @Override
    public void proceedCommand(String command) {
        String[] cmd = ServerCommands.parseCommand(command);
        String[] params = Arrays.copyOfRange(cmd, 1, cmd.length);
        try {
            Class<?> c = Class.forName("cz.cuni.mff.bc.server.ServerCommands");
            Method method = c.getMethod(cmd[0], new Class[]{String[].class});
            method.invoke(commands, new Object[]{params});
        } catch (ClassNotFoundException e) {
            // will be never thrown
        } catch (IllegalAccessException e) {
        } catch (IllegalArgumentException e) {
        } catch (InvocationTargetException e) {
        } catch (NoSuchMethodException e) {
            LOG.log(Level.WARNING, "No such command");
        } catch (SecurityException e) {
        }

    }

    public void startListening() {
        if (basedir == null) {
            LOG.log(Level.INFO, "Server base dir has to be set before starting the server");
        } else {
            try {
                checkFolders();
                env = new Environment(numThreads);
                sesAcceptor = env.newSessionAcceptor(port);
                sesAcceptor.accept(new CustomSessionListener(remoteMethods, sesAcceptor));
                LOG.log(Level.INFO, "Server is listening for incoming sessions on port {0}",port);

            } catch (IOException e) {
                LOG.log(Level.WARNING, "Starting server: {0}", e.getMessage());
            }
        }
    }

    public void stopListening() {
        try {
            Set<String> clients = activeConnections.keySet();
            for (String client : clients) {
                activeConnections.get(client).close();
                activeConnections.remove(client);
            }
            if (sesAcceptor != null) {
                sesAcceptor.close();
            }
            if (env != null) {
                env.close();

            }
            LOG.log(Level.INFO, "Server succesfully stopped.");
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Stopping server: {0}", e.getMessage());
        }
    }

    public void exitServer() {
        stopListening();
        System.exit(0);
    }
}