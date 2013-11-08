/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import cz.cuni.mff.bc.common.main.IServer;
import cz.cuni.mff.bc.common.main.Logger;
import cz.cuni.mff.bc.common.main.Task;
import cz.cuni.mff.bc.common.main.TaskID;
import cz.cuni.mff.bc.common.enums.ELoggerMessages;
import static cz.cuni.mff.bc.server.Server.getUploadedDir;
import cz.cuni.mff.bc.common.main.ProjectUID;
import cz.cuni.mff.bc.common.enums.InformMessage;
import cz.cuni.mff.bc.common.main.ProjectInfo;
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
import org.cojen.dirmi.Pipe;

/**
 *
 * @author Jakub
 */
public class IServerImpl implements IServer {

    private TaskManager taskManager;
    private Logger logger;
    private final int timerPeriodSec = 11;
    private HashMap<String, Timer> clientTimers;
    private HashMap<String, Boolean> clientsTimeout;
    private ArrayList<String> activeConnections;
    // private SessionAcceptor sessionAcceptor;

    public IServerImpl() {//SessionAcceptor sessionAcceptor) {
        // this.sessionAcceptor = sessionAcceptor;
        this.logger = Server.getLogger();
        this.taskManager = Server.getTaskManager();
        this.clientTimers = new HashMap<>();
        this.clientsTimeout = new HashMap<>();
        this.activeConnections = Server.getActiveConnections();
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
    public byte[] getClassData(TaskID taskID) throws RemoteException {
        try {
            return taskManager.getClassData(taskID);
        } catch (IOException e) {
            logger.log("Data for class: " + taskID.getClassName() + " could not be load; " + e.toString(), ELoggerMessages.ERROR);
            return null;
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
        taskManager.classManager.getClassLoader(clientSessionID).setProjectUID(projectUID);
    }

    @Override
    public void saveCompletedTask(String clientID, Task task) throws RemoteException {
        if (!task.hasDataBeenSaved()) {
            task.saveData(taskManager.createTaskSavePath(task.getUnicateID()));
            taskManager.addCompletedTask(clientID, task.getUnicateID());
            logger.log("Task saving: Task " + task.getUnicateID() + " has been saved", ELoggerMessages.DEBUG);
        }
    }

    @Override
    public Pipe uploadProject(String clientID, String projectID, int priority, String extension, Pipe pipe) throws RemoteException {
        File file = new File(getUploadedDir() + File.separator + clientID + "_" + projectID + "." + extension);
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
            int n;
            byte[] buffer = new byte[8192];
            while ((n = pipe.read(buffer)) > -1) {
                out.write(buffer, 0, n);
            }
            pipe.close();

        } catch (IOException e) {
            logger.log("Saving uploaded file: " + e.getMessage(), ELoggerMessages.ERROR);
        }
        taskManager.addProject(clientID, projectID, priority, extension);
        return null;
    }

    @Override
    public boolean isProjectReadyForDownload(String clientID, String projectID) {
        return taskManager.isProjectReadyForDownload(clientID, projectID);
    }

    @Override
    public Pipe downloadProject(String clientID, String projectID, Pipe pipe) throws RemoteException {
        File input = new File(TaskManager.getProjectDir(projectID, clientID) + clientID + "_" + projectID + "_completed" + ".zip");
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(input))) {

            int n;
            byte[] buffer = new byte[8192];
            while ((n = in.read(buffer)) > -1) {
                pipe.write(buffer, 0, n);
            }
            pipe.close();

        } catch (IOException e) {
            logger.log("Loading project for download: " + e.getMessage(), ELoggerMessages.ERROR);
        }
        taskManager.removeDownloadedProject(new ProjectUID(clientID, projectID));
        return null;
    }

    @Override
    public long getProjectFileSize(String clientID, String projectID) throws RemoteException {
        File output = new File(TaskManager.getProjectDir(projectID, clientID) + clientID + "_" + projectID + "_completed" + ".zip");
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
        logger.log("Timer for client " + clientID + " started");
        final Timer t = new Timer(clientID);
        clientsTimeout.put(clientID, Boolean.TRUE);
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (clientsTimeout.get(clientID).equals(Boolean.TRUE)) {
                    // vse ok, klient se ozval, znovu nastavuju timer
                    clientsTimeout.put(clientID, Boolean.FALSE);
                    logger.log("Client " + clientID + " is active");
                } else {
                    ArrayList<TaskID> tasks = taskManager.cancelTasksAssociation(clientID);
                    logger.log("Client  " + clientID + " has not sent ping message, disconnected", ELoggerMessages.ERROR);
                    if (tasks != null) {
                        for (TaskID taskID : tasks) {
                            logger.log("Task " + taskID + " calculated by " + clientID + " is again in tasks pool");
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
