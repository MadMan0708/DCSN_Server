/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server.strategies;

import cz.cuni.mff.bc.api.main.ProjectUID;
import cz.cuni.mff.bc.api.main.TaskID;
import cz.cuni.mff.bc.server.ActiveClient;
import cz.cuni.mff.bc.server.Project;
import cz.cuni.mff.bc.server.ServerParams;
import cz.cuni.mff.bc.server.TaskManager;
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
    private ServerParams serverParams;
    private static final java.util.logging.Logger LOG_PLAN = java.util.logging.Logger.getLogger(Planner.class.getName());
    private static final java.util.logging.Logger LOG_TASKS = java.util.logging.Logger.getLogger(Planner.class.getName());
    private static final java.util.logging.Logger LOG_PROJECTS_CURRENT_CLIENTS = java.util.logging.Logger.getLogger(Planner.class.getName());
    private static final java.util.logging.Logger LOG_PROJECTS_POSSIBLE_CLIENTS = java.util.logging.Logger.getLogger(Planner.class.getName());

    public Planner(ServerParams serverParams) {
        this.serverParams = serverParams;

        LOG_PLAN.addHandler(CustomHandler.createLogHandler("server.plan.log"));
        LOG_TASKS.addHandler(CustomHandler.createLogHandler("server.tasks.log"));
        LOG_PROJECTS_CURRENT_CLIENTS.addHandler(CustomHandler.createLogHandler("server.projects_current_clients.log"));
        LOG_PROJECTS_POSSIBLE_CLIENTS.addHandler(CustomHandler.createLogHandler("server.projects_possible_clients.log"));

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
        for (ActiveClient activeClient : computing) {
            logCurrentPlan(activeClient);
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
            logCurrentPlan(activeClient);
        }
    }

    private HashMap<Project, Integer> getNotPlannedLatelyNumbers() {
        return strategies.get(serverParams.getStrategy()).getNotPlannedLatelyNumbers();
    }
    /*
     * Log plan for one client
     */

    /**
     * Create log message which contains projects planned for this client
     *
     * @param activeClient active client
     */
    public void logCurrentPlan(ActiveClient activeClient) {
        LOG_TASKS.log(Level.INFO, "{0} >> Current plan: ", activeClient.getClientName());
        if (!activeClient.getCurrentPlan().isEmpty()) {
            for (Entry<ProjectUID, Integer> entry : activeClient.getCurrentPlan().entrySet()) {
                LOG_PLAN.log(Level.INFO, "\t Tasks from project {0}, {1}x", new Object[]{entry.getKey().getProjectName(), entry.getValue()});
            }
        } else {
            LOG_PLAN.log(Level.INFO, " \t Plan is empty");
        }
    }

    /*
     * Basic function use to log the plan
     */
    public void logCurrentTasks(ActiveClient activeClient) {
        LOG_TASKS.log(Level.INFO, "{0} >> Current task: ", activeClient.getClientName());
        for (Entry<ProjectUID, ArrayList<TaskID>> entry : activeClient.getCurrentTasks().entrySet()) {
            for (TaskID taskID : entry.getValue()) {
                LOG_TASKS.log(Level.INFO, "\t Task {0} from project {1}", new Object[]{taskID.getTaskName(), entry.getKey().getProjectName()});
            }
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
