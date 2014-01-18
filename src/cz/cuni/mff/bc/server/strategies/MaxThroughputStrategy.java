/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server.strategies;

import cz.cuni.mff.bc.server.ActiveClient;
import cz.cuni.mff.bc.server.Project;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Implementation of Maximal Throughput strategy
 *
 * @author Jakub Hava
 */
public class MaxThroughputStrategy implements IStrategy {

    private int replanned = 0;

    @Override
    public void planForAll(ArrayList<ActiveClient> activeClients, Collection<Project> activeProjects) {
    }

    @Override
    public void planForOne(ActiveClient active) {
    }

    /**
     * Gets number of how many times replanning has been done
     *
     * @return number of how many times replanning has been done
     */
    public int getNumberOfReplanning() {
        return replanned;
    }

    /**
     * Resets the number of how many times replanning has been done
     */
    public void resetNumberOfReplanning() {
        replanned = 0;
    }
}
