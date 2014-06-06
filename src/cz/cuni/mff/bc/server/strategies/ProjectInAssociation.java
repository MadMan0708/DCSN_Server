/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.cuni.mff.bc.server.strategies;

import cz.cuni.mff.bc.computation.Project;

/**
 * Contains projects along with a integer saying how many times it can be planned on the client
 * @author Jakub Hava
 */
public class ProjectInAssociation {
    private final int count;
    private final Project project;

    public ProjectInAssociation(int count, Project project) {
        this.count = count;
        this.project = project;
    }

    public int getCount() {
        return count;
    }

    public Project getProject() {
        return project;
    }
    
    
}
