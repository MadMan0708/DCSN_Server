/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import cz.cuni.mff.bc.api.main.TaskID;
import java.io.File;
import java.nio.file.Path;

/**
 * Provides methods to access files on the server
 *
 * @author Jakub Hava
 */
public class FilesStructure {

    private static String jarFilesName = "project";
    private static String dataFilesName = "data";
    private ServerParams serverParams;

    /**
     * Constructor
     *
     * @param serverParams server parameters
     */
    public FilesStructure(ServerParams serverParams) {
        this.serverParams = serverParams;
    }

    /**
     *
     * @return general project jar file names
     */
    public static String getJarFilesName() {
        return jarFilesName;
    }

    /**
     *
     * @param jarFilesNameNew general project jar file names
     */
    public static void setJarFilesName(String jarFilesNameNew) {
        jarFilesName = jarFilesNameNew;
    }

    /**
     *
     * @return general data file names
     */
    public static String getDataFilesName() {
        return dataFilesName;
    }

    /**
     *
     * @param dataFilesNameNew general data file names
     */
    public static void setDataFilesName(String dataFilesNameNew) {
        dataFilesName = dataFilesNameNew;
    }

    /**
     * Gets projects directory
     *
     * @return projects directory
     */
    public File getProjectsDir() {
        return new File(serverParams.getBaseDir() + File.separator + "Projects");
    }

    /**
     * Gets uploaded directory
     *
     * @return uploaded directory
     */
    public File getUploadedDir() {
        return new File(serverParams.getBaseDir() + File.separator + "Uploaded");
    }

    /**
     *
     * @param clientName client's name
     * @return client's directory
     */
    public File getClientDir(String clientName) {
        return new File(getProjectsDir(), clientName);
    }

    /**
     *
     * @param clientName client's name
     * @param projectName project name
     * @return project directory inside client's directory
     */
    public File getClientProjectsDir(String clientName, String projectName) {
        return new File(getClientDir(clientName), projectName);
    }

    /**
     *
     * @param clientName client's name
     * @param projectName project name
     * @return complete directory in the project directory inside client's
     * directory
     */
    public File getCompleteDirInProject(String clientName, String projectName) {
        return new File(getClientProjectsDir(clientName, projectName), "complete");
    }

    /**
     *
     * @param clientName client's name
     * @param projectName project name
     * @return ready directory in the project directory inside client's
     * directory
     */
    public File getReadyDirInProject(String clientName, String projectName) {
        return new File(getClientProjectsDir(clientName, projectName), "ready");
    }

    /**
     *
     * @param clientName client's name
     * @param projectName project name
     * @return temp task directory in the project directory inside client's
     * directory
     */
    public File getTempDirInProject(String clientName, String projectName) {
        return new File(getClientProjectsDir(clientName, projectName), "temp");
    }

    /**
     *
     * @param clientName client's name
     * @param projectName project name
     * @return client's directory in uploaded directory
     */
    public File getClientUploadedDir(String clientName, String projectName) {
        return new File(getUploadedDir() + File.separator + clientName + "_" + projectName + File.separator);
    }

    /**
     *
     * @param clientName client's name
     * @param projectName project name
     * @return project jar file
     */
    public File getProjectJarFile(String clientName, String projectName) {
        return new File(getClientProjectsDir(clientName, projectName), dataFilesName + ".jar");
    }

    /**
     *
     * @param clientName client's name
     * @param projectName project name
     * @return archive with packed data after the calculation finished
     */
    public File getCalculatedDataFile(String clientName, String projectName) {
        return new File(getClientProjectsDir(clientName, projectName), dataFilesName + "_completed.zip");
    }

    /**
     * Builds the path to the task in complete directory from unique task id
     *
     * @param id unique task id
     * @return path to the task in complete directory
     */
    public Path getTaskSavePath(TaskID id) {
        return new File(getCompleteDirInProject(id.getClientName(), id.getProjectName()), id.getTaskName()).toPath();
    }

    /**
     * Builds the path to the task in ready directory from unique task id
     *
     * @param id unique task id
     * @return path to the task in ready directory
     */
    public Path getTaskLoadPath(TaskID id) {
        return new File(getReadyDirInProject(id.getClientName(), id.getProjectName()), id.getTaskName()).toPath();
    }

    /**
     * Builds the path to the task in temp directory from unique task id
     *
     * @param id unique task id
     * @return path to the task in temp directory
     */
    public Path getTaskLoadDataPath(TaskID id) {
        return new File(getTempDirInProject(id.getClientName(), id.getProjectName()), id.getTaskName()).toPath();
    }

    /**
     * Creates client's directory, project directory and ready, temp and
     * complete directories in the project directory
     *
     * @param clientName client's name
     * @param projectName project name
     */
    public void createClientProjectDirs(String clientName, String projectName) {
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
