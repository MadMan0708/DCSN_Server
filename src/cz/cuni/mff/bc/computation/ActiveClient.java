/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.computation;

import cz.cuni.mff.bc.api.main.ProjectUID;
import cz.cuni.mff.bc.api.main.TaskID;
import cz.cuni.mff.bc.server.IActiveClientListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
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
    private int memoryLimit;
    private int coresLimit;
    private final HashMap<ProjectUID, ArrayList<TaskID>> currentTasks;
    // preserve order of inserted item
    private LinkedHashMap<ProjectUID, Integer> currentPlan;
    private boolean computing;
    private final IActiveClientListener listener;

    /**
     * Constructor
     *
     * @param clientName client's name
     * @param session client's session
     * @param listener listener which listen to changes in client's computation
     * state
     */
    public ActiveClient(String clientName, Session session, IActiveClientListener listener) {
        this.listener = listener;
        this.currentPlan = new LinkedHashMap<>();
        this.currentTasks = new HashMap<>();
        this.clientName = clientName;
        this.session = session;
    }

    /**
     * Sets client's state to: in task computation
     *
     */
    public void setToComputing() {
        if (this.computing == false) {
            this.computing = true;
            listener.afterChange();
        }
    }

    /**
     * Sets client's state to: not in task computation
     */
    public void setToNotComputing() {
        this.computing = false;

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
     * Associates client with the given task
     *
     * @param taskID task to add
     */
    public synchronized void associateClientWithTask(TaskID taskID) {
        if (!currentTasks.containsKey(taskID.getProjectUID())) {
            currentTasks.put(taskID.getProjectUID(), new ArrayList<TaskID>());
        }
        currentTasks.get(taskID.getProjectUID()).add(taskID);
    }

    /**
     *
     * Unassociates client with the given task
     *
     * @param taskID task to unassociate
     */
    public synchronized void unassociateClientWithTask(TaskID taskID) {
        currentTasks.get(taskID.getProjectUID()).remove(taskID);
        if (currentTasks.get(taskID.getProjectUID()).isEmpty()) {
            currentTasks.remove(taskID.getProjectUID());
        }
    }

    /**
     * Gets the current plan
     *
     * @return current plan
     */
    public LinkedHashMap<ProjectUID, Integer> getCurrentPlan() {
        return currentPlan;
    }

    /**
     * Sets the current plan
     *
     * @param currentPlan list with planned projects
     */
    public void setCurrentPlan(LinkedHashMap<ProjectUID, Integer> currentPlan) {
        this.currentPlan = currentPlan;
    }

    /**
     * Gets number of cores used on the client
     *
     * @return number of cores used on the client
     */
    public int getCurrentCoresUsed() {
        int coreUsage = 0;
        for (Entry<ProjectUID, ArrayList<TaskID>> entry : currentTasks.entrySet()) {
            coreUsage += entry.getValue().get(0).getCores() * entry.getValue().size();
        }
        return coreUsage;
    }

    /**
     * Gets client's current tasks
     *
     * @return current tasks
     */
    public HashMap<ProjectUID, ArrayList<TaskID>> getCurrentTasks() {
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
     * Gets client's name
     *
     * @return client's name
     */
    public String getClientName() {
        return clientName;
    }

    /**
     * Sets client's name
     *
     * @param clientName client's name
     */
    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    /**
     * Gets the timer
     *
     * @return timer
     */
    public Timer getTimer() {
        return timer;
    }

    /**
     * Sets the timer
     *
     * @param timer timer
     */
    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    /**
     * Gets the timeout which tells if user has sent inform message
     *
     * @return timeout
     */
    public Boolean getTimeout() {
        return timeout;
    }

    /**
     * Sets the timeout which tells if user has sent inform message
     *
     * @param timeout timeout
     */
    public void setTimeout(Boolean timeout) {
        this.timeout = timeout;
    }

    /**
     * Gets the client's session
     *
     * @return client's session
     */
    public Session getSession() {
        return session;
    }

    /**
     * Sets the client's session
     *
     * @param session client's session
     */
    public void setSession(Session session) {
        this.session = session;
    }

    /**
     * Gets the memory limit
     *
     * @return memory limit
     */
    public int getMemoryLimit() {
        return memoryLimit;
    }

    /**
     * Sets the memory limit
     *
     * @param memoryLimit memory limit
     */
    public void setMemoryLimit(int memoryLimit) {
        this.memoryLimit = memoryLimit;
    }

    /**
     * Gets the cores limit
     *
     * @return cores limit
     */
    public int getCoresLimit() {
        return coresLimit;
    }

    /**
     * Sets the cores limit
     *
     * @param coresLimit cores limit
     */
    public void setCoresLimit(int coresLimit) {
        this.coresLimit = coresLimit;
    }
}
