package de.skuzzle.semantic;

public class AbstractVersionPerformanceTest {

    private static final int WARM_UP = 11000;
    protected static final int RUN = 100000;

    public AbstractVersionPerformanceTest() {
        super();
    }

    private void warmUp(Runnable subject) {
        for (int i = 0; i < WARM_UP; ++i) {
            subject.run();
        }
    }

    protected void performTest(String description, int iterations, Runnable subject) {
    
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