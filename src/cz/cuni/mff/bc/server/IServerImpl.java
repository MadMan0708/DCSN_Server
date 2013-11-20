/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import cz.cuni.mff.bc.api.main.IServer;
import cz.cuni.mff.bc.api.main.Task;
import cz.cuni.mff.bc.api.main.TaskID;
import cz.cuni.mff.bc.api.main.ProjectUID;
import cz.cuni.mff.bc.api.enums.InformMessage;
import cz.cuni.mff.bc.api.main.ProjectInfo;
import cz.cuni.mff.bc.api.main.CustomIO;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;
import java.util.logging.Level;
import org.cojen.dirmi.Pipe;

/**
 *
 * @author Jakub
 */
public class IServerImpl implements IServer {

    private TaskManager taskManager;
    private final int timerPeriodSec = 11;
    private HashMap<String, Timer> clientTimers;
    private HashMap<String, Boolean> clientsTimeout;
    private ArrayList<String> activeConnections;
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(IServerImpl.class.getName());

    public IServerImpl(Handler logHandler) {
        this.taskManager = Server.getTaskManager();
        this.clientTimers = new HashMap<>();
        this.clientsTimeout = new HashMap<>();
        this.activeConnections = Server.getActiveConnections();
        LOG.addHandler(logHandler);
    }

    @Override
    public TaskID getTaskIdBeforeCalculation(String clientID) throws RemoteException {
        return taskManager.getTaskIDBeforeCalculation(clientID);
    }

    @Override
    public Task getTask(String clientID, TaskID taskID) throws RemoteException {
        return taskManager.getTask(clientID, taskID);
    }

    @Override
    public void sendInformMessage(String clientID, InformMessage message) throws RemoteException {
        switch (message) {
            case CALCULATION_STARTED:
                startClientTimer(clientID);
                break;
            case CALCULATION_ENDED:
                stopClientTimer(clientID);
                break;
        }
    }

    @Override
    public boolean isConnected(String clientName) throws RemoteException {
        if (activeConnections.contains(clientName)) {
            return true;
        } else {
            return false;
        }
    }

    /*    @Override
     public EUserAddingState addClient(String client) throws RemoteException {
     if (activeConnections.contains(client)) {
     return EUserAddingState.EXIST;
     } else {
     this.activeConnections.add(client);
     logger.log("Client " + client + " has been connected to the server");
     sessionAcceptor.accept(new CustomSessionListener(this)); // starts listening for possible new session
     return EUserAddingState.OK;
     }
     }
     */
    @Override
    public void setSessionClassLoaderDetails(String clientSessionID, ProjectUID projectUID) throws RemoteException {
        // taskManager.classManager.getClassLoader(clientSessionID).setProjectUID(projectUID);
    }

    @Override
    public void saveCompletedTask(String clientID, Task task) throws RemoteException {
        if (!task.hasDataBeenSaved()) {
            task.saveData(FilesStructure.getTaskSavePath(task.getUnicateID()));
            taskManager.addCompletedTask(clientID, task.getUnicateID());
            LOG.log(Level.INFO, "Task saving: Task {0} has been saved", task.getUnicateID());
        }
    }

