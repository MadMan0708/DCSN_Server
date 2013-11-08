/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import cz.cuni.mff.bc.common.main.Logger;
import cz.cuni.mff.bc.server.exceptions.ExtractionException;
import cz.cuni.mff.bc.server.exceptions.NotSupportedArchiveException;
import static cz.cuni.mff.bc.server.TaskManager.getDirInProject;
import static cz.cuni.mff.bc.server.TaskManager.getProjectDir;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarInputStream;

/**
 *
 * @author Jakub
 */
public class Extractor {//implements Callable<Boolean> {

    private String extension;
    private String projectName;
    private String clientName;
    private Project project;
    private Logger logger;

    public Extractor(String extension, Project project, Logger logger) {
        projectName = project.getProjectName();
        clientName = project.getClientName();
        this.extension = extension;
        this.project = project;
        this.logger = logger;
    }

    // @Override
    public void unpack() throws NotSupportedArchiveException, ExtractionException {
        switch (extension.toLowerCase()) {
            case "tar":
                logger.log("Extracting " + projectName + " by " + clientName + " from " + extension + " archive");
                extractFromTar();
                logger.log("Extraction of " + projectName + " by " + clientName + " was sucesfull");
                //return null;
                break;
            case "zip":
                logger.log("Extracting " + projectName + " by " + clientName + " from " + extension + " archive");
                extractFromZip();
                logger.log("Extraction of " + projectName + " by " + clientName + " was sucesfull");
                //return null;
                break;
            default:
                throw new NotSupportedArchiveException("Archive with extension " + extension + " is not supported");
        }
    }

    private File getOutputFileDestination(String fileName) {
        if (fileName.endsWith(".class")) {
            project.setClassName(fileName.substring(0, fileName.length() - 6)); // cutting out ".class" from the end of the file
            return new File(getProjectDir(projectName, clientName) + fileName);

        } else {
            return new File(getDirInProject(projectName, clientName, "temp") + fileName);
        }
    }

    private void extractFromZip() throws ExtractionException {
        File archive = new File(Server.getUploadedDir() + File.separator + clientName + "_" + projectName + ".zip");

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(archive))) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {//while there are entries
                int count;
                byte buffer[] = new byte[2048];
                File outputFile = getOutputFileDestination(entry.getName());
                try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile))) {
                    while ((count = zis.read(buffer)) != -1) {
                        out.write(buffer, 0, count);
                    }
                } catch (IOException e) {
                    throw new ExtractionException("Problem with extracting file " + entry.getName() + " from archive " + archive.getName(), e);
                }
                entry = zis.getNextEntry();
            }
        } catch (IOException e) {
            throw new ExtractionException("Problem with opening project archive", e);
        }
    }

    private void extractFromTar() throws ExtractionException {
        File archive = new File(Server.getUploadedDir() + File.separator + clientName + "_" + projectName + ".tar");

        try (TarInputStream tis = new TarInputStream(new BufferedInputStream(new FileInputStream(archive)))) {
            TarEntry entry = tis.getNextEntry();
            while (entry != null) {
                int count;
                byte buffer[] = new byte[2048];
                File outputFile = getOutputFileDestination(entry.getName());
                try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile))) {
                    while ((count = tis.read(buffer)) != -1) {
                        out.write(buffer, 0, count);
                    }
                    out.flush();
                } catch (IOException e) {
                    throw new ExtractionException("Problem with extracting file " + entry.getName() + " from archive " + archive.getName(), e);
                }
                tis.getNextEntry();
            }


        } catch (IOException e) {
            throw new ExtractionException("Problem with opening project archive", e);
        }
    }
}
