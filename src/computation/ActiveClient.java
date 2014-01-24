/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package computation;

import cz.cuni.mff.bc.api.main.ProjectUID;
import cz.cuni.mff.bc.api.main.TaskID;
import cz.cuni.mff.bc.misc.IClient;
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
    private IClient clientMethods;
    private int memoryLimit;
    private int coresLimit;
    private HashMap<ProjectUID, ArrayList<TaskID>> currentTasks;
    // preserve order of inserted item
    private LinkedHashMap<ProjectUID, Integer> currentPlan;
    private boolean computing;
    private IActiveClientListener listener;

    /**
     * Constructor
     *
     * @param clientName client's name
     * @param session client's session
     * @param listener listener which listen to changes in client's computation state
     */
    public ActiveClient(String clientName, Session session, IActiveClientListener listener) {
        this.listener = listener;
        this.currentPlan = new LinkedHashMap<>();
        this.currentTasks = new HashMap<>();
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
     * Gets current plan
     *
     * @return current plan
     */
    public LinkedHashMap<ProjectUID, Integer> getCurrentPlan() {
        return currentPlan;
    }

    /**
     * Sets the current plan
     *
     * @param currentPlan list with projects planned
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
