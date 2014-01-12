/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import cz.cuni.mff.bc.api.main.TaskID;
import java.io.File;
import java.nio.file.Path;

/**
 *
 * @author Jakub
 */
public class FilesStructure {

    private static String jarFilesName = "project";
    private static String dataFilesName = "data";

    public static String getJarFilesName() {
        return jarFilesName;
    }

    public static void setJarFilesName(String jarFilesNameNew) {
        jarFilesName = jarFilesNameNew;
    }

    public static String getDataFilesName() {
        return dataFilesName;
    }

    public static void setDataFilesName(String dataFilesNameNew) {
        dataFilesName = dataFilesNameNew;
    }

    public static File getProjectsDir() {
        return new File(Server.getProjectsDir());
    }

    public static File getUploadedDir() {
        return new File(Server.getUploadedDir());
    }

    public static File getClientDir(String clientName) {
        return new File(Server.getProjectsDir(), clientName);
    }

    public static File getClientProjectsDir(String clientName, String projectName) {
        return new File(getClientDir(clientName), projectName);
    }

    public static File getCompleteDirInProject(String clientName, String projectName) {
        return new File(getClientProjectsDir(clientName, projectName), "complete");
    }

    public static File getReadyDirInProject(String clientName, String projectName) {
        return new File(getClientProjectsDir(clientName, projectName), "ready");
    }

    public static File getTempDirInProject(String clientName, String projectName) {
        return new File(getClientProjectsDir(clientName, projectName), "temp");
    }

    public static File getClientUploadedDir(String clientName, String projectName) {
        return new File(getUploadedDir() + File.separator + clientName + "_" + projectName + File.separator);
    }

    public static File getProjectJarFile(String clientName, String projectName) {
        return new File(getClientProjectsDir(clientName, projectName), dataFilesName + ".jar");
    }

    public static File getCalculatedDataFile(String clientName, String projectName) {
        return new File(getClientProjectsDir(clientName, projectName), dataFilesName + "_completed.zip");
    }

    public static Path getTaskSavePath(TaskID id) {
        return new File(getCompleteDirInProject(id.getClientName(), id.getProjectName()), id.getTaskName()).toPath();
    }

    public static Path getTaskLoadPath(TaskID id) {
        return new File(getReadyDirInProject(id.getClientName(), id.getProjectName()), id.getTaskName()).toPath();
    }

    public static Path getTaskLoadDataPath(TaskID id) {
        return new File(getTempDirInProject(id.getClientName(), id.getProjectName()), id.getTaskName()).toPath();
    }

    public static void createClientProjectDirs(String clientName, String projectName) {
        //creating user folder
        File clientDir = getClientDir(clientName);
        if (!clientDir.exists()) {
            clientDir.mkdir();
        }
        //creating project folder inside client folder
        getClientProjectsDir(clientName, projectName).mkdir();

        // creating calculation folders - temp, complete and ready
        getTempDirInProject(clientName, projectName).mkdir();
        getCompleteDirInProject(clientName, projectName).mkdir();
        getReadyDirInProject(clientName, projectName).mkdir();
    }
}
