/****************************************
 *
 * Author: Jeremiah McDonald
 * Assignment: Program 3
 * Class: CSC-4180 Operating Systems
 *
 ***************************************/

package com.tryright;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PointStore implementation for text-encoded point files.
 *
 * <p>Each non-empty line must contain a pair of integers representing
 * the x and y coordinates of a point.</p>
 *
 * @author Jeremiah McDonald
 */
public final class TextPointStore implements PointStore {

    /**
     * X-coordinates of all points in file order.
     */
    private final int[] xs;

    /**
     * Y-coordinates of points in file order.
     */
    private final int[] ys;

    /**
     * Constructs a TextPointStore backed by a text point file.
     *
     * <p>All parsing and validation is performed during construction.
     * Once created, the store provides read-only access to the
     * parsed point data.</p>
     *
     * <p>This implementation loads all points into memory to provide
     * fast indexed access during triangle counting.</p>
     *
     * @param filename path to the text-encoded point file
     * @throws IOException if filename is null, unreadable, or contains malformed data
     */
    public TextPointStore(final String filename) throws IOException {
        if (filename == null) {
            throw new IOException("Filename cannot be null");
        }

        /*
         * Temporary lists allow dynamic growth while parsing the file.
         * Data is copied into fixed-size arrays after validation.
         */
        final List<Integer> xList = new ArrayList<>();
        final List<Integer> yList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                final String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                final String[] tokens = trimmed.split("\\s+");
                if (tokens.length != 2) {
                    throw new IOException(
                            "Invalid point at line " + lineNumber + ": expected 2 integers"
                    );
                }

                try {
                    xList.add(Integer.parseInt(tokens[0]));
                    yList.add(Integer.parseInt(tokens[1]));
                } catch (NumberFormatException e) {
                    throw new IOException(
                            "Non-integer value at line " + lineNumber,
                            e
                    );
                }
            }
        }

        // Parsing uses dynamic lists because the number of points is not known
        // in advance. After validation, the data is copied into arrays to provide
        // compact storage and fast indexed access
        xs = new int[xList.size()];
        ys = new int[yList.size()];

        for (int i = 0; i < xList.size(); i++) {
            xs[i] = xList.get(i);
            ys[i] = yList.get(i);
        }
    }

    /**
     * Returns the X coordinate of the point at the specified index.
     *
     * @param idx index of the point
     * @return X coordinate
     * @throws IndexOutOfBoundsException if idx is outside valid range
     */
    @Override
    public int getX(final int idx) {
        checkIndex(idx);
        return xs[idx];
    }

    /**
     * Returns the Y coordinate of the point at the specified index.
     *
     * @param idx index of the point
     * @return Y coordinate
     * @throws IndexOutOfBoundsException if idx is outside valid range
     */
    @Override
    public int getY(final int idx) {
        checkIndex(idx);
        return ys[idx];
    }

    /**
     * Returns the number of points stored in the data store.
     *
     * @return number of stored points
     */
    @Override
    public int numPoints() {
        return xs.length;
    }

    /**
     * Releases any system resources associated with this store.
     *
     * <p>No resources need to be released for text implementation.</p>
     */
    @Override
    public void close() {
        // nothing to close for text implementation
    }

    /**
     * Validates that the index is within the valid range.
     *
     * @param idx index to validate
     * @throws IndexOutOfBoundsException if index is out of range
     */
    private void checkIndex(final int idx) {
        if (idx < 0 || idx >= xs.length) {
            throw new IndexOutOfBoundsException("Index " + idx + " out of bounds");
        }
    }
}
