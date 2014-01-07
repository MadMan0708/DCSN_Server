/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.api.main;

import java.io.Serializable;
import java.util.Objects;

/**
 *
 * @author Aku
 */
public class TaskID implements Serializable {

    private int coreUsage;
    private int memoryUsage;
    private int runningTime;
    private int priority;
    private String projectID;
    private String clientID;
    private String taskID;

    public TaskID(String projectID, String clientID, String taskID, int priority, int cores, int memory, int runningTime) {
        this.projectID = projectID;
        this.clientID = clientID;
        this.taskID = taskID;
        this.coreUsage = cores;
        this.memoryUsage = memory;
        this.runningTime = runningTime;
        this.priority = priority;
    }

    public int getCoreUsage() {
        return coreUsage;
    }

    public void setCoreUsage(int coreUsage) {
        this.coreUsage = coreUsage;
    }

    public int getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(int memoryUsage) {
        this.memoryUsage = memoryUsage;
    }

    public int getRunningTime() {
        return runningTime;
    }

    public void setRunningTime(int runningTime) {
        this.runningTime = runningTime;
    }

    public int getPriority() {
        return priority;
    }

    public String getProjectID() {
        return projectID;
    }

    public ProjectUID getProjectUID() {
        return new ProjectUID(clientID, projectID);
    }

    public String getClientID() {
        return clientID;
    }

    public String getTaskID() {
        return taskID;
    }

    @Override
    public String toString() {
        return clientID + "_" + projectID + "_" + taskID;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof TaskID)) {
            return false;
        } else {
            TaskID ID = (TaskID) other;
            if (this.clientID.equals(ID.getClientID()) && this.projectID.equals(ID.getProjectID()) && this.taskID.equals(ID.getTaskID())) {
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + Objects.hashCode(this.projectID);
        hash = 53 * hash + Objects.hashCode(this.clientID);
        hash = 53 * hash + Objects.hashCode(this.taskID);
        return hash;
    }
}
