/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.computation;

import cz.cuni.mff.bc.api.main.ProjectUID;
import cz.cuni.mff.bc.api.main.TaskID;
import cz.cuni.mff.bc.api.enums.ProjectState;
import cz.cuni.mff.bc.api.main.ProjectInfo;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents DCSN project
 *
 * @author Jakub Hava
 */
public class Project implements Serializable {

    private ProjectState state;
    private int numberOfTask;
    private final String clientName;
    private final String projectName;
    private final int priority;
    private final int cores;
    private final int memory;
    private final int time;
    private final Set<TaskID> tasksUncompleted;
    private final Set<TaskID> tasksCompleted;

    /**
     * Constructor
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

    /**
     * Gets the cores limit
     *
     * @return cores limit
     */
    public int getCores() {
        return cores;
    }


    /**
     * Gets average task time
     *
     * @return average task time
     */
    public int getTime() {
        return time;
    }

    /**
     * Gets the memory limit
     *
     * @return memory limit
     */
    public int getMemory() {
        return memory;
    }


    /**
     * Checks if the task is completed
     *
     * @param taskID task to be checked
     * @return true if tasks is completed, false otherwise
     */
    public boolean isTaskCompleted(TaskID taskID) {
        if (tasksCompleted.contains(taskID)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Gets all uncompleted tasks
     *
     * @return all uncompleted tasks
     */
    public Set<TaskID> getUncompletedTasks() {
        return tasksUncompleted;
    }

    /**
     * Gets number of all completed tasks
     *
     * @return number of completed tasks
     */
    public int getNumOfTasksCompleted() {
        return tasksCompleted.size();
    }

    /**
     * Gets number of all uncompleted tasks
     *
     * @return number of uncompleted tasks
     */
    public int getNumOfTasksUncompleted() {
        return tasksUncompleted.size();
    }

    /**
     * Gets number of all tasks
     *
     * @return number of all tasks
     */
    public int getNumOfAllTasks() {
        return numberOfTask;
    }

    /**
     * Sets number of all tasks
     *
     * @param numberOfTasks number of all tasks
     */
    public void setNumOfAllTasks(int numberOfTasks) {
        this.numberOfTask = numberOfTasks;
    }

    /**
     * Adds task to the project uncompleted list
     *
     * @param task task to add
     */
    public void addTask(TaskID task) {
        tasksUncompleted.add(task);
    }


    /**
     * Adds completed task
     *
     * @param task task to add
     */
    public void addCompletedTask(TaskID task) {
        tasksCompleted.add(task);
        tasksUncompleted.remove(task);
    }

    /**
     * Checks if there are tasks to be calculated
     *
     * @return true if there are no more tasks to be calculated, false otherwise
     */
    public boolean zeroTasks() {
        return tasksUncompleted.isEmpty();
    }

    /**
     * Checks if all tasks are completed
     *
     * @return true if all tasks are completed, false otherwise
     */
    public boolean allTasksCompleted() {
        return tasksCompleted.size() == numberOfTask;
    }

    /**
     * Sets the project state
     *
     * @param state project state
     */
    public void setState(ProjectState state) {
        this.state = state;
    }

    /**
     * Gets the project state
     *
     * @return project state
     */
    public ProjectState getState() {
        return state;
    }

    /**
     * Gets project name
     *
     * @return project name
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * Gets project owner name
     *
     * @return project owner name
     */
    public String getClientName() {
        return clientName;
    }

    /**
     * Gets project priority
     *
     * @return project priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Gets project unique ID
     *
     * @return project unique ID
     */
    public ProjectUID getProjectUID() {
        return new ProjectUID(clientName, projectName);
    }

    /**
     * Gets project info object
     *
     * @return project info object
     */
    public ProjectInfo getProjectInfo() {
        return new ProjectInfo(projectName, clientName, tasksCompleted.size(), numberOfTask, state, priority, cores, memory, time);
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
