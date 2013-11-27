/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import cz.cuni.mff.bc.api.main.CustomIO;
import cz.cuni.mff.bc.server.exceptions.ExtractionException;
import cz.cuni.mff.bc.server.exceptions.NotSupportedArchiveException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private File archive;
    private File destination;
    private static final Logger LOG = Logger.getLogger(Server.class.getName());

    public Extractor(File archive, Project project) {
        projectName = project.getProjectName();
        clientName = project.getClientName();
        extension = CustomIO.getExtension(archive);
        this.project = project;
        this.archive = archive;
        destination = FilesStructure.getTempDirInProject(clientName, projectName);
    }

    public void unpack() throws NotSupportedArchiveException, ExtractionException {
        switch (extension.toLowerCase()) {
            case "tar":
                LOG.log(Level.INFO, "Extracting {0} by {1} from {2} archive", new Object[]{projectName, clientName, extension});
                extractFromTar();
                LOG.log(Level.INFO, "Extraction of {0} by {1} was sucesfull", new Object[]{projectName, clientName});
                break;
            case "zip":
                LOG.log(Level.INFO, "Extracting {0} by {1} from {2} archive", new Object[]{projectName, clientName, extension});
                extractFromZip();
                LOG.log(Level.INFO, "Extraction of {0} by {1} was sucesfull", new Object[]{projectName, clientName});
                break;
            default:
                throw new NotSupportedArchiveException("Archive with extension " + extension + " is not supported");
        }
    }

    private void extractFromZip() throws ExtractionException {
        try {
            CustomIO.extractZipFile(archive, destination);
        } catch (IOException e) {
            throw new ExtractionException(e.getMessage(), e);
        }
    }

    private static void extractTarFile(File tar, File dest) throws IOException {
        try (TarInputStream tis = new TarInputStream(new BufferedInputStream(new FileInputStream(tar)))) {
            TarEntry entry = tis.getNextEntry();
            while (entry != null) {
                int count;
                byte buffer[] = new byte[2048];
                String fileName = entry.getName();
                File newFile = new File(dest, fileName);
                try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(newFile))) {
                    while ((count = tis.read(buffer)) != -1) {
                        out.write(buffer, 0, count);
                    }
                    out.flush();
                } catch (IOException e) {
                    throw new IOException("Problem with extracting file " + entry.getName() + " from archive " + tar.getName(), e);
                }
                tis.getNextEntry();
            }
        } catch (IOException e) {
            throw new IOException("Problem with opening project archive " + tar.getName(), e);
        }
    }

    private void extractFromTar() throws ExtractionException {
        try {
            extractTarFile(archive, destination);
        } catch (IOException e) {
            throw new ExtractionException(e.getMessage(), e);
        }
    }
}
