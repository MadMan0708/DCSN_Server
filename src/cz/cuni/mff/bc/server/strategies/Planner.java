/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server.strategies;

import cz.cuni.mff.bc.api.main.ProjectUID;
import cz.cuni.mff.bc.server.ActiveClient;
import cz.cuni.mff.bc.server.Project;
import cz.cuni.mff.bc.server.logging.CustomFormater;
import cz.cuni.mff.bc.server.logging.CustomHandler;
import cz.cuni.mff.bc.server.logging.FileLogger;
import static cz.cuni.mff.bc.server.strategies.StrategiesList.HIGHEST_PRIORITY_FIRST;
import static cz.cuni.mff.bc.server.strategies.StrategiesList.MAXIMAL_THROUGHPUT;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;

/**
 * Implementation of plan creating for the clients
 *
 * @author Jakub Hava
 */
public class Planner {

    /**
     * This limit says when any project has less then TASK_LIMIT uncompleted
     * tasks, the absolute priority is given to the project
     */
    public static int TASK_LIMIT_FOR_ABSOLUTE_PROCCESING = 5;
    private HashMap<StrategiesList, IStrategy> strategies;
    private CustomHandler logHandler;
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Planner.class.getName());

    public Planner() {
        logHandler = new CustomHandler();
        logHandler.setFormatter(new CustomFormater());
        logHandler.setLevel(Level.ALL);
        logHandler.addLogTarget(new FileLogger(new File("server.plan.log")));
        LOG.addHandler(logHandler);


        strategies = new HashMap<>();
        strategies.put(MAXIMAL_THROUGHPUT, new MaxThroughputStrategy(5));
        strategies.put(HIGHEST_PRIORITY_FIRST, new HighestPriorityStrategy());
    }

    /**
     * Creates plans for all the clients according to the active strategy
     *
     * @param activeClients list of active clients
     * @param activeProjects list of active projects
     * @param strategy actual strategy
     */
    public synchronized void planForAll(Collection<ActiveClient> activeClients, Collection<Project> activeProjects, StrategiesList strategy) {
        ArrayList<ActiveClient> computing = getClientsInComputation(activeClients);
        strategies.get(strategy).planForAll(computing, activeProjects);
        LOG.log(Level.INFO, "Replanning for all clients:");
        for (ActiveClient activeClient : computing) {
            logPlanForOne(activeClient);
        }
    }

    /**
     * Creates plans for one client according to the active strategy
     *
     * @param activeClient active client
     * @param strategy actual strategy
     */
    public synchronized void planForOne(ActiveClient activeClient, StrategiesList strategy) {
        if (activeClient.isComputing()) {
            strategies.get(strategy).planForOne(activeClient);
            LOG.log(Level.INFO, "Replanning for client {0}", activeClient.getClientName());
            logPlanForOne(activeClient);
        }
    }

    /*
     * Log plan for one client
     */
    private void logPlanForOne(ActiveClient activeClient) {
        LOG.log(Level.INFO, "\t{0}: ", activeClient.getClientName());
        if (!activeClient.getCurrentPlan().isEmpty()) {
            for (Entry<ProjectUID, Integer> entry : activeClient.getCurrentPlan().entrySet()) {
                LOG.log(Level.INFO, "\t\t {0}x : {1} by {2}", new Object[]{entry.getValue(), entry.getKey().getProjectName(), entry.getKey().getClientName()});
            }
        } else {
            LOG.log(Level.INFO, "\t\t Plan empty");
        }
    }
    /*
     * Gets only clients which are currently in computation state
     */

    private ArrayList<ActiveClient> getClientsInComputation(Collection<ActiveClient> activeClients) {
        ArrayList<ActiveClient> onlyComputing = new ArrayList<>();
        for (ActiveClient activeClient : activeClients) {
            if (activeClient.isComputing()) {
                onlyComputing.add(activeClient);
            }
        }
        return onlyComputing;
    }
}
