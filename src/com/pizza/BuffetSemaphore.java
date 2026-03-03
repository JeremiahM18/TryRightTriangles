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
import java.util.concurrent.Semaphore;

/**
 * Semaphore-based implementation of Buffet interface.
 *
 * <p>This implementation uses semaphores only (single-permit acquire/release)
 * to coordinate server threads (adding slices) and patron threads (taking slices).
 * Shared state (queue, counters, and closed flag) is protected by {@link #mutex}.</p>
 *
 * <p>Because semaphores do not provide condition variables, this class uses two
 * condition queues using semaphores: one for {@code TakeAny} patrons
 * ({@link #anyWake}/{@link #anyWaiters}) and one for {@code TakeVeg} patrons
 * ({@link #vegWake}/{@link #vegWaiters}). Waiters block on their respective
 * semaphore and are released when buffet state changes or the buffet closes.</p>
 *
 * <p>The buffet is modeled as a FIFO queue: the oldest slice is removed first.</p>
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
     * True once the buffet has been closed.
     */
    private boolean closed;

    /**
     * Number of vegetarian patrons currently waiting in TakeVeg().
     */
    private int waitingVeg;

    /**
     * Condition-style wakeup semaphore for patrons.
     *
     * <p>Patrons block on this when they cannot yet obtain all required slices.
     * When buffet state changes (pizza added / slices removed / close), "broadcast"
     * by releasing this semaphore once per blocked patron.</p>
     */
    private final Semaphore anyWake;

    /**
     * Condition-style wakeup semaphore for vegetarian patrons.
     *
     * <p>Vegetarian patrons block on this when there are not enough vegetarian
     * slices to satisfy {@code TakeVeg(desired)}.</p>
     */
    private final Semaphore vegWake;

    /**
     * Number of patron threads currently blocked on {@link #anyWake}.
     *
     * <p>Protected by {@link #mutex}. Used to implement broadcast wakeups
     * without non-blocking or multi-permit semaphore operations.</p>
     */
    private int anyWaiters;

    /**
     * Number of vegetarian patron threads currently blocked on {@link #vegWake}.
     *
     * <p>Protected by {@link #mutex}. Used to release waiting veg patrons on state
     * changes or close.</p>
     */
    private int vegWaiters;

    /**
     * Number of server threads currently blocked waiting for free buffet slots.
     *
     * <p>Protected by {@link #mutex}. Used so {@link #close()} can release enough
     * permits on {@link #spaceAvailable} to unblock servers and force false returns.</p>
     */
    private int addWaiters;

    /**
     * Cached count of vegetarian slices currently on the buffet.
     *
     * <p>Maintained under {@link #mutex} to allow O(1) availability checks.</p>
     */
    private int vegCount;

    /**
     * Cached count of non-vegetarian slices currently on the buffet.
     *
     * <p>Maintained under {@link #mutex} to allow O(1) availability checks.</p>
     */
    private int nonVegCount;

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

        this.closed = false;
        this.waitingVeg = 0;

        this.anyWake = new Semaphore(0);
        this.vegWake = new Semaphore(0);

        this.anyWaiters = 0;
        this.vegWaiters = 0;

        this.addWaiters = 0;

        this.vegCount = 0;
        this.nonVegCount = 0;
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
        if (desired < 0 || desired > maxSlices) {
            throw new IllegalArgumentException("desired must be between 0 and " + maxSlices + ".");
        }
        if (desired == 0) {
            return List.of();
        }

        lock();
        try {
            if(closed){
                signalWaitersLocked();
                return null;
            }

            while (!closed && eligibleAnyCountLocked() < desired){
                anyWaiters++;
                try {
                    unlock();
                    anyWake.acquireUninterruptibly();
                } finally {
                    lock();
                    anyWaiters--;
                }
            }

            if (closed) {
                signalWaitersLocked();
                return null;
            }

            final boolean restricted = waitingVeg > 0;
            final List<SliceType> out = new ArrayList<>(desired);

            int remaining = desired;
            final Deque<SliceType> rebuilt = new ArrayDeque<>(buffet.size());

            while (!buffet.isEmpty()) {
                final SliceType s = buffet.removeFirst();
                final boolean eligible = !restricted || !s.isVeg();

                if (remaining > 0 && eligible) {
                    out.add(s);
                    remaining--;
                    if (s.isVeg()) {
                        vegCount--;
                    } else {
                        nonVegCount--;
                    }
                    spaceAvailable.release();
                } else {
                    rebuilt.addLast(s);
                }
            }

            buffet.addAll(rebuilt);

            signalWaitersLocked();
            return out;
        } finally {
            unlock();
        }
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
        if (desired < 0 || desired > maxSlices) {
            throw new IllegalArgumentException("desired must be between 0 and " + maxSlices + ".");
        }
        if (desired == 0) {
            return List.of();
        }

        boolean countedAsWaiting = false;

        lock();
        try {
            if (closed){
                signalWaitersLocked();
                return null;
            }

            waitingVeg++;
            countedAsWaiting = true;

            while (!closed && vegCount < desired) {
                vegWaiters++;
                try {
                    unlock();
                    vegWake.acquireUninterruptibly();
                } finally {
                    lock();
                    vegWaiters--;
                }
            }

            if (closed) {
                signalWaitersLocked();
                return null;
            }

            final List<SliceType> out = new ArrayList<>(desired);
            int remaining = desired;
            final Deque<SliceType> rebuilt = new ArrayDeque<>(buffet.size());

            while (!buffet.isEmpty()) {
                final SliceType s = buffet.removeFirst();
                if (remaining > 0 && s.isVeg()) {
                    out.add(s);
                    remaining--;
                    vegCount--;
                    spaceAvailable.release();
                } else {
                    rebuilt.addLast(s);
                }
            }
            buffet.addAll(rebuilt);

            // State changes: wake blocked patrons to re-check conditions
            signalWaitersLocked();
            return out;

        } finally {
            if (countedAsWaiting){
                waitingVeg--;
            }
            // wake patrons to re-check
            signalWaitersLocked();
            unlock();
        }
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
        if (count < 0) {
            throw new IllegalArgumentException("count must be >= 0");
        }
        if (stype == null) {
            throw new NullPointerException("stype must not be null");
        }
        if (count == 0) {
            return true;
        }

        int remaining = count;

        while (remaining > 0) {

            // Register as an adder that may block, under mutex
            lock();
            try {
                if (closed) {
                    return false;
                }
                addWaiters++;
            } finally {
                unlock();
            }

            // Block until a slot is available
            spaceAvailable.acquireUninterruptibly();

            // Now released, unregister as a waiter and either add or fail on close
            lock();
            try {
                addWaiters--;

                if (closed) {
                    // Give the slot back and stop immediately
                    spaceAvailable.release();
                    return false;
                }

                // Add exactly one slice
                buffet.addLast(stype);
                if (stype.isVeg()) {
                    vegCount++;
                } else {
                    nonVegCount++;
                }
                remaining--;

                // Wake patrons who may now be satisfied
                signalWaitersLocked();
            } finally {
                unlock();
            }
        }

        return true;
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
     */
    @Override
    public void close() {
        lock();
        if (closed) {
            unlock();
            return;
        }
        closed = true;

        // Unblock blocked patrons
        signalWaitersLocked();

        // Unblock adders potentially stuck on spaceAvailable
        for (int i = 0; i < addWaiters; i++) {
            spaceAvailable.release();
        }

        unlock();
    }

