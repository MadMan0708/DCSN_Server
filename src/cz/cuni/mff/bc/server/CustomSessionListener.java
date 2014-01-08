/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import org.cojen.dirmi.Link;
import org.cojen.dirmi.Session;
import org.cojen.dirmi.SessionAcceptor;
import org.cojen.dirmi.SessionCloseListener;

/**
 *
 * @author Jakub
 */
public class CustomSessionListener implements org.cojen.dirmi.SessionListener {

    private TaskManager taskManager;
    private HashMap<String, ActiveClient> activeClients;
    private IServerImpl remoteMethods;
    private SessionAcceptor sesAcceptor;
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Server.class.getName());

    public CustomSessionListener(IServerImpl remoteMethods, SessionAcceptor sesAcceptor, TaskManager taskManager) {
        this.taskManager = taskManager;
        this.remoteMethods = remoteMethods;
        this.activeClients = remoteMethods.getActiveClients();
        this.sesAcceptor = sesAcceptor;
    }

    @Override
    public synchronized void established(Session session) throws IOException {
        sesAcceptor.accept(this); // starts listening for possible new session on same session listener
        final String clientID = (String) session.receive();
        if (!activeClients.containsKey(clientID)) {
            ActiveClient activeClient = new ActiveClient(clientID, session);
            activeClients.put(clientID, activeClient);
            taskManager.classManager.setCustomClassLoader(clientID);
            session.setClassLoader(taskManager.classManager.getClassLoader(clientID));
            session.send(Boolean.TRUE);
            session.send(remoteMethods);
            LOG.log(Level.INFO, "Client {0} has been connected to the server", clientID);
            session.addCloseListener(new SessionCloseListener() {
                @Override
                public void closed(Link sessionLink, SessionCloseListener.Cause cause) {
                    taskManager.classManager.deleteCustomClassLoader(clientID);
                    try {
                        activeClients.get(clientID).getSession().close();
                        activeClients.remove(clientID);
                        LOG.log(Level.INFO, "Client {0} has been disconnected form the server", clientID);
                        sessionLink.close();
                    } catch (IOException e) {
                        LOG.log(Level.WARNING, "Closing session; Cause: {0}; {1}", new Object[]{cause.name(), e.getMessage()});
                    }

                }
            });

        } else {
            session.send(Boolean.FALSE);
        }
    }

    @Override
    public void establishFailed(IOException cause) throws IOException {
        //TODO
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void acceptFailed(IOException cause) throws IOException {
        // closing acception of new clients
    }
}
