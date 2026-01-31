/*************************************
 * Author: Jeremiah McDonald
 * Assignment: Program 1
 * Class: CSC-4180 Operating Systems
 *************************************/

package com.tryright;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/**
 * Single-process right triangle counter.
 *
 * <p>This class can also act as a child process when invoked using
 * {@code --child <startInclusive> <endExclusive>}.</p>
 *
 * @author Jeremiah McDonald
 */
public final class Triangles {

    /**
     * Exit code used when command-line parameters are invalid.
     */
    private static final int EXIT_BAD_ARGS = 1;

    /**
     * Exit code used when file input fails.
     */
    private static final int EXIT_IO_ERROR = 2;

    /**
     * Exit code used when the point file format is invalid.
     */
    private static final int EXIT_FORMAT_ERROR = 3;

    /**
     * Exit code used when child arguments or execution are invalid.
     */
    private static final int EXIT_CHILD_ERROR = 4;

    /**
     * Prints the expected command-line parameters to {@code stderr}.
     */
    private static void printUsage(){
        System.err.println("Parameter(s): <filename>");
    }

    /**
     * Prints the expected parameter usage format for child mode to {@code stderr}.
     */
    private static void printChildUsage(){
        System.err.println("Parameter(s): --child <startInclusive> <endExclusive>");
    }

    /**
     * Program entry point.
     *
     * <p>Normal mode expects one parameter: the input filename.</p>
     *
     * <p>Child mode expects three parameters:</p>
     * <ul>
     *     <li>{@code --child}</li>
     *     <li>{@code startInclusive}</li>
     *     <li>{@code endExclusive}</li>
     * </ul>
     *
     * @param args command-line arguments
     */
    public static void main(final String[] args) {
        Objects.requireNonNull(args, "args cannot be null");

        if (args.length >= 1 && "--child".equals(args[0])) {
            runChild(args);
            return;
        }

        if (args.length != 1) {
            System.err.println("Error: expected exactly 1 parameter.");
            printUsage();
            System.exit(EXIT_BAD_ARGS);
            return;
        }

        final File file = new File(args[0]);
        final List<Point> points;

        try {
            points = PointsIO.readPointsFromFile(file);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(EXIT_IO_ERROR);
            return;
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(EXIT_FORMAT_ERROR);
            return;
        }

        final long answer = RightTriangleCounter.countRightTriangles(points);
        System.out.println(answer);
    }

    /**
     * Runs in child mode.
     *
     * <p>The child process reads the full point list from {@code stdin}, counts right triangles
     * for pivot indices in {@code [startInclusive, endExclusive)}, and prints the partial count
     * to {@code stdout}.</p>
     *
     * @param args expected parameters: {@code --child startInclusive endExclusive}
     */
    private static void runChild(final String[] args) {
        if (args.length != 3) {
            System.err.println("Error: bad child parameters.");
            printChildUsage();
            System.exit(EXIT_CHILD_ERROR);
            return;
        }

        final int start;
        final int end;

        try {
            start = Integer.parseInt(args[1]);
            end = Integer.parseInt(args[2]);
        } catch (final NumberFormatException e) {
            System.err.println("Error: child range must be integers.");
            printChildUsage();
            System.exit(EXIT_CHILD_ERROR);
            return;
        }

        final List<Point> points;
        try (Scanner scanner = new Scanner(System.in, "US-ASCII")) {
            points = PointsIO.readPointsFromScanner(scanner);
        } catch (final IllegalArgumentException e){
            System.err.println("Error: " + e.getMessage());
            System.exit(EXIT_FORMAT_ERROR);
            return;
        }

        try {
            final long partial = RightTriangleCounter.countRightTriangles(points, start, end);
            System.out.println(partial);
        } catch (final IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(EXIT_CHILD_ERROR);
        }
    }
}

