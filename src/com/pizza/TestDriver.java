/****************************************
 *
 * Author: Jeremiah McDonald
 * Assignment: Program 4 - Pizza Buffet Controller
 * Class: CSC-4180 Operating Systems
 *
 ***************************************/

package com.pizza;

import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test driver for buffet controller implementations.
 *
 * <p>This driver uses a set of concurrency tests to validate the required
 * blocking and shutdown semantics of the buffet controller.</p>
 *
 * <p>The specific implementation under test should be selected
 * by changing exactly one line in {@code main} to select {@code BuffetMonitor},
 * {@code BuffetSemaphore}, or {@code BuffetLock}.</p>
 *
 */
public class TestDriver {

    /**
     * Short delay used to give threads time to reach a blocked state.
     */
    private static final long SHORT_MS = 250;

    /**
     * Maximum time to wait for a thread to terminate before failing the test.
     */
    private static final long LONG_MS = 2000;

    /**
     * Prevents instantiation of this utility class.
     */
    private TestDriver() {
        // Utility class; no instances.
    }

    /**
     * Entry point for the buffet controller tests.
     *
     * <p>The first executable line must assign a {@link Buffet} reference
     * to exactly one implementation (monitor, semaphore, or lock). This
     * allows the same test harness to be reused across all solutions.</p>
     *
     * @param args command-line arguments (not used)
     */
    public static void main(final String[] args) {
        // REQUIRED: single line to choose implementation:
        final Buffet buffet = new BuffetMonitor(20);
        // final Buffet buffet = new BuffetSemaphore(20);
        // final Buffet buffet = new BuffetLock(20);

        log("=== TestDriver starting ===");

        try{
            testCloseUnblocks(buffet, 20);
        } finally {
            // Always close at the end, even if a test throws.
            buffet.close();
        }

        // Later: Run additional tests on fresh buffet instances so tests don't interfere

        log("=== ALL TESTS PASSED ===");
    }

    /**
     * Factory for creating a fresh buffet instance for tests that must not share state.
     *
     * @param maxSlices buffet capacity
     * @return a new buffet implementation instance
     */
    private static Buffet createBuffet(final int maxSlices) {
        return new BuffetMonitor(maxSlices);
        // return new BuffetSemaphore(maxSlices);
        // return new BuffetLock(maxSlices);
    }

    /**
     * Test 1: {@link Buffet#close()} must unblock threads and force return values:
     * <ul>
     *     <li>{@link Buffet#TakeAny(int)} returns null</li>
     *     <li>{@link Buffet#TakeVeg(int)} returns null</li>
     *     <li>{@link Buffet#AddPizza(int, SliceType)} returns false</li>
     * </ul>
     *
     * <p>This test puts threads into blocked states, calls close(), and
     * verifies they terminate with the correct return values.</p>
     *
     * @param buffet buffet implementation under test
     * @param maxSlices capacity used by this buffet instance (keeps test readable)
     */
    private static void testCloseUnblocks(final Buffet buffet, final int maxSlices) {
        log("[Test 1] close() unblocks TakeAny/TakeVeg/AddPizza");

        final AtomicReference<List<SliceType>> anyResult = new AtomicReference<>();
        final AtomicReference<List<SliceType>> vegResult = new AtomicReference<>();
        final AtomicReference<Boolean> addResult = new AtomicReference<>();

        final CountDownLatch started = new CountDownLatch(3);

        Thread any = new Thread(() -> {
            started.countDown();
            anyResult.set(buffet.TakeAny(6));   // empty, blocks until close
        }, "takeAny-blocker");

        Thread veg = new Thread(() -> {
            started.countDown();
            vegResult.set(buffet.TakeVeg(1));   // empty, blocks until close
        }, "takeVeg-blocker");

        Thread add = new Thread(() -> {
            started.countDown();
            addResult.set(buffet.AddPizza(100, SliceType.Cheese));  // fills then blocks until close
        }, "addPizza-blocker");

        any.start();
        veg.start();
        add.start();

        awaitOrFail(started, 1000, "Test1: threads did not start");

        // Give threads time to block
        sleepMs(SHORT_MS);

        buffet.close();

        joinOrFail(any, LONG_MS);
        joinOrFail(veg, LONG_MS);
        joinOrFail(add, LONG_MS);

        require(anyResult.get() == null, "Test1: TakeAny should return null after close()");
        require(vegResult.get() == null, "Test1: TakeVeg should return null after close()");
        require(addResult.get() != null && !addResult.get(),
                "Test1: AddPizza should return false after close()");

        log("[Test 1] Passed.");
    }

    // Helper Methods

    /**
     * Logs a message with a timestamp.
     *
     * @param msg message to log
     */
    private static void log(final String msg) {

        System.out.println(LocalTime.now() + " " + msg);
    }

    /**
     * Sleeps for the approximately {@code ms} milliseconds, handling interrupts internally.
     *
     * @param ms milliseconds to sleep
     */
    private static void sleepMs(final long ms) {
        final long end = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < end) {
            try {
                Thread.sleep(Math.min(50, end - System.currentTimeMillis()));
            } catch (final InterruptedException ignored) {
                // per assignment: handle interrupts internally.
            }
        }
    }

    /**
     * Joins a thread with a timeout and fails if it does not terminate.
     *
     * @param t thread to join
     * @param timeoutMs join timeout in milliseconds
     */
    private static void joinOrFail(final Thread t, final long timeoutMs) {
        try {
            t.join(timeoutMs);
        } catch (final InterruptedException ignored) {
            // Per assignment: handle interrupts internally (driver can still fail if thread is alive)
        }
        if (t.isAlive()) {
            throw new IllegalStateException("Thread did not terminate: " + t.getName());
        }
    }

    /**
     * Waits for a latch or throws if it does not reach zero within the timeout.
     *
     * @param latch latch to await
     * @param timeoutMs timeout in milliseconds
     * @param msg error messsage on timeout
     */
    private static void awaitOrFail(final CountDownLatch latch, final long timeoutMs, final String msg) {
        try {
            if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException(msg);
            }
        } catch (final InterruptedException ignored) {
            throw new IllegalStateException(msg + " (interrupted)");
        }
    }

    /**
     * Enforces a required condition for a test.
     *
     * @param condition condition that must be true
     * @param msgIfFalse message for the thrown exception if the condition is false
     */
    private static void require(final boolean condition, final String msgIfFalse) {
        if (!condition) {
            throw new IllegalStateException(msgIfFalse);
        }
    }
}
