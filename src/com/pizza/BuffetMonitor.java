/****************************************
 *
 * Author: Jeremiah McDonald
 * Assignment: Program 4 - Pizza Buffet Controller
 * Class: CSC-4180 Operating Systems
 *
 ***************************************/

package com.pizza;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Monitor-style (synchronized) implementation of the Buffet interface.
 *
 * <p>This class coordinates server threads (adding slices) and patron
 * threads (taking slices) using Java's intrinsic synchronization
 * (synchronized methods) along with wait() and notifyAll().</p>
 *
 * <p>The buffet is modeled as a FIFO queue where the oldest slice is
 * removed first (oldest slice at the head).</p>
 *
 * <p>All shared state is protected by the monitor of this object.
 * At all times, the number of slices on the buffet remains between
 * 0 and maxSlices inclusive.</p>
 */
public class BuffetMonitor implements Buffet {

    /**
     * Maximum number of slices allowed on the buffet at any time.
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
     * @throws IllegalArgumentException if desired is < 0 or desired > maxSlices
     */
    @Override
    public synchronized List<SliceType> TakeAny(final int desired){
        if (desired < 0 || desired > maxSlices) {
            throw new IllegalArgumentException("desired must be between 0 and " + maxSlices + ".");
        }
        if (closed) {
            return null;
        }
        if (desired == 0) {
            return new ArrayList<>(0);
        }

        while (!closed && countEligibleAnySlices() < desired) {
            try {
                wait();
            } catch (final InterruptedException ignored) {
                // Per interface: handle interrupts internally; continue waiting
            }
        }

        if (closed) {
            return null;
        }

        final List<SliceType> result = new ArrayList<>(desired);
        removeOldestEligibleAnySlices(desired, result);

        notifyAll();
        return result;
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
     * @throws IllegalArgumentException if desired is < 0 or desired > maxSlices
     */
    @Override
    public synchronized List<SliceType> TakeVeg(final int desired){
        if (desired < 0 || desired > maxSlices) {
            throw new IllegalArgumentException("desired must be between 0 and " + maxSlices + ".");
        }
        if (closed) {
            return null;
        }
        if (desired == 0) {
            return new ArrayList<>(0);
        }

        waitingVeg++;
        try {
            while (!closed && countVegOnBuffet() < desired) {
                try {
                    wait();
                } catch (InterruptedException ignored) {
                    // Per interface: handle interrupts internally; continue waiting
                }
            }

            if (closed) {
                return null;
            }

            // Remove the oldest vegetarian slices (FIFO among veg)
            final List<SliceType> result = new ArrayList<>(desired);
            removeOldestVegSlices(desired, result);

            return result;
        } finally {
            waitingVeg--;
            // waitingVeg affects TakeAny eligibility; wake everyone
            notifyAll();
        }
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
        if (count < 0) {
            throw new IllegalArgumentException("count must be >= 0.");
        }
        if (sType == null) {
            throw new NullPointerException("sType must not be null.");
        }
        if (closed) {
            return false;
        }
        if (count == 0){
            return true;
        }

        int remaining = count;

        while (remaining > 0) {

            // Wait while Full
            while (!closed && buffet.size() >= maxSlices) {
                try {
                    wait();
                } catch (final InterruptedException ignored) {
                    // Per interface: handle interrupts internally; keep waiting
                }
            }

            // If closed while waiting, return false immediately
            if (closed) {
                return false;
            }

            // At least one slot exists: add exactly one slice (atomic under monitor)
            buffet.addLast(sType);      // newest goes to tail; oldest remains at head
            remaining--;

            // wake takers/servers
            notifyAll();
        }
        return true;
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
        if (closed) {
            return;
        }
        closed = true;
        notifyAll();
    }

    /**
     * Counts vegetarian slices currently on the buffet without modifying it.
     *
     * @return number of vegetarian slices on the buffet
     */
    private int countVegOnBuffet() {
        int count = 0;
        for (final SliceType s : buffet) {
            if (s.isVeg()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Removes the oldest vegetarian slices from the buffet and appends them to out.
     *
     * <p>This preserves overall buffet order for remaining slices and ensures the
     * returned vegetarian slices are in oldest-first order among vegetarian slices.</p>
     *
     * @param desired number of vegetarian slices to remove
     * @param out list to append removed slices into
     */
    private void removeOldestVegSlices(final int desired, final List<SliceType> out) {
        int remaining = desired;

        // Preserve FIFO of all slices, pull out veg slices, and rebuild the deque for non-removed slices
        final Deque<SliceType> rebuilt = new ArrayDeque<>(buffet.size());

        while (!buffet.isEmpty()) {
            final SliceType s = buffet.removeFirst();
            if (remaining > 0 && s.isVeg()) {
                out.add(s);
                remaining--;
            } else {
                rebuilt.addLast(s);
            }
        }

        buffet.addAll(rebuilt);

        if (remaining != 0) {
            throw new IllegalStateException("Internal error: insufficient veg slices despite check.");
        }
    }

    /**
     * Counts slices currently eligible for TakeAny based on vegetarian waiters.
     *
     * @return number of eligible slices
     */
    private int countEligibleAnySlices() {
        final boolean vegRestricted = waitingVeg > 0;

        int count = 0;
        for (final SliceType s : buffet) {
            if (!vegRestricted || !s.isVeg()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Removes the oldest eligible slices for TakeAny and appends them to out.
     *
     * <p>If vegetarians are waiting, only non-veg slices are eligible.</p>
     *
     * @param desired number of slices to remove
     * @param out list to append removed slices into
     */
    private void removeOldestEligibleAnySlices(final int desired, final List<SliceType> out) {
        int remaining = desired;
        final boolean  vegRestricted = waitingVeg > 0;

        final Deque<SliceType> rebuilt = new ArrayDeque<>(buffet.size());

        while (!buffet.isEmpty()) {
            final SliceType s = buffet.removeFirst();
            final boolean eligible = !vegRestricted || !s.isVeg();

            if (remaining > 0 && eligible) {
                out.add(s);
                remaining--;
            } else {
                rebuilt.addLast(s);
            }
        }

        buffet.addAll(rebuilt);

        if (remaining != 0) {
            throw new IllegalStateException("Internal error: insufficient eligible slices despite check.");
        }
    }
}
