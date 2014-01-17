/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import java.util.Collection;

/**
 * Listens to changes related to client computation status
 *
 * @author Jakub Hava
 */
public interface IActiveClientListener {

    /**
     * Does the planning after the client computation status has changed
     *
     */
    public void afterChange();
}
