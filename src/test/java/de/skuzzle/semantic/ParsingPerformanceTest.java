package de.skuzzle.semantic;

import org.junit.Test;

public class ParsingPerformanceTest extends AbstractVersionPerformanceTest {

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
}
