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
import cz.cuni.mff.bc.server.logging.CustomLogger;
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
    private ArrayList<ActiveClient> computingClients;
    private ArrayList<Project> activeProjects;
    private CustomLogger LOG_PLAN;
    private CustomLogger LOG_TASKS;
    private CustomLogger LOG_PROJECTS_CURRENT_CLIENTS;
    private CustomLogger LOG_PROJECTS_POSSIBLE_CLIENTS;

    public Planner(ServerParams serverParams) {
        this.serverParams = serverParams;
        this.computingClients = new ArrayList<>();
        this.activeProjects = new ArrayList<>();
        LOG_PLAN = new CustomLogger("server.current_plans.log");
        LOG_TASKS = new CustomLogger("server.current_tasks.log");
        LOG_PROJECTS_CURRENT_CLIENTS = new CustomLogger("server.projects_current_clients.log");
        LOG_PROJECTS_POSSIBLE_CLIENTS = new CustomLogger("server.projects_possible_clients.log");

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
    public synchronized void planForAll(Collection<ActiveClient> activeClients, ArrayList<Project> activeProjects, StrategiesList strategy) {
        ArrayList<ActiveClient> computing = getClientsInComputation(activeClients);
        this.computingClients = new ArrayList<>(computing);
        this.activeProjects = new ArrayList<>(activeProjects);
        strategies.get(strategy).planForAll(computing, activeProjects);
        for (ActiveClient activeClient : computing) {
            logCurrentPlan(activeClient);
            logProjectAssociation();
            logPossibleProjectAssociation();
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
            if (!computingClients.contains(activeClient)) {
                computingClients.add(activeClient);
            }
            strategies.get(strategy).planForOne(activeClient);
            logCurrentPlan(activeClient);
            logProjectAssociation();
            logPossibleProjectAssociation();
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
        LOG_PLAN.log("-----------------------------");
        LOG_PLAN.log(activeClient.getClientName() + " >> Current plan: ");
        if (!activeClient.getCurrentPlan().isEmpty()) {
            for (Entry<ProjectUID, Integer> entry : activeClient.getCurrentPlan().entrySet()) {
                LOG_PLAN.log("\t Tasks from project " + entry.getKey().getProjectName() + ", " + entry.getValue() + "x");
            }
        } else {
            LOG_PLAN.log(" \t Plan is empty");
        }
    }

    /**
     * Create log message which contains current tasks running on this client
     *
     * @param activeClient active client
     */
    public void logCurrentTasks(ActiveClient activeClient) {
        LOG_TASKS.log("-----------------------------");
        LOG_TASKS.log(activeClient.getClientName() + " >> Current tasks: ");
        for (Entry<ProjectUID, ArrayList<TaskID>> entry : activeClient.getCurrentTasks().entrySet()) {
            for (TaskID taskID : entry.getValue()) {
                LOG_TASKS.log("\t Task " + taskID.getTaskName() + " from project " + entry.getKey().getProjectName());
            }
        }
    }

    /**
     * Create log message which contains projects and clients on which these
     * projects can be calculated
     *
     */
    public void logPossibleProjectAssociation() {
        LOG_PROJECTS_POSSIBLE_CLIENTS.log("------------NEW POSSIBLE ASSOCIATION------------");
        for (Entry<Project, ArrayList<ActiveClient>> entry : getPossibleProjectsAssociation().entrySet()) {
            LOG_PROJECTS_POSSIBLE_CLIENTS.log("Project " + entry.getKey().getProjectName() + " could be planned on:");
            for (ActiveClient activeClient : entry.getValue()) {
                LOG_PROJECTS_POSSIBLE_CLIENTS.log(activeClient.getClientName());
            }
        }
    }

    public void logProjectAssociation() {
        HashMap<Project, Integer> notPlannedLatelyNumbers = getNotPlannedLatelyNumbers();
        LOG_PROJECTS_CURRENT_CLIENTS.log("------------PROJECTS PLANNED ON------------");
        for (Entry<Project, ArrayList<ActiveClient>> entry : getPossibleProjectsAssociation().entrySet()) {

            if (entry.getValue().isEmpty()) {
                LOG_PROJECTS_CURRENT_CLIENTS.log("Project " + entry.getKey().getProjectName() + " hasn't been planned for " + notPlannedLatelyNumbers.get(entry.getKey()) + " planning");
            } else {
                LOG_PROJECTS_CURRENT_CLIENTS.log("Project " + entry.getKey().getProjectName() + " is planned on:");
                for (ActiveClient activeClient : entry.getValue()) {
                    LOG_PROJECTS_CURRENT_CLIENTS.log(activeClient.getClientName());
                }
            }
        }
    }

    /*
     * Gets list of projects with list of clients on which project is planned
     */
    private HashMap<Project, ArrayList<ActiveClient>> getProjectsAssociation() {
        HashMap<Project, ArrayList<ActiveClient>> list = new HashMap<>();

        for (Project project : activeProjects) {
            list.put(project, new ArrayList<ActiveClient>());
            for (ActiveClient activeClient : computingClients) {
                if (activeClient.getCurrentPlan().containsKey(project.getProjectUID())) {
                    list.get(project).add(activeClient);
                }
            }
        }
        return list;
    }

    /*
     * Gets list of projects with list of clients on which project could be planned
     */
    private HashMap<Project, ArrayList<ActiveClient>> getPossibleProjectsAssociation() {
        HashMap<Project, ArrayList<ActiveClient>> list = new HashMap<>();
        for (Project project : activeProjects) {
            list.put(project, new ArrayList<ActiveClient>());
            for (ActiveClient activeClient : computingClients) {
                if (project.getMemory() <= activeClient.getMemoryLimit() && project.getCores() <= activeClient.getCoresLimit()) {
                    list.get(project).add(activeClient);
                }
            }
        }
        return list;
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
