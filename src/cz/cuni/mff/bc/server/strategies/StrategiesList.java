/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server.strategies;

/**
 * Server strategies
 *
 * @author Jakub Hava
 */
public enum StrategiesList {

    /**
     * Task with highest priority goes first
     */
    HIGHEST_PRIORITY_FIRST,
    /**
     * Plan is optimalised for maximal throughput
     */
    MAXIMAL_THROUGHPUT
}
