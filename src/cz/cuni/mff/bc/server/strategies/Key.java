/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server.strategies;

/**
 * Used as key in the list needed for planning
 *
 * @author Jakub Hava
 */
public class Key {

    private int priority;
    private int cores;

    public Key(int priority, int cores) {
        this.priority = priority;
        this.cores = cores;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + this.priority;
        hash = 89 * hash + this.cores;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Key other = (Key) obj;
        if (this.priority != other.priority) {
            return false;
        }
        if (this.cores != other.cores) {
            return false;
        }
        return true;
    }
}