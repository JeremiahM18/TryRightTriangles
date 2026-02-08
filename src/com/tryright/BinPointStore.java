/****************************************
 *
 * Author: Jeremiah McDonald
 * Assignment: Program 3
 * Class: CSC-4180 Operating Systems
 *
 ***************************************/

package com.tryright;

/**
 * PointStore implementation for binary-encoded point files
 * accessed via memory-mapped I/O.
 *
 * @author Jeremiah McDonald
 */
public class BinPointStore implements PointStore {

    /**
     * Constructs a BinPointStore backed by a binary file.
     *
     * @param filename path to the binary-encoded point file
     */
    public BinPointStore(final String filename) {
        // TODO: implementation later
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getX(final int idx) {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getY(final int idx) {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int numPoints() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close () {
        // TODO: implementation later
    }
}
