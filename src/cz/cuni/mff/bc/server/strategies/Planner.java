/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server.strategies;

import cz.cuni.mff.bc.api.main.ProjectUID;
import cz.cuni.mff.bc.api.main.TaskID;
import cz.cuni.mff.bc.computation.ActiveClient;
import cz.cuni.mff.bc.computation.Project;
import cz.cuni.mff.bc.server.ServerParams;
import cz.cuni.mff.bc.server.logging.CustomLogger;
import static cz.cuni.mff.bc.server.strategies.StrategiesList.HIGHEST_PRIORITY_FIRST;
import static cz.cuni.mff.bc.server.strategies.StrategiesList.MAXIMAL_THROUGHPUT;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Implementation of plan creating for the clients
 *
 * @author Jakub Hava
 */
public class Planner {

    private final HashMap<StrategiesList, IStrategy> strategies;
    private final ServerParams serverParams;
    private ArrayList<ActiveClient> computingClients;
    private ArrayList<Project> activeProjects;
    private final CustomLogger LOG_PLAN;
    private final CustomLogger LOG_TASKS;
    private final CustomLogger LOG_PROJECTS_CURRENT_CLIENTS;
    private final CustomLogger LOG_PROJECTS_POSSIBLE_CLIENTS;

    /**
     * Constructor
     *
     * @param serverParams server parameters
     */
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
        LOG_PLAN.log("--PLANNING FOR ALL");
        for (ActiveClient activeClient : computing) {
            logCurrentPlan(activeClient);
        }
        LOG_PROJECTS_CURRENT_CLIENTS.log("--PLANNING FOR ALL");
        logProjectAssociation();
        LOG_PROJECTS_POSSIBLE_CLIENTS.log("--PLANNING FOR ALL");
        logPossibleProjectAssociation();
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
            LOG_PLAN.log("--PLANNING FOR ONE");
            logCurrentPlan(activeClient);
            LOG_PROJECTS_CURRENT_CLIENTS.log("--PLANNING FOR ONE");
            logProjectAssociation();
            LOG_PROJECTS_POSSIBLE_CLIENTS.log("--PLANNING FOR ONE");
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
        LOG_PLAN.log("%s >> Current plan: ", activeClient.getClientName());
        if (!activeClient.getCurrentPlan().isEmpty()) {
            for (Entry<ProjectUID, Integer> entry : activeClient.getCurrentPlan().entrySet()) {
                LOG_PLAN.log("\t Tasks from project %s, %sx", entry.getKey().getProjectName(), entry.getValue());
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
        LOG_TASKS.log("%s >> Current tasks: ", activeClient.getClientName());
        for (Entry<ProjectUID, ArrayList<TaskID>> entry : activeClient.getCurrentTasks().entrySet()) {
            for (TaskID taskID : entry.getValue()) {
                LOG_TASKS.log("\t Task %s from project %s", taskID.getTaskName(), entry.getKey().getProjectName());
            }
        }
    }

    /**
     * Create log message which contains projects and clients on which these
     * projects can be calculated
     *
     */
    public void logPossibleProjectAssociation() {
        HashMap<Project, ArrayList<ActiveClient>> possibleProjectsAssociation = getPossibleProjectsAssociation();
        if (!possibleProjectsAssociation.isEmpty()) {
            LOG_PROJECTS_POSSIBLE_CLIENTS.log("------------NEW POSSIBLE ASSOCIATION------------");
            for (Entry<Project, ArrayList<ActiveClient>> entry : possibleProjectsAssociation.entrySet()) {
                LOG_PROJECTS_POSSIBLE_CLIENTS.log("\t Project %s could be planned on:", entry.getKey().getProjectName());
                for (ActiveClient activeClient : entry.getValue()) {
                    LOG_PROJECTS_POSSIBLE_CLIENTS.log("\t\t %s", activeClient.getClientName());
                }
            }
        }
    }

    /**
     * Create log message which contains projects and clients on which these
     * projects are calculated
     *
     */
    public void logProjectAssociation() {
        HashMap<Project, ArrayList<ActiveClient>> projectsAssociation = getProjectsAssociation();
        if (!projectsAssociation.isEmpty()) {
            HashMap<Project, Integer> notPlannedLatelyNumbers = getNotPlannedLatelyNumbers();
            LOG_PROJECTS_CURRENT_CLIENTS.log("------------PROJECTS PLANNED ON------------");
            for (Entry<Project, ArrayList<ActiveClient>> entry : projectsAssociation.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    LOG_PROJECTS_CURRENT_CLIENTS.log("Project  hasn't been planned for %s planning", entry.getKey().getProjectName(), notPlannedLatelyNumbers.get(entry.getKey()));
                } else {
                    LOG_PROJECTS_CURRENT_CLIENTS.log("\t Project %s is planned on:", entry.getKey().getProjectName());
                    for (ActiveClient activeClient : entry.getValue()) {
                        LOG_PROJECTS_CURRENT_CLIENTS.log("\t\t %s", activeClient.getClientName());
                    }
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
