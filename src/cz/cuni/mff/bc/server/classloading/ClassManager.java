/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server.classloading;

import cz.cuni.mff.bc.api.main.JarAPI;
import cz.cuni.mff.bc.api.main.ProjectUID;
import cz.cuni.mff.bc.server.FilesStructure;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Collections;
import java.util.Map;

/**
 *
 * @author Jakub
 */
public class ClassManager {

    private Map<String, CustomCL> loaderCache = Collections.synchronizedMap(new HashMap<String, CustomCL>());

    public Class<?> loadClass(String clientSessionID, ProjectUID uid) throws ClassNotFoundException, IOException {
        CustomCL cl = getClassLoader(clientSessionID);
        File jar = FilesStructure.getProjectJarFile(uid.getClientName(), uid.getProjectName());
        cl.addNewUrl(jar.toURI().toURL());
        String name = JarAPI.getAttributeFromManifest(jar.toPath(), "Main-Comp-Class");
        return cl.loadClass(name);
    }

    public void setCustomClassLoader(String clientSessionID) { // classLoader je asociovan s otevrenou session klienta
        // don't have to be synchronized, each client has unique sessionID, no clients at same time with same session ID can
        // insert new CustomClassLoader
        synchronized (loaderCache) {
            if (!loaderCache.containsKey(clientSessionID)) {
                loaderCache.put(clientSessionID, new CustomCL());
            }

        }
    }

    public CustomCL getClassLoader(String clientSessionID) {
        setCustomClassLoader(clientSessionID);
        // it is surely there, becouse setCustomClassLoader possible inserted new ClassLoader
        return loaderCache.get(clientSessionID);
    }

    public void deleteCustomClassLoader(String clientSessionID) {

        // don't have to be synchronized, as each client has associated onli one clientSessionID1
        if (loaderCache.containsKey(clientSessionID)) {
            loaderCache.remove(clientSessionID);
        }

    }
}
