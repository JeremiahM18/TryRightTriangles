/****************************************
 *
 * Author: Jeremiah McDonald
 * Assignment: Program 4 - Pizza Buffet Controller
 * Class: CSC-4180 Operating Systems
 *
 ***************************************/

package com.pizza;

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
        // final Buffet Buffet = new BuffetLock(20);

        // TODO: create server threads and patron threads
        // TODO: run long enough to observe blocking/unblocking behavior
        // TODO: close the buffet and verify all threads terminate cleanly
    }
}
