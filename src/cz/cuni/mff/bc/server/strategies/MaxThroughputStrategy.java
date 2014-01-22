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
import java.util.Map;

/**
 * Implementation of Maximal Throughput strategy
 *
 * @author Jakub Hava
 */
public class MaxThroughputStrategy implements IStrategy {

    private int replannedLimit;
    private LinkedList<Project> allProjectsSorted;
    private HashMap<Key, LinkedList<Project>> availableProjects;
    private HashMap<Key, LinkedList<Project>> usedProjects;
    private Comparator<Project> comparator;
    private LinkedList<Project> notPlannedLately;
    private LinkedList<Project> notPlannedLatelyIncrement;
    private HashMap<Project, Integer> notPlannedLatelyNumbers;

    public MaxThroughputStrategy(int replannedLimit) {
        this.replannedLimit = replannedLimit;
        this.notPlannedLatelyNumbers = new LinkedHashMap<>();
        this.notPlannedLatelyIncrement = new LinkedList<>();
        this.notPlannedLately = new LinkedList<>();
        this.allProjectsSorted = new LinkedList<>();
        this.availableProjects = getAvailableProjectsList(allProjectsSorted);
        this.usedProjects = new HashMap<>();
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
                        if (p1.getPriority() > p2.getPriority()) { // thirdly  sort  by priority in descending order
                            return -1;
                        } else if (p1.getPriority() == p2.getPriority()) {
                            return 0;
                        } else {
                            return 1;
                        }
                    }
                }
            }
        }; /*      this.comparator = new Comparator<Project>() {
         @Override
         public int compare(Project p1, Project p2) {
         if (p1.getTime() > p2.getTime()) { // firstly sort by time in ascending
         return -1;
         } else if (p1.getTime() < p2.getTime()) {
         return 1;
         } else {
         if (p1.getCores() > p2.getCores()) { //secondly sort by cores in ascending order
         return -1;
         } else if (p1.getCores() < p2.getCores()) {
         return 1;
         } else {
         if (p1.getMemory() > p2.getMemory()) { // lastly sort by memory in descending order
         return 1;
         } else if (p1.getMemory() == p2.getMemory()) {
         return 0;
         } else {
         return -1;
         }
         }
         }
         }
         };*/
    } /* Values are same as weights (stored in array items)
     / Weights (stored in array items)
     / Number of distinct items (items.length)
     / Knapsack capacity (capacity)
     * 
     * Returns the list where key is how many times can projects from list in hashmap value be used
     */


    public HashMap<Key, ArrayList<Project>> solveKnapsack(ActiveClient active, LinkedList<Project> projects) {
        int capacity = active.getCoresLimit();
        HashMap<Integer, ArrayList<Project>> filtered = filterProjects(active, projects);
        HashMap<Key, ArrayList<Project>> toReturn = new HashMap<>();
        Integer[] items = prepareArrayOfWeights(active, filtered);
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
            toReturn.put(new Key(entry.getKey(), entry.getValue()), filtered.get(entry.getKey()));
        }
        return toReturn;
    }

    private Integer[] prepareArrayOfWeights(ActiveClient activeClient, HashMap<Integer, ArrayList<Project>> filtredProejcts) {
        ArrayList<Integer> weights = new ArrayList<>();
        int coresLimit = activeClient.getCoresLimit();
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

    private HashMap<Integer, ArrayList<Project>> filterProjects(ActiveClient active, LinkedList<Project> projects) {
        HashMap<Integer, ArrayList<Project>> list = new HashMap<>();
        for (Project project : projects) {
            if (project.getMemory() <= active.getMemoryLimit() && project.getCores() <= active.getCoresLimit()) {
                if (!list.containsKey(project.getCores())) {
                    list.put(project.getCores(), new ArrayList<Project>());
                }
                list.get(project.getCores()).add(project);
            }
        }
        for (ArrayList<Project> l : list.values()) {
            Collections.sort(l, comparator);
        }
        return list;
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
        notPlannedLatelyIncrement = new LinkedList<>(allProjectsSorted);
    }

    @Override
    public HashMap<Project, Integer> getNotPlannedLatelyNumbers() {
        return notPlannedLatelyNumbers;
    }

    private void notPlannedLatelyIncrement() {
        for (Project project : notPlannedLatelyIncrement) {
            int newValue = notPlannedLatelyNumbers.get(project) + 1;
            if (newValue == replannedLimit) {
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
        LinkedHashMap<ProjectUID, Integer> newerPlan = new LinkedHashMap<>();
        if (!notPlannedLately.isEmpty()) {
            ArrayList<Project> chosenProjects = new ArrayList<>();
            for (Project project : notPlannedLately) {
                if (project.getMemory() <= active.getMemoryLimit() && coresLeft - project.getCores() >= 0) {
                    coresLeft = assignProjectForClient(coresLeft, newerPlan, project);
                }
                if (coresLeft == 0) {
                    // client plan is full, stopping the loop
                    break;
                }
            }
            notPlannedLately.removeAll(chosenProjects);
            if (coresLeft == 0) {
                active.setCurrentPlan(newerPlan);
                // stop the function, no reason to continue
                return;
            }
        }
        // continue with regular planning
        for (Project project : allProjectsSorted) {
            if (project.getMemory() <= active.getMemoryLimit() && coresLeft - project.getCores() >= 0) { // if project cannot be used because it has big memory or core demands
                Key key = new Key(project.getTime(), project.getCores());
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
        Collections.sort(asList, Collections.reverseOrder(comparator));
        return asList;
    }

    /*
     * Create list which is used to distribute task evenly.
     * The list in the parameter should be sorted list by getSortedList method
     */
    private HashMap<Key, LinkedList<Project>> getAvailableProjectsList(LinkedList<Project> activeProjects) {
        HashMap<Key, LinkedList<Project>> distributionList = new HashMap<>();
        for (Project project : activeProjects) {
            Key key = new Key(project.getTime(), project.getCores());
            if (!distributionList.containsKey(key)) {
                distributionList.put(key, new LinkedList<Project>());
            }
            distributionList.get(key).addFirst(project);
        }
        return distributionList;
    }
}
