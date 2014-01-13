/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import cz.cuni.mff.bc.api.main.CustomIO;
import cz.cuni.mff.bc.api.main.ProjectUID;
import cz.cuni.mff.bc.api.main.TaskID;
import cz.cuni.mff.bc.api.main.Task;
import cz.cuni.mff.bc.api.enums.ProjectState;
import cz.cuni.mff.bc.api.main.ProjectInfo;
import cz.cuni.mff.bc.server.misc.CustomObjectInputStream;
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
 * Task manager
 *
 * @author Jakub Hava
 */
public class TaskManager {

    private ExecutorService executor;
    private FilesStructure filesStructure;
    private ClassManager classManager;
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
    private ConcurrentHashMap<ProjectUID, Project> projectsCorrupted = new ConcurrentHashMap<>();
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Server.class.getName());

    /**
     * Constructor
     *
     * @param filesStructure files structure
     */
    public TaskManager(FilesStructure filesStructure) {
        this.filesStructure = filesStructure;
        this.classManager = new ClassManager(filesStructure);
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
     * Checks if there are no tasks in the task pool
     */
    private boolean zeroTasks() {
        if (tasksPool.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * Check if there is next task for the computation
     */
    private boolean hasNextTask() {
        if (!zeroTasks()) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * Gets next task ID
     */
    private TaskID getNextTask() {
        return tasksPool.poll();
    }

    /*
     * Cleans task pool. Deletes all task of one project from the tasks pool
     */
    private void cleanTasksPool(String clientName, String projectName) {
        ArrayList<TaskID> toDel = new ArrayList<>();
        ProjectUID UID = new ProjectUID(clientName, projectName);
        synchronized (tasksPool) {
            for (TaskID taskID : tasksPool) {
                if (taskID.getProjectUID().equals(UID)) {
                    toDel.add(taskID);
                }
            }
            tasksPool.removeAll(toDel);
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
        Set<TaskID> uncompleted = projectsAll.get(new ProjectUID(clientName, projectName)).getUncompletedTasks();
        synchronized (tasksPool) {
            tasksPool.addAll(uncompleted);
        }
    }

    /**
     * Gets task associated with client
     *
     * @param clientName client's name
     * @return tasks associated with client
     */
    public CopyOnWriteArrayList<TaskID> getClientAssociatedTasks(String clientName) {
        return tasksByClients.get(clientName);
    }

    /*
     * Unassociates client with the given task
     */
    private void unassociateClientWithTask(String clientName, TaskID id) {
        if (tasksByClients.get(clientName) != null) { //if client has some tasks associated
            tasksByClients.get(clientName).remove(id);
            if (tasksByClients.get(clientName).isEmpty()) {
                tasksByClients.remove(clientName);
            }
        }
    }

    /*
     * Associates client with the given task
     */
    private void associateClientWithTask(String clientName, TaskID taskID) {
        synchronized (tasksByClients) {
            if (tasksByClients.get(clientName) == null) {
                tasksByClients.put(clientName, new CopyOnWriteArrayList<TaskID>());
            }
            tasksByClients.get(clientName).add(taskID);
        }
    }

    /**
     * Unassociates task from the client
     *
     * @param clientName client's name
     * @param taskID task ID
     */
    public void cancelTaskAssociation(String clientName, TaskID taskID) {
        unassociateClientWithTask(clientName, taskID); // unassociate them
        tasksInProgress.remove(taskID);
        Project p = projectsAll.get(taskID.getProjectUID());
        p.addTaskAgain(taskID); // adds task again to the project's uncompleted list
        LOG.log(Level.INFO, "Task {0} calculated by {1} is again in tasks pool", new Object[]{taskID, clientName});
        if (p.getState().equals(ProjectState.ACTIVE)) {
            tasksPool.add(taskID);// if the project is active, the tasks is added to the task pool
            // otherwise no
        }
    }

    /**
     * Unassociates all task from the given client
     *
     * @param clientName client's name
     * @return list of unassociated tasks
     */
    public ArrayList<TaskID> cancelTasksAssociation(String clientName) {
        CopyOnWriteArrayList<TaskID> tasks = tasksByClients.get(clientName);
        ArrayList<TaskID> toPrint = new ArrayList<>();
        if (tasks != null) {
            toPrint.addAll(tasks);
            for (TaskID taskID : tasks) {
                cancelTaskAssociation(clientName, taskID);
            }
        }
        return toPrint;
    }

    /**
     * Gets the Task ID before calculation
     *
     * @param clientName client's name
     * @return task ID
     */
    public TaskID getTaskIDBeforeCalculation(String clientName) {
        if (hasNextTask()) {
            TaskID id = getNextTask();
            tasksBeforeCalc.add(id);
            return id;
        } else {
            // No more tasks, checkers on clients will be forced to sleep for a while
            return null;
        }
    }

    /**
     * Gets the task and associates it with the client
     *
     * @param clientName client's name
     * @param id task ID
     * @return task
     */
    public Task getTask(String clientName, TaskID id) {
        File f = filesStructure.getTaskLoadPath(id).toFile();
        File jar = filesStructure.getProjectJarFile(id.getClientName(), id.getProjectName());
        try {
            classManager.getClassLoader(clientName).addNewUrl(jar.toURI().toURL());
            try (CustomObjectInputStream ois = new CustomObjectInputStream(new FileInputStream(f), classManager.getClassLoader(clientName))) {
                Task task = (Task) ois.readObject();
                associateClientWithTask(clientName, id);
                tasksBeforeCalc.remove(id);
                tasksInProgress.add(id);
                LOG.log(Level.INFO, "Task: {0} is sent for computation by client: {1}", new Object[]{task.getUnicateID(), clientName});
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
                //tasksPool.add(task.getUnicateID()); //- pridano do commentu protoze je tam pak pridavam vsechny najednou
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
     * Cancels and deletes the project
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

            cleanTasksPool(clientName, projectName);
            cleanTasksInProgress(clientName, projectName);
            deleteProject(clientName, projectName);
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
     * Adds completed task to the project list of completed tasks
     *
     * @param clientName
     * @param ID task ID
     */
    public void addCompletedTask(String clientName, TaskID ID) {

        HashMap<ProjectUID, Project> pausedActive = new HashMap<>();
        // if the project is paused, tasks on the clients are let to finish computation and saved after
        // if computation fails on the client, the task is then returned to the project uncompleted list
        pausedActive.putAll(projectsPaused);
        pausedActive.putAll(projectsActive);
        final Project project = pausedActive.get(ID.getProjectUID());
        project.addCompletedTask(ID);
        tasksByClients.get(clientName).remove(ID);
        tasksInProgress.remove(ID);

        // IF all project tasks are completed
        if (project.allTasksCompleted()) {
            project.setState(ProjectState.COMPLETED);
            projectsCompleted.put(ID.getProjectUID(), project);
            projectsActive.remove(ID.getProjectUID());
            File sourceDirectory = filesStructure.getCompleteDirInProject(project.getClientName(), project.getProjectName());
            File destination = filesStructure.getCalculatedDataFile(project.getClientName(), project.getProjectName());
            final Future<Boolean> f = executor.submit(new ProjectPacker(project, sourceDirectory, destination));
            try {
                Boolean result = f.get();
                projectsCompleted.remove(project.getProjectUID());
                projectsForDownload.put(project.getProjectUID(), project);
            } catch (ExecutionException e) {
                LOG.log(Level.WARNING, "Error durong project packing: {0}", ((Exception) e.getCause()).toString());
            } catch (InterruptedException e) {
                LOG.log(Level.WARNING, "Interpution during project packing");
            }
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
