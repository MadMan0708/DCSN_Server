/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server.strategies;

import cz.cuni.mff.bc.server.ActiveClient;
import cz.cuni.mff.bc.server.Project;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Common interface for strategies
 *
 * @author Jakub Hava
 */
public interface IStrategy {

    /**
     * Creates the plan for all the clients
     *
     * @param activeClients active clients
     * @param activeProjects active projects
     */
    public void planForAll(ArrayList<ActiveClient> activeClients, Collection<Project> activeProjects);

    /**
     * Plans for the one client. Planning for one client is done only when one
     * client is connected. We can use actual list of used projects and
     * available projects to plan for client in the way that project will be
     * distributed evenly according to last planning for all the clients
     *
     * @param active active project
     */
    public void planForOne(ActiveClient active);

    /**
     * Gets list of projects with numbers how many times they haven't been
     * planned
     *
     * @return list of projects with numbers how many times they haven't been
     * planned
     */
    public HashMap<Project, Integer> getNotPlannedLatelyNumbers();
}
