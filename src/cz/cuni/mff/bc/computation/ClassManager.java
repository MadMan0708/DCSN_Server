/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.computation;

import cz.cuni.mff.bc.misc.CustomClassLoader;
import cz.cuni.mff.bc.api.main.JarAPI;
import cz.cuni.mff.bc.api.main.ProjectUID;
import cz.cuni.mff.bc.server.FilesStructure;
import cz.cuni.mff.bc.server.Server;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class manager
 *
 * @author Jakub Hava
 */
public class ClassManager {

    private CustomClassLoader classLoader = new CustomClassLoader();
    private FilesStructure filesStructure;
    private static final Logger LOG = Logger.getLogger(Server.class.getName());

    /**
     * Constructor
     *
     * @param filesStructure files structure
     */
    public ClassManager(FilesStructure filesStructure) {
        this.filesStructure = filesStructure;
    }

    /**
     * Loads class
     *
     * @param uid project unique id
     * @return loaded class
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public synchronized Class<?> loadClass(ProjectUID uid) throws ClassNotFoundException, IOException {
        File jar = filesStructure.getProjectJarFile(uid.getClientName(), uid.getProjectName());
        String name = JarAPI.getAttributeFromManifest(jar.toPath(), "Main-Comp-Class");
        return classLoader.loadClass(name);
    }

    /**
     * Gets class loader
     *
     * @return custom class loader
     */
    public synchronized CustomClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Deletes project URL from classloader
     *
     * @param projectUID project unique ID
     */
    public synchronized void deleteProjectFromCL(ProjectUID projectUID) {
        try {
            URL projectURL = filesStructure.getProjectJarFile(projectUID.getClientName(), projectUID.getProjectName()).toURI().toURL();
            URL[] urls = classLoader.getURLs();
            ArrayList<URL> urlList = new ArrayList<>(java.util.Arrays.asList(urls));
            urlList.remove(projectURL);
            classLoader.close();
            classLoader = new CustomClassLoader(urlList.toArray(new URL[urlList.size()]));
        } catch (MalformedURLException e) {
            LOG.log(Level.WARNING, "Problem with finding project url for project {0}", projectUID.getProjectName());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Problem with closing classloader ");
        }
    }

    /**
     * Adds project URL to the classloader
     *
     * @param projectUID project unique ID
     */
    public synchronized void appendProjectToCL(ProjectUID projectUID) {
        try {
            File jar = filesStructure.getProjectJarFile(projectUID.getClientName(), projectUID.getProjectName());
            classLoader.addNewUrl(jar.toURI().toURL());
        } catch (MalformedURLException e) {
            LOG.log(Level.WARNING, "Problem with finding project url for project {0}", projectUID.getProjectName());
        }
    }
}
