/****************************************
 *
 * Author: Jeremiah McDonald
 * Assignment: Program 2
 * Class: CSC-4180 Operating Systems
 *
 ***************************************/

package com.tryright;

import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * Multithreaded right triangle counter
 *
 * <p>This program computes the number of right triangles using multiple
 * threads that share memory within a single Java Virtual Machine.</p>
 *
 * @author Jeremiah McDonald
 */
public class ThreadTriangles {

    /**
     * Exit code used when command-line arguments are invalid.
     */
    private static final int EXIT_BAD_ARGS = 1;

    /**
     * Exit code used when input file cannot be read.
     */
    private static final int EXIT_IO_ERROR = 2;

    /**
     * Exit code used when input file format is invalid.
     */
    private static final int EXIT_FORMAT_ERROR = 3;

    /**
     * Program entry point.
     *
     * @param args command-line arguments:
     *             args[0] input file
     *             args[1] number of threads
     */
    public static void main(final String[] args) {
        Objects.requireNonNull(args, "args cannot be null");

        // TODO:
        // 1. Validate argument count
        // 2. Parse and validate thread count
        // 3. Load points from file
        // 4. Partition work across threads
        // 5. Create TriangleCounterTask instances
        // 6. Start threads
        // 7. Join threads
        // 8. Aggregate partial results
        // 9. Print final count to stdout
    }

    /**
     * Parses and validates the number of threads.
     *
     * @param s thread count string
     * @return parsed thread count
     *
     * @throws IllegalArgumentException if thread count is invalid
     */
    private static int parseThreadCount(final String s){
        // TODO:
        // Parse integer
        // Ensure > 0
        // Throw IllegalArgumentException
        return 0;
    }

    /**
     * Loads points from input file.
     *
     * @param file input file
     * @return list of points
     *
     * @throws IllegalArgumentException if file is invalid or unreadable
     */
    private static List<Point> loadPoints(final File file){
        // TODO:
        // Validate file existence
        // Delegate to PointsIO
        // Exception Handling
        return null;
    }

}
