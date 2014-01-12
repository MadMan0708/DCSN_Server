/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import cz.cuni.mff.bc.server.classloading.ClassManager;
import cz.cuni.mff.bc.api.main.CustomIO;
import cz.cuni.mff.bc.api.main.ProjectUID;
import cz.cuni.mff.bc.api.main.TaskID;
import cz.cuni.mff.bc.api.main.Task;
import cz.cuni.mff.bc.api.enums.ProjectState;
import cz.cuni.mff.bc.api.main.ProjectInfo;
import cz.cuni.mff.bc.server.classloading.CustObjectInputStream;
import cz.cuni.mff.bc.server.exceptions.ExtractionException;
import cz.cuni.mff.bc.server.exceptions.NotSupportedArchiveException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Level;

/**
 *
 * @author Aku
 */
public class TaskManager {

    private ExecutorService executor;
    public ClassManager classManager = new ClassManager();
    private final int inititalCapacity = 1000;
    private Comparator<TaskID> comp;
    private final BlockingQueue<TaskID> tasksPool;
    private ConcurrentHashMap<String, CopyOnWriteArrayList<TaskID>> tasksByClients = new ConcurrentHashMap<>();
    private CopyOnWriteArrayList<TaskID> tasksBeforeCalc = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<TaskID> tasksInProgress = new CopyOnWriteArrayList<>();
    private ConcurrentHashMap<ProjectUID, Project> projectsActive = new ConcurrentHashMap<>();
    private ConcurrentHashMap<ProjectUID, Project> projectsPaused = new ConcurrentHashMap<>();
    private ConcurrentHashMap<ProjectUID, Project> projectsAll = new ConcurrentHashMap<>();
    private ConcurrentHashMap<ProjectUID, Project> projectsPreparing = new ConcurrentHashMap<>();
    private ConcurrentHashMap<ProjectUID, Project> projectsCompleted = new ConcurrentHashMap<>();
    private ConcurrentHashMap<ProjectUID, Project> projectsForDownload = new ConcurrentHashMap<>();
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Server.class.getName());

    public boolean clientInActiveComputation(String clientName) {
        Set<ProjectUID> projectsUIDs = projectsAll.keySet();
        for (ProjectUID projectUID : projectsUIDs) {
            if (projectUID.getClientName().equals(clientName)) {
                return true;
            }
        }
        return false;
    }

    public TaskManager() {
        this.executor = Executors.newCachedThreadPool();
        comp = new Comparator<TaskID>() {
            @Override
            public int compare(TaskID o1, TaskID o2) {
                int p1 = o1.getPriority();
                int p2 = o2.getPriority();
                if (p1 < p2) {
                    return -1;
                } else if (p1 == p2) {
                    return 0;
                } else {
                    return 1;
                }
            }
        };
        tasksPool = new PriorityBlockingQueue<>(inititalCapacity, comp);
    }

    public boolean isProjectReadyForDownload(String clientID, String projectID) {
        if (projectsForDownload.containsKey(new ProjectUID(clientID, projectID))) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isTaskCompleted(TaskID ID) {
        return projectsAll.get(ID.getProjectUID()).isTaskCompleted(ID);
    }

    public boolean isTaskInProgress(TaskID ID) {
        if (tasksInProgress.contains(ID)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isProjectInManager(String clientID, String projectID) {
        if (projectsAll.containsKey(new ProjectUID(clientID, projectID))) {
            return true;
        } else {
            return false;
        }
    }

    private boolean zeroTasks() {
        if (tasksPool.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    private boolean hasNextTask() {
        if (!zeroTasks()) {
            return true;
        } else {
            return false;
        }
    }

    private TaskID getNextTask() {
        return tasksPool.poll();
    }

    private void cleanTasksPool(String clientID, String projectID) {
        ArrayList<TaskID> toDel = new ArrayList<>();
        ProjectUID UID = new ProjectUID(clientID, projectID);
        synchronized (tasksPool) {
            for (TaskID taskID : tasksPool) {
                if (taskID.getProjectUID().equals(UID)) {
                    toDel.add(taskID);
                }
            }
            tasksPool.removeAll(toDel);
        }
    }

    private void addTasksToPool(String clientID, String projectID) {
        Set<TaskID> uncompleted = projectsAll.get(new ProjectUID(clientID, projectID)).getUncompletedTasks();
        synchronized (tasksPool) {
            tasksPool.addAll(uncompleted);
        }
    }

    public CopyOnWriteArrayList<TaskID> getClientAssociatedTasks(String clientID) {
        return tasksByClients.get(clientID);
    }

    public void cancelTaskAssociation(String clientID, TaskID taskID) {
        if (tasksByClients.get(clientID) != null) { // if client has some tasks associated
            unassociateClientWithTask(clientID, taskID); // unassociate them
            tasksInProgress.remove(taskID);
            Project p = projectsAll.get(taskID.getProjectUID());
            p.addTaskAgain(taskID); // prida znovu ulohu do seznamu uncompleted v projectu
            LOG.log(Level.INFO, "Task {0} calculated by {1} is again in tasks pool", new Object[]{taskID, clientID});
            if (p.getState().equals(ProjectState.ACTIVE)) {
                tasksPool.add(taskID); // pokud je stav projektu active, prida se uloha znovu do poolu
                // pokud je cokoli jineho, neprida se do poolu
            }
        }
    }

    public ArrayList<TaskID> cancelTasksAssociation(String clientID) {
        CopyOnWriteArrayList<TaskID> tasks = tasksByClients.get(clientID);
        ArrayList<TaskID> toPrint = new ArrayList<>();
        if (tasks != null) {
            toPrint.addAll(tasks);
            for (TaskID taskID : tasks) {
                unassociateClientWithTask(clientID, taskID); // zrusi asociaci zpracovani
                tasksInProgress.remove(taskID); // odstraneni ze seznamu InProgress
                Project p = projectsAll.get(taskID.getProjectUID());
                p.addTaskAgain(taskID); // prida znovu ulohu do seznamu uncompleted v projectu
                if (p.getState().equals(ProjectState.ACTIVE)) {
                    tasksPool.add(taskID); // pokud je stav projektu active, prida se uloha znovu do poolu
                    // pokud je cokoli jineho, neprida se do poolu
                }

            }
        }
        return toPrint;
    }

    private void unassociateClientWithTask(String clientID, TaskID id) {
        tasksByClients.get(clientID).remove(id);
        if (tasksByClients.get(clientID).isEmpty()) {
            tasksByClients.remove(clientID);
        }
    }

    private void associateClientWithTask(String clientName, TaskID taskID) {
        synchronized (tasksByClients) {
            if (tasksByClients.get(clientName) == null) {
                tasksByClients.put(clientName, new CopyOnWriteArrayList<TaskID>());
            }
            tasksByClients.get(clientName).add(taskID);
        }
    }

    public TaskID getTaskIDBeforeCalculation(String clientID) {
        if (hasNextTask()) {
            TaskID id = getNextTask();
            tasksBeforeCalc.add(id);
            return id;
        } else {
            // No more tasks, checkers on client will be forced to sleep for a while
            return null;
        }

    }

    public Task getTask(String clientID, TaskID id) {
        File f = FilesStructure.getTaskLoadPath(id).toFile();
        File jar = FilesStructure.getProjectJarFile(id.getClientName(), id.getProjectName());
        try {
            classManager.getClassLoader(clientID).addNewUrl(jar.toURI().toURL());
            try (CustObjectInputStream ois = new CustObjectInputStream(new FileInputStream(f), classManager.getClassLoader(clientID))) {
                Task task = (Task) ois.readObject();
                associateClientWithTask(clientID, id);
                tasksBeforeCalc.remove(id);
                tasksInProgress.add(id);
                LOG.log(Level.INFO, "Task: {0} is sent for computation by client: {1}", new Object[]{task.getUnicateID(), clientID});
                return task;
            } catch (ClassNotFoundException | IOException e) {
                LOG.log(Level.WARNING, "Problem during unpacking task: {0}", e.toString());
                return null;
            }
        } catch (MalformedURLException e) {
            LOG.log(Level.WARNING, "Problem during accessing jar file needed to computation: {0}", e.toString());
            return null;
        }
    }

    private void createTasks(Project project) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        File dataFolder = FilesStructure.getTempDirInProject(project.getClientName(), project.getProjectName());
        File saveFolder = FilesStructure.getReadyDirInProject(project.getClientName(), project.getProjectName());
        File[] dataFiles = dataFolder.listFiles();
        int numberOfTasks = 0;
        for (File file : dataFiles) {
            Task task = new Task(project.getProjectName(), project.getClientName(), file.getName(), project.getPriority(), project.getCores(), project.getMemory(), project.getTime());
            task.setClass(classManager.loadClass(task.getClientName(), task.getProjectUID()));
            task.loadData(FilesStructure.getTaskLoadDataPath(task.getUnicateID()));
            File output = new File(saveFolder, file.getName());
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(output))) {
                out.writeObject(task);
                project.addTask(task.getUnicateID());
                //tasksPool.add(task.getUnicateID()); //- pridano do commentu protoze je tam pak pridavam vsechny najednou
                LOG.log(Level.INFO, "Task created: {0}, {1}{2}", new Object[]{task.getUnicateID(), saveFolder, file.getName()});
                out.close();
            }
            numberOfTasks++;
        }
        project.setNumOfAllTasks(numberOfTasks);
        project.setState(ProjectState.ACTIVE);
    }

    public synchronized Project createPreparingProject(String clientID, String projectID, int priority, int cores, int memory, int time) {
        Project project = new Project(ProjectState.PREPARING, priority, cores, memory, time, clientID, projectID);
        putProjectIntoAllPreparing(project);
        return project;
    }

    private void moveJarAndExtractTasks(Project project) throws ExtractionException, NotSupportedArchiveException, IOException {
        File inUploadDir = FilesStructure.getClientUploadedDir(project.getClientName(), project.getProjectName());
        File[] files = inUploadDir.listFiles();
        for (File file : files) {
            if (file.getName().endsWith(".jar")) {
                CustomIO.copyFile(file, FilesStructure.getProjectJarFile(project.getClientName(), project.getProjectName()));
            } else {
                new Extractor(file, project).unpack();
            }
        }

    }

    public boolean isProjectCompleted(String clientID, String projectID) {
        if (projectsCompleted.contains(new ProjectUID(clientID, projectID))) {
            return true;
        } else {
            return false;
        }
    }

    public void putProjectIntoAllPreparing(Project project) {
        projectsAll.put(project.getProjectUID(), project);
        projectsPreparing.put(project.getProjectUID(), project);
    }

    public void undoProject(Project project) {
        projectsPreparing.remove(project.getProjectUID());
        projectsAll.remove(project.getProjectUID());
    }

    private void changePreparingToActive(Project project) {
        projectsPreparing.remove(project.getProjectUID());
        projectsActive.put(project.getProjectUID(), project);
    }

    public void addProject(final Project project) {
        new Thread() {
            public void run() {
                //preparation of folders for client's project
                FilesStructure.createClientProjectDirs(project.getClientName(), project.getProjectName());
                try {
                    moveJarAndExtractTasks(project);
                    createTasks(project);
                    changePreparingToActive(project);
                    addTasksToPool(project.getClientName(), project.getProjectName());
                } catch (ExtractionException | NotSupportedArchiveException e) {
                    LOG.log(Level.WARNING, e.getMessage());
                    undoProject(project);
                } catch (ClassNotFoundException e) {
                    LOG.log(Level.WARNING, "ClassNotFoundException during task creation : {0}", e.toString());
                    undoProject(project);
                } catch (IllegalAccessException e) {
                    LOG.log(Level.WARNING, "Illegal access: {0}", e.getMessage());
                    undoProject(project);
                } catch (InstantiationException e) {
                    LOG.log(Level.WARNING, "Instantiation problem: {0}", e.getMessage());
                    undoProject(project);
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Creating tasks: {0}", e.toString());
                    undoProject(project);
                }
            }
        }.start();
    }

    private void deleteProject(String clientID, String projectID) {
        File projectDir = FilesStructure.getClientProjectsDir(clientID, projectID);
        File uploadedDir = FilesStructure.getClientUploadedDir(clientID, projectID);
        CustomIO.deleteDirectory(uploadedDir);
        CustomIO.deleteDirectory(projectDir);
    }

    public boolean cancelProject(String clientID, String projectID) {
        if (isProjectInManager(clientID, projectID)) {

            Project project = projectsAll.get(new ProjectUID(clientID, projectID));
            project.setState(ProjectState.CANCELED);
            projectsAll.remove(project.getProjectUID());
            projectsCompleted.remove(project.getProjectUID());
            projectsActive.remove(project.getProjectUID());
            projectsPaused.remove(project.getProjectUID());

            cleanTasksPool(clientID, projectID);
            // TODO smazat ulohy ze seznamu tasks in progress
            deleteProject(clientID, projectID);
            return true;
        } else {
            return false;
        }
    }

    public boolean pauseProject(String clientID, String projectID) {
        if (isProjectInManager(clientID, projectID)) {
            Project project = projectsAll.get(new ProjectUID(clientID, projectID));
            project.setState(ProjectState.PAUSED);
            projectsActive.remove(project.getProjectUID());
            projectsPaused.put(project.getProjectUID(), project);
            cleanTasksPool(clientID, projectID);
            return true;
        } else {
            return false;
        }
    }

    public boolean resumeProject(String clientID, String projectID) {
        if (isProjectInManager(clientID, projectID)) {
            Project project = projectsAll.get(new ProjectUID(clientID, projectID));
            project.setState(ProjectState.ACTIVE);
            projectsActive.put(project.getProjectUID(), project);
            projectsPaused.remove(project.getProjectUID());
            addTasksToPool(clientID, projectID);
            return true;
        } else {
            return false;
        }
    }

    public ArrayList<ProjectInfo> getProjectList(String cliendID) {
        ArrayList<ProjectInfo> projects = new ArrayList<>();

        for (Project project : projectsAll.values()) {
            if (project.getClientName().equals(cliendID)) {
                projects.add(project.getProjectInfo());
            }
        }
        return projects;
    }

    public void addCompletedTask(String clientID, TaskID ID) {

        HashMap<ProjectUID, Project> pausedActive = new HashMap<>();
        // pokud je projekt pozastaven, ulohy na klientech necham dopocitat a ulozim je
        // pokud se klient selze, je uloha vracena do seznamu nezpracovanych u projektu
        pausedActive.putAll(projectsPaused);
        pausedActive.putAll(projectsActive);
        final Project project = pausedActive.get(ID.getProjectUID());
        project.addCompletedTask(ID);
        tasksByClients.get(clientID).remove(ID);
        tasksInProgress.remove(ID);


        // IF all project tasks are completed
        if (project.allTasksCompleted()) {
            project.setState(ProjectState.COMPLETED);
            projectsCompleted.put(ID.getProjectUID(), project);
            projectsActive.remove(ID.getProjectUID());
            final Future<Boolean> f = executor.submit(new ProjectPacker(project));
            try {
                Boolean result = f.get();
                // if (result == Boolean.TRUE) {
                projectsCompleted.remove(project.getProjectUID());
                projectsForDownload.put(project.getProjectUID(), project);
                // }//TODO osetrit kdyz vysledek je ne
            } catch (ExecutionException e) {
                LOG.log(Level.WARNING, "Error durong project packing: {0}", ((Exception) e.getCause()).toString());
            } catch (InterruptedException e) {
                LOG.log(Level.WARNING, "Interpution during project packing");
            }
            /*       executor.submit(new Runnable() {
             @Override
             public void run() {
             try {
             Boolean result = f.get();
             if (result == Boolean.TRUE) {
             projectsCompleted.remove(project.getProjectUID());
             projectsForDownload.put(project.getProjectUID(), project);
             }//TODO osetrit kdyz vysledek je ne
             } catch (ExecutionException e) {
             logger.log("Error inside task, client has to fix it!", ELoggerMessages.ERROR);
             } catch (InterruptedException e) {
             logger.log("interpution");
             }
             }
             });*/
            // ProjectPacker packer = new ProjectPacker(temp, projectsForDownload, logger);
            // packer.packIt();
        }

    }

    public void removeDownloadedProject(ProjectUID uid) {

        deleteProject(uid.getClientName(), uid.getProjectName());
        projectsAll.remove(uid);
        classManager.deleteCustomClassLoader(uid.getClientName());
        projectsForDownload.remove(uid);
    }
}
