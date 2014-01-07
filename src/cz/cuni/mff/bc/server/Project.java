/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import cz.cuni.mff.bc.api.main.ProjectUID;
import cz.cuni.mff.bc.api.main.TaskID;
import cz.cuni.mff.bc.api.enums.ProjectState;
import cz.cuni.mff.bc.api.main.ProjectInfo;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author Aku
 */
public class Project implements Serializable {

    private ProjectState state;
    private int numberOfTask;
    private String clientName;
    private String projectName;
    private int priority;
    private int cores;
    private int memory;
    private int time;
    private Set<TaskID> tasksUncompleted;
    private Set<TaskID> tasksCompleted;

    /**
     *
     * @param state project state during creation
     * @param priority project priority
     * @param cores number of cores used by tasks in this project
     * @param memory amount of memory used by tasks in this project
     * @param time average running time of tasks in this project
     * @param clientName owner of project
     * @param projectName project name
     */
    public Project(ProjectState state, int priority, int cores, int memory, int time, String clientName, String projectName) {
        this.tasksUncompleted = java.util.Collections.synchronizedSet(new HashSet<TaskID>());
        this.tasksCompleted = java.util.Collections.synchronizedSet(new HashSet<TaskID>());
        this.state = state;
        this.priority = priority;
        this.cores = cores;
        this.memory = memory;
        this.time = time;
        this.clientName = clientName;
        this.projectName = projectName;

    }

    public int getCores() {
        return cores;
    }

    public void setCores(int cores) {
        this.cores = cores;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getMemory() {
        return memory;
    }

    public void setMemory(int memory) {
        this.memory = memory;
    }

  
    /**
     *
     * @param taskID task to be checked
     * @return true if tasks is completed, else false
     */
    public boolean isTaskCompleted(TaskID taskID) {
        if (tasksCompleted.contains(taskID)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     *
     * @return all uncompletedTasks
     */
    public Set<TaskID> getUncompletedTasks() {
        return tasksUncompleted;
    }

    /**
     *
     * @return number of completed tasks
     */
    public int getNumOfTasksCompleted() {
        return tasksCompleted.size();
    }

    /**
     *
     * @return number of uncompleted tasks
     */
    public int getNumOfTasksUncompleted() {
        return tasksUncompleted.size();
    }

    /**
     *
     * @return number of all tasks
     */
    public int getNumOfAllTasks() {
        return numberOfTask;
    }

    /**
     *
     * @param numberOfTasks number of all tasks
     */
    public void setNumOfAllTasks(int numberOfTasks) {
        this.numberOfTask = numberOfTasks;
    }

    /**
     *
     * @param task add project task
     */
    public void addTask(TaskID task) {
        tasksUncompleted.add(task);
    }

    /**
     *
     * @param task task to be re-added
     */
    public void addTaskAgain(TaskID task) {
        if (tasksCompleted.contains(task)) {
            return; // leave, task is allready completed
        } else {
            synchronized (tasksUncompleted) {
                if (!tasksUncompleted.contains(task)) {
                    tasksUncompleted.add(task);
                }
            }
        }

    }

    /**
     *
     * @param task add completed task
     */
    public void addCompletedTask(TaskID task) {
        tasksCompleted.add(task);
        tasksUncompleted.remove(task);

    }

    /**
     *
     * @return checks if there are tasks to be calculated
     */
    public boolean zeroTasks() {
        if (tasksUncompleted.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     *
     * @return checks if all tasks are completed
     */
    public boolean allTasksCompleted() {
        if (tasksCompleted.size() == numberOfTask) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * sets project state
     *
     * @param state new state
     */
    public void setState(ProjectState state) {
        this.state = state;
    }

    /**
     *
     * @return project state
     */
    public ProjectState getState() {
        return this.state;
    }

    /**
     *
     * @return project name
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     *
     * @return project owner name
     */
    public String getClientName() {
        return clientName;
    }

    /**
     *
     * @return project priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     *
     * @return project unique ID
     */
    public ProjectUID getProjectUID() {
        return new ProjectUID(clientName, projectName);
    }

    public ProjectInfo getProjectInfo() {
        return new ProjectInfo(projectName, clientName, tasksCompleted.size(), numberOfTask, state, priority);
    }

    @Override
    public String toString() {
        return "ProjectID: " + projectName + "; State: " + state + "; Priority " + priority + "; Num of completed tasks: " + tasksCompleted.size() + "/" + numberOfTask;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + Objects.hashCode(this.clientName);
        hash = 47 * hash + Objects.hashCode(this.projectName);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Project other = (Project) obj;
        if (!Objects.equals(this.clientName, other.clientName)) {
            return false;
        }
        if (!Objects.equals(this.projectName, other.projectName)) {
            return false;
        }
        return true;
    }
}
