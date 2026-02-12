/*************************************
 *
 * Author: Jeremiah McDonald
 * Assignment: Program 3
 * Class: CSC-4180 Operating Systems
 *
 *************************************/

package com.tryright;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Multiprocess right triangle counter using {@link ProcessBuilder}
 * and pipes for Inter-Process Communication (IPC).
 *
 * <p>This program:</p>
 * <ul>
 *     <li>Loads points through the {@link PointStore} abstraction</li>
 *     <li>Partitions pivot indices across multiple child JVM processes</li>
 *     <li>Streams point data to each child via {@code stdin}</li>
 *     <li>Aggregates partial triangle counts from child {@code stdout}</li>
 * </ul>
 *
 * <p>Each child process executes {@link Triangles} in {@code --child} mode.</p>
 *
 * <p>This class is not instantiable.</p>
 *
 * @author Jeremiah McDonald
 */
public final class ProcessTriangles {

    /**
     * Exit code for invalid command-line parameters.
     */
    private static final int EXIT_BAD_ARGS = 1;

    /**
     * Exit code for I/O-related failures.
     */
    private static final int EXIT_IO_ERROR = 2;

    /**
     * Exit code for invalid input format.
      */
    private static final int EXIT_FORMAT_ERROR = 3;

    /**
     * Prevents instantiation.
     */
    private ProcessTriangles() {
        // Utility class
    }

    /**
     * Prints usage information to {@code stderr}.
     */
    private static void printUsage() {
        System.err.println("Usage: <filename> <numProcesses>");
    }

    /**
     * Parses and validates the number of child processes.
     *
     * @param arg string representation of process count
     * @return validated positive process count
     * @throws IllegalArgumentException if the argument is invalid
     */
    private static int parseProcessCount(final String arg) {
        final int count;

        try {
            count = Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("numProcesses must be an integer.");
        }

        if (count <= 0) {
            throw new IllegalArgumentException("numProcesses must be a positive integer.");
        }

        return count;
    }

    /**
     * Partitions pivot indices {@code [0, total)} into contiguous ranges.
     *
     * <p>Each returned row represents a half-open interval
     * {@code [startInclusive, endExclusive)}.</p>
     *
     * @param total total number of pivot indices
     * @param numProcesses number of processes
     * @return a 2D array of index ranges
     */
    private static int[][] partitionIndices(int total, int numProcesses) {

        final int[][] ranges = new int[numProcesses][2];

        // Compute ceiling(total / numProcesses) to evenly distribute work
        int boxSize = (total + numProcesses - 1) / numProcesses;

        for (int i = 0; i < numProcesses; i++) {
            int start = i * boxSize;
            int end = Math.min(start + boxSize, total);
            ranges[i][0] = start;
            ranges[i][1] = end;
        }

        return ranges;
    }

    /**
     * Program entry point.
     *
     * @param args command-line arguments:
     *             {@code <filename> <numProcesses>}
     */
    public static void main(final String[] args) {

        Objects.requireNonNull(args, "args cannot be null.");

        if (args.length != 2) {
            System.err.println("Error: expected exactly 2 parameters.");
            printUsage();
            System.exit(EXIT_BAD_ARGS);
            return;
        }

        final int numProcesses;

        try{
            numProcesses = parseProcessCount(args[1]);
        } catch (IllegalArgumentException e){
            System.err.println("Error: " + e.getMessage());
            printUsage();
            System.exit(EXIT_BAD_ARGS);
            return;
        }

        try {
            // Parent reads list of points once
            // The point list is immutable after this and shared with children
            // by streaming its contents through stdin.
            final PointStore store = PointStoreFactory.open(args[0]);

            try {

            // Partition the outer-loop pivot indices [0, points.size())
            // into contiguous ranges so each child process is responsible
            // for a disjoint subset of triangle computations.
            final int[][] ranges = partitionIndices(store.numPoints(), numProcesses);

            // Maintain references to child processes and their stdout streams
            // so parent can collect partial results deterministically.
            final List<Process> processes = new ArrayList<>();
            final List<BufferedReader> readers = new ArrayList<>();

            // Spawn one child Java Virtual Machine(JVM) per index range using ProcessBuilder.
            // Each child runs the Triangles program in "--child" mode.
            for (int i = 0; i < numProcesses; i++) {
                final int start = ranges[i][0];
                final int end = ranges[i][1];

                // Skip empty ranges to avoid spawning unnecessary processes
                if (start >= end) {
                    continue;
                }

                final String classPath = System.getProperty("java.class.path");

                // Create a new JVM process executing the Triangles class
                // with arguments specifying the index range it is responsible for.
                final ProcessBuilder processBuilder =
                        new ProcessBuilder(
                        "java",
                        "-cp",
                        classPath,
                        "com.tryright.Triangles",
                        "--child",
                        String.valueOf(start),
                        String.valueOf(end)
                );

                // Redirect child stderr to parent stderr for visibility of errors.
                processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

                final Process process = processBuilder.start();
                processes.add(process);

                // Send full point list to child via stdin
                // Each child independently parses the same point list
                // only processes its assigned index range
                try (PrintWriter writer =
                             new PrintWriter(new OutputStreamWriter(process.getOutputStream()))) {
                    for (int j = 0; j < store.numPoints(); j++) {
                        writer.println(store.getX(j) + " " + store.getY(j));
                    }
                }

                // Capture the child's stdout so the parent can read
                // the partial triangle count produced by that child.
                readers.add(new BufferedReader(
                        new InputStreamReader(process.getInputStream())));
            }

            // Read one line of output from each child process.
            // Each line represents the number of triangles found in that child's index range
            long total = 0;

            for (BufferedReader reader : readers) {
                final String line = reader.readLine();
                if (line == null) {
                    throw new IOException("Child process produced no output");
                }

                total += Long.parseLong(line.trim());
            }

            // Ensure all child processes have terminated before exiting.
            for (Process p : processes) {
                p.waitFor();
            }

            // The final aggregated triangle count to stdout.
            System.out.println(total);

        } finally {
            store.close();
        }

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(EXIT_IO_ERROR);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(EXIT_FORMAT_ERROR);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Error: interrupted while waiting for child process");
            System.exit(EXIT_IO_ERROR);
        }
    }
}
