/*************************************
 *
 * Author: Jeremiah McDonald
 * Assignment: Program 1
 * Class: CSC-4180 Operating Systems
 *
 ************************************/

package com.tryright;

/**
 * Represents a 2D point with integer coordinates.
 *
 * @author Jeremiah McDonald
 */
public final class Point {

    /**
     * X-coordinate of the point.
     */
    public final int x;

    /**
     * Y-coordinate of the point.
     */
    public final int y;

    /**
     * Creates a point with the given coordinates.
     *
     * @param x the x-coordinate
     * @param y the y-coordinate
     */
    public Point(final int x, final int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Returns a readable string representation of this point.
     *
     * @return formatted string representation
     */
    @Override
    public String toString() {
        return "Point{x=" + x + ", y=" + y + "}";
    }
}
