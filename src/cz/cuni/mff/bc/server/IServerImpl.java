/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import cz.cuni.mff.bc.computation.ActiveClient;
import cz.cuni.mff.bc.computation.TaskManager;
import cz.cuni.mff.bc.computation.Project;
import cz.cuni.mff.bc.api.main.IServer;
import cz.cuni.mff.bc.api.main.Task;
import cz.cuni.mff.bc.api.main.TaskID;
import cz.cuni.mff.bc.api.main.ProjectUID;
import cz.cuni.mff.bc.api.enums.InformMessage;
import cz.cuni.mff.bc.api.enums.ProjectState;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.cojen.dirmi.Pipe;

/**
 * Implementation of remote interface
 *
 * @author Jakub Hava
 */
public class IServerImpl implements IServer {

    private TaskManager taskManager;
    private final int timerPeriodSec = 40;
    private ConcurrentHashMap<String, ActiveClient> activeClients;
    private FilesStructure filesStructure;
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Server.class.getName());

    /**
     * Constructor
     *
     * @param activeClients list of active client's
     * @param filesStructure files structure
     * @param serverParams server parameters
     */
    public IServerImpl(ConcurrentHashMap<String, ActiveClient> activeClients, FilesStructure filesStructure, ServerParams serverParams) {
        this.activeClients = activeClients;
        this.filesStructure = filesStructure;
        this.taskManager = new TaskManager(activeClients, filesStructure, serverParams);
    }

    /**
     * Gets active clients
     *
     * @return list with active clients
     */
    public ConcurrentHashMap<String, ActiveClient> getActiveClients() {
        return activeClients;
    }

    /**
     * Gets task manager
     *
     * @return task manager
     */
    public TaskManager getTaskManager() {
        return taskManager;
    }

    @Override
    public void setClientsMemoryLimit(String clientID, int memory) throws RemoteException {
        activeClients.get(clientID).setMemoryLimit(memory);
        LOG.log(Level.FINE, "Memory limit on client {0} is now set to {1}m", new Object[]{clientID, memory});
    }

    @Override
    public void setClientsCoresLimit(String clientID, int cores) throws RemoteException {
        activeClients.get(clientID).setCoresLimit(cores);
        LOG.log(Level.FINE, "Cores limit on client {0} is now set to {1}", new Object[]{clientID, cores});
    }

    @Override
    public ProjectUID getProjectIdBeforeCalculation(String clientID) throws RemoteException {
        return taskManager.getProjectIDBeforeCalculation(clientID);
    }

    @Override
    public boolean hasClientTasksInProgress(String clientID) throws RemoteException {
        return taskManager.clientInActiveComputation(clientID);
    }

    @Override
    public Task getTask(String clientID, ProjectUID projectUID) throws RemoteException {
        return taskManager.getTask(clientID, projectUID);
    }

    @Override
    public void sendInformMessage(String clientID, InformMessage message) throws RemoteException {
        switch (message) {
            case CALCULATION_STARTED:
                startClientTimer(clientID);
                activeClients.get(clientID).setToComputing();
                break;
            case CALCULATION_ENDED:
                stopClientTimer(clientID);
                activeClients.get(clientID).setToNotComputing();
                break;
        }
    }

    @Override
    public boolean isConnected(String clientName) throws RemoteException {
        if (activeClients.containsKey(clientName)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void saveCompletedTask(String clientID, Task task) throws RemoteException {
        if (!task.hasDataBeenSaved()) {
            task.saveData(filesStructure.getTaskSavePath(task.getUnicateID()));
            taskManager.addCompletedTask(clientID, task.getUnicateID());
            LOG.log(Level.FINE, "Task saving: Task {0} has been saved", task.getUnicateID());
        }
    }

    @Override
    public Pipe uploadProject(String clientName, String projectName, int priority, int cores, int memory, int time, Pipe pipe) throws RemoteException {
        Project project = taskManager.createPreparingProject(clientName, projectName, priority, cores, memory, time);
        File upDir = CustomIO.createFolder(filesStructure.getClientUploadedDir(clientName, projectName).toPath());
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
            taskManager.addProject(project);
        } catch (IOException e) {
            taskManager.undoProject(project);
            LOG.log(Level.FINE, "Problem during saving uploaded file: {0}", e.toString());
        }

        return null;
    }

    @Override
    public boolean isProjectReadyForDownload(String clientID, String projectID) {
        return taskManager.isProjectReadyForDownload(clientID, projectID);
    }

    @Override
    public Pipe downloadProjectJar(ProjectUID uid, Pipe pipe) throws RemoteException {
        File input = filesStructure.getProjectJarFile(uid.getClientName(), uid.getProjectName());
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(input))) {
            int n;
            byte[] buffer = new byte[8192];
            while ((n = in.read(buffer)) > -1) {
                pipe.write(buffer, 0, n);
            }
            pipe.close();

        } catch (IOException e) {
            LOG.log(Level.FINE, "Loading project JAR for client class loader: {0}", e.getMessage());
        }
        return null;
    }

    @Override
    public Pipe downloadProject(String clientID, String projectID, Pipe pipe) throws RemoteException {
        File input = filesStructure.getCalculatedDataFile(clientID, projectID);
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(input))) {

            int n;
            byte[] buffer = new byte[8192];
            while ((n = in.read(buffer)) > -1) {
                pipe.write(buffer, 0, n);
            }
            pipe.close();
            taskManager.removeDownloadedProject(new ProjectUID(clientID, projectID));
        } catch (IOException e) {
            LOG.log(Level.FINE, "Loading project for download: {0}", e.getMessage());
        }

        return null;
    }

    @Override
    public long getProjectFileSize(String clientID, String projectID) throws RemoteException {
        File output = filesStructure.getCalculatedDataFile(clientID, projectID);
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
    public ProjectState pauseProject(String clientID, String projectID) throws RemoteException {
        return taskManager.pauseProject(clientID, projectID);
    }

    @Override
    public boolean cancelProject(String clientID, String projectID) throws RemoteException {
        return taskManager.cancelProject(clientID, projectID);
    }

    @Override
    public ProjectState resumeProject(String clientID, String projectID) throws RemoteException {
        return taskManager.resumeProject(clientID, projectID);
    }

    @Override
    public void cancelTaskOnClient(String clientID, TaskID taskToCancel) throws RemoteException {
        taskManager.cancelTaskAssociation(clientID, taskToCancel);
    }

    @Override
    public ArrayList<TaskID> sendTasksInCalculation(String clientID, ArrayList<TaskID> tasks) throws RemoteException {
        ArrayList<TaskID> toCancel = new ArrayList<>();
        for (TaskID ID : tasks) {
            if (!taskManager.isProjectInManager(ID.getClientName(), ID.getProjectName())) {
                toCancel.add(ID);
                taskManager.cancelTaskAssociation(clientID, ID);
            } else if (taskManager.isProjectCorrupted(ID.getClientName(), ID.getProjectName())) {
                toCancel.add(ID);
                taskManager.cancelTaskAssociation(clientID, ID);
            } else if (taskManager.isTaskCompleted(ID)) {
                toCancel.add(ID);
                taskManager.cancelTaskAssociation(clientID, ID);
            }
        }
        activeClients.get(clientID).setTimeout(Boolean.TRUE);
        return toCancel;
    }

    @Override
    public void markProjectAsCorrupted(String clientName, String projectName) throws RemoteException {
        taskManager.markProjectAsCorrupted(clientName, projectName);
    }

    /* 
     * Starts the client timer
     */
    private void startClientTimer(final String clientID) {
        LOG.log(Level.FINE, "Timer for client {0} started", clientID);
        final Timer t = new Timer(clientID);
        activeClients.get(clientID).setTimeout(Boolean.TRUE);
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                if (taskManager.isClientActive(clientID) && activeClients.get(clientID).getTimeout().equals(Boolean.TRUE)) {
                    // OK, client has sent an inform message to the server, resetting timer
                    activeClients.get(clientID).setTimeout(Boolean.FALSE);
                    LOG.log(Level.FINE, "Client {0} is active", clientID);
                } else {
                    ArrayList<TaskID> tasks = taskManager.cancelTasksAssociation(clientID);
                    LOG.log(Level.FINE, "Client {0} has not sent ping message, disconnected", clientID);
                    if (tasks != null) {
                        for (TaskID taskID : tasks) {
                            LOG.log(Level.FINE, "Task {0} calculated by {1} is again in tasks pool", new Object[]{taskID, clientID});
                        }
                    }
                    stopClientTimer(clientID);
                    activeClients.remove(clientID);
                }

            }
        }, 0, timerPeriodSec * 1000);
        activeClients.get(clientID).setTimer(t);
    }

    /*
     * Stops the client timer
     */
    private void stopClientTimer(String clientID) {
        if (taskManager.isClientActive(clientID)) {
            Timer t = activeClients.get(clientID).getTimer();
            t.cancel();
            activeClients.get(clientID).setTimeout(false);
            activeClients.get(clientID).setTimer(new Timer());
        }
    }
}
