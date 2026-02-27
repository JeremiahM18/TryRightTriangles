/****************************************
 *
 * Author: Jeremiah McDonald
 * Assignment: Program 4 - Pizza Buffet Controller
 * Class: CSC-4180 Operating Systems
 *
 ***************************************/

package com.pizza;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Monitor-style (synchronized) implementation of the Buffet interface.
 *
 * <p>This implementation uses Java's intrinsic synchronization
 * (synchronized methods) along with wait() and notifyAll()
 * to coordinate server and patron threads.</p>
 *
 * <p>All shared state is protected by the monitor of this object.
 * At all times, the nuber of slices on the buffet remains between
 * 0 and maxSlices inclusive.</p>
 */
public class BuffetMonitor implements Buffet {

    /**
     * Maximum number of slices allowed on the buffet.
     */
    private final int maxSlices;

    /**
     * FIFO buffer of slices (oldest at head).
     */
    private final Deque<SliceType> buffet;

    /**
     * True once the buffet has been closed.
     */
    private boolean closed;

    /**
     * Number of vegetarian patrons currently waiting in TakeVeg().
     */
    private int waitingVeg;

    /**
     * Constructs a buffet controller with fixed maximum capacity.
     *
     * @param maxSlices maximum number of slices allowed on the buffet
     * @throws IllegalArgumentException if {@code maxSlices <= 0}
     */
    public BuffetMonitor(final int maxSlices) {
        if (maxSlices <= 0) {
            throw new IllegalArgumentException("maxSlices must be greater than 0.");
        }
        this.maxSlices = maxSlices;
        this.buffet = new ArrayDeque<>(maxSlices);
        this.closed = false;
        this.waitingVeg = 0;
    }

    /**
     * Attempts to take the requested number of slices of any type.
     *
     * <p>This method blocks until the requested number of eligible
     * slices are available or until the buffet is closed.</p>
     *
     * <p>If vegetarian patrons are currently waiting, this method
     * must not remove vegetarian slices from the buffet.</p>
     *
     * @param desired the number of slices requested
     * @return a list containing exactly {@code desired} slices in
     *          FIFO order, or null if the buffet has been closed
     * @throws IllegalArgumentException if desired is negative
     */
    @Override
    public synchronized List<SliceType> TakeAny(final int desired){
        // TODO: validate desired
        // TODO: block until desired eligible slices available or closed
        // TODO: enforce vegetarian priority rule
        return null;
    }

    /**
     * Attempts to take the requested number of vegetarian slices.
     *
     * <p>Vegetarian slices are defined as Cheese or Veggie.</p>
     *
     * <p>This method blocks until the requested number of vegetarian
     * slices are available or until the buffet is closed.</p>
     *
     * @param desired the number of vegetarian slices requested
     * @return a list containing exactly {@code desired} vegetarian slices
     *          in FIFO order, or null if the buffet has been closed
     * @throws IllegalArgumentException if desired is negative
     */
    @Override
    public synchronized List<SliceType> TakeVeg(final int desired){
        // TODO: validate desired
        // TODO: increment waitingVeg before blocking
        // TODO: block until desired veg slices available or closed
        return null;
    }

    /**
     * Adds slices of the specified type to the buffet.
     *
     * <p>If insufficient space exists, this method adds as many slices
     * as possible and then blocks until additional space becomes available
     * or until the buffet is closed.</p>
     *
     * @param count the number of slices to add
     * @param sType the type of slices being added
     * @return true if all slices were added successfully,
     *          false if the buffet was closed before completion
     * @throws IllegalArgumentException if count is negative
     * @throws NullPointerException if sType is null
     */
    @Override
    public synchronized boolean AddPizza(final int count, final SliceType sType) {
        // TODO: validate parameters
        // TODO: add as  many as possible
        // TODO: block for remaining slices if necessary
        return false;
    }

    /**
     * Closes the buffet.
     *
     * <p>After closing:
     * <ul>
     *     <li>All blocked threads are awakened.</li>
     *     <li>Blocked TakeAny and TakeVeg calls return null.</li>
     *     <li>Blocked AddPizza calls return false.</li>
     *     <li>Future calls do not block.</li>
     * </ul>
     * </p>
     */
    @Override
    public synchronized void close() {
        // TODO: set closed to true
        // TODO: notifyAll waiting threads

    }
}
