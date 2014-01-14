/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import static cz.cuni.mff.bc.server.StrategiesList.HIGHEST_PRIORITY_FIRST;
import static cz.cuni.mff.bc.server.StrategiesList.MAXIMAL_THROUGHPUT;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Implementation of strategies plans creating for the clients
 *
 * @author Jakub Hava
 */
public class Planner {

    private int replanned = 0;

    public int getNumberOfReplanning() {
        return replanned;
    }

    public void resetNumberOfReplanning() {
    }

    /**
     * Creates plans for all the clients according to the active strategy
     *
     * @param activeClients list of active clients
     * @param activeProjects list of active projects
     * @param strategy actual strategy
     */
    public static void plan(ArrayList<ActiveClient> activeClients, Collection<Project> activeProjects, StrategiesList strategy) {
        ArrayList<ActiveClient> computing = filterProjects(activeClients);
        switch (strategy) {
            case HIGHEST_PRIORITY_FIRST:
                planForHighestPriority(computing, activeProjects);
                break;
            case MAXIMAL_THROUGHPUT:
                planForMaxThroughput(computing, activeProjects);
                break;
        }
    }

    /**
     * Creates plans for one client according to the active strategy
     *
     * @param activeClient active client
     * @param activeProjects list of active projects
     * @param strategy actual strategy
     */
    public static void plan(ActiveClient activeClient, Collection<Project> activeProjects, StrategiesList strategy) {
        switch (strategy) {
            case HIGHEST_PRIORITY_FIRST:
                if (activeClient.isComputing()) {
                    planForHighestPriority(activeClient, activeProjects);
                }
                break;
            case MAXIMAL_THROUGHPUT:
                if (activeClient.isComputing()) {
                    planForMaxThroughput(activeClient, activeProjects);
                }
                break;
        }
    }

    /*
     * Filters only clients which are currently in computation
     */
    private static ArrayList<ActiveClient> filterProjects(ArrayList<ActiveClient> activeClients) {
        ArrayList<ActiveClient> onlyComputing = new ArrayList<>();
        for (ActiveClient activeClient : activeClients) {
            if (activeClient.isComputing()) {
                onlyComputing.add(activeClient);
            }
        }
        return onlyComputing;
    }

    /*
     * Creates the plan for all the clients according the strategy Highest Priority First
     */
    private static void planForHighestPriority(ArrayList<ActiveClient> activeClients, Collection<Project> activeProjects) {
    }

    /*
     * Creates the plan for one client according the strategy Highest Priority First
     */
    private static void planForHighestPriority(ActiveClient activeClient, Collection<Project> activeProjects) {
    }

    /*
     * Creates the plan for all the clients according the strategy Maximal Throughput
     */
    private static void planForMaxThroughput(ArrayList<ActiveClient> activeClients, Collection<Project> activeProjects) {
    }

    /*
     * Creates the plan for one client according the strategy Maximal Throughput
     */
    private static void planForMaxThroughput(ActiveClient activeClient, Collection<Project> activeProjects) {
    }
}
