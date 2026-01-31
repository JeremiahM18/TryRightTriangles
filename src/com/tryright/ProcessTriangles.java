/*************************************
 *
 * Author: Jeremiah McDonald
 * Assignment: Program 1
 * Class: CSC-4180 Operating Systems
 *
 *************************************/

package com.tryright;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Multiprocess right triangle counter using {@link ProcessBuilder} and pipes
 * for Inter-Process Communication (IPC).
 *
 * <p>This program reads points from an input file, spawns multiple child processes,
 * and sums their partial triangle counts into the final answer.</p>
 *
 * @author Jeremiah McDonald
 */
public final class ProcessTriangles {

    /**
     * Exit code used when the command-line parameters are invalid.
     */
    private static final int EXIT_BAD_ARGS = 1;
    /**
     * Exit code used for failures related to reading the input file.
     */
    private static final int EXIT_IO_ERROR = 2;
    /**
     * Exit code used when the input file format is invalid.
      */
    private static final int EXIT_FORMAT_ERROR = 3;

    /**
     * Prints the expected command-line parameters to {@code stderr}.
     */
    private static void printUsage() {
        System.err.println("Parameter(s): <filename> <numProcesses>");
    }

    /**
     * Parses and validates the process count argument.
     *
     * @param arg the command-line argument representing the process count
     * @return validated positive integer process count
     * @throws IllegalArgumentException if the argument is invalid
     */
    private static int parseProcessCount(final String arg) {
        final int count;

        try {
            count = Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("numProcesses must be an integer");
        }

        if (count <= 0) {
            throw new IllegalArgumentException("numProcesses must be a positive integer");
        }

        return count;
    }

    /**
     * Partitions a range of indices {@code [0, total)} into contiguous blocks
     * based on the number of processes.
     *
     * <p>Each block represents the subset of outer-loop indices that a single
     * child process will be responsible for. The final block may be smaller
     * if {@code total} is not evenly divisible by {@code numProcesses}.</p>
     *
     * <p>Ranges are half-open: {@code [start, end)} where {@code end} is exclusive.</p>
     *
     * @param total total number of work items
     * @param numProcesses number of processes to partition the work across
     * @return a 2D array where each row is {@code [startIndex, endIndex)}
     */
    private static int[][] partitionIndices(int total, int numProcesses) {
        int[][] ranges = new int[numProcesses][2];

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
     * <p>Parameters:</p>
     * <ul>
     *     <li>{@code args[0]}: input filename containing points</li>
     *     <li>{@code args[1]}: number of child processes to spawn</li>
     * </ul>
     *
     * @param args command-line arguments
     */
    public static void main(final String[] args) {
        Objects.requireNonNull(args, "args cannot be null");

        if (args.length != 2) {
            System.err.println("Error: expected exactly 2 parameters.");
            printUsage();
            System.exit(EXIT_BAD_ARGS);
            return;
        }

        final File file = new File(args[0]);
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
            final List<Point> points = PointsIO.readPointsFromFile(file);

            // Partition the outer-loop pivot indices [0, points.size())
            // into contiguous ranges so each child process is responsible
            // for a disjoint subset of triangle computations.
            final int[][] ranges = partitionIndices(points.size(), numProcesses);

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
                final ProcessBuilder processBuilder = new ProcessBuilder(
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
                    PointsIO.writePoints(points, writer);
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
                if(line == null){
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
