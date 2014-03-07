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
import java.util.Map;
import java.util.Map.Entry;

/**
 * Implementation of Maximal Throughput strategy
 *
 * @author Jakub Hava
 */
public class MaxThroughputStrategy implements IStrategy {

    private int notPlannedLimit;
    private LinkedList<Project> allProjectsSorted;
    private Comparator<Project> comparator;
    private LinkedList<Project> notPlannedLately;
    private LinkedList<Project> notPlannedLatelyIncrement;
    private HashMap<Project, Integer> notPlannedLatelyNumbers;

    /**
     * Constructor
     *
     * @param notPlannedLimit number of planning after projects which haven't
     * been planned yet will be planned with higher priority
     */
    public MaxThroughputStrategy(int notPlannedLimit) {
        this.notPlannedLimit = notPlannedLimit;
        this.notPlannedLatelyNumbers = new LinkedHashMap<>();
        this.notPlannedLatelyIncrement = new LinkedList<>();
        this.notPlannedLately = new LinkedList<>();
        this.allProjectsSorted = new LinkedList<>();
        this.comparator = new Comparator<Project>() {
            @Override
            public int compare(Project p1, Project p2) {
                if (p1.getMemory() > p2.getMemory()) {
                    return -1;
                } else if (p1.getMemory() < p2.getMemory()) { // firstly sort by memory descending
                    return 1;
                } else {
                    if (p1.getTime() > p2.getTime()) { // secondly sort by time in ascending order
                        return 1;
                    } else if (p1.getTime() < p2.getTime()) {
                        return -1;
                    } else {
                        if (p1.getPriority() > p2.getPriority()) { // thirdly sort by priority in descending order
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
        allProjectsSorted = new LinkedList<>(activeProjects);
        Collections.sort(allProjectsSorted, comparator);
        for (Project project : activeProjects) {
            if (!notPlannedLatelyNumbers.containsKey(project)) {
                notPlannedLatelyNumbers.put(project, 0);
            }
        }
        notPlannedLatelyIncrement();
        for (ActiveClient active : activeClients) {
            planForOne(active);
        }
        notPlannedLatelyIncrement = new LinkedList<>(allProjectsSorted);
    }

    /*
     * Increments number of projects which haven't been planned
     */
    private void notPlannedLatelyIncrement() {
        for (Project project : notPlannedLatelyIncrement) {
            int newValue = notPlannedLatelyNumbers.get(project) + 1;
            if (newValue == notPlannedLimit) {
                if (!notPlannedLately.contains(project)) {
                    notPlannedLately.addLast(project);
                }
                notPlannedLatelyNumbers.put(project, 0);
            }
            notPlannedLatelyNumbers.put(project, newValue);
        }
    }

    @Override
    public void planForOne(ActiveClient active) {
        int coresLeft = active.getCoresLimit();
        int memoryLimit = active.getMemoryLimit();
        LinkedHashMap<ProjectUID, Integer> newPlan = new LinkedHashMap<>();
        ArrayList<Project> chosenProjects = new ArrayList<>();
        if (!notPlannedLately.isEmpty()) {
            for (Project project : notPlannedLately) {
                if (project.getMemory() <= memoryLimit && project.getCores() <= coresLeft) {
                    int repeatProject = coresLeft / project.getCores();
                    coresLeft = coresLeft % project.getCores();
                    assignProjectForClient(newPlan, project, repeatProject);
                    chosenProjects.add(project);
                    if (coresLeft == 0) {
                        // client plan is full, stopping the loop
                        break;
                    }
                }
            }
            notPlannedLately.removeAll(chosenProjects);
            if (coresLeft == 0) {
                active.setCurrentPlan(newPlan);
                // stop the function, no reason to continue
                return;
            }
        }
        // continue with regular planning
        LinkedList<Project> forDistribution = new LinkedList<>(allProjectsSorted);
        forDistribution.removeAll(chosenProjects); // remove projects which has been added cause of immediate planning
        HashMap<Pair, LinkedList<Project>> distributionList = findBestAssociation(coresLeft, memoryLimit, forDistribution);
        for (Entry<Pair, LinkedList<Project>> entry : distributionList.entrySet()) {
            for (int i = entry.getKey().getSecond(); i > 0; i--) {
                Project project = entry.getValue().getFirst();
                assignProjectForClient(newPlan, project, entry.getKey().getSecond());
            }
        }
        active.setCurrentPlan(newPlan);
    }

    @Override
    public HashMap<Project, Integer> getNotPlannedLatelyNumbers() {
        return notPlannedLatelyNumbers;
    }

    /*
     * Returns the list where the keys tell how many times can projects from list in hashmap value be used
     */
    private HashMap<Pair, LinkedList<Project>> findBestAssociation(int coresLimit, int memoryLimit, LinkedList<Project> projects) {
        int capacity = coresLimit;
        HashMap<Integer, LinkedList<Project>> filtered = filterProjects(coresLimit, memoryLimit, projects);
        HashMap<Pair, LinkedList<Project>> toReturn = new HashMap<>();
        Integer[] items = prepareArrayOfWeights(coresLimit, filtered);
        Integer[][] result = new Integer[items.length + 1][capacity + 1];
        Integer[][] picked = new Integer[items.length + 1][capacity + 1];
        HashMap<Integer, Integer> selectedItems = new HashMap<>();
        // initialize
        for (int j = 0; j <= capacity; j++) {
            result[0][j] = 0;
            picked[0][j] = 0;
        }
        // solve
        for (int i = 1; i <= items.length; i++) {
            for (int j = 0; j <= capacity; j++) {
                if (items[i - 1] <= j) {
                    int withNewItem = result[i - 1][j - items[i - 1]] + items[i - 1];
                    int previousItem = result[i - 1][j];
                    if (withNewItem < previousItem) {
                        result[i][j] = previousItem;
                        picked[i][j] = 0;
                    } else {
                        result[i][j] = withNewItem;
                        picked[i][j] = 1;
                    }
                } else {
                    result[i][j] = result[i - 1][j];
                    picked[i][j] = 0;
                }
            }
        }
        // find the cores to use
        int c = capacity;
        for (int i = items.length; i > 0; i--) {
            if (picked[i][c] == 1) {
                if (!selectedItems.containsKey(items[i - 1])) {
                    selectedItems.put(items[i - 1], 1);
                } else {
                    selectedItems.put(items[i - 1], selectedItems.get(items[i - 1]) + 1);
                }
                c = c - items[i - 1];
            }
        }
        // now we have how many times project with specific cores limit can be in most effective list
        for (Map.Entry<Integer, Integer> entry : selectedItems.entrySet()) {
            toReturn.put(new Pair(entry.getKey(), entry.getValue()), filtered.get(entry.getKey()));
        }
        return toReturn;
    }

    /*
     * Prepares array of core limits
     * Supportive function for findBestAssociation
     */
    private Integer[] prepareArrayOfWeights(int coresLimit, HashMap<Integer, LinkedList<Project>> filtredProejcts) {
        ArrayList<Integer> weights = new ArrayList<>();
        int coresTemp;
        for (Integer cores : filtredProejcts.keySet()) {
            coresTemp = coresLimit - cores;
            while (coresTemp >= 0) {
                weights.add(cores);
                coresTemp = coresTemp - cores;
            }
        }
        return (Integer[]) weights.toArray(new Integer[weights.size()]);
    }

    /*
     * Filters project for method findBestAssociation, removes project with memory limit and core limit higher than client can handle
     */
    private HashMap<Integer, LinkedList<Project>> filterProjects(int coresLimit, int memoryLimit, LinkedList<Project> projects) {
        HashMap<Integer, LinkedList<Project>> list = new HashMap<>();
        for (Project project : projects) {
            if (project.getMemory() <= memoryLimit && project.getCores() <= coresLimit) {
                if (!list.containsKey(project.getCores())) {
                    list.put(project.getCores(), new LinkedList<Project>());
                }
                list.get(project.getCores()).add(project);
            }
        }
        for (LinkedList<Project> l : list.values()) {
            Collections.sort(l, comparator);
        }
        return list;
    }

    /*
     * Assign prject to the client's new plan
     */
    private void assignProjectForClient(LinkedHashMap<ProjectUID, Integer> newPlan, Project project, int repeat) {
        notPlannedLatelyIncrement.remove(project);
        notPlannedLatelyNumbers.put(project, 0);
        newPlan.put(project.getProjectUID(), repeat);
    }
}