    @Override
    public Pipe uploadProject(String clientName, String projectName, int priority, Pipe pipe) throws RemoteException {
        File upDir = CustomIO.createFolder(FilesStructure.getClientUploadedDir(clientName, projectName));
        try {
            File tmp = File.createTempFile(clientName, projectName + ".zip");
            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tmp))) {
                int n;
                byte[] buffer = new byte[8192];
                while ((n = pipe.read(buffer)) > -1) {
                    out.write(buffer, 0, n);
                }
                pipe.close();
            }
            CustomIO.extractZipFile(tmp, upDir);
            tmp.delete();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Problem during saving uploaded file: {0}", e.toString());
        }
        taskManager.addProject(clientName, projectName, priority);
        return null;
    }

    @Override
    public boolean isProjectReadyForDownload(String clientID, String projectID) {
        return taskManager.isProjectReadyForDownload(clientID, projectID);
    }

    @Override
    public Pipe downloadProjectJar(ProjectUID uid, Pipe pipe) throws RemoteException {
        File input = FilesStructure.getProjectJarFile(uid.getClientID(), uid.getProjectID());
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(input))) {
            int n;
            byte[] buffer = new byte[8192];
            while ((n = in.read(buffer)) > -1) {
                pipe.write(buffer, 0, n);
            }
            pipe.close();

        } catch (IOException e) {
            LOG.log(Level.WARNING, "Loading project JAR for client class loader: {0}", e.getMessage());
        }
        return null;
    }

    @Override
    public Pipe downloadProject(String clientID, String projectID, Pipe pipe) throws RemoteException {
        File input = FilesStructure.getCalculatedDataFile(clientID, projectID);
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(input))) {

            int n;
            byte[] buffer = new byte[8192];
            while ((n = in.read(buffer)) > -1) {
                pipe.write(buffer, 0, n);
            }
            pipe.close();

        } catch (IOException e) {
            LOG.log(Level.WARNING, "Loading project for download: {0}", e.getMessage());
        }
        taskManager.removeDownloadedProject(new ProjectUID(clientID, projectID));
        return null;
    }

    @Override
    public long getProjectFileSize(String clientID, String projectID) throws RemoteException {
        File output = FilesStructure.getCalculatedDataFile(clientID, projectID);
        return output.length();
    }

    @Override
    public ArrayList<ProjectInfo> getProjectList(String clientID) throws RemoteException {
        return taskManager.getProjectList(clientID);
    }

    @Override
    public boolean isProjectExists(String clientID, String projectID) throws RemoteException {
        if (taskManager.isProjectInManager(clientID, projectID)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean pauseProject(String clientID, String projectID) throws RemoteException {
        return taskManager.pauseProject(clientID, projectID);
    }

    @Override
    public boolean cancelProject(String clientID, String projectID) throws RemoteException {
        return taskManager.cancelProject(clientID, projectID);
    }

    @Override
    public boolean unpauseProject(String clientID, String projectID) throws RemoteException {
        return taskManager.unpauseProject(clientID, projectID);
    }

    @Override
    public void cancelTaskFromClient(String clientID, TaskID taskToCancel) throws RemoteException {
        taskManager.cancelTaskAssociation(clientID, taskToCancel);
    }

    @Override
    public ArrayList<TaskID> calculatedTasks(String clientID, ArrayList<TaskID> tasks) throws RemoteException {
        ArrayList<TaskID> toCancel = new ArrayList<>();
        for (TaskID ID : tasks) {
            if (taskManager.isTaskCompleted(ID) && taskManager.isTaskInProgress(ID)) {
                toCancel.add(ID);
            }
        }
        clientsTimeout.put(clientID, Boolean.TRUE);
        return toCancel;
    }

    private void startClientTimer(final String clientID) {
        LOG.log(Level.INFO, "Timer for client {0} started", clientID);
        final Timer t = new Timer(clientID);
        clientsTimeout.put(clientID, Boolean.TRUE);
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (clientsTimeout.get(clientID).equals(Boolean.TRUE)) {
                    // vse ok, klient se ozval, znovu nastavuju timer
                    clientsTimeout.put(clientID, Boolean.FALSE);
                    LOG.log(Level.INFO, "Client {0} is active", clientID);
                } else {
                    ArrayList<TaskID> tasks = taskManager.cancelTasksAssociation(clientID);
                    LOG.log(Level.WARNING, "Client  {0} has not sent ping message, disconnected", clientID);
                    if (tasks != null) {
                        for (TaskID taskID : tasks) {
                            LOG.log(Level.INFO, "Task {0} calculated by {1} is again in tasks pool", new Object[]{taskID, clientID});
                        }
                    }
                    stopClientTimer(clientID);
                    activeConnections.remove(clientID);
                    // znovu zarazeni uloh pocitanych timto klientem do seznamu in progress
                }
            }
        }, 0, timerPeriodSec * 1000);
        clientTimers.put(clientID, t);

    }

    private void stopClientTimer(String clientID) {
        Timer t = clientTimers.get(clientID);
        t.cancel();
        clientTimers.remove(clientID);
        clientsTimeout.remove(clientID);
    }
}
