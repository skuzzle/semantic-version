package de.skuzzle.semantic;

import org.junit.Test;

public class ParsingPerformanceTest {

    private static final int WARM_UP = 11000;
    private static final int RUN = 100000;
    private static final String TEST_STRING = "10.153.132-very.long-prelease.id.1234+with.build.md";

    @Test
    public void testNoRegex() throws Exception {
        performTest("Without regex", RUN, new Runnable() {

            @Override
            public void run() {
                Version.parseVersion(TEST_STRING);
            }
        });
    }

    @Test
    public void testWithRegex() throws Exception {
        performTest("With regex", RUN, new Runnable() {

            @Override
            public void run() {
                VersionRegEx.parseVersion(TEST_STRING);
            }
        });
    }

    private void warmUp(Runnable subject) {
        for (int i = 0; i < WARM_UP; ++i) {
            subject.run();
        }
    }

    private void performTest(String description, int iterations, Runnable subject) {

        System.out.println("Test: " + description);
        warmUp(subject);
        long min = Long.MAX_VALUE;
        long max = 0;
        long sum = 0;
        for (int i = 0; i < iterations; ++i) {
            final long start = System.nanoTime();
            subject.run();
            final long time = System.nanoTime() - start;
            min = Math.min(min, time);
            max = Math.max(max, time);
            sum += time;
        }
        final long avg = sum / iterations;
        System.out.println("Min " + min);
        System.out.println("Max " + max);
        System.out.println("Avg " + avg);
        System.out.println();
    }
}
