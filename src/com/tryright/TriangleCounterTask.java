/****************************************
 *
 * Author: Jeremiah McDonald
 * Assignment: Program 2
 * Class: CSC-4180 Operating Systems
 *
 ***************************************/

package com.tryright;

import java.util.List;
import java.util.Objects;

/**
 * Runnable task that counts right triangles for a subset
 * of pivot points using shared memory.
 *
 * <p>This class represents a unit of work and is intentionally
 * decoupled from thread creation and scheduling.</p>
 *
 * @author Jeremiah McDonald
 */
public class TriangleCounterTask implements Runnable{

    /**
     * Shared, immutable list of all input points.
     */
    private final List<Point> points;

    /**
     * Starting pivot index
     */
    private final int startIndex;

    /**
     * Ending pivot index
     */
    private final int endIndex;

    /**
     * Shared array used to store partial results from each task.
     */
    private final long[] partialCounts;

    /**
     * Index into the shared partialCounts array where this
     * task stores its result.
     */
    private final int resultIndex;

    /**
     * Constructs a TriangleCounterTask.
     *
     * @param points shared list of points
     * @param startIndex starting pivot index
     * @param endIndex ending pivot index
     * @param partialCounts shared array for partial results
     * @param resultIndex index for this task's result
     *
     * @throws NullPointerException if points or partialCounts is null
     * @throws IllegalArgumentException if indices are invalid
     */
    public TriangleCounterTask(
            final List<Point> points,
            final int startIndex,
            final int endIndex,
            final long[] partialCounts,
            final int resultIndex){

        this.points = Objects.requireNonNull(points, "points cannot be null");
        this.partialCounts = Objects.requireNonNull(partialCounts,  "partialCounts cannot be null");

        // Parameter validation deferred to implementation phase
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.resultIndex = resultIndex;
    }

    /**
     * Executes the triangle counting task.
     *
     * <p>Implementation with count right triangles for pivot points
     * in the range [startIndex, endIndex) and store result in
     * partialCounts[resultIndex].</p>
     */
    @Override
    public void run() {
        long count = RightTriangleCounter.countRightTriangles(points, startIndex, endIndex);

        partialCounts[resultIndex] = count;
    }
}
