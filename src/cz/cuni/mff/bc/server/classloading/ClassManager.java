/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server.classloading;

import cz.cuni.mff.bc.server.misc.ProjectUID;
import cz.cuni.mff.bc.server.TaskManager;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Collections;
import java.util.Map;

/**
 *
 * @author Jakub
 */
public class ClassManager {

    private Map<ProjectUID, byte[]> classData = Collections.synchronizedMap(new HashMap<ProjectUID, byte[]>());
    // private ConcurrentHashMap<ProjectUID, byte[]> classData = new ConcurrentHashMap<>();
    private final String prefix = "cz.cuni.mff.bc.comp.";
    //private ConcurrentHashMap<String, CustomClassLoader> loaderCache = new ConcurrentHashMap<>();
    private Map<String, CustomClassLoader> loaderCache = Collections.synchronizedMap(new HashMap<String, CustomClassLoader>());

    public Class<?> loadClass(String clientSessionID, ProjectUID uid, String className) throws ClassNotFoundException {
        CustomClassLoader cl = getClassLoader(clientSessionID);
        cl.setProjectUID(uid);
        return cl.loadClass(prefix + className);
    }

    public void setCustomClassLoader(String clientSessionID) { // classLoader je asociovan s otevrenou session klienta
        // don't have to be synchronized, each client has unique sessionID, no clients at same time with same session ID can
        // insert new CustomClassLoader
        //synchronized (loaderCache) {
        if (!loaderCache.containsKey(clientSessionID)) {
            loaderCache.put(clientSessionID, new CustomClassLoader(classData));
        }

    }

    public CustomClassLoader getClassLoader(String clientSessionID) {
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

    public byte[] getClassData(ProjectUID uid, String className) throws IOException {
        // synchronized becouse different clients might not find the class at same time and try to load it at same time
        synchronized (classData) {
            if (!classData.containsKey(uid)) {
                ByteArrayOutputStream out;
                try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(
                        TaskManager.getProjectDir(uid.getProjectID(), uid.getClientID()) + className + ".class"))) {

                    out = new ByteArrayOutputStream();
                    int i;
                    while ((i = in.read()) != -1) {
                        out.write(i);
                    }
                    classData.put(uid, out.toByteArray());
                }
                out.close();
            }
        }
        return classData.get(uid);
    }
}
