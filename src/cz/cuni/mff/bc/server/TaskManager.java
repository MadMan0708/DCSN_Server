/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import cz.cuni.mff.bc.server.strategies.Planner;
import cz.cuni.mff.bc.api.main.CustomIO;
import cz.cuni.mff.bc.api.main.ProjectUID;
import cz.cuni.mff.bc.api.main.TaskID;
import cz.cuni.mff.bc.api.main.Task;
import cz.cuni.mff.bc.api.enums.ProjectState;
import cz.cuni.mff.bc.api.main.ProjectInfo;
import cz.cuni.mff.bc.misc.CustomObjectInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

/**
 * Task manager
 *
 * @author Jakub Hava
 */
public class TaskManager {

    private ExecutorService executor;
    private FilesStructure filesStructure;
    private ConcurrentHashMap<String, ActiveClient> activeClients;
    private ClassManager classManager;
    private final ConcurrentHashMap<ProjectUID, BlockingQueue<TaskID>> tasksPool;
    private CopyOnWriteArrayList<TaskID> tasksInProgress;
    private ConcurrentHashMap<ProjectUID, Project> projectsActive;
    private ConcurrentHashMap<ProjectUID, Project> projectsPaused;
    private ConcurrentHashMap<ProjectUID, Project> projectsAll;
    private ConcurrentHashMap<ProjectUID, Project> projectsPreparing;
    private ConcurrentHashMap<ProjectUID, Project> projectsCompleted;
    private ConcurrentHashMap<ProjectUID, Project> projectsForDownload;
    private ConcurrentHashMap<ProjectUID, Project> projectsCorrupted;
    private SortedSet<Project> finishingProjects;
    private Planner planner;
    private ServerParams serverParams;
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Server.class.getName());

    /**
     * Constructor
     *
     * @param activeClients list of active client's
     * @param filesStructure files structure
     * @param serverParams server parameters
     */
    public TaskManager(ConcurrentHashMap<String, ActiveClient> activeClients, FilesStructure filesStructure, ServerParams serverParams) {
        this.projectsCorrupted = new ConcurrentHashMap<>();
        this.projectsForDownload = new ConcurrentHashMap<>();
        this.projectsCompleted = new ConcurrentHashMap<>();
        this.projectsPreparing = new ConcurrentHashMap<>();
        this.projectsAll = new ConcurrentHashMap<>();
        this.projectsPaused = new ConcurrentHashMap<>();
        this.projectsActive = new ConcurrentHashMap<>();
        this.tasksInProgress = new CopyOnWriteArrayList<>();
        Comparator<Project> comparatorForFinishing = new Comparator<Project>() {
            @Override
            public int compare(Project p1, Project p2) { // for highest priority first
                if (p1.getPriority() > p2.getPriority()) {
                    return 1;
                } else if (p1.getPriority() == p2.getPriority()) {
                    return 0;
                } else {
                    return -1;
                }
            }
        };
        this.finishingProjects = new ConcurrentSkipListSet<>(comparatorForFinishing);
        this.planner = new Planner();
        this.serverParams = serverParams;
        this.filesStructure = filesStructure;
        this.activeClients = activeClients;
        this.classManager = new ClassManager(filesStructure);
        this.executor = Executors.newCachedThreadPool();
        this.tasksPool = new ConcurrentHashMap<>();
    }

    /**
     * Gets the class manager
     *
     * @return class manager
     */
    public ClassManager getClassManager() {
        return classManager;
    }

    /**
     * Gets the files structure
     *
     * @return files structure
     */
    public FilesStructure getFileStructure() {
        return filesStructure;
    }

    /**
     * Checks if the client has any project in progress
     *
     * @param clientName client's name
     * @return true if client has project in progress, false otherwise
     */
    public boolean clientInActiveComputation(String clientName) {
        Set<ProjectUID> projectsUIDs = projectsAll.keySet();
        for (ProjectUID projectUID : projectsUIDs) {
            if (projectUID.getClientName().equals(clientName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the project is ready for download
     *
     * @param clientName client's name
     * @param projectName project name
     * @return true if the project is ready for download, false otherwise
     */
    public boolean isProjectReadyForDownload(String clientName, String projectName) {
        if (projectsForDownload.containsKey(new ProjectUID(clientName, projectName))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks of the project is corrupted
     *
     * @param clientName client's name
     * @param projectName project name
     * @return true if the project is corrupted, false otherwise
     */
    public boolean isProjectCorrupted(String clientName, String projectName) {
        if (projectsCorrupted.containsKey(new ProjectUID(clientName, projectName))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks if the project is completed
     *
     * @param clientName client's name
     * @param projectName project name
     * @return true if the project is completed, false otherwise
     */
    public boolean isProjectCompleted(String clientName, String projectName) {
        if (projectsCompleted.contains(new ProjectUID(clientName, projectName))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks if task is completed
     *
     * @param ID task ID
     * @return true if the task computation is completed, false otherwise
     */
    public boolean isTaskCompleted(TaskID ID) {
        return projectsAll.get(ID.getProjectUID()).isTaskCompleted(ID);
    }

    /**
     * Checks if task is in progress
     *
     * @param ID task ID
     * @return true if the task computation is in progress, false otherwise
     */
    public boolean isTaskInProgress(TaskID ID) {
        if (tasksInProgress.contains(ID)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks if the project is in the task manager
     *
     * @param clientName client's name
     * @param projectName project's name
     * @return true if project exists in task manager, false otherwise
     */
    public boolean isProjectInManager(String clientName, String projectName) {
        if (projectsAll.containsKey(new ProjectUID(clientName, projectName))) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * Cleans task pool. Deletes all task of one project from the tasks pool
     */
    private void cleanTasksPool(String clientName, String projectName) {
        ProjectUID projectUID = new ProjectUID(clientName, projectName);
        synchronized (tasksPool) {
            tasksPool.remove(projectUID);
        }
    }

    /*
     * Cleans tasks in progress. Deletes all task of one project from the tasks in progress
     */
    private void cleanTasksInProgress(String clientName, String projectName) {
        ArrayList<TaskID> toDel = new ArrayList<>();
        ProjectUID UID = new ProjectUID(clientName, projectName);
        synchronized (tasksInProgress) {
            for (TaskID taskID : tasksInProgress) {
                if (taskID.getProjectUID().equals(UID)) {
                    toDel.add(taskID);
                }
            }
            tasksInProgress.removeAll(toDel);
        }
    }

    /*
     * Adds tasks to the task pool
     */
    private void addTasksToPool(String clientName, String projectName) {
        ProjectUID projectUID = new ProjectUID(clientName, projectName);
        Set<TaskID> uncompleted = projectsAll.get(projectUID).getUncompletedTasks();
        synchronized (tasksPool) {
            tasksPool.remove(projectUID);
            tasksPool.put(projectUID, new LinkedBlockingQueue<>(uncompleted));
        }
    }

    /*
     * Adds task back to the task pool
     */
    private void addTaskBackToPool(TaskID taskID) {
        // tasks pool has to contain taskID.getProjectUID()
        tasksPool.get(taskID.getProjectUID()).add(taskID);
    }

    /**
     * Checks if the client is in list of active clients
     *
     * @param clientName client's name
     * @return true if the client is active, false otherwise
     */
    public boolean isClientActive(String clientName) {
        if (activeClients.containsKey(clientName)) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * Checks if the client is active and computing tasks
     */
    private boolean isClientComputing(String clientName) {
        if (isClientActive(clientName) && activeClients.get(clientName).isComputing()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Unassociates task from the client
     *
     * @param clientName client's name
     * @param taskID task ID
     */
    public void cancelTaskAssociation(String clientName, TaskID taskID) {
        if (isClientComputing(clientName)) { // othwerwise the client is not in the list and nothing can be unassociated from his lists
            activeClients.get(clientName).unassociateClientWithTask(taskID);
        }
        tasksInProgress.remove(taskID);
        Project p = projectsAll.get(taskID.getProjectUID());
        p.addTaskAgain(taskID); // adds task again to the project's uncompleted list

        if (p.getState().equals(ProjectState.ACTIVE)) {
            addTaskBackToPool(taskID);// if the project is active, the tasks is added to the task pool
            LOG.log(Level.INFO, "Task {0} calculated by {1} is again in tasks pool", new Object[]{taskID, clientName});
            // otherwise no
        }
    }

    /**
     * Unassociates all task from the given client
     *
     * @param clientName client's name
     * @return list of unassociated tasks
     */
    public synchronized ArrayList<TaskID> cancelTasksAssociation(String clientName) {
        HashMap<ProjectUID, ArrayList<TaskID>> tasks = null;
        if (isClientComputing(clientName)) {
            tasks = activeClients.get(clientName).getCurrentTasks();
        }

        ArrayList<TaskID> toPrint = new ArrayList<>();
        if (tasks != null) {
            for (Entry<ProjectUID, ArrayList<TaskID>> entry : tasks.entrySet()) {
                for (TaskID taskID : entry.getValue()) {
                    toPrint.add(taskID);
                    cancelTaskAssociation(clientName, taskID);
                }
            }
        }
        return toPrint;
    }

    /*
     * Check if there are unassigned tasks in projects which are in finishing projects
     */
    private boolean finishingProjectsHasTasks(SortedSet<Project> projects) {
        for (Project project : projects) {
            if (project.getNumOfTasksUncompleted() != 0) {
                return true;
            }
        }
        return false;
    }

    /*
     * Check if there are unassigned tasks in projects which are part of regular planning
     */
    private boolean regularProjectsHasTasks(Set<ProjectUID> projects) {
        for (ProjectUID projectUID : projects) {
            if (projectsAll.get(projectUID).getNumOfTasksUncompleted() != 0) {
                return true;
            }
        }
        return false;
    }
    /*
     * Gets project which will user calculate from finishing projects
     */

    private ProjectUID getTaskFromFinishing(ActiveClient active) {
        int coresAvailable = active.getAvailableCores();
        int memoryLimit = active.getMemoryLimit();
        for (Project project : finishingProjects) {
            if (project.getMemory() <= memoryLimit && project.getCores() <= coresAvailable
                    && project.getNumOfTasksUncompleted() != 0) { // if num of uncompleted tasks is 0, the tasks will be propably
                // be soon calculated by different client, it is better to skip
                // to the next project
                return project.getProjectUID();
            }
        }
        return null; // return null to wait for current tasks to finish so the
        // almost finished tasks are calculated as soon as possible
    }

    /*
     * Gets project which will user calculate from projects which aren't finishing
     */
    private ProjectUID getTaskFromRegularPlan(ActiveClient active) {
        // regular planning
        HashMap<ProjectUID, ArrayList<TaskID>> currentTasks = active.getCurrentTasks();
        for (Entry<ProjectUID, Integer> entry : active.getCurrentPlan().entrySet()) {
            if (!currentTasks.containsKey(entry.getKey())) {
                return entry.getKey();
            } else if (currentTasks.get(entry.getKey()).size() < entry.getValue()) { // if there can be more tasks of one project
                return entry.getKey();
            } else {
                continue;
            }
        }
        // this part never returns null because of design of planning, but it has to be here to preserve correctness of the function
        return null;
    }

    /**
     * Gets the Project ID before calculation
     *
     * @param clientName client's name
     * @return project unique ID
     */
    public synchronized ProjectUID getProjectIDBeforeCalculation(String clientName) {
        ActiveClient active = activeClients.get(clientName);
        if (!finishingProjects.isEmpty() && finishingProjectsHasTasks(finishingProjects)) {
            // planning for finishing projects
            return getTaskFromFinishing(active);
        } else {
            if (regularProjectsHasTasks(active.getCurrentPlan().keySet())) {
                return getTaskFromRegularPlan(active);
            } else {
                // No more tasks, checkers on clients will be forced to sleep for a while
                return null;
            }
        }
    }

    /**
     * Gets the task and associates it with the client
     *
     * @param clientName client's name
     * @param projectUID unique project ID from which task will be gotten
     * @return task
     */
    public synchronized Task getTask(String clientName, ProjectUID projectUID) {
        TaskID id = tasksPool.get(projectUID).poll();
        if (id == null) {
            return null; // the pool for this project is empty, in that case client checker will wait for a while
        }
        File f = filesStructure.getTaskLoadPath(id).toFile();
        File jar = filesStructure.getProjectJarFile(id.getClientName(), id.getProjectName());
        try {
            classManager.getClassLoader(clientName).addNewUrl(jar.toURI().toURL());
            try (CustomObjectInputStream ois = new CustomObjectInputStream(new FileInputStream(f), classManager.getClassLoader(clientName))) {
                Task task = (Task) ois.readObject();
                // client is in the list for sure, because he just asked for new task to compute, therefore is connected
                activeClients.get(clientName).associateClientWithTask(id);
                tasksInProgress.add(id);
                LOG.log(Level.INFO, "Task: {0} is sent for computation by client: {1}", new Object[]{task.getUnicateID(), clientName});
                return task;
            } catch (ClassNotFoundException | IOException e) {
                LOG.log(Level.WARNING, "Problem during unpacking task: {0}", e.toString());
                addTaskBackToPool(id);
                return null;
            }
        } catch (MalformedURLException e) {
            LOG.log(Level.WARNING, "Problem during accessing jar file needed to computation: {0}", e.toString());
            addTaskBackToPool(id);
            return null;
        }

    }

    /*
     * Move the project jar file to client's directory and extracts the tasks
     */
    private void moveJarAndExtractTasks(Project project) throws IOException {
        File inUploadDir = filesStructure.getClientUploadedDir(project.getClientName(), project.getProjectName());
        File[] files = inUploadDir.listFiles();
        for (File file : files) {
            if (file.getName().endsWith(".jar")) {
                CustomIO.copyFile(file, filesStructure.getProjectJarFile(project.getClientName(), project.getProjectName()));
            } else {
                new Extractor(file, project.getProjectName(), project.getClientName(), filesStructure.getTempDirInProject(project.getClientName(), project.getProjectName())).unpack();
            }
        }
    }

    /*
     * Creates the tasks after the project data are uploaded
     */
    private void createTasks(Project project) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        File dataFolder = filesStructure.getTempDirInProject(project.getClientName(), project.getProjectName());
        File saveFolder = filesStructure.getReadyDirInProject(project.getClientName(), project.getProjectName());
        File[] dataFiles = dataFolder.listFiles();
        int numberOfTasks = 0;
        for (File file : dataFiles) {
            Task task = new Task(project.getProjectName(), project.getClientName(), file.getName(), project.getPriority(), project.getCores(), project.getMemory(), project.getTime());
            task.setClass(classManager.loadClass(task.getClientName(), task.getProjectUID()));
            task.loadData(filesStructure.getTaskLoadDataPath(task.getUnicateID()));
            File output = new File(saveFolder, file.getName());
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(output))) {
                out.writeObject(task);
                project.addTask(task.getUnicateID());
                LOG.log(Level.INFO, "Task created: {0}, {1}{2}", new Object[]{task.getUnicateID(), saveFolder, file.getName()});
                out.close();
            }
            numberOfTasks++;
        }
        project.setNumOfAllTasks(numberOfTasks);
        project.setState(ProjectState.ACTIVE);
    }

    /**
     * Creates the project object with preparing state
     *
     * @param clientName client's name
     * @param projectName project name
     * @param priority project priority
     * @param cores cores limit
     * @param memory memory limit
     * @param time average task time
     * @return created project
     */
    public synchronized Project createPreparingProject(String clientName, String projectName, int priority, int cores, int memory, int time) {
        Project project = new Project(ProjectState.PREPARING, priority, cores, memory, time, clientName, projectName);
        putProjectIntoAllPreparing(project);
        return project;
    }

    /*
     * Puts project into set of projects with preparing state
     */
    private void putProjectIntoAllPreparing(Project project) {
        projectsAll.put(project.getProjectUID(), project);
        projectsPreparing.put(project.getProjectUID(), project);
    }

    /**
     * Removes the project if something went wrong
     *
     * @param project project to remove
     */
    public void undoProject(Project project) {
        projectsPreparing.remove(project.getProjectUID());
        projectsAll.remove(project.getProjectUID());
        classManager.deleteCustomClassLoader(project.getProjectUID().getClientName());
        deleteProject(project.getClientName(), project.getProjectName());
    }

    /*
     * Deletes the project files
     */
    private void deleteProject(String clientName, String projectName) {
        File projectDir = filesStructure.getClientProjectsDir(clientName, projectName);
        File uploadedDir = filesStructure.getClientUploadedDir(clientName, projectName);
        CustomIO.deleteDirectory(uploadedDir);
        CustomIO.deleteDirectory(projectDir);
    }

    /*
     * Move project from set of preparing projects to set of active projects
     */
    private void changePreparingToActive(Project project) {
        projectsPreparing.remove(project.getProjectUID());
        projectsActive.put(project.getProjectUID(), project);
    }

    /**
     * Adds the project to the task manager
     *
     * @param project project to add
     */
    public void addProject(final Project project) {
        new Thread() {
            @Override
            public void run() {
                // directory preparation for client's project
                filesStructure.createClientProjectDirs(project.getClientName(), project.getProjectName());
                try {
                    moveJarAndExtractTasks(project);
                    createTasks(project);
                    changePreparingToActive(project);
                    addTasksToPool(project.getClientName(), project.getProjectName());
                    planForAll();
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

    /**
     * Marks the project as corrupted
     *
     * @param clientName client's name
     * @param projectName project name
     */
    public void markProjectAsCorrupted(String clientName, String projectName) {
        if (isProjectInManager(clientName, projectName)) {
            Project project = projectsAll.get(new ProjectUID(clientName, projectName));
            project.setState(ProjectState.CORRUPTED);
            projectsActive.remove(project.getProjectUID());
            projectsCorrupted.put(project.getProjectUID(), project);
            cleanTasksPool(clientName, projectName);
            cleanTasksInProgress(clientName, projectName);
            planForAll();
        }
    }

    /**
     * Cancels and deletes the project, creates new plan after
     *
     * @param clientName client's name
     * @param projectName project name
     * @return true if project has been successfully cancelled, false otherwise
     */
    public boolean cancelProject(String clientName, String projectName) {
        if (isProjectInManager(clientName, projectName)) {
            Project project = projectsAll.get(new ProjectUID(clientName, projectName));
            projectsAll.remove(project.getProjectUID());
            projectsCompleted.remove(project.getProjectUID());
            projectsActive.remove(project.getProjectUID());
            projectsPaused.remove(project.getProjectUID());
            projectsCorrupted.remove(project.getProjectUID());
            cleanTasksPool(clientName, projectName);
            cleanTasksInProgress(clientName, projectName);
            deleteProject(clientName, projectName);
            planForAll();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Pauses the project
     *
     * @param clientName client's name
     * @param projectName project name
     * @return true if project has been successfully paused, false otherwise
     */
    public boolean pauseProject(String clientName, String projectName) {
        if (isProjectInManager(clientName, projectName)) {
            Project project = projectsAll.get(new ProjectUID(clientName, projectName));
            project.setState(ProjectState.PAUSED);
            projectsActive.remove(project.getProjectUID());
            projectsPaused.put(project.getProjectUID(), project);
            cleanTasksPool(clientName, projectName);
            cleanTasksInProgress(clientName, projectName);
            planForAll();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Resumes the project
     *
     * @param clientName client's name
     * @param projectName project name
     * @return true if project has been successfully resumed, false otherwise
     */
    public boolean resumeProject(String clientID, String projectID) {
        if (isProjectInManager(clientID, projectID)) {
            Project project = projectsAll.get(new ProjectUID(clientID, projectID));
            project.setState(ProjectState.ACTIVE);
            projectsActive.put(project.getProjectUID(), project);
            projectsPaused.remove(project.getProjectUID());
            addTasksToPool(clientID, projectID);
            planForAll();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Gets the list of client's projects
     *
     * @param clientName client's name
     * @return list of client's projects
     */
    public ArrayList<ProjectInfo> getProjectList(String clientName) {
        ArrayList<ProjectInfo> projects = new ArrayList<>();

        for (Project project : projectsAll.values()) {
            if (project.getClientName().equals(clientName)) {
                projects.add(project.getProjectInfo());
            }
        }
        return projects;
    }

    /**
     * Creates a plan for one client
     *
     * @param activeClient active client
     */
    public void planForOne(ActiveClient activeClient) {
        if (isClientComputing(activeClient.getClientName())) {
            planner.planForOne(activeClient, serverParams.getStrategy());
            LOG.log(Level.INFO, "Plan for the client {0} have been created, strategy used : {1}", new Object[]{activeClient.getClientName(), serverParams.getStrategy()});
        }
    }

    /**
     * Creates a plan for one client
     *
     * @param activeClient active client's name
     */
    public void planForOne(String activeClient) {
        planForOne(activeClients.get(activeClient));
    }

    /*
     * Plan for all clients. Removes project with absolute priority from the list of active
     * projects, because faster processing is done for them
     */
    private synchronized void planForAll() {
        ArrayList<Project> values = new ArrayList<>(projectsActive.values());
        values.removeAll(finishingProjects);
        // create new plan because finishing projects are not part of the planning process
        planner.planForAll(activeClients.values(), values, serverParams.getStrategy());
        LOG.log(Level.INFO, "Plans for all clients have been created, strategy used : {0}", serverParams.getStrategy());
    }

    /**
     * Adds completed task to the project list of completed tasks
     *
     * @param clientName
     * @param ID task ID
     */
    public synchronized void addCompletedTask(String clientName, TaskID ID) {
        HashMap<ProjectUID, Project> pausedActive = new HashMap<>();
        // if the project is paused, tasks on the clients are let to finish computation and saved after
        // if computation fails on the client, the task is then returned to the project uncompleted list
        pausedActive.putAll(projectsPaused);
        pausedActive.putAll(projectsActive);
        final Project project = pausedActive.get(ID.getProjectUID());
        if (project != null) { // if the project still exists ( it may have been cancelled but client didn't know about that
            project.addCompletedTask(ID);
            tasksInProgress.remove(ID);
            activeClients.get(clientName).unassociateClientWithTask(ID);
            if (project.getNumOfTasksUncompleted() <= Planner.TASK_LIMIT_FOR_ABSOLUTE_PROCCESING && !finishingProjects.contains(project)) {
                finishingProjects.add(project);
                planForAll();
                // new plan for active clients is created
            }

            if (project.allTasksCompleted()) {
                // if all project tasks are completed, the task is packed
                packProject(project);
            }
        }

    }

    /*
     * Packs the completed project
     */
    private void packProject(Project project) {
        project.setState(ProjectState.COMPLETED);
        projectsCompleted.put(project.getProjectUID(), project);
        projectsActive.remove(project.getProjectUID());
        File sourceDirectory = filesStructure.getCompleteDirInProject(project.getClientName(), project.getProjectName());
        File destination = filesStructure.getCalculatedDataFile(project.getClientName(), project.getProjectName());
        final Future<Boolean> f = executor.submit(new ProjectPacker(project, sourceDirectory, destination));
        try {
            Boolean result = f.get(); // waits utill the packing is done
            projectsCompleted.remove(project.getProjectUID());
            projectsForDownload.put(project.getProjectUID(), project);
        } catch (ExecutionException e) {
            LOG.log(Level.WARNING, "Error durong project packing: {0}", ((Exception) e.getCause()).toString());
        } catch (InterruptedException e) {
            LOG.log(Level.WARNING, "Interpution during project packing");
        }
    }

    /**
     * Removes the downloaded project
     *
     * @param uid project unique id
     */
    public void removeDownloadedProject(ProjectUID uid) {
        deleteProject(uid.getClientName(), uid.getProjectName());
        projectsAll.remove(uid);
        classManager.deleteCustomClassLoader(uid.getClientName());
        projectsForDownload.remove(uid);
    }
}
