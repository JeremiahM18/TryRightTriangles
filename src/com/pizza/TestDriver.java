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
 * <p>This driver runs a set of concurrency tests that validate:</p>
 * <ul>
 *     <li>Blocking behavior of TakeAny / TakeVeg</li>
 *     <li>Vegetarian priority rules</li>
 *     <li>close() unblocking and return semantics</li>
 * </ul>
 *
 * <p>To test a different implementation, change exactly one line in {@link #createBuffet(int)}
 * to select {@code BuffetMonitor}, {@code BuffetSemaphore}, or {@code BuffetLock}.</p>
 *
 */
public final class TestDriver {

    /**
     * Short delay to give threads time to reach a blocked state.
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
        final int maxSlices = 20;

        // REQUIRED: choose implementation by changing ONE line in createBuffet()
        final Buffet buffet = createBuffet(maxSlices);

        log("=== TestDriver starting ===");

        // Each test gets a fresh instance to avoid cross-test interference
        testCloseUnblocks(maxSlices);
        testTakeAnyBlocksUntilAll(createBuffet(maxSlices));
        testVegPriorityBlocksTakeAny(createBuffet(maxSlices));

        // Test 4 needs a small capacity to reliably fill and block
        testAddPizzaBlocksWhenFullAndCloseReturnsFalse(createBuffet(3));

        log("=== ALL TESTS PASSED ===");
    }

    /**
     * Creates a fresh buffet instance so tests don't interfere with each other.
     *
     * @param maxSlices buffet capacity
     * @return a new buffet implementation instance
     */
    private static Buffet createBuffet(final int maxSlices) {
        // REQUIRED: single line to choose implementation:
        return new BuffetMonitor(maxSlices);
        // return new BuffetSemaphore(maxSlices);
        // return new BuffetLock(maxSlices);
    }

    /**
     * Test 1: {@link Buffet#close()} must unblock waiting calls and force return values:
     * <ul>
     *     <li>{@link Buffet#TakeAny(int)} returns null</li>
     *     <li>{@link Buffet#TakeVeg(int)} returns null</li>
     *     <li>{@link Buffet#AddPizza(int, SliceType)} returns false</li>
     * </ul>
     *
     * <p>Runs in two independent parts (fresh buffets) to avoid interference:</p>
     * <ul>
     *     <li>Part A blocks {@code TakeAny}/{@code TakeVeg} on an empty buffet</li>
     *     <li>Part B blocks {@code AddPizza} on a full buffet</li>
     * </ul>
     *
     * @param maxSlices capacity used by this buffet instance (keeps test readable)
     */
    private static void testCloseUnblocks(final int maxSlices) {
        log("[Test 1] close() unblocks TakeAny/TakeVeg/AddPizza");

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

        awaitOrFail(takeStarted, 1000, "Test 1A: taker threads did not start");

        // Give them time to enter the wait() loop
        sleepMs(SHORT_MS);
        require(any.isAlive(), "Test 1A: TakeAny thread should be blocked");
        require(veg.isAlive(), "Test 1A: TakeVeg thread should be blocked");

        takeBuffet.close();

        joinOrFail(any, LONG_MS);
        joinOrFail(veg, LONG_MS);

        require(anyResult.get() == null, "Test 1A: TakeAny should return null after close()");
        require(vegResult.get() == null, "Test 1A: TakeVeg should return null after close()");

        // Part B: AddPizza must unblock on close
        final Buffet addBuffet = createBuffet(maxSlices);

        // Fill buffet to capacity and attempt to add more while full
        require(addBuffet.AddPizza(maxSlices, SliceType.Cheese), "Test 1B: failed to prefill buffet");

        final AtomicReference<Boolean> addResult = new AtomicReference<>();

        final CountDownLatch addStarted = new CountDownLatch(1);

        final Thread add = new Thread(() -> {
            addStarted.countDown();
            // Large add will fill the buffer then block trying to add more
            addResult.set(addBuffet.AddPizza(100, SliceType.Cheese));
        }, "addPizza-blocker");

        add.start();
        awaitOrFail(addStarted, 1000, "Test 1B: add thread did not start");

        // Give threads time to block
        sleepMs(SHORT_MS);
        require(add.isAlive(), "Test 1B: addPizza thread should be blocked");

        addBuffet.close();

        joinOrFail(add, LONG_MS);
        require(addResult.get() != null && !addResult.get(),
                "Test 1B: AddPizza should return false after close()");

        log("[Test 1] Passed.");
    }

    /**
     * Test 2: TakeAny must block until it can return ALL desired slices.
     *
     * <p>Setup:</p>
     * <ul>
     *     <li>Add 1 slice</li>
     *     <li>Start TakeAny(2) -> must block</li>
     *     <li>Add 1 more slice -> TakeAny returns 2</li>
     * </ul>
     *
     * @param buffet buffet implementation under test
     */
    private static void testTakeAnyBlocksUntilAll(final Buffet buffet) {
        log("[Test 2] TakeAny blocks until it can return ALL desired slices");

        require(buffet.AddPizza(1, SliceType.Meat), "Test 2: AddPizza failed unexpectedly" );

        final AtomicReference<List<SliceType>> result = new AtomicReference<>();
        final CountDownLatch started = new CountDownLatch(1);

        final Thread t = new Thread(() -> {
            started.countDown();
            result.set(buffet.TakeAny(2));
        }, "takeAny-wants-2");

        t.start();
        awaitOrFail(started, 1000, "Test2: threads did not start");

        sleepMs(SHORT_MS);
        require(t.isAlive(), "Test2: TakeAny(2) should be blocked but thread ended early");

        require(buffet.AddPizza(1, SliceType.Meat), "Test 2: AddPizza failed unexpectedly");

        joinOrFail(t, LONG_MS);

        final List<SliceType> got = result.get();
        require(got != null, "Test2: expected non-null result");
        require(got.size() == 2, "Test2: expected 2 slices, but got " + got.size());

        buffet.close();
        log("[Test 2] Passed.");
    }

    /**
     * Test 3: Vegetarian priority.
     *
     * <p>If a vegetarian is waiting in TakeVeg(), then TakeAny must NOT remove
     * vegetarian slices (Cheese/Veggie). To test this:</p>
     * <ol>
     *     <li>Add 1 veg slice</li>
     *     <li>Start TakeVeg(2) -> blocks (veg waiter now exists)</li>
     *     <li>Start TakeAny(1) -> must block because only veg slices exist</li>
     *     <li>Add 1 meat slice -> TakeAny should return Meat</li>
     *     <li>Add 1 veg slice -> TakeVeg returns 2 veg slices</li>
     * </ol>
     *
     * @param buffet buffet implementation under test
     */
    private static void testVegPriorityBlocksTakeAny(final Buffet buffet) {
        log("[Test 3] Veg priority: TakeAny must not take veg while veg patron is waiting");

        require(buffet.AddPizza(1, SliceType.Cheese), "Test 3: AddPizza failed unexpectedly");

        final AtomicReference<List<SliceType>> vegResult = new AtomicReference<>();
        final AtomicReference<List<SliceType>> anyResult = new AtomicReference<>();

        final CountDownLatch vegStarted = new CountDownLatch(1);
        final Thread veg  = new Thread(() -> {
            vegStarted.countDown();
            vegResult.set(buffet.TakeVeg(2));
        }, "veg-wants-2");
        veg.start();
        awaitOrFail(vegStarted, 1000, "Test3: veg thread did not start");

        sleepMs(SHORT_MS);
        require(veg.isAlive(), "Test3: veg thread should be blocked");

        final CountDownLatch anyStarted = new CountDownLatch(1);
        final Thread any = new Thread(() -> {
            anyStarted.countDown();
            anyResult.set(buffet.TakeAny(1));
        }, "any-wants-1");
        any.start();
        awaitOrFail(anyStarted, 1000, "Test3: any thread did not start");

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
     * Test 4: AddPizza behavior.
     */
    private static void testAddPizzaBlocksWhenFullAndCloseReturnsFalse(final Buffet buffet) {
        log("[Test 4] AddPizza blocks when full; close() forces false");

        final AtomicReference<Boolean> addResult = new AtomicReference<>();
        final CountDownLatch started = new CountDownLatch(1);

        final Thread server = new Thread(() -> {
            started.countDown();
            addResult.set(buffet.AddPizza(10, SliceType.Meat));
        }, "server-add-10");

        server.start();
        awaitOrFail(started, 1000, "Test4: server did not start");

        sleepMs(SHORT_MS);
        require(server.isAlive(), "Test4: server should still be running (blocked after filling capacity)");

        // Consume exactly capacity to create room
        final List<SliceType> got = buffet.TakeAny(3);
        require(got != null && got.size() == 3, "Test 4: expected TakeAny(3) to succeed");

        // Close; server must return false
        buffet.close();
        joinOrFail(server, LONG_MS);

        Boolean ok = addResult.get();
        require(ok != null && !ok, "Test4: expected AddPizza to return false after close(), got " + ok);

        log("[Test 4] Passed.");
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
     * @param msg error message on timeout
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
