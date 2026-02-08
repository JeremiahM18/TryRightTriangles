/****************************************
 *
 * Author: Jeremiah McDonald
 * Assignment: Program 3
 * Class: CSC-4180 Operating Systems
 *
 ***************************************/

package com.tryright;

/**
 * PointStore implementation for text-encoded point files.
 *
 * @author Jeremiah McDonald
 */
public final class TextPointStore implements PointStore {

    /**
     * Constructs a TextPointStore backed by a text file.
     *
     * @param filename path to the text-encoded point file
     */
    public TextPointStore(final String filename) {
        // TODO: implementation later
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getX(final int idx){
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getY(final int idx){
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
    public void close() {
        // nothing to close for text implementation
    }
}
