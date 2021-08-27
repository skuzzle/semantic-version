package de.skuzzle.semantic;

import org.junit.jupiter.api.Test;

import com.vdurmont.semver4j.Semver;

public class ParsingPerformanceIT extends VersionPerformanceTestBase {

    private static final String TEST_STRING = "10.153.132-123a.sdfasd.asd.asdhd.124545f.very.long-prelease.012aid.1234+with.build.md.000112";

    @Test
    public void testNoRegex() throws Exception {
        performTest("Without regex", RUNS, () -> Version.parseVersion(TEST_STRING));
    }

    @Test
    public void testWithRegex() throws Exception {
        performTest("With regex", RUNS, () -> VersionRegEx.parseVersion(TEST_STRING));
    }

    @Test
    public void testJSemver() throws Exception {
        performTest("jsemver", RUNS, () -> com.github.zafarkhaja.semver.Version.valueOf(TEST_STRING));
    }

    @Test
    public void testsemver4j() throws Exception {
        performTest("semver4j", RUNS, () -> new Semver(TEST_STRING));
    }
}