// Helpers

    /**
     * Acquires mutual exclusion protecting all shared state.
     *
     * <p>Uses uninterruptible acquisition to satisfy "handle interrupts internally".</p>
     */
    private void lock() {
        mutex.acquireUninterruptibly();
    }

    /**
     * Releases mutual exclusion protecting all shared state.
     */
    private void unlock() {
        mutex.release();
    }

    /**
     * Wakes blocked patron threads so they can re-check eligibility.
     *
     * <p>Must be called while holding {@link #mutex}. If the buffet is closed,
     * this performs a true broadcast (releasing one permit per blocked waiter)
     * so all blocked patrons can return {@code null}. Otherwise, it releases at
     * most one permit for each waiter class (veg/any) to prompt progress without
     * stampeding.</p>
     */
    private void signalWaitersLocked(){
        // mutex MUST already be held

        // If closed, wake everybody (broadcast)
        if (closed) {
            for (int i = 0; i < anyWaiters; i++) {
                anyWake.release();
            }
            for (int i = 0; i < vegWaiters; i++) {
                vegWake.release();
            }
            return;
        }

        // Not closed: nudge at most one waiter from each queue to re-check conditions
        for (int i = 0; i < vegWaiters; i++) {
            vegWake.release();
        }
        for (int i = 0; i < anyWaiters; i++) {
            anyWake.release();
        }
    }

    /**
     * Computes the number of slices currently eligible for {@code TakeAny}.
     *
     * <p>If any vegetarian patrons are waiting, non-vegetarian patrons may only take
     * non-vegetarian slices (Meat/Works). Otherwise, they may take any slice type.</p>
     *
     * <p>Must be called while holding {@link #mutex}.</p>
     */
    private int eligibleAnyCountLocked() {
        // mutex MUST already be held
        if (waitingVeg > 0) {
            return nonVegCount;
        }
        return vegCount + nonVegCount;
    }
}
