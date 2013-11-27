/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import cz.cuni.mff.bc.api.enums.ProjectState;
import cz.cuni.mff.bc.api.main.CustomIO;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Aku
 */
public class ProjectPacker implements Callable<Boolean> {

    private Project project;
    private String clientID;
    private String projectID;
    private static final Logger LOG = Logger.getLogger(Server.class.getName());

    @Override
    public Boolean call() throws Exception {
        return packProject();
    }

    public ProjectPacker(Project project) {
        this.project = project;
        this.clientID = project.getClientName();
        this.projectID = project.getProjectName();
    }

    private Boolean packProject() {
        File completeDir = FilesStructure.getCompleteDirInProject(clientID, projectID);
        File[] files = completeDir.listFiles();
        File output = FilesStructure.getCalculatedDataFile(clientID, projectID);
        try {
            CustomIO.zipFiles(output, files);
            LOG.log(Level.INFO, "Project {0} by client {1} packed", new Object[]{projectID, clientID});
            project.setState(ProjectState.READY_FOR_DOWNLOAD);
            // projectsReadyForDownload.add(project);
            // projectsReadyForDownload.put(project., project)
            return Boolean.TRUE;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Packing project problem {0}", e.getMessage());
            return Boolean.FALSE;
        }

    }
}
