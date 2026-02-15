/*************************************
 *
 * Author: Jeremiah McDonald
 * Assignment: Program 1
 * Class: CSC-4180 Operating Systems
 *
 *************************************/

package com.tryright;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/**
 * Utility methods for reading and writing point data.
 *
 * @author Jeremiah McDonald
 */
public final class PointsIO {

    /**
     * Prevents instantiation.
     */
    private PointsIO() {
        // This class should not be instantiated.
    }

    /**
     * Reads point data from a file.
     *
     * <p>File format:</p>
     * <ul>
     *     <li>First integer: n (number of points)</li>
     *     <li>Then n pairs of integers: x y</li>
     * </ul>
     *
     * @param file input file
     * @return list of points read from the file
     * @throws IOException if file cannot be opened or read
     * @throws IllegalArgumentException if the input format is invalid
     */
    public static List<Point> readPointsFromFile(final File file) throws IOException {
        Objects.requireNonNull(file, "file cannot be null");

        // FileInputStream gives OS-level exceptions.
        try (FileInputStream in = new FileInputStream(file);
             Scanner sc = new Scanner(in, "US-ASCII")) {

            if (!sc.hasNextInt()) {
                throw new IllegalArgumentException("missing number of points.");
            }

            final int n = sc.nextInt();

            if (n < 0) {
                throw new IllegalArgumentException("number of points cannot be negative.");
            }

            return readPointsFromScanner(sc, n);
        }
    }

    /**
     * Reads n points from a scanner.
     *
     * @param sc scanner containing point coordinates
     * @param n number of points expected
     * @return list of points
     * @throws IllegalArgumentException if fewer coordinates exist than expected
     */
    public static List<Point> readPointsFromScanner(final Scanner sc, final int n) {
        Objects.requireNonNull(sc, "scanner cannot be null");

        final List<Point> points = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            if (!sc.hasNextInt()) {
                throw new IllegalArgumentException("fewer coordinates than specified.");
            }
            final int x =  sc.nextInt();

            if (!sc.hasNextInt()) {
                throw new IllegalArgumentException("fewer coordinates than specified.");
            }
            final int y =  sc.nextInt();

            points.add(new Point(x, y));
        }

        return points;
    }

    /**
     * Reads points from a scanner.
     *
     * <p>Scanner format:</p>
     * <ul>
     *     <li>First integer: n (number of points)</li>
     *     <li>Then n pairs of integers: x y</li>
     * </ul>
     *
     * @param sc scanner containing point data
     * @return list of points
     * @throws IllegalArgumentException if the input format is invalid
     */
    public static List<Point> readPointsFromScanner(final Scanner sc) {
        Objects.requireNonNull(sc, "scanner cannot be null");

        if (!sc.hasNextInt()) {
            throw new IllegalArgumentException("missing number of points.");
        }

        final int n = sc.nextInt();

        if (n < 0) {
            throw new IllegalArgumentException("number of points cannot be negative.");
        }

        return readPointsFromScanner(sc, n);
    }

    /**
     * Writes points in the same numeric format:
     * first n, then n pairs of integers.
     *
     * @param points points to write
     * @param out destination writer
     * @throws NullPointerException if points or out is null
     * @throws NullPointerException if points contains a null element
     */
    public static void writePoints(final List<Point> points, final PrintWriter out) {
        Objects.requireNonNull(points, "points cannot be null");
        Objects.requireNonNull(out, "out cannot be null");

        out.println(points.size());

        for (Point p : points) {
            Objects.requireNonNull(p, "point cannot contain null elements");
            out.println(p.x + " " + p.y);
        }

        out.flush();
    }
}
