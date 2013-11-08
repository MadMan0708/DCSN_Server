/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import cz.cuni.mff.bc.common.main.Logger;
import cz.cuni.mff.bc.common.enums.ELoggerMessages;
import cz.cuni.mff.bc.common.enums.ProjectState;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
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
    private Logger logger;
    //private ConcurrentHashMap<ProjectUID,Project> projectsReadyForDownload;

    @Override
    public Boolean call() throws Exception {
      return packProject();
    }

   

    public ProjectPacker(Project project,Logger logger){// ConcurrentHashMap<ProjectUID,Project> projectsReadyForDownload, Logger logger) {
       // this.projectsReadyForDownload = projectsReadyForDownload;
        this.project = project;
        this.clientID = project.getClientName();
        this.projectID = project.getProjectName();
        this.logger = logger;

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
            logger.log("Project " + projectID + " by client " + clientID + " packed");
            project.setState(ProjectState.READY_FOR_DOWNLOAD);
          // projectsReadyForDownload.add(project);
         //   projectsReadyForDownload.put(project., project)
            return Boolean.TRUE;
        } catch (IOException e) {
            logger.log("Packing project problem " + e.toString(), ELoggerMessages.ERROR);
            return Boolean.FALSE;
        }

    }
}
