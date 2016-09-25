package de.skuzzle.semantic;

import java.util.function.Function;

import org.junit.Test;

public class ParsingPerformanceTest {

    private static final int WARM_UP = 11000;
    private static final int RUN = 100000;
    private static final String TEST_STRING = "10.153.132-very.long-prelease.id.1234+with.build.md";

    @Test
    public void testNoRegex() throws Exception {
        performTest("Without regex", RUN, TEST_STRING,
                new Function<String, Version>() {

                    @Override
                    public Version apply(String t) {
                        return Version.parseVersion(t);
                    }
                });
    }

    @Test
    public void testWithRegex() throws Exception {
        performTest("With regex", RUN, TEST_STRING,
                new Function<String, VersionRegEx>() {

                    @Override
                    public VersionRegEx apply(String t) {
                        return VersionRegEx.parseVersion(t);
                    }
                });
    }

    private void warmUp(String s, Function<String, ?> producer) {
        for (int i = 0; i < WARM_UP; ++i) {
            producer.apply(s);
        }
    }

    private void performTest(String description, int iterations, String toParse,
            Function<String, ?> producer) {

        System.out.println("Test: " + description);
        warmUp(toParse, producer);
        long min = Long.MAX_VALUE;
        long max = 0;
        long sum = 0;
        for (int i = 0; i < iterations; ++i) {
            final long start = System.nanoTime();
            producer.apply(toParse);
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
