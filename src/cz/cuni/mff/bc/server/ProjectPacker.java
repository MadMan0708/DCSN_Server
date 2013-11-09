/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import cz.cuni.mff.bc.api.enums.ProjectState;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author Aku
 */
public class ProjectPacker implements Callable<Boolean> {

    private Project project;
    private String clientID;
    private String projectID;
    private static final Logger LOG = Logger.getLogger(ProjectPacker.class.getName());

    @Override
    public Boolean call() throws Exception {
        return packProject();
    }

    public ProjectPacker(Project project, Handler logHandler) {
        this.project = project;
        this.clientID = project.getClientName();
        this.projectID = project.getProjectName();
        LOG.addHandler(logHandler);

    }

    private void addFileToZip(File sourceFile, ZipOutputStream zos) throws IOException {

        ZipEntry ze = new ZipEntry(sourceFile.getName());
        zos.putNextEntry(ze);

        FileInputStream in = new FileInputStream(sourceFile);
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) > 0) {
            zos.write(buffer, 0, len);
        }

        in.close();

    }
    // pack project into zip file

    private Boolean packProject() {
        File completeDir = new File(TaskManager.getDirInProject(projectID, clientID, "complete"));
        File[] files = completeDir.listFiles();
        File cl = new File(TaskManager.getProjectDir(projectID, clientID) + project.getClassName() + ".class");
        File output = new File(TaskManager.getProjectDir(projectID, clientID) + clientID + "_" + projectID + "_completed" + ".zip");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(output))) {
            addFileToZip(cl, zos);
            for (File file : files) {
                addFileToZip(file, zos);
            }
            zos.closeEntry();
            zos.close();
            LOG.log(Level.INFO, "Project {0} by client {1} packed", new Object[]{projectID, clientID});
            project.setState(ProjectState.READY_FOR_DOWNLOAD);
            // projectsReadyForDownload.add(project);
            //   projectsReadyForDownload.put(project., project)
            return Boolean.TRUE;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Packing project problem {0}", e.getMessage());
            return Boolean.FALSE;
        }

    }
}
