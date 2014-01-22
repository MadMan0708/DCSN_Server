/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server.strategies;

import cz.cuni.mff.bc.api.enums.ProjectState;
import cz.cuni.mff.bc.server.ActiveClient;
import cz.cuni.mff.bc.server.Project;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

/**
 *
 * @author UP711643
 */
public class TestKnapsack {

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        MaxThroughputStrategy m = new MaxThroughputStrategy(5);
        ActiveClient active = new ActiveClient("Jakub", null, null);
        active.setCoresLimit(13);
        active.setMemoryLimit(50);

        LinkedList<Project> list = new LinkedList<>();

        list.add(new Project(ProjectState.ACTIVE, 7, 2, 20, 15, null, "Weather"));
        list.add(new Project(ProjectState.ACTIVE, 7, 2, 39, 15, null, "UFO"));
        list.add(new Project(ProjectState.ACTIVE, 9, 2, 39, 10, null, "TestOne"));
        list.add(new Project(ProjectState.ACTIVE, 7, 2, 39, 10, null, "Same"));
        HashMap<Key, LinkedList<Project>> solveKnapsack = m.findBestAssigning(active.getCoresLimit(),active.getMemoryLimit(), list);
        for (Entry<Key, LinkedList<Project>> entry : solveKnapsack.entrySet()) {
            for (Project project : entry.getValue()) {
                System.out.println(entry.getKey().getFirst() + "x Cores : " + entry.getKey().getSecond() + " : " + project.getProjectName());
            }
        }
    }
}
