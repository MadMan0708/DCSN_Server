/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server.classloading;

import cz.cuni.mff.bc.api.main.JarAPI;
import cz.cuni.mff.bc.api.main.ProjectUID;
import cz.cuni.mff.bc.server.TaskManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Collections;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 *
 * @author Jakub
 */
public class ClassManager {

    private Map<ProjectUID, Class<?>> classData = Collections.synchronizedMap(new HashMap<ProjectUID, Class<?>>());
    // private final String prefix = "cz.cuni.mff.bc.comp.";
    // private Map<String, CustomClassLoader> loaderCache = Collections.synchronizedMap(new HashMap<String, CustomClassLoader>());
    private Map<String, CustomCL> loaderCache = Collections.synchronizedMap(new HashMap<String, CustomCL>());

    public File getJarFile(ProjectUID uid) throws IOException {
        File[] jar = new File(TaskManager.getProjectDir(uid.getClientID(), uid.getProjectID())).listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.getName().endsWith(".jar")) {
                    return true;
                } else {
                    return false;
                }
            }
        });
        return jar[0];
    }

    public Class<?> loadClass(String clientSessionID, ProjectUID uid) throws ClassNotFoundException, IOException {
//        CustomClassLoader cl = getClassLoader(clientSessionID);
//        cl.setProjectUID(uid);
//        String name = JarAPI.getAttributeFromManifest(getJarFile(uid).toPath(), "Main-Comp-Class");
//        return cl.loadClass(name);
        CustomCL cl = getClassLoader(clientSessionID);
        cl.addNewUrl(getJarFile(uid).toURI().toURL());
        String name = JarAPI.getAttributeFromManifest(getJarFile(uid).toPath(), "Main-Comp-Class");
        return cl.loadClass(name);
    }

    public void setCustomClassLoader(String clientSessionID) { // classLoader je asociovan s otevrenou session klienta
        // don't have to be synchronized, each client has unique sessionID, no clients at same time with same session ID can
        // insert new CustomClassLoader
        synchronized (loaderCache) {
//            if (!loaderCache.containsKey(clientSessionID)) {
//                loaderCache.put(clientSessionID, new CustomClassLoader(classData));
//            }
            if (!loaderCache.containsKey(clientSessionID)) {
                loaderCache.put(clientSessionID, new CustomCL());
            }

        }
    }

    // public CustomClassLoader getClassLoader(String clientSessionID) {
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

    /*  public byte[] getClassData(ProjectUID uid, String className) throws IOException {
     // synchronized becouse different clients might not find the class at same time and try to load it at same time
     synchronized (classData) {
     if (!classData.containsKey(uid)) {
     try (ByteArrayOutputStream out = new ByteArrayOutputStream();
     JarInputStream jar = new JarInputStream(new FileInputStream(getJarFile(uid)))) {
     JarEntry entry;
     while ((entry = jar.getNextJarEntry()) != null) {
     if (entry.getName().equals(className + ".class")) {

     byte[] buffer = new byte[8192];
     int len;
     while ((len = jar.read(buffer)) != -1) {
     out.write(buffer, 0, len);
     }
     out.close();
     break;
     }
     }
     classData.put(uid, out.toByteArray());
     out.close();
     }

     }
     return classData.get(uid);
     }*/
}
