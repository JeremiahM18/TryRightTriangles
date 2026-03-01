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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Stress test driver for the buffet  controller implementations.
 *
 * <p>This driver is intended to exercise the Buffet interface using
 * multiple threads that simulate servers (adding slices) and patrons
 * (taking slices). The specific implementation under test should be
 * selected by changing exactly one line in {@code main}.</p>
 *
 * <p>Test logic will be added in stages to validate blocking behavior,
 * ordering, capacity constraints, vegetarian priority, and close semantics.</p>
 */
public class TestDriver {

    /**
     * Prevents instantiation of this utility class.
     */
    private TestDriver() {
        // Utility class; no instances.
    }

    /**
     * Entry point for the buffet stress test.
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
            runPhase1Sanity(buffet);
            // run Phase2BlockingTests(buffet);
            // run Phase3StressTest(buffet);
        } finally {
            // Ensure close even if a phase throws.
            buffet.close();
        }
    }

    /**
     * Phase 1: Minimal sanity checks to confirm basic wiring.
     *
     * <p>This phase avoid assumptions about full correctness. It
     * verifies that the driver can start threads, call methods, and close
     * cleanly.</p>
     *
     * @param buffet buffet implementation under test
     */
    private static void runPhase1Sanity(final Buffet buffet) {
        log("[Phase 1] Sanity: force blocking, then close and ensure threads exit.");

        final AtomicBoolean stop = new AtomicBoolean(false);

        // Force a patron to block: buffet starts empty, desired=maxSlices(20)
        final Thread patron = new Thread(
                new AnyPatron(buffet, stop, 20),
                "patron-any-1"
        );

        // Server adds in batches; fill the buffet and eventually blocks trying to add more
        final Thread server = new Thread(
                new Server(buffet, stop, 5, SliceType.Cheese),
                "server-1"
        );

        // Start patron first to ensure it's waiting
        patron.start();
        sleepMs(150);
        server.start();

        // Let them run long enough to reach a blocked state
        sleepMs(800);

        log("[Phase 1] Closing buffet (must unblock waiting threads).");
        buffet.close();         // Per interface: unblocks TakeAny/AddPizza and causes null/false returns
        stop.set(true);         // Stops loops for threads that are not blocked

        joinOrFail(server, 2000);
        joinOrFail(patron, 2000);

        log("[Phase 1] Completed.");
    }

    // Runnables

    /**
     * Server thread that repeatedly attempts to add pizza slices.
     */
    private static final class Server implements Runnable {

        private final Buffet buffet;
        private final AtomicBoolean stop;
        private final int batchSize;
        private final SliceType type;

        /**
         * Constructs a server worker.
         *
         * @param buffet buffet controller to add slices to
         * @param stop stop flag used to end the thread
         * @param batchSize number of slices to attempt per AddPizza call
         * @param type slice type to add
         */
        private Server(final Buffet buffet,
                       final AtomicBoolean stop,
                       final int batchSize,
                       final SliceType type) {
            this.buffet = buffet;
            this.stop = stop;
            this.batchSize = batchSize;
            this.type = type;
        }

        @Override
        public void run() {
            while (!stop.get()) {
                final boolean ok = buffet.AddPizza(batchSize, type);
                if (!ok) {
                    log(Thread.currentThread().getName() + " AddPizza returned false (closed).");
                    return;
                }
                sleepMs(50);
            }
            log(Thread.currentThread().getName() + " stopping normally.");
        }
    }

    /**
     * Non-vegetarian patron that repeatedly attempts to take any slices.
     */
    private static final class AnyPatron implements Runnable {

        private final Buffet buffet;
        private final AtomicBoolean stop;
        private final int desired;

        /**
         * Constructs a non-vegetarian patron worker.
         *
         * @param buffet buffet controller to take slices from
         * @param stop stop flag used to end the thread
         * @param desired number of slices per TakeAny call
         */
        private AnyPatron(final Buffet buffet,
                          final AtomicBoolean stop,
                          final int desired) {
            this.buffet = buffet;
            this.stop = stop;
            this.desired = desired;
        }

        @Override
        public void run() {
            while (!stop.get()) {
                final List<SliceType> got = buffet.TakeAny(desired);
                if (got == null) {
                    log(Thread.currentThread().getName() + " TakeAny returned null (closed).");
                    return;
                }
                sleepMs(50);
            }
            log(Thread.currentThread().getName() + " stopping normally.");
        }
    }

    // Test utilities

    /**
     * Logs a message with a timestamp.
     *
     * @param msg message to log
     */
    private static void log(final String msg) {
        System.out.println(LocalTime.now() + " " + msg);
    }

    /**
     * Sleeps for the requested duration, ignoring interrupts.
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
     * Joins a thread or fails fast if it does not terminate.
     *
     * @param t thread to join
     * @param timeoutMs join timeout in milliseconds
     */
    private static void joinOrFail(final Thread t, final long timeoutMs) {
        try {
            t.join(timeoutMs);
        } catch (final InterruptedException ignored) {
            // Treat as not fatal for the driver; check liveness.
        }
        if (t.isAlive()) {
            throw new IllegalStateException("Thread did not terminate: " + t.getName());
        }
    }
}
