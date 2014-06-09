/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server.strategies;

import cz.cuni.mff.bc.api.main.ProjectUID;
import cz.cuni.mff.bc.computation.ActiveClient;
import cz.cuni.mff.bc.computation.Project;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

/**
 * Implementation of Maximal Throughput strategy
 *
 * @author Jakub Hava
 */
public class MaxThroughputStrategy implements IStrategy {

    private int notPlannedLimit;
    private LinkedList<Project> activeProjects;
    private final Comparator<Project> comparator;
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
        this.activeProjects = new LinkedList<>();
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
        this.activeProjects = new LinkedList<>(activeProjects);
        for (Project project : this.activeProjects) {
            if (!notPlannedLatelyNumbers.containsKey(project)) {
                notPlannedLatelyNumbers.put(project, 0);
            }
        }
        notPlannedLatelyIncrement();
        for (ActiveClient active : activeClients) {
            planForOne(active);
        }
        notPlannedLatelyIncrement = new LinkedList<>(this.activeProjects);
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
        LinkedList<ProjectInAssociation> distributionList = findBestAssociation(coresLeft, memoryLimit, activeProjects);
        for (ProjectInAssociation projectInAssociation : distributionList) {
            for (int i = projectInAssociation.getCount(); i > 0; i--) {
                assignProjectForClient(newPlan, projectInAssociation.getProject(), projectInAssociation.getCount());
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
    private LinkedList<ProjectInAssociation> findBestAssociation(int coresLimit, int memoryLimit, LinkedList<Project> projects) {
        HashMap<Integer, LinkedList<Project>> filtered = filterProjects(coresLimit, memoryLimit, projects);
        LinkedList<ProjectInAssociation> toReturn = new LinkedList<>();

        // initialize
        int maxWeight = coresLimit; // KnapSack capacity
        Integer[] w = filtered.keySet().toArray(new Integer[filtered.size()]); // weight of each item
        Integer[] v = Arrays.copyOf(w, w.length); // weight==value, value of each item
        int[] m = new int[maxWeight + 1];        //maximum value each knapsack
        int[] l = new int[maxWeight + 1];        //last item added into each knapsack
        int n = w.length; // number if items
        int[] selectedItems = new int[n];

        // prepare arrays
        for (int j = 0; j <= maxWeight; j++) {
            m[j] = 0;
            l[j] = -1;
        }
        // solve the unbounded knapsack problem
        for (int i = 1; i < m.length; i++) {
            for (int j = 0; j < n; j++) {
                if (w[j] <= i
                        && (v[j] + m[i - w[j]]) > m[i]) {
                    m[i] = v[j] + m[i - w[j]];
                    l[i] = j;
                }
            }
        }
        // find the chosen items (find the cores to use)
        int currentBag = l.length - 1;
        int lastItem = l[currentBag];

        while (lastItem != -1 && currentBag > 0) {
            selectedItems[lastItem]++;
            currentBag = currentBag - w[lastItem];
            lastItem = l[currentBag];
        }
        // return a list containg information about how many times can be a project planned on the client
        for (int i = 0; i < selectedItems.length; i++) {
            Project first = filtered.get(v[i]).getFirst();
            toReturn.add(new ProjectInAssociation(selectedItems[i], first));
        }
        return toReturn;
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
