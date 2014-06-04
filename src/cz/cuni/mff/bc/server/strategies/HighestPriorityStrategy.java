/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server.strategies;

import cz.cuni.mff.bc.api.main.ProjectUID;
import cz.cuni.mff.bc.computation.ActiveClient;
import cz.cuni.mff.bc.computation.Project;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

/**
 * Implementation of Highest Priority First strategy
 *
 * @author Jakub Hava
 */
public class HighestPriorityStrategy implements IStrategy {

    private LinkedList<Project> allProjectsSorted;
    private HashMap<Pair, LinkedList<Project>> availableProjects;
    private HashMap<Pair, LinkedList<Project>> availableProjectsBackup;
    private HashMap<Pair, LinkedList<Project>> usedProjects;
    private final Comparator<Project> comparator;
    private LinkedList<Project> notPlannedLatelyIncrement;
    private HashMap<Project, Integer> notPlannedLatelyNumbers;

    /**
     * Constructor
     */
    public HighestPriorityStrategy() {
        this.allProjectsSorted = new LinkedList<>();
        this.availableProjects = getAvailableProjectsList(allProjectsSorted);
        this.usedProjects = new HashMap<>();
        this.notPlannedLatelyNumbers = new LinkedHashMap<>();
        this.notPlannedLatelyIncrement = new LinkedList<>();
        this.comparator = new Comparator<Project>() {
            @Override
            public int compare(Project p1, Project p2) {
                if (p1.getPriority() > p2.getPriority()) { // firstly, sort by priority in descending order
                    return -1;
                } else if (p1.getPriority() < p2.getPriority()) {
                    return 1;
                } else {
                    if (p1.getCores() > p2.getCores()) { //secondly, sort by cores in descending order
                        return -1;
                    } else if (p1.getCores() < p2.getCores()) {
                        return 1;
                    } else {
                        if (p1.getMemory() < p2.getMemory()) { // lastly, sort by memory in ascending order
                            return -1;
                        } else if (p1.getMemory() == p2.getMemory()) {
                            return 0;
                        } else {
                            return 1;
                        }
                    }
                }
            }
        };
    }

    @Override
    public void planForAll(ArrayList<ActiveClient> activeClients, Collection<Project> activeProjects) {
        allProjectsSorted = getAllProjectsSortedList(activeProjects);
        availableProjects = getAvailableProjectsList(allProjectsSorted);
        availableProjectsBackup = getAvailableProjectsList(allProjectsSorted);
        for (Project project : activeProjects) {
            if (!notPlannedLatelyNumbers.containsKey(project)) {
                notPlannedLatelyNumbers.put(project, 0);
            }
        }
        notPlannedLatelyIncrement();
        for (ActiveClient active : activeClients) {
            planForOne(active);
        }
    }

    @Override
    public void planForOne(ActiveClient active) {
        int coresLeft = active.getCoresLimit();
        int memoryLimit = active.getMemoryLimit();
        LinkedHashMap<ProjectUID, Integer> newerPlan = new LinkedHashMap<>();

        for (Project project : allProjectsSorted) {
            if (project.getMemory() > memoryLimit) {
                // rest of the projects in the list have higher memory requirement then client can handle
            } else if (project.getCores() > coresLeft) {
                // skip projects with higher cores requirements then client can hangdle
            } else {
                Pair key = new Pair(project.getPriority(), project.getCores());
                if (availableProjects.get(key).contains(project)) {
                    // project hasn't been chosen in this round
                    availableProjects.get(key).remove(project);
                    coresLeft = assignProjectForClient(coresLeft, newerPlan, project);
                } else {
                    // project is not in the available list, it has been used sooner in this round
                    if (availableProjects.get(key).isEmpty()) {
                        // start new round, all possible projects has been used
                        availableProjects.get(key).addAll(availableProjectsBackup.get(key));// fill again available list
                        availableProjects.get(key).remove(project);   // this project has been chosen as the first one in the new round
                        coresLeft = assignProjectForClient(coresLeft, newerPlan, project);
                    } else {
                        // project is not in the available list, but the list of possible projects is not empty
                        // different project from available list with same cores requirement and priority will be chosen if there is one
                        Project available = availableProjects.get(key).getFirst();
                        if (available.getMemory() <= project.getMemory()) {
                            // suitable project has been found
                            availableProjects.get(key).remove(available);
                            coresLeft = assignProjectForClient(coresLeft, newerPlan, available);
                        }else{
                            // if the first selected project from the list of available projects with same key has 
                            // higher memory requirements then client can handle, all projects later in the list will have higher
                            // requiremts as well because the list is sorted
                            // 
                            // therefore we chose the actual project in the main loop, which has lower memory requirements 
                            coresLeft = assignProjectForClient(coresLeft, newerPlan, project);
                        }
                    }
                }
                if (coresLeft == 0) {
                    // client plan is full, stopping loop
                    break;
                }
            }
        }
        active.setCurrentPlan(newerPlan);
    }

    @Override
    public HashMap<Project, Integer> getNotPlannedLatelyNumbers() {
        return notPlannedLatelyNumbers;
    }

    /*
     * Assign prject to the client's new plan
     */
    private void notPlannedLatelyIncrement() {
        for (Project project : notPlannedLatelyIncrement) {
            int newValue = notPlannedLatelyNumbers.get(project) + 1;
            notPlannedLatelyNumbers.put(project, newValue);
        }
    }

    /*
     * Assigns project to client's new plan
     */
    private int assignProjectForClient(int coresLeft, LinkedHashMap<ProjectUID, Integer> newerPlan, Project project) {
        int repeatProject = 0;
        notPlannedLatelyIncrement.remove(project);
        notPlannedLatelyNumbers.put(project, 0);
        // append project as many times as it can be added
        repeatProject = coresLeft / project.getCores();
        coresLeft = coresLeft % project.getCores();
        newerPlan.put(project.getProjectUID(), repeatProject);
        return coresLeft;
    }


    /*
     * Sorts active projects by memory, then cores and then by priorities and returns sorted list
     */
    private LinkedList<Project> getAllProjectsSortedList(Collection<Project> activeProjects) {
        LinkedList<Project> asList = new LinkedList<>(activeProjects);
        Collections.sort(asList, comparator);
        return asList;
    }

    /*
     * Create list which is used to distribute task evenly.
     * The list in the parameter has be sorted list by getSortedList method
     */
    private HashMap<Pair, LinkedList<Project>> getAvailableProjectsList(LinkedList<Project> activeProjects) {
        HashMap<Pair, LinkedList<Project>> distributionList = new HashMap<>();
        for (Project project : activeProjects) {
            Pair key = new Pair(project.getPriority(), project.getCores());
            if (!distributionList.containsKey(key)) {
                distributionList.put(key, new LinkedList<Project>());
            }
            distributionList.get(key).addFirst(project);
        }
        return distributionList;
    }
}
