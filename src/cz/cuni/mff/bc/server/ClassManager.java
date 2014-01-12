/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import cz.cuni.mff.bc.server.misc.CustomClassLoader;
import cz.cuni.mff.bc.api.main.JarAPI;
import cz.cuni.mff.bc.api.main.ProjectUID;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Collections;
import java.util.Map;

/**
 * Class manager
 *
 * @author Jakub Hava
 */
public class ClassManager {

    private Map<String, CustomClassLoader> loaderCache = Collections.synchronizedMap(new HashMap<String, CustomClassLoader>());

    /**
     * Loads class
     *
     * @param clientSessionID client's name
     * @param uid project unique id
     * @return loaded class
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public Class<?> loadClass(String clientSessionID, ProjectUID uid) throws ClassNotFoundException, IOException {
        CustomClassLoader cl = getClassLoader(clientSessionID);
        File jar = FilesStructure.getProjectJarFile(uid.getClientName(), uid.getProjectName());
        cl.addNewUrl(jar.toURI().toURL());
        String name = JarAPI.getAttributeFromManifest(jar.toPath(), "Main-Comp-Class");
        return cl.loadClass(name);
    }

    /**
     * Sets custom class loader
     *
     * @param clientSessionID client's name
     */
    public void setCustomClassLoader(String clientSessionID) {
        // it doesn't have to be synchronized, each client has unique sessionID, no clients at same time with same session ID can
        // insert new CustomClassLoader
        synchronized (loaderCache) {
            if (!loaderCache.containsKey(clientSessionID)) {
                loaderCache.put(clientSessionID, new CustomClassLoader());
            }
        }
    }

    /**
     * Gets client's class loader
     *
     * @param clientSessionID client's name
     * @return custom class loader
     */
    public CustomClassLoader getClassLoader(String clientSessionID) {
        setCustomClassLoader(clientSessionID);
        // it is surely in loaderCache, because setCustomClassLoader possible inserted new class loader
        return loaderCache.get(clientSessionID);
    }

    /**
     * Deletes custom class loader
     *
     * @param clientSessionID client's name
     */
    public void deleteCustomClassLoader(String clientSessionID) {
        // it doesn't have to be synchronized, as each client is associated only with one clientSessionID
        if (loaderCache.containsKey(clientSessionID)) {
            loaderCache.remove(clientSessionID);
        }

    }
}
