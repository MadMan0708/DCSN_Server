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
import java.util.Iterator;
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
    private LinkedList<Pair> listForIteration;
    private final Comparator<Project> comparator;
    private LinkedList<Project> notPlannedLatelyIncrement;
    private HashMap<Project, Integer> notPlannedLatelyNumbers;

    /**
     * Constructor
     */
    public HighestPriorityStrategy() {
        this.listForIteration = new LinkedList<>();
        this.allProjectsSorted = new LinkedList<>();
        this.availableProjects = getAvailableProjectsList(allProjectsSorted);
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
                        return 0;
                    }
                }
            }
        };
    }

    @Override
    public void planForAll(ArrayList<ActiveClient> activeClients, Collection<Project> activeProjects) {
        allProjectsSorted = getAllProjectsSortedList(activeProjects);
        availableProjects = getAvailableProjectsList(allProjectsSorted);
        listForIteration = new LinkedList<>(availableProjects.keySet());
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

    private Project getAndAppendAsLast(Pair key, int memoryLimit) {
        Project selected = null;
        for (Iterator<Project> it = availableProjects.get(key).listIterator(); it.hasNext();) {
            if (it.next().getMemory() <= memoryLimit) {
                selected = it.next();
                break;
            }
        }
        if (selected != null) {
            availableProjects.get(key).remove(selected);
            availableProjects.get(key).addLast(selected);
        }
        return selected;
    }

    @Override
    public void planForOne(ActiveClient active) {
        int coresLeft = active.getCoresLimit();
        int memoryLimit = active.getMemoryLimit();
        LinkedHashMap<ProjectUID, Integer> newerPlan = new LinkedHashMap<>();
        for (Pair key : listForIteration) {
            if (key.getSecond() > coresLeft) {
                // skip projects with higher cores requirements than client can handle
            } else {
                // find the project from the list of the projects with the same keys
                //, plan it on the client and move it from its current position in the list in the hash map 
                // to the end to ensure even project distribution
                Project selected = getAndAppendAsLast(key, memoryLimit);
                if (selected != null) {
                    coresLeft = assignProjectForClient(coresLeft, newerPlan, selected);
                }
                if (coresLeft == 0) {
                    // the client's plan is full, stopping loop
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
            distributionList.get(key).addLast(project);
        }
        return distributionList;
    }
}
