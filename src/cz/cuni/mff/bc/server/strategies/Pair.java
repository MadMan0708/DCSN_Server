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
public class Pair {

    private final int first;
    private final int second;

    /**
     * Creates the pair
     *
     * @param first first integer
     * @param second second integer
     */
    public Pair(int first, int second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + this.first;
        hash = 89 * hash + this.second;
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
        final Pair other = (Pair) obj;
        if (this.first != other.first) {
            return false;
        }
        if (this.second != other.second) {
            return false;
        }
        return true;
    }

    /**
     * Gets the first integer from the pair
     *
     * @return the first integer from the pair
     */
    public int getFirst() {
        return first;
    }

    /**
     * Gets the second integer from the pair
     *
     * @return the second integer from the pair
     */
    public int getSecond() {
        return second;
    }
}