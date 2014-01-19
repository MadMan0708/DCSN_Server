/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import cz.cuni.mff.bc.api.main.CustomIO;
import cz.cuni.mff.bc.misc.GConsole;
import cz.cuni.mff.bc.misc.IConsole;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.cojen.dirmi.Environment;
import org.cojen.dirmi.SessionAcceptor;

/**
 * Main server class, handles basic server behaviour
 *
 * @author Jakub Hava
 */
public class Server implements IConsole {

    private static final int numThreads = 100;
    private Environment env;
    private SessionAcceptor sesAcceptor;
    private ConcurrentHashMap<String, ActiveClient> activeClients;
    private IServerImpl remoteMethods;
    private CustomHandler logHandler;
    private ServerParams serverParams;
    private ServerCommands commands;
    private DiscoveryThread discoveryThread;
    private FilesStructure filesStructure;
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Server.class.getName());

    /**
     * Constructor
     */
    public Server() {
        logHandler = new CustomHandler();
        logHandler.setFormatter(new CustomFormater());
        logHandler.setLevel(Level.ALL);
        logHandler.addLogTarget(new FileLogger(new File("server.main.log")));
        LOG.addHandler(logHandler);

        activeClients = new ConcurrentHashMap<>();
        serverParams = new ServerParams(logHandler);
        filesStructure = new FilesStructure(serverParams);
        remoteMethods = new IServerImpl(activeClients, filesStructure, serverParams);
        commands = new ServerCommands(this);
    }

    /**
     * Initialises server
     */
    public void initialise() {
        serverParams.initialiseParameters();
        deleteContentOfBaseDirectory();
        discoveryThread = new DiscoveryThread(serverParams.getPort());
        CustomIO.deleteDirectory(serverParams.getBaseDir()); // delete directories from last run of the server
        startListening();
    }

    /*
     * Deletes content of temporary directory
     */
    private void deleteContentOfBaseDirectory() {
        File[] files = serverParams.getBaseDir().toFile().listFiles();
        for (File file : files) {
            CustomIO.deleteDirectory(file.toPath());
        }
    }

    /**
     * Gets server parameters
     *
     * @return server parameters
     */
    public ServerParams getServerParams() {
        return serverParams;
    }

    /**
     * Gets active clients
     *
     *
     * @return list with active clients
     */
    public ConcurrentHashMap<String, ActiveClient> getActiveClients() {
        return activeClients;
    }

    /*
     * Checks if all basic server directories exist
     */
    private void checkFolders() {
        File base = serverParams.getBaseDir().toFile();
        if (base.exists() && base.isDirectory()) {
        } else {
            base.mkdir();
        }

        File uploaded = filesStructure.getUploadedDir();
        if (uploaded.exists() && uploaded.isDirectory()) {
        } else {
            uploaded.mkdir();
        }

        File projects = filesStructure.getProjectsDir();
        if (projects.exists() && projects.isDirectory()) {
        } else {
            projects.mkdir();
        }
    }

    @Override
    public void proceedCommand(String command) {
        String[] cmd = ServerCommands.parseCommand(command);
        String[] params = Arrays.copyOfRange(cmd, 1, cmd.length);
        try {
            // don't want to execute functions which aren't real console commands
            switch (command) {
                case "ServerCommands":
                    ;
                case "parseCommand":
                    ;
                case "checkParamNum":
                    throw new NoSuchMethodException();
            }
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

    /**
     * Server starts listening for incoming sessions
     */
    public void startListening() {
        try {
            checkFolders();
            env = new Environment(numThreads);
            sesAcceptor = env.newSessionAcceptor(serverParams.getPort());
            sesAcceptor.accept(new CustomSessionListener(remoteMethods, sesAcceptor));
            LOG.log(Level.INFO, "Server is listening for incoming sessions on port {0}", serverParams.getPort());
            discoveryThread.startDiscovering();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Starting server: {0}", e.getMessage());
        }

    }

    /**
     * Server stops listening for incoming sessions
     */
    public void stopListening() {
        try {
            if (discoveryThread != null) {
                discoveryThread.stopDiscovering();
            }
            Set<String> clients = activeClients.keySet();
            for (String client : clients) {
                activeClients.get(client).getSession().close();
                activeClients.remove(client);
            }
            if (sesAcceptor != null) {
                sesAcceptor.close();
            }
            if (env != null) {
                env.close();
            }
            LOG.log(Level.INFO, "Server succesfully stopped.");
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Stopping server problem: {0}", e.getMessage());
        }
    }

    /**
     * Exits the server
     */
    public void exitServer() {
        stopListening();
        System.exit(0);
    }
}