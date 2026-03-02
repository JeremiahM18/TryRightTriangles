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
 * Test driver for {@link Buffet} controller implementations.
 *
 * <p>This driver validates core specification requirements:</p>
 * <ul>
 *     <li>{@link Buffet#TakeAny(int)} / {@link Buffet#TakeVeg(int)} block until they can return all desired slices</li>
 *     <li>Vegetarian priority: {@link Buffet#TakeAny(int)} does not take veg slices while a veg patron is waiting</li>
 *     <li>FIFO: returned slices are oldest-first</li>
 *     <li>{@link Buffet#AddPizza(int, SliceType)} makes partial progress and blocks when full</li>
 *     <li>{@link Buffet#close()} unblocks waiting calls and forces null/false return values</li>
 * </ul>
 *
 * <p>To test a different implementation, change exactly one line in {@link #main(String[])}
 * (the assignment to {@code selected}) to select {@code BuffetMonitor}, {@code BuffetSemaphore},
 * or {@code BuffetLock}. Each test uses {@link #createBuffet(int)} to create fresh instances of
 * the selected implementation.</p>
 *
 * <p>Error handling requirements: on failure, print details to stderr and exit with a non-zero code.</p>
 */
public final class TestDriver {

    /**
     * Delay to allow threads to reach a blocked state.
     */
    private static final long SHORT_MS = 300;

    /**
     * Maximum time to wait for a thread to terminate before failing the test.
     */
    private static final long LONG_MS = 2000;

    /**
     * Timeout for "thread started" latches.
     */
    private static final long START_LATCH_MS = 1000;

    /**
     * Exit code: all tests passed.
     */
    private static final int EXIT_OK = 0;

    /**
     * Exit code: a test assertion failed.
     */
    private static final int EXIT_TEST_FAILED = 2;

    /**
     * Exit code: a thread failed to terminate.
     */
    private static final int EXIT_TIMEOUT = 3;

    /**
     * Exit code: unexpected runtime failure in the driver itself.
     */
    private static final int EXIT_DRIVER_ERROR = 4;

    /**
     * Buffet implementation chosen in {@link #main(String[])}.
     */
    private static Buffet selected;

    /**
     * Prevents instantiation of this utility class.
     */
    private TestDriver() {
        // Utility class; no instances.
    }

    /**
     * Entry point for the buffet controller tests.
     *
     * <p>The implementation is selected by a single assignment in {@code main}
     * to (monitor, semaphore, or lock). This allows the same test harness to
     * be reused across all solutions.</p>
     *
     * @param args command-line arguments (not used)
     */
    public static void main(final String[] args) {

        final int maxSlices = 20;

        // REQUIRED: single line to choose implementation:
        selected = new BuffetMonitor(maxSlices);
        // selected = new BuffetSemaphore(maxSlices);
        // selected = new BuffetLock(maxSlices);

        log("=== TestDriver starting ===");

        try {
            // Each test creates fresh Buffet instances via createBuffet() to avoid cross-test interference
            testCloseUnblocksTakers(maxSlices);
            testCloseUnblocksAddPizza(maxSlices);
            testTakeAnyBlocksUntilAll(maxSlices);
            testVegPriorityBlocksTakeAny(maxSlices);
            testFifoOldestFirst(maxSlices);

            // Test 5 needs a small capacity to reliably fill and block
            testAddPizzaBlocksWhenFullAndCloseReturnsFalse(3);

            log("=== ALL TESTS PASSED ===");
            System.exit(EXIT_OK);
        } catch (final TestTimeoutException e) {
            error("TIMEOUT: " + e.getMessage(), e);
            System.exit(EXIT_TIMEOUT);
        } catch (final IllegalStateException e) {
            // Any require() failure is treated as a test failure
            error("TEST FAILED: " + e.getMessage(), e);
            System.exit(EXIT_TEST_FAILED);
        } catch (final RuntimeException e) {
            error("DRIVER ERROR: " + e.getMessage(), e);
            System.exit(EXIT_DRIVER_ERROR);
        }

    }

    /**
     * Creates a new buffet instance of the same implementation type selected in {@link #main(String[])}.
     *
     * <p>This prevents cross-test interference while keeping the selected implementation consistent
     * across all tests.</p>
     *
     * @param maxSlices maximum number of slices allowed on the buffet
     * @return a new {@link Buffet} implementation instance of the selected type
     * @throws IllegalStateException if the selected implementation type is unknown
     */
    private static Buffet createBuffet(final int maxSlices) {
        if (selected instanceof BuffetMonitor) {
            return new BuffetMonitor(maxSlices);
        }
        if (selected instanceof BuffetSemaphore) {
            return new BuffetSemaphore(maxSlices);
        }
        if (selected instanceof BuffetLock) {
            return new BuffetLock(maxSlices);
        }
        throw new IllegalStateException("Unknown Buffet implementation: " + selected);
    }

    /**
     * Test 1A: {@link Buffet#close()} must unblock threads blocked in {@link Buffet#TakeAny(int)}
     * and {@link Buffet#TakeVeg(int)} and force them to return null.
     * <ul>
     *     <li>{@link Buffet#TakeAny(int)} returns null</li>
     *     <li>{@link Buffet#TakeVeg(int)} returns null</li>
     * </ul>
     *
     * @param maxSlices buffet capacity for this test
     */
    private static void testCloseUnblocksTakers(final int maxSlices) {
        log("[Test 1A] close() unblocks TakeAny/TakeVeg and returns null");

        // Part A: TakeAny/TakeVeg must unblock on close
        final Buffet takeBuffet = createBuffet(maxSlices);

        final AtomicReference<List<SliceType>> anyResult = new AtomicReference<>();
        final AtomicReference<List<SliceType>> vegResult = new AtomicReference<>();

        // Ensures all threads have started before proceeding
        final CountDownLatch takeStarted = new CountDownLatch(2);

        final Thread any = new Thread(() -> {
            takeStarted.countDown();
            // Buffet starts empty; requesting maxSlices guarantees this blocks
            anyResult.set(takeBuffet.TakeAny(maxSlices));
        }, "takeAny-blocker");

        final Thread veg = new Thread(() -> {
            takeStarted.countDown();
            // Buffet starts empty; any positive desired blocks until close()
            vegResult.set(takeBuffet.TakeVeg(1));
        }, "takeVeg-blocker");

        any.start();
        veg.start();

        awaitOrFail(takeStarted, START_LATCH_MS, "Test 1A: taker threads did not start");

        // Give them time to enter the wait() loop
        sleepMs(SHORT_MS);
        require(any.isAlive(), "Test 1A: TakeAny thread should be blocked");
        require(veg.isAlive(), "Test 1A: TakeVeg thread should be blocked");

        takeBuffet.close();

        joinOrFail(any, LONG_MS);
        joinOrFail(veg, LONG_MS);

        require(anyResult.get() == null, "Test 1A: TakeAny should return null after close()");
        require(vegResult.get() == null, "Test 1A: TakeVeg should return null after close()");

        log("[Test 1A] Passed.");
    }

    /**
     * Test 1B: {@link Buffet#close()} must unblock {@link Buffet#AddPizza(int, SliceType)}
     * when the buffet is full and force it to return false.
     *
     * <p>Per spec, AddPizza adds as many as it can and then blocks when full. When closed,
     * AddPizza returns false immediately.</p>
     *
     * @param maxSlices buffet capacity for this test
     */
    private static void testCloseUnblocksAddPizza(final int maxSlices) {
        log("[Test 1B] close() unblocks AddPizza and returns false");

        final Buffet addBuffet = createBuffet(maxSlices);

        // Fill buffet to capacity and attempt to add more while full
        require(addBuffet.AddPizza(maxSlices, SliceType.Cheese), "Test 1B: failed to prefill buffet");

        final AtomicReference<Boolean> addResult = new AtomicReference<>();
        final CountDownLatch addStarted = new CountDownLatch(1);

        final Thread server = new Thread(() -> {
            addStarted.countDown();
            // Buffer is full; must block until close()
            addResult.set(addBuffet.AddPizza(1, SliceType.Meat));
        }, "addPizza-blocker");

        server.start();
        awaitOrFail(addStarted, START_LATCH_MS, "Test 1B: add thread did not start");

        // Give threads time to block
        sleepMs(SHORT_MS);
        require(server.isAlive(), "Test 1B: addPizza thread should be blocked");

        addBuffet.close();

        joinOrFail(server, LONG_MS);
        require(Boolean.FALSE.equals(addResult.get()),
                "Test 1B: AddPizza should return false after close()");

        log("[Test 1B] Passed.");
    }

    /**
     * Test 2: {@link Buffet#TakeAny(int)} blocks until it can return ALL desired slices
     * (does not take partial results).
     *
     * <p>Validates FIFO order for the returned list.</p>
     *
     * @param maxSlices buffet capacity for this test
     */
    private static void testTakeAnyBlocksUntilAll(final int maxSlices) {
        log("[Test 2] TakeAny blocks until it can return ALL desired slices");

        final Buffet buffet = createBuffet(maxSlices);

        require(buffet.AddPizza(1, SliceType.Meat), "Test 2: AddPizza failed unexpectedly" );

        final AtomicReference<List<SliceType>> result = new AtomicReference<>();
        final CountDownLatch started = new CountDownLatch(1);

        final Thread t = new Thread(() -> {
            started.countDown();
            result.set(buffet.TakeAny(2));
        }, "takeAny-wants-2");

        t.start();
        awaitOrFail(started, START_LATCH_MS, "Test2: threads did not start");

        sleepMs(SHORT_MS);
        require(t.isAlive(), "Test2: TakeAny(2) should be blocked but thread ended early");

        require(buffet.AddPizza(1, SliceType.Veggie), "Test 2: AddPizza failed unexpectedly");

        joinOrFail(t, LONG_MS);

        final List<SliceType> got = result.get();
        require(got != null, "Test2: expected non-null result");
        require(got.size() == 2, "Test2: expected 2 slices, got " + got.size());

        require(got.get(0) == SliceType.Meat, "Test2: expected first slice Meat, got " +  got.get(0));
        require(got.get(1) == SliceType.Veggie, "Test 2: expected second slice Veggie, got " + got.get(1));

        buffet.close();
        log("[Test 2] Passed.");
    }

    /**
     * Test 3: Vegetarian priority.
     *
     * <p>If a vegetarian patron is waiting in {@link Buffet#TakeVeg(int)}, then
     * {@link Buffet#TakeAny(int)} must not take vegetarian slices (Cheese/Veggie).</p>
     *
     * @param maxSlices buffet capacity for this test
     */
    private static void testVegPriorityBlocksTakeAny(final int maxSlices) {
        log("[Test 3] Veg priority: TakeAny must not take veg while veg patron is waiting");

        final Buffet buffet = createBuffet(maxSlices);

        require(buffet.AddPizza(1, SliceType.Cheese), "Test 3: AddPizza failed unexpectedly");

        final AtomicReference<List<SliceType>> vegResult = new AtomicReference<>();
        final AtomicReference<List<SliceType>> anyResult = new AtomicReference<>();

        final CountDownLatch vegStarted = new CountDownLatch(1);
        final Thread veg  = new Thread(() -> {
            vegStarted.countDown();
            vegResult.set(buffet.TakeVeg(2));
        }, "veg-wants-2");
        veg.start();

        awaitOrFail(vegStarted, START_LATCH_MS, "Test3: veg thread did not start");

        sleepMs(SHORT_MS);
        require(veg.isAlive(), "Test3: veg thread should be blocked");

        final CountDownLatch anyStarted = new CountDownLatch(1);
        final Thread any = new Thread(() -> {
            anyStarted.countDown();
            anyResult.set(buffet.TakeAny(1));
        }, "any-wants-1");
        any.start();

        awaitOrFail(anyStarted, START_LATCH_MS, "Test3: any thread did not start");

        sleepMs(SHORT_MS);
        require(any.isAlive(), "Test3: TakeAny(1) should block because only veg exists and veg is waiting");

        require(buffet.AddPizza(1, SliceType.Meat), "Test3: AddPizza failed unexpectedly");

        joinOrFail(any, LONG_MS);

        final List<SliceType> gotAny = anyResult.get();
        require(gotAny != null && gotAny.size() ==1, "Test3: TakeAny should return exactly one slice");
        require(!gotAny.get(0).isVeg(), "Test3: TakeAny returned a veg slice while veg was waiting: " + gotAny.get(0));

        require(veg.isAlive(), "Test3: veg should still be blocked (needs 2 veg slices)");

        // Add a vegetarian slice to satisfy TakeVeg(2)
        require(buffet.AddPizza(1, SliceType.Veggie), "Test3: AddPizza failed unexpectedly");

        joinOrFail(veg, LONG_MS);

        final List<SliceType> gotVeg = vegResult.get();
        require(gotVeg != null && gotVeg.size() == 2, "Test3: TakeVeg should return 2 slices");
        require(gotVeg.get(0).isVeg() && gotVeg.get(1).isVeg(), "Test3: TakeVeg returned non-veg slices: " + gotVeg);

        buffet.close();
        log("[Test 3] Passed.");
    }

    /**
     * Test 4: FIFO ordering for {@link Buffet#TakeAny(int)}.
     *
     * @param maxSlices buffet capacity for this test
     */
    private static void testFifoOldestFirst(final int maxSlices) {
        log("[Test 4] FIFO: TakeAny returns slices oldest-first");

        final Buffet buffet = createBuffet(maxSlices);

        require(buffet.AddPizza(1, SliceType.Meat), "Test 4: AddPizza Meat failed");
        require(buffet.AddPizza(1, SliceType.Cheese), "Test 4: AddPizza Cheese failed");
        require(buffet.AddPizza(1, SliceType.Veggie), "Test 4: AddPizza Veggie failed");

        final List<SliceType> got = buffet.TakeAny(3);
        require(got != null && got.size() == 3, "Test 4: expected TakeAny(3) to return 3 slices");

        require(got.get(0) == SliceType.Meat, "Test 4: expected [0]=Meat, got " + got.get(0));
        require(got.get(1) == SliceType.Cheese, "Test 4: expected [1]=Cheese, got " + got.get(1));
        require(got.get(2) == SliceType.Veggie, "Test 4: expected [2]=Veggie, got " + got.get(2));

        buffet.close();
        log("[Test 4] Passed.");
    }

    /**
     * Test 5: AddPizza partial-progress semantics, blocking when full, and close forces false.
     *
     * <p>AddPizza adds as many slices as it can and then blocks when the buffet is full. This test validates:</p>
     *
     * <ul>
     *     <li>The server fills the buffet</li>
     *     <li>The server remains blocked attempting to add remaining slices</li>
     *     <li>{@link Buffet#close()} forces AddPizza to return false</li>
     * </ul>
     *
     * @param capacity small capacity to make the test deterministic
     */
    private static void testAddPizzaBlocksWhenFullAndCloseReturnsFalse(final int capacity) {
        log("[Test 5] AddPizza partial progress then blocks; close() forces false");

        final Buffet buffet = createBuffet(capacity);

        final AtomicReference<Boolean> addResult = new AtomicReference<>();
        final CountDownLatch started = new CountDownLatch(1);

        final Thread server = new Thread(() -> {
            started.countDown();
            addResult.set(buffet.AddPizza(10, SliceType.Meat));
        }, "server-add-10");

        server.start();
        awaitOrFail(started, START_LATCH_MS, "Test 5: server thread did not start");

        // Blocks until at least 'capacity' slices exist; observe partial progress externally
        final List<SliceType> got = buffet.TakeAny(capacity);
        require(got != null && got.size() == capacity, "Test 5: expected " + capacity + ", got " + got);

        for (int i = 0; i < got.size(); i++) {
            require(got.get(i) == SliceType.Meat, "Test 5: expected Meat at index " + i + ", got " + got.get(i));
        }

        // With no further patrons, server cannot complete AddPizza(10)
        sleepMs(SHORT_MS);
        require(server.isAlive(), "Test 5: server should still be running (blocked after filling capacity)");

        // Close; server must return false
        buffet.close();
        joinOrFail(server, LONG_MS);

        require(Boolean.FALSE.equals(addResult.get()), "Test 5: expected AddPizza to return false after close()");

        log("[Test 5] Passed.");
    }

    // Helper Methods

    /**
     * Logs a message with a timestamp to stdout.
     *
     * @param msg message to log
     */
    private static void log(final String msg) {
        System.out.println(LocalTime.now() + " " + msg);
    }

    /**
     * Logs an error message to stderr.
     *
     * @param msg error message
     * @param t throwable cause
     */
    private static void error(final String msg, final Throwable t) {
        System.err.println(LocalTime.now() + " " + msg);
        if (t != null) {
            t.printStackTrace(System.err);
        }
    }

    /**
     * Sleeps approximately {@code ms} milliseconds, handling interrupts internally.
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
            // Per assignment: handle interrupts internally
        }
        if (t.isAlive()) {
            throw new TestTimeoutException("Thread did not terminate: " + t.getName());
        }
    }

    /**
     * Waits for a latch or throws if it does not reach zero within the timeout.
     *
     * @param latch latch to await
     * @param timeoutMs timeout in milliseconds
     * @param msg error message on timeout
     */
    private static void awaitOrFail(final CountDownLatch latch, final long timeoutMs, final String msg) {
        try {
            if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                throw new TestTimeoutException(msg);
            }
        } catch (final InterruptedException ignored) {
            throw new TestTimeoutException(msg + " (interrupted)");
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

    /**
     * Exception type used to distinguish timeouts/deadlocks from ordinary test failures.
     */
    private static final class TestTimeoutException extends RuntimeException {

        /**
         * Constructs a timeout exception with a message.
         *
         * @param msg timeout message
         */
        TestTimeoutException(final String msg) {
            super(msg);
        }
    }
}
