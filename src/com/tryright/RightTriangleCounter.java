/*************************************
 *
 * Author: Jeremiah McDonald
 * Assignment: Program 1
 * Class: CSC-4180 Operating Systems
 *
 ************************************/

package com.tryright;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Counts the number of right triangles that can be formed from a list of points.
 *
 * <p>A triangle is counted when the vectors from a pivot point to two other points
 * are perpendicular.</p>
 *
 * @author Jeremiah McDonald
 */
public final class RightTriangleCounter {

    /**
     * Prevents installation.
     */
    private RightTriangleCounter() {
        // Utility class
    }

    /**
     * Counts right triangles by treating each point as the right-angle vertex.
     *
     * @param points list of points to evaluate
     * @return total number of right triangles
     * @throws NullPointerException if points is null or contains a null element
     */
    public static long countRightTriangles(final List<Point> points) {
        Objects.requireNonNull(points, "points cannot be null.");
        return countRightTriangles(points, 0, points.size());
    }

    /**
     * Counts right triangles where the right-angle vertex index {@code i} is in the range
     * {@code [startInclusive, endExclusive)}.
     *
     * @param points list of points to evaluate
     * @param startInclusive first pivot index (inclusive)
     * @param endExclusive last pivot index (exclusive)
     * @return number of right triangles in the specified pivot range
     * @throws NullPointerException if points is null or contains a null element
     * @throws IllegalArgumentException if the pivot index range is invalid
     */
    public static long countRightTriangles(final List<Point> points,
                                           final int startInclusive,
                                           final int endExclusive) {
        Objects.requireNonNull(points, "points cannot be null");

        if (startInclusive < 0 || endExclusive < startInclusive || endExclusive > points.size()) {
            throw new IllegalArgumentException("invalid pivot index range.");
        }

        long total = 0;

        // Each pivot point p is treated as the potential right-angle vertex.
        for (int i = startInclusive; i < endExclusive; i++) {
            final Point p = Objects.requireNonNull(points.get(i), "points cannot contain null elements");

            // Map normalized direction vectors to number of points in that direction.
            final Map<Dir, Integer> counts = new HashMap<>();

            // Build direction counts relative to p
            for (int j = 0; j < points.size(); j++) {
                if (i == j) {
                    continue;
                }

                final Point q = Objects.requireNonNull(points.get(j), "points cannot contain null elements");

                final int dx = q.x - p.x;
                final int dy = q.y - p.y;

                final Dir d = normalize(dx, dy);
                counts.put(d, counts.getOrDefault(d, 0) + 1);
            }

            // For each direction, multiply with its perpendicular
            long local = 0;

            for (Map.Entry<Dir, Integer> entry : counts.entrySet()) {
                final Dir d = entry.getKey();
                final int countD = entry.getValue();

                final Dir perp1 = new Dir(-d.dy, d.dx);
                final Dir perp2 = new Dir(d.dy, -d.dx);

                final Integer countPerp1 = counts.get(perp1);
                final Integer countPerp2 = counts.get(perp2);

                if (countPerp1 != null) {
                    local += (long) countD * countPerp1;
                }
                if (countPerp2 != null) {
                    local += (long) countD * countPerp2;
                }
            }

            // Each perpendicular pair counted twice, so divide by 2
            total += local / 2;
        }

        return total;
    }

    /**
     * Converts a vector (dx, dy) into a normalized direction by dividing by {@code gcd(|dx|,|dy|)}.
     *
     * @param dx x-change
     * @param dy y-change
     * @return normalized direction
     */
    private static Dir normalize(int dx, int dy) {
        final int g = gcd(Math.abs(dx), Math.abs(dy));
        dx /= g;
        dy /= g;
        return new Dir(dx, dy);
    }

    /**
     * Computes the GCD using the Euclidean algorithm.
     *
     * @param a first non-negative integer
     * @param b second non-negative integer
     * @return greatest common divisor(a, b), or 1 if both are 0
     */
    private static int gcd(int a, int b) {
        while (b != 0) {
            final int t = a % b;
            a = b;
            b = t;
        }

        // If both dx and dy are 0, don't divide by 0 return 1
        return (a == 0) ? 1 : a;
    }
}
