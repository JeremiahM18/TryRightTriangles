/****************************************
 *
 * Author: Jeremiah McDonald
 * Assignment: Program 3
 * Class: CSC-4180 Operating Systems
 *
 ***************************************/

package com.tryright;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;

/**
 * PointStore implementation for binary-encoded point files
 * accessed via memory-mapped I/O.
 *
 * <p>Each point is stored as a pair of 4-byte big-endian integers:
 * X followed by Y.</p>
 *
 * <p>The file must contain complete (x,y) pairs and its size must be
 * a multiple of 8 bytes.</p>
 *
 * @author Jeremiah McDonald
 */
public class BinPointStore implements PointStore {

    /**
     * Number of bytes per integer.
     */
    private static final int INT_BYTES = 4;

    /**
     * Number of bytes per point (x, y).
     */
    private static final int POINT_BYTES = 8;

    /**
     * Memory-mapped buffer for file contents.
     */
    private final MappedByteBuffer mappedByteBuffer;

    /**
     * File channel backing the mapped buffer.
     */
    private final FileChannel fileChannel;

    /**
     * RandomAccessFile backing the channel.
     */
    private final RandomAccessFile randomAccessFile;

    /**
     * Total number of points in the file.
     */
    private final int pointCount;

    /**
     * Constructs a BinPointStore backed by a binary file.
     *
     * @param filename path to the binary-encoded point file
     *
     * @throws IllegalArgumentException if filename is null or file is invalid
     * @throws RuntimeException if file cannot be mapped
     */
    public BinPointStore(final String filename) {
        Objects.requireNonNull(filename, "filename cannot be null");

        try {
            File file = new File(filename);

            if (!file.exists()) {
                throw new IllegalArgumentException("File does not exist: " + filename);
            }
            if (!file.isFile()) {
                throw new IllegalArgumentException("Not a file: " + filename);
            }
            if (!file.canRead()) {
                throw new IllegalArgumentException("File cannot be read: " + filename);
            }

            randomAccessFile = new RandomAccessFile(file, "r");
            fileChannel = randomAccessFile.getChannel();

            long fileSize = fileChannel.size();

            if (fileSize % POINT_BYTES != 0) {
                throw new  IllegalArgumentException(
                        "Invalid binary point file: size not multiple of " + POINT_BYTES + " bytes");
            }

            if (fileSize > Integer.MAX_VALUE) {
                throw new  IllegalArgumentException(
                        "File too large to memory-map"
                );
            }

            pointCount = (int) (fileSize /  POINT_BYTES);

            // Memory-Map the entire file for fast random access and OS-level caching.
            // Avoids copying file contents into the JVM heap and improves performance for large datasets
            mappedByteBuffer = fileChannel.map(
                    FileChannel.MapMode.READ_ONLY,
                    0,
                    fileSize
            );
        } catch (IOException e) {
            throw new RuntimeException("Error opening or mapping file", e);
        }
    }

    /**
     * Returns the X coordinate of the point at the specified index.
     *
     * @param idx index of the point
     * @return X coordinate
     * @throws IndexOutOfBoundsException if idx is invalid
     */
    @Override
    public int getX(final int idx) {
        checkIndex(idx);
        int position = idx * POINT_BYTES;
        return mappedByteBuffer.getInt(position);
    }

    /**
     * Returns the Y coordinate of the point at the specified index.
     *
     * @param idx index of the point
     * @return Y coordinate
     * @throws IndexOutOfBoundsException if idx is invalid
     */
    @Override
    public int getY(final int idx) {
        checkIndex(idx);
        int position = idx * POINT_BYTES + INT_BYTES;
        return mappedByteBuffer.getInt(position);
    }

    /**
     * Returns the number of points stores in the file.
     *
     * @return number of points available
     */
    @Override
    public int numPoints() {
        return pointCount;
    }

    /**
     * Releases system resources associated with the file.
     */
    @Override
    public void close () {
        try {
            fileChannel.close();
            randomAccessFile.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing binary point store", e);
        }
    }

    /**
     * Validates index is within the valid range.
     */
    private void checkIndex (final int idx) {
        if (idx < 0 || idx >= pointCount) {
            throw new IndexOutOfBoundsException(
                    "Index " + idx + " out of bounds for " + pointCount + " points");
        }
    }
}
