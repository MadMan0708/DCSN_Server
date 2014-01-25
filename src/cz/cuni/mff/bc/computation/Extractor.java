/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.computation;

import cz.cuni.mff.bc.api.main.CustomIO;
import cz.cuni.mff.bc.server.Server;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Extracts the data files from the archive
 *
 * @author Jakub Hava
 */
public class Extractor {

    private String extension;
    private String projectName;
    private String clientName;
    private File archive;
    private File destination;
    private static final Logger LOG = Logger.getLogger(Server.class.getName());

    /**
     * Constructor
     *
     * @param archive archive with data
     * @param projectName project name
     * @param clientName client's name
     * @param destination file where to unpack the data
     */
    public Extractor(File archive, String projectName, String clientName, File destination) {
        this.projectName = projectName;
        this.clientName = clientName;
        this.extension = CustomIO.getExtension(archive);
        this.archive = archive;
        this.destination = destination;
    }

    /**
     * Unpacks the archive
     *
     * @throws IOException
     */
    public void unpack() throws IOException {
        switch (extension.toLowerCase()) {
            case "zip":
                LOG.log(Level.FINE, "Extracting {0} by {1} from {2} archive", new Object[]{projectName, clientName, extension});
                CustomIO.extractZipFile(archive, destination);
                LOG.log(Level.FINE, "Extraction of {0} by {1} was sucesfull", new Object[]{projectName, clientName});
                break;
            default:
                throw new IOException("Data archive with extension " + extension + " is not supported");
        }
    }
}
