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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link ReentrantLock}-based implementation of {@link Buffet} interface.
 *
 * <p>This implementation coordinates server threads (adding slices) and patron
 * threads (taking slices) using a {@link ReentrantLock} and {@link Condition}
 * variables. The buffet is modeled as a FIFO queue where the oldest slice is
 * removed first.</p>
 *
 *<p>Semantics enforced:</p>
 * <ul>
 *     <li>{@link #TakeAny(int)}/{@link #TakeVeg(int)} are all-or-nothing:
 *          each blocks until it can return <em>all</em> requested slices.</li>
 *     <li>{@link #AddPizza(int, SliceType)} makes partial-progress: it adds as
 *          many slices as it can and blocks when the buffet is full.</li>
 *     <li>Vegetarian priority: if any veg patron is waiting, {@link #TakeAny(int)}
 *          does not take vegetarian slices.</li>
 *     <li>{@link #close()} unblocks all waiters and forces {@code null}/{@code false}
 *          returns per the interface specification.</li>
 * </ul>
 *
 */
public class BuffetLock implements Buffet{

    /**
     * Maximum number of slices allowed on the buffet at any time.
     */
    private final int maxSlices;

    /**
     * FIFO buffer of slices (oldest slice at the head)
     */
    private final Deque<SliceType> buffet;

    /**
     * Lock protecting all shared state in this controller.
     */
    private final ReentrantLock lock;

    /**
     * Condition signaled when slices may be taken (availability changes).
     */
    private final Condition canTake;

    /**
     * Condition signaled when slices may be added (space becomes available).
     */
    private final Condition canAdd;

    /**
     * True once the buffet has been closed.
     */
    private boolean closed;

    /**
     * Number of vegetarian patrons currently waiting in {@link #TakeVeg(int)}.
     */
    private int waitingVeg;

    /**
     * Constructs a buffet controller with a fixed maximum capacity.
     *
     * @param maxSlices maximum number of slices allowed on buffet
     * @throws IllegalArgumentException if {@code maxSlices <= 0}
     */
    public BuffetLock(int maxSlices){
        if(maxSlices <= 0){
            throw new IllegalArgumentException("maxSlices must be greater than 0");
        }

        this.maxSlices = maxSlices;
        this.buffet = new ArrayDeque<>();

        this.lock = new ReentrantLock();
        this.canTake = this.lock.newCondition();
        this.canAdd = this.lock.newCondition();

        this.closed = false;
        this.waitingVeg = 0;
    }

    /**
     * Attempts to take the requested number of slices of any type.
     *
     * <p>This method must block until the requested number of eligible
     * slices can be returned or until the buffet is closed.</p>
     *
     * <p>If any vegetarian patrons are waiting, this method does not
     * remove vegetarian slices from the buffet.</p>
     *
     * @param desired the number of slices requested
     * @return a list containing exactly {@code desired} slices in FIFO order,
     *          or {@code null} if the buffet has been closed
     *
     * @throws IllegalArgumentException if {@code desired < 0} or {@code desired > maxSlices}
     */
    @Override
    public List<SliceType> TakeAny(final int desired) {
        if (desired < 0 || desired > maxSlices) {
            throw new IllegalArgumentException("desired must be between 0 and " + maxSlices + ".");
        }
        if (desired == 0) {
            return List.of();
        }

        lock.lock();
        try {
            if (closed) {
                return null;
            }

            while (!closed && eligibleAnyCountLocked() < desired) {
                // Handle interrupts internally
                canTake.awaitUninterruptibly();
            }

            if (closed) {
                return null;
            }

            final List<SliceType> out = new ArrayList<>(desired);
            removeOldestEligibleAnySlicesLocked(desired, out);

            // State changed: space available and/ot eligibility changed
            canAdd.signalAll();
            canTake.signalAll();
            return out;
        } finally {
            lock.unlock();
        }
    }


    /**
     * Attempts to take the requested number of vegetarian slices.
     *
     * <p>Vegetarian slices are defined as Cheese or Veggie.</p>
     *
     * <p>This method blocks until the requested number of vegetarian
     * slices can be returned or until the buffet is closed.</p>
     *
     * @param desired the number of vegetarian slices requested
     * @return a list containing exactly {@code desired} vegetarian slices
     *          or {@code null} if the buffet has been closed
     * @throws IllegalArgumentException if {@code desired < 0} or {@code desired > maxSlices}
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

        lock.lock();
        try {
            if (closed) {
                return null;
            }

            waitingVeg++;
            countedAsWaiting = true;

            while (!closed && countVegOnBuffetLocked() < desired) {
                // Handle interrupts internally
                canTake.awaitUninterruptibly();
            }

            if (closed) {
                return null;
            }

            final List<SliceType> out = new ArrayList<>(desired);
            removeOldestVegSlicesLocked(desired, out);

            // State changed: space available and TakeAny restriction may be affected
            canAdd.signalAll();
            canTake.signalAll();
            return out;
        } finally {
            if (countedAsWaiting) {
                waitingVeg--;
            }
            // waitingVeg affects TakeAny eligibility; wake everyone to re-check
            canTake.signalAll();
            lock.unlock();
        }
    }

    /**
     * Adds slices of the specified type to the buffet.
     *
     * <p>If insufficient space exists, then this method adds as many slices
     * as possible, then blocks until additional space becomes available or until
     * the buffet is closed.</p>
     *
     * @param count the number of slices to add
     * @param stype the type of slices being added
     * @return {@code true} if all slices were added successfully;
     *          {@code false} if the buffet was closed before completion
     * @throws IllegalArgumentException if {@code count < 0}
     * @throws NullPointerException if {@code stype} is null
     */
    @Override
    public boolean AddPizza(final int count, final SliceType stype) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be >= 0");
        }
        if (stype == null) {
            throw new NullPointerException("stype must not be null");
        }
        if (count ==0) {
            return true;
        }

        int remaining = count;

        lock.lock();
        try {
            if (closed) {
                return false;
            }

            while (remaining > 0) {
                while (!closed && buffet.size() >= maxSlices) {
                    // Handle interrupts internally
                    canAdd.awaitUninterruptibly();
                }

                if (closed) {
                    return false;
                }

                // Partial progress: add one slice per iteration
                buffet.addLast(stype);
                remaining--;

                // Wake takers who may now be satisfied
                canTake.signalAll();
            }

            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Closes the buffet and unblocks all waiting threads.
     *
     * <p>After closing:</p>
     * <ul>
     *     <li>Blocked {@link #TakeAny(int)} / {@link #TakeVeg(int)} calls return null.</li>
     *     <li>Blocked {@link #AddPizza(int, SliceType)} calls return {@code false}.</li>
     *     <li>Future calls do not block.</li>
     * </ul>
     */
    @Override
    public void close() {
       lock.lock();
       try {
           if (closed) {
               return;
           }
           closed = true;

           // Unblock everyone
           canTake.signalAll();
           canAdd.signalAll();
       } finally {
           lock.unlock();
       }
    }

    // Helpers (lock must be held)

    /**
     * Counts vegetarian slices currently on the buffet.
     *
     * <p>Caller must hold {@link #lock}.</p>
     *
     * @return number of vegetarian slices currently on the buffet
     */
    private int countVegOnBuffetLocked() {
        int count = 0;
        for (final SliceType s : buffet) {
            if (s.isVeg()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Counts slices eligible for {@link #TakeAny(int)} based on vegetarian waiters.
     *
     * <p>If {@link #waitingVeg} is greater than 0, only non-vegetarian slices are eligible.</p>
     *
     * <p>Caller must hold {@link #lock}.</p>
     *
     * @return number of slices currently eligible for {@code TakeAny}
     */
    private int eligibleAnyCountLocked() {
        final boolean restricted = waitingVeg > 0;

        int count = 0;
        for (final SliceType s : buffet) {
            if (!restricted || !s.isVeg()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Removes the oldest vegetarian slices from the buffet and appends them to {@code out}.
     *
     * <p>Preserves FIFO order for remaining slices.</p>
     *
     * <p>Caller must hold {@link #lock} and must have already verified availability.</p>
     *
     * @param desired number of vegetarian slices to remove
     * @param out destination list to receive removed slices
     * @throws IllegalStateException if the buffet does not contain enough vegetarian slices
     */
    private void removeOldestVegSlicesLocked(final int desired, final List<SliceType> out) {
        int remaining = desired;
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
            throw new IllegalStateException("Internal error: insufficient veg slices despite pre-check.");
        }
    }

    /**
     * Removes the oldest eligible slices for {@link #TakeAny(int)} and appends them to {@code out}.
     *
     * <p>If vegetarians are waiting, only non-vegetarian slices are eligible.</p>
     *
     * <p>Preserves FIFO order of the remaining slices.</p>
     *
     * <p>Caller must hold {@link #lock} and must have already verified availability.</p>
     *
     * @param desired number of slices to remove
     * @param out destination list to receive removed slices
     * @throws IllegalStateException if the buffet does not contain enough eligible slices
     */
    private void removeOldestEligibleAnySlicesLocked(final int desired, final List<SliceType> out) {
        int remaining = desired;
        final boolean restricted = waitingVeg > 0;

        final Deque<SliceType> rebuilt = new ArrayDeque<>(buffet.size());

        while (!buffet.isEmpty()) {
            final SliceType s = buffet.removeFirst();
            final boolean eligible = !restricted || !s.isVeg();

            if (remaining > 0 && eligible) {
                out.add(s);
                remaining--;
            } else {
                rebuilt.addLast(s);
            }
        }

        buffet.addAll(rebuilt);

        if (remaining != 0) {
            throw new IllegalStateException("Internal error: insufficient eligible slices despite pre-check.");
        }
    }
}
