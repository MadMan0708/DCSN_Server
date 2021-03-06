/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.computation;

import cz.cuni.mff.bc.api.enums.ProjectState;
import cz.cuni.mff.bc.api.main.CustomIO;
import cz.cuni.mff.bc.server.Server;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Packs the project data files
 *
 * @author Jakub Hava
 */
public class ProjectPacker implements Callable<Boolean> {

    private final Project project;
    private final File sourceDirectory;
    private final File destination;
    private static final Logger LOG = Logger.getLogger(Server.class.getName());

    @Override
    public Boolean call() throws Exception {
        return packProject();
    }

    /**
     * Constructor
     *
     * @param project project whose data files will be packed
     * @param sourceDirectory directory from which all the files will be packed
     * @param destination destination file to which data files will be packed
     */
    public ProjectPacker(Project project, File sourceDirectory, File destination) {
        this.project = project;
        this.sourceDirectory = sourceDirectory;
        this.destination = destination;
    }

    /* 
     * Packs the project 
     */
    private Boolean packProject() {
        File[] files = sourceDirectory.listFiles();
        try {
            CustomIO.zipFiles(destination, files);
            LOG.log(Level.FINE, "Project {0} by client {1} packed", new Object[]{project.getProjectName(), project.getClientName()});
            project.setState(ProjectState.READY_FOR_DOWNLOAD);
            return Boolean.TRUE;
        } catch (IOException e) {
            LOG.log(Level.FINE, "Packing project problem {0}", e.getMessage());
            return Boolean.FALSE;
        }
    }
}
