/****************************************
 *
 * Author: Jeremiah McDonald
 * Assignment: Program 3
 * Class: CSC-4180 Operating Systems
 *
 ***************************************/

package com.tryright;

/**
 * Factory for creating PointStore implementations based on input file type.
 *
 * @author Jeremiah McDonald
 */
public class PointStoreFactory {

    /**
     * Prevent instantiation of utility class.
     */
    private PointStoreFactory() {
        throw new AssertionError("PointStoreFactory cannot be instantiated");
    }

    /**
     * Create an appropriate PointStore for the given file.
     *
     * @param filename path to the point file
     * @return PointStore implementation
     * @throws IllegalArgumentException if filename is null
     */
    public static PointStore open(final String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("filename cannot be null");
        }

        if (filename.endsWith(".dat")) {
            return new BinPointStore(filename);
        }

        return new TextPointStore(filename);
    }
}
