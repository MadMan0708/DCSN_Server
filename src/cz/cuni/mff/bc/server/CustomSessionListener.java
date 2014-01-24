/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import computation.ActiveClient;
import computation.TaskManager;
import cz.cuni.mff.bc.api.main.TaskID;
import cz.cuni.mff.bc.misc.IClient;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.cojen.dirmi.Link;
import org.cojen.dirmi.Session;
import org.cojen.dirmi.SessionAcceptor;
import org.cojen.dirmi.SessionCloseListener;

/**
 * Session listener
 *
 * @author Jakub Hava
 */
public class CustomSessionListener implements org.cojen.dirmi.SessionListener {
    
    private TaskManager taskManager;
    private ConcurrentHashMap<String, ActiveClient> activeClients;
    private IServerImpl remoteMethods;
    private SessionAcceptor sesAcceptor;
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Server.class.getName());

    /**
     * Constructor
     *
     * @param remoteMethods implementation of remote interface
     * @param sesAcceptor session acceptor
     */
    public CustomSessionListener(IServerImpl remoteMethods, SessionAcceptor sesAcceptor) {
        this.remoteMethods = remoteMethods;
        this.taskManager = remoteMethods.getTaskManager();
        this.activeClients = remoteMethods.getActiveClients();
        this.sesAcceptor = sesAcceptor;
    }

    /**
     * Method called when connection is established
     *
     * @param session client's session
     * @throws IOException
     */
    @Override
    public synchronized void established(Session session) throws IOException {
        sesAcceptor.accept(this); // starts listening for possible new session on the same session listener
        final String clientID = (String) session.receive();
        if (!activeClients.containsKey(clientID)) {
            ActiveClient activeClient = new ActiveClient(clientID, session, new IActiveClientListener() {
                @Override
                public void afterChange() {
                    taskManager.planForOne(clientID);
                }
            });
            activeClients.put(clientID, activeClient);
            taskManager.getClassManager().setCustomClassLoader(clientID);
            session.setClassLoader(taskManager.getClassManager().getClassLoader(clientID));
            session.send(Boolean.TRUE);
            session.send(remoteMethods);
            activeClient.setClientMethods((IClient) session.receive());
            LOG.log(Level.INFO, "Client {0} has been connected to the server", clientID);
            session.addCloseListener(new SessionCloseListener() {
                @Override
                public void closed(Link sessionLink, SessionCloseListener.Cause cause) {
                    taskManager.getClassManager().deleteCustomClassLoader(clientID);
                    try {
                        if (taskManager.isClientActive(clientID)) {
                            activeClients.get(clientID).getSession().close();
                            Collection<ArrayList<TaskID>> values = activeClients.get(clientID).getCurrentTasks().values();
                            for (ArrayList<TaskID> arrayList : values) {
                                for (TaskID taskID : arrayList) {
                                    LOG.log(Level.INFO, "Task " + taskID.getTaskName() + " sent back to the task pool");
                                    taskManager.addTaskBackToPool(taskID);
                                }
                            }
                            activeClients.remove(clientID);
                            LOG.log(Level.INFO, "Client {0} has been disconnected form the server", clientID);
                            sessionLink.close();
                        }
                    } catch (IOException e) {
                        LOG.log(Level.WARNING, "Closing session; Cause: {0}; {1}", new Object[]{cause.name(), e.getMessage()});
                    }
                }
            });
            
        } else {
            session.send(Boolean.FALSE);
        }
    }

    /**
     * Method called when establishing failed
     *
     * @param cause cause
     * @throws IOException
     */
    @Override
    public void establishFailed(IOException cause) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Method called when session is not accepted
     *
     * @param cause cause
     * @throws IOException
     */
    @Override
    public void acceptFailed(IOException cause) throws IOException {
        // it's not happening
    }
}
