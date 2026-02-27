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
import java.util.concurrent.Semaphore;

/**
 * Semaphore-based implementation of Buffet interface.
 *
 * <p>This class coordinates server threads (adding slices) and patron
 * threads (taking slices) using Java semaphores. The buffet is modeled
 * as a FIFO queue where the oldest slice is removed first.</p>
 *
 * <p>Semaphore usage constraints for this assignment:
 * only single-permit operations are allowed (acquire()/release()).
 * Multiple-permit and non-blocking acquisition calls are not used.</p>
 *
 * <p>Concurrency logic is intentionally deferred for later development.</p>
 *
 */
public class BuffetSemaphore implements Buffet {

    /**
     * Maximum number of slices allowed on the buffet at any time.
     */
    private final int maxSlices;

    /**
     * FIFO buffer of slices (oldest slice at the head).
     */
    private final Deque<SliceType> buffet;

    /**
     * Mutual exclusion semaphore protecting all shared state
     */
    private final Semaphore mutex;

    /**
     * Counting semaphore tracking available capacity (free slots).
     */
    private final Semaphore spaceAvailable;

    /**
     * Counting semaphore tracking total slices currently available.
     */
    private final Semaphore sliceAvailable;

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
     * @param maxSlices maximum number of slices allowed on buffet
     * @throws IllegalArgumentException if maxSlices <= 0
     */
    public BuffetSemaphore(final int maxSlices) {
        if (maxSlices <= 0) {
            throw new IllegalArgumentException("maxSlices must be positive");
        }
        this.maxSlices = maxSlices;
        this.buffet = new ArrayDeque<>();

        this.mutex = new Semaphore(1);
        this.spaceAvailable = new Semaphore(maxSlices);
        this.sliceAvailable = new Semaphore(0);

        this.closed = false;
        this.waitingVeg = 0;
    }

    /**
     * Attempts to take the requested number of slices of any type.
     *
     * <p>This method must block until the requested number of eligible
     * slices can be returned or until the buffet is closed.</p>
     *
     * <p>If any vegetarian patrons are waiting, this method must not
     * remove vegetarian slices from the buffet.</p>
     *
     * @param desired the number of slices requested
     * @return a list containing exactly {@code desired} slices in FIFO order,
     *          or null if the buffet has been closed
     * @throws IllegalArgumentException if desired is negative
     */
    @Override
    public List<SliceType> TakeAny(final int desired){
        // TODO: validate desired
        // TODO: block until desired eligible slices are available or buffet is closed
        // TODO: enforce vegetarian-priority rule using waitingVeg
        // TODO: return slices in FIFO order
        return null;
    }

    /**
     * Attempts to take the requested number of vegetarian slices.
     *
     * <p>Vegetarian slices are defined as Cheese or Veggie.</p>
     *
     * <p>This method must block until the requested number of vegetarian
     * slices can be returned or until the buffet is closed.</p>
     *
     * @param desired the number of vegetarian slices requested
     * @return a list containing exactly {@code desired} vegetarian slices
     *          or null if the buffet has been closed
     * @throws IllegalArgumentException if desired is negative
     */
    @Override
    public List<SliceType> TakeVeg(final int desired){
        // TODO: validate desired
        // TODO: increment waitingVeg before blocking; decrement after unblocking
        // TODO: block until desired vegetarian slices are available or buffet is closed
        // TODO: return slices in FIFO order
        return null;
    }

    /**
     * Adds slices of the specified type to the buffet.
     *
     * <p>If insufficient space exists, then this method must add as many slices
     * as possible, then block until additional space becomes available or until
     * the buffet is closed.</p>
     *
     * @param count the number of slices to add
     * @param stype the type of slices being added
     * @return true if all slices were added successfully, false if the buffet
     *          was closed before completion
     * @throws IllegalArgumentException if count is negative
     * @throws NullPointerException if stype is null
     */
    @Override
    public boolean AddPizza(final int count, final SliceType stype) {
        // TODO: validate count and stype
        // TODO: add slices up to capacity; block if remaining slices cannot be added yet
        // TODO: return false if closed occurs before completion
        return false;
    }

    /**
     * Closes the buffet.
     *
     * <p>After closing:
     * <ul>
     *     <li>Blocked TakeAny and TakeVeg calls must return null.</li>
     *     <li>Blocked AddPizza calls must return false.</li>
     *     <li>Future calls must not block.</li>
     * </ul>
     * </p>
     *
     */
    @Override
    public void close() {
        // TODO: mark closed and wake any blocked threads as required by the semaphore design
    }
}
