/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server.strategies;

import cz.cuni.mff.bc.api.main.ProjectUID;
import cz.cuni.mff.bc.server.ActiveClient;
import cz.cuni.mff.bc.server.Project;
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
    private HashMap<Key, LinkedList<Project>> availableProjects;
    private HashMap<Key, LinkedList<Project>> usedProjects;
    private Comparator<Project> comparator;
    private LinkedList<Project> notPlannedLatelyIncrement;
    private HashMap<Project, Integer> notPlannedLatelyNumbers;

    public HighestPriorityStrategy() {
        this.allProjectsSorted = new LinkedList<>();
        this.availableProjects = getAvailableProjectsList(allProjectsSorted);
        this.usedProjects = new HashMap<>();
        this.notPlannedLatelyNumbers = new LinkedHashMap<>();
        this.notPlannedLatelyIncrement = new LinkedList<>();
        this.comparator = new Comparator<Project>() {
            @Override
            public int compare(Project p1, Project p2) {
                if (p1.getMemory() > p2.getMemory()) { // firstly,sort by memory in descending order
                    return -1;
                } else if (p1.getMemory() < p2.getMemory()) {
                    return 1;
                } else {
                    if (p1.getCores() > p2.getCores()) { //secondly sort by cores in descending order
                        return -1;
                    } else if (p1.getCores() < p2.getCores()) {
                        return 1;
                    } else {
                        if (p1.getPriority() > p2.getPriority()) { // lastly, sort by priorities in descening order
                            return -1;
                        } else if (p1.getPriority() == p2.getPriority()) {
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
        usedProjects = new HashMap<>();
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
            if (project.getMemory() > memoryLimit || project.getCores() > coresLeft) {
                // go fastly through start of the list where are the projects with memory limit and
                // core limit higher then actual client can handle
                continue;
            } else {
                Key key = new Key(project.getPriority(), project.getCores());
                if (availableProjects.get(key).contains(project)) {
                    // project hasn't been chosen in this round
                    if (!usedProjects.containsKey(key)) {
                        usedProjects.put(key, new LinkedList<Project>());
                    }
                    usedProjects.get(key).add(project);
                    availableProjects.get(key).remove(project);
                    coresLeft = assignProjectForClient(coresLeft, newerPlan, project);
                } else {
                    // project is not in the available list, it has been used sooner in this round
                    if (availableProjects.get(key).isEmpty()) {
                        // start new round, all possible projects has been used
                        availableProjects.get(key).addAll(usedProjects.get(key));// fill again available list
                        usedProjects.get(key).removeAll(usedProjects.get(key)); // clear used list
                        availableProjects.get(key).remove(project);   // this project has been chosen as the first one in the new round
                        coresLeft = assignProjectForClient(coresLeft, newerPlan, project);
                    } else {
                        // project is not in the available list, but list of possible projects is not empty
                        // different project from available list with same requirements will be chosen if there is one
                        boolean chosen = false;
                        for (Project nextAvailable : availableProjects.get(key)) {
                            if (nextAvailable.getMemory() <= project.getMemory()) {// suitable project has been found
                                usedProjects.get(key).add(nextAvailable);
                                availableProjects.get(key).remove(nextAvailable);
                                coresLeft = assignProjectForClient(coresLeft, newerPlan, nextAvailable);
                                chosen = true;
                            }
                        }
                        if (!chosen) {
                            // chose the actual project as there is no suitable project due to memory constrains
                            // but do not put project back into distribution as this is not the end of the round
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
        while (project.getCores() <= coresLeft) {
            repeatProject++;
            coresLeft = coresLeft - project.getCores();
        }
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
     * The list in the parameter should be sorted list by getSortedList method
     */
    private HashMap<Key, LinkedList<Project>> getAvailableProjectsList(LinkedList<Project> activeProjects) {
        HashMap<Key, LinkedList<Project>> distributionList = new HashMap<>();
        for (Project project : activeProjects) {
            Key key = new Key(project.getPriority(), project.getCores());
            if (!distributionList.containsKey(key)) {
                distributionList.put(key, new LinkedList<Project>());
            }
            distributionList.get(key).addFirst(project);
        }
        return distributionList;
    }
}
