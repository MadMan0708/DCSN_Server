/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server.strategies;

import cz.cuni.mff.bc.server.ActiveClient;
import cz.cuni.mff.bc.server.Project;
import static cz.cuni.mff.bc.server.strategies.StrategiesList.HIGHEST_PRIORITY_FIRST;
import static cz.cuni.mff.bc.server.strategies.StrategiesList.MAXIMAL_THROUGHPUT;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

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
    public static int TASK_LIMIT_FOR_ABSOLUTE_PROCCESING = 2;
    private HashMap<StrategiesList, IStrategy> strategies;

    public Planner() {
        strategies = new HashMap<>();
        strategies.put(MAXIMAL_THROUGHPUT, new MaxThroughputStrategy());
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
