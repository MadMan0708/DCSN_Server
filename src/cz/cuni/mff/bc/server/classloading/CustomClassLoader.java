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
import java.io.Serializable;
import java.rmi.Remote;
import java.util.Map;

/**
 *
 * @author Aku
 */
public class CustomClassLoader extends ClassLoader implements Remote, Serializable {

    private ProjectUID projectUID;
    private Map<ProjectUID, byte[]> classCache;

    public CustomClassLoader(Map<ProjectUID, byte[]> classCache) {
        this.classCache = classCache;
    }

    public void setProjectUID(String clientID, String projectID) {
        projectUID = new ProjectUID(clientID, projectID);
    }

    public void setProjectUID(ProjectUID projectUID) {
        this.projectUID = projectUID;
    }

    public ProjectUID getProjectUID() {
        return projectUID;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            if (!classCache.containsKey(projectUID)) {
                {
                    classCache.put(projectUID, loadClassData(name));
                }
            }
        } catch (IOException e) {
            throw new ClassNotFoundException("Class [" + name + "] could not be found", e);
        }
        byte[] oneClassData = classCache.get(projectUID);

        Class<?> c = defineClass(name, oneClassData, 0, oneClassData.length);
        resolveClass(c);
        return c;
    }

    /**
     * Load the class file into byte array
     *
     * @param name The name of the class e.g. com.codeslices.test.TestClass}
     * @return The class file as byte array
     * @throws IOException
     */
    private byte[] loadClassData(String name) throws IOException {
        String[] parts = name.split("\\.");
        String last = parts[parts.length - 1];
        ByteArrayOutputStream out;
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(
                TaskManager.getProjectDir(projectUID.getProjectID(), projectUID.getClientID()) + last + ".class"))) {

            out = new ByteArrayOutputStream();
            int i;
            while ((i = in.read()) != -1) {
                out.write(i);
            }
        }
        byte[] classData = out.toByteArray();
        out.close();

        return classData;
    }
}
