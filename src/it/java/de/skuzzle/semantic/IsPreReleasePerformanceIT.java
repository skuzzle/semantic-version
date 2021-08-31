package de.skuzzle.semantic;

import org.junit.jupiter.api.Test;

public class IsPreReleasePerformanceIT extends VersionPerformanceTestBase {

    private static final String INVALID_PRE_RELEASE = "very.long-prelease.id.1234.01";

    @Test
    public void testIsNoPreReleaseWithRegex() throws Exception {
        performTest("Is no pre-release with regex", RUNS, new Runnable() {

            @Override
            public void run() {
                VersionRegEx.isValidPreRelease(INVALID_PRE_RELEASE);
            }
        });
    }

    @Test
    public void testIsNoPreRelease() throws Exception {
        performTest("Is no pre-release without regex", RUNS, new Runnable() {

            @Override
            public void run() {
                Version.isValidPreRelease(INVALID_PRE_RELEASE);
            }
        });
    }
}
