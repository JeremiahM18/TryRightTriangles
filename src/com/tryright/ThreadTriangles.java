/****************************************
 *
 * Author: Jeremiah McDonald
 * Assignment: Program 2
 * Class: CSC-4180 Operating Systems
 *
 ***************************************/

package com.tryright;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

        if (args.length != 2) {
            System.err.println("Error: expected 2 arguments, got " + args.length);
            printUsage();
            System.exit(EXIT_BAD_ARGS);
        }

        final File inputFile = new File(args[0]);

        final int threadCount;
        try {
            threadCount = parseThreadCount(args[1]);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            printUsage();
            System.exit(EXIT_BAD_ARGS);
            return;     // unreachable, but documents control flow
        }

        final List<Point> points;
        try {
            points = loadPoints(inputFile);
        } catch (IllegalArgumentException e) {
            // loadPoints uses IllegalArgumentException for validation and format errors
            System.err.println("Error: " + e.getMessage());
            printUsage();
            System.exit(EXIT_FORMAT_ERROR);
            return;     // unreachable
        }

        if (points.isEmpty()) {
            System.err.println("Error: input file contains no points");
            System.exit(EXIT_FORMAT_ERROR);
        }

        // Don't create more threads than there are pivot points
        final int pivots = points.size();
        final int actualThreads = Math.min(threadCount, pivots);

        // Shared memory for results (one slot per thread).
        final long[] partialCounts = new long[actualThreads];
        final Thread[] threads = new Thread[actualThreads];

        // Partition pivots into contiguous batches.
        final int batchSize = (pivots + actualThreads - 1) / actualThreads;

        int taskIndex = 0;
        for (int i = 0; i < actualThreads; i++) {
            final int start = i * batchSize;
            if (start >= pivots) {
                break;
            }

            final int end = Math.min(start + batchSize, pivots);

            final TriangleCounterTask task =
                    new TriangleCounterTask(points, start, end, partialCounts, taskIndex);

            threads[taskIndex] = new Thread(task, "TriangleCounter-" + taskIndex);
            threads[taskIndex].start();
            taskIndex++;
        }

        // Join all threads and aggregate partial results
        long total = 0;
        for (int j = 0; j < taskIndex; j++) {
            try {
                threads[j].join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Error: main thread interrupted while waiting for workers: "
                        + e.getMessage());
                System.exit(EXIT_IO_ERROR);
            }
            total += partialCounts[j];
        }

        System.out.println(total);
    }

    /**
     * Prints a usage message to stderr.
     */
    private static void printUsage() {
        System.err.println("Usage: ThreadTriangles <input file> <num_threads>");
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
        Objects.requireNonNull(s, "thread count string cannot be null");

        final int threadCount;
        try {
            threadCount = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Thread count must be an integer: " + s,
                    e
            );
        }

        if (threadCount < 1) {
            throw new IllegalArgumentException(
                    "Thread count must be greater than zero: " + threadCount
            );
        }

        return threadCount;
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
        Objects.requireNonNull(file, "file cannot be null");

        if (!file.exists()) {
            throw new IllegalArgumentException("input file does not exist: " + file);
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException("input file is not a file: " + file);
        }
        if (!file.canRead()) {
            throw new IllegalArgumentException("input file cannot be read: " + file);
        }

        try {
            return PointsIO.readPointsFromFile(file);
        } catch (IOException e) {
            // Treat as IO error; wrap as IllegalArgumentException so main can exit cleanly
            throw new IllegalArgumentException("I/O error while reading file: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            // Format problems from PointsIO
            throw new IllegalArgumentException("Invalid input format: " + e.getMessage(), e);
        }
    }
}
