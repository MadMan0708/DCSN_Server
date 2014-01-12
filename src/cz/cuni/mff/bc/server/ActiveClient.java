/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import java.util.Timer;
import org.cojen.dirmi.Session;

/**
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

    public String getClientName() {
        return clientName;
    }

    public ActiveClient(String clientName, Session session) {
        this.clientName = clientName;
        this.session = session;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public Timer getTimer() {
        return timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    public Boolean getTimeout() {
        return timeout;
    }

    public void setTimeout(Boolean timeout) {
        this.timeout = timeout;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public int getMemoryLimit() {
        return memoryLimit;
    }

    public void setMemoryLimit(int memoryLimit) {
        this.memoryLimit = memoryLimit;
    }

    public int getCoresLimit() {
        return coresLimit;
    }

    public void setCoresLimit(int coresLimit) {
        this.coresLimit = coresLimit;
    }
}
