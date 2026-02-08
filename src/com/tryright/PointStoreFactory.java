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
 * <p>This class centralized the police for selecting an appropriate
 * PointStore implementation, allowing storage decisions to remain
 * decoupled from computational logic.</p>
 *
 * @author Jeremiah McDonald
 */
public class PointStoreFactory {

    /**
     * Prevent instantiation of utility class.
     *
     * <p>All functionality is provided via static methods.</p>
     */
    private PointStoreFactory() {
        throw new AssertionError("PointStoreFactory cannot be instantiated");
    }

    /**
     * Create an appropriate PointStore for the given file.
     *
     * @param filename path to the point file
     * @return PointStore implementation
     * @throws IllegalArgumentException if filename is null or fails validation
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
