package com.tryright;

/**
 * Point storage interface
 * @version 1.0
 */

public interface PointStore {
    /**
     * Get X value at given index
     *
     * @param idx index from which to fetch values
     * @return x value at given index
     */
    int getX (int idx);
    /**
     * Get Y value at given index
     *
     * @param idx index from which to fetch values
     * @return Y value at given index
     */
    int getY (int idx);
    /**
     * Get number of points in store
     *
     * @return number of points in store
     */
    int numPoints();
    /**
     * close point store
     */
    void close();
}
