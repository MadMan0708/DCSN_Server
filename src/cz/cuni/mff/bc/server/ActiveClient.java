/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

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

    /**
     * Constructor
     *
     * @param clientName client's name
     * @param session client's session
     */
    public ActiveClient(String clientName, Session session) {
        this.clientName = clientName;
        this.session = session;
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
