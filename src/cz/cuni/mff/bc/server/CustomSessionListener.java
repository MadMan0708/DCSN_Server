/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import cz.cuni.mff.bc.common.main.Logger;
import cz.cuni.mff.bc.common.enums.ELoggerMessages;
import cz.cuni.mff.bc.common.enums.EUserAddingState;
import java.io.IOException;
import java.util.ArrayList;
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
    private ArrayList<String> activeConnections;
    private Logger logger;
    private IServerImpl remoteMethods;
    private SessionAcceptor sesAcceptor;

    public CustomSessionListener(IServerImpl remoteMethods, SessionAcceptor sesAcceptor) {
        this.taskManager = Server.getTaskManager();
        this.activeConnections = Server.getActiveConnections();
        this.logger = Server.getLogger();
        this.remoteMethods = remoteMethods;
        this.sesAcceptor = sesAcceptor;
    }

    @Override
    public synchronized void established(Session session) throws IOException {
        sesAcceptor.accept(this); // starts listening for possible new session on same session listener
        final String clientID = (String) session.receive();
        if (!activeConnections.contains(clientID)) {
            activeConnections.add(clientID);
            taskManager.classManager.setCustomClassLoader(clientID);
            session.setClassLoader(taskManager.classManager.getClassLoader(clientID));
            session.send(EUserAddingState.OK);
            session.send(remoteMethods);
            logger.log("Client " + clientID + " has been connected to the server");
            session.addCloseListener(new SessionCloseListener() {
                @Override
                public void closed(Link sessionLink, SessionCloseListener.Cause cause) {
                    taskManager.classManager.deleteCustomClassLoader(clientID);
                    activeConnections.remove(clientID);
                    logger.log("Client " + clientID + " has been disconnected form the server");
                    try {
                        sessionLink.close();
                    } catch (IOException e) {
                        logger.log("Closing session; Caouse: " + cause.name() + "; " + e.getMessage(), ELoggerMessages.ERROR);
                    }

                }
            });

        } else {
            session.send(EUserAddingState.EXIST);
        }
    }

    @Override
    public void establishFailed(IOException cause) throws IOException {
        //TODO
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void acceptFailed(IOException cause) throws IOException {
        //TODO
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
