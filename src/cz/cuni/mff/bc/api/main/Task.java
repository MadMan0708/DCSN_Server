/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.api.main;

import cz.cuni.mff.bc.api.enums.TaskState;
import java.io.Serializable;
import java.nio.file.Path;

/**
 *
 * @author Aku
 */
public class Task implements Serializable {

    private TaskID unicateTaskID;
    public ITask computeTask;
    private TaskState state = TaskState.BEFORE_START;
    private boolean dataHasBeenSaved = false;

    public Task(String projectID, String clientID, String taskID, int priority, int cores, int memory, int time) {
        this.unicateTaskID = new TaskID(projectID, clientID, taskID, priority, cores, memory, time);
    }

    public void setClass(Class<?> className) throws IllegalAccessException, InstantiationException {
        this.computeTask = (ITask) className.newInstance();
    }

    public boolean hasDataBeenSaved() {
        return dataHasBeenSaved;
    }

    public String getClientID() {
        return unicateTaskID.getClientID();
    }

    public String getProjectID() {
        return unicateTaskID.getProjectID();
    }

    public TaskID getUnicateID() {
        return unicateTaskID;
    }

    public ProjectUID getProjectUID() {
        return new ProjectUID(unicateTaskID.getClientID(), unicateTaskID.getProjectID());
    }

    public void calculate() {

        computeTask.calculate();
    }

    public void loadData(Path from) {
        computeTask.loadData(from);
    }

    public void saveData(Path to) {
        computeTask.saveData(to);
        dataHasBeenSaved = true;
    }

    public void setState(TaskState state) {
        this.state = state;
    }

    public TaskState getState() {
        return this.state;
    }
}
