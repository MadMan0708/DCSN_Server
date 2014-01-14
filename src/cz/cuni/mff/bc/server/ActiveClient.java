/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import cz.cuni.mff.bc.api.main.ProjectUID;
import cz.cuni.mff.bc.api.main.TaskID;
import cz.cuni.mff.bc.misc.IClient;
import java.util.ArrayList;
import java.util.Timer;
import org.cojen.dirmi.Session;

/**
 * Stores information about active client connection
 *
 * @author Jakub Hava
 */
public class ActiveClient {

    private String clientName;
    private Timer timer;
    private Boolean timeout;
    private Session session;
    private IClient clientMethods;
    private int memoryLimit;
    private int coresLimit;
    private ArrayList<TaskID> currentTasks;
    private ArrayList<ProjectUID> currentPlan;
    private boolean computing;

    /**
     * Constructor
     *
     * @param clientName client's name
     * @param session client's session
     */
    public ActiveClient(String clientName, Session session) {
        this.currentPlan = new ArrayList<>();
        this.currentTasks = new ArrayList<>();
        this.clientName = clientName;
        this.session = session;
    }

    /**
     * Gets client implementation of remote interface
     *
     * @return client implementation of remote interface
     */
    public IClient getClientMethods() {
        return clientMethods;
    }


    /**
     * Sets client implementation of client remote interface
     *
     * @param clientMethods client implementation of remote interface
     */
    public void setClientMethods(IClient clientMethods) {
        this.clientMethods = clientMethods;
    }

    /**
     * Sets if the client is computing task or not
     *
     * @param computing true if client is computing tasks, false if not
     */
    public void setComputing(boolean computing) {
        this.computing = computing;
    }

    /**
     * Checks if the client is computing the tasks
     *
     * @return true of client is computing, false if not
     */
    public boolean isComputing() {
        return computing;
    }

    /**
     * Sets new plan for the client
     *
     * @param plan new plan for the client
     */
    public void setPlan(ArrayList<ProjectUID> plan) {
        currentPlan = plan;
    }

    /**
     * Associates client with the given task
     *
     * @param taskID task to add
     */
    public synchronized void associateClientWithTask(TaskID taskID) {
        currentTasks.add(taskID);
    }

    /**
     *
     * Unassociates client with the given task
     *
     * @param id task to unassociate
     */
    public synchronized void unassociateClientWithTask(TaskID id) {
        currentTasks.remove(id);
    }

    /**
     * Gets current plan
     *
     * @return current plan
     */
    public ArrayList<ProjectUID> getCurrentPlan() {
        return currentPlan;
    }

    /**
     * Sets the current plan
     *
     * @param currentPlan list with projects planned
     */
    public void setCurrentPlan(ArrayList<ProjectUID> currentPlan) {
        this.currentPlan = currentPlan;
    }

    /**
     * Gets number of cores used on the client
     *
     * @return number of cores used on the client
     */
    public int getCurrentCoresUsed() {
        int coreUsage = 0;
        for (TaskID taskID : currentTasks) {
            coreUsage = coreUsage + taskID.getCores();
        }
        return coreUsage;
    }

    /**
     * Gets client's current tasks
     *
     * @return current tasks
     */
    public ArrayList<TaskID> getCurrentTasks() {
        return currentTasks;
    }

    /**
     * Gets number of available cores
     *
     * @return number of available cores
     */
    public int getAvailableCores() {
        return coresLimit - getCurrentCoresUsed();
    }

    /**
     *
     * @return client's name
     */
    public String getClientName() {
        return clientName;
    }

    /**
     *
     * @param clientName client's name
     */
    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    /**
     *
     * @return timer
     */
    public Timer getTimer() {
        return timer;
    }

    /**
     *
     * @param timer timer
     */
    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    /**
     *
     * @return timeout
     */
    public Boolean getTimeout() {
        return timeout;
    }

    /**
     *
     * @param timeout timeout
     */
    public void setTimeout(Boolean timeout) {
        this.timeout = timeout;
    }

    /**
     *
     * @return client's session
     */
    public Session getSession() {
        return session;
    }

    /**
     *
     * @param session client's session
     */
    public void setSession(Session session) {
        this.session = session;
    }

    /**
     *
     * @return memory limit
     */
    public int getMemoryLimit() {
        return memoryLimit;
    }

    /**
     *
     * @param memoryLimit memory limit
     */
    public void setMemoryLimit(int memoryLimit) {
        this.memoryLimit = memoryLimit;
    }

    /**
     *
     * @return cores limit
     */
    public int getCoresLimit() {
        return coresLimit;
    }

    /**
     *
     * @param coresLimit cores limit
     */
    public void setCoresLimit(int coresLimit) {
        this.coresLimit = coresLimit;
    }
}
