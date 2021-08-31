package de.skuzzle.semantic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vdurmont.semver4j.Semver;

public class SortingPerformanceIT extends VersionPerformanceTestBase {

    private static final Random SHUFFLE_RANDOM = new Random(0);

    private List<String> sortMe;

    @BeforeEach
    public void setup() {
        final List<String> mutable = new ArrayList<>(
                List.of(
                        "1.0.0-alpha.beta",
                        "1.0.0-alpha",
                        "1.0.0-alpha.1",
                        "1.0.0-beta.11",
                        "1.0.0-beta",
                        "1.0.0-rc.1",
                        "1.0.0",
                        "1.0.0-beta.2",
                        "2.1.1",
                        "2.1.0",
                        "1.0.0-alpha.beta",
                        "2.0.0",
                        "1.0.0-alpha",
                        "1.0.0-rc.1",
                        "1.0.0-alpha.1",
                        "1.0.0-beta",
                        "1.0.0-beta.11",
                        "1.0.0",
                        "1.0.0-beta.2",
                        "2.1.0",
                        "2.0.0",
                        "2.1.1"));
        Collections.shuffle(mutable, SHUFFLE_RANDOM);
        sortMe = Collections.unmodifiableList(mutable);
    }

    @Test
    public void testOldVersion() throws Exception {
        performTest("Regex Impl", RUNS,
                () -> sortMe.stream()
                        .map(VersionRegEx::parseVersion)
                        .toArray(VersionRegEx[]::new),
                Arrays::sort);
    }

    @Test
    public void testCurrentVersion() throws Exception {
        performTest("Current Impl", RUNS,
                () -> sortMe.stream()
                        .map(Version::parseVersion)
                        .toArray(Version[]::new),
                Arrays::sort);
    }

    @Test
    public void testJSemver() throws Exception {
        performTest("JSemver", RUNS,
                () -> sortMe.stream()
                        .map(com.github.zafarkhaja.semver.Version::valueOf)
                        .toArray(com.github.zafarkhaja.semver.Version[]::new),
                Arrays::sort);
    }

    @Test
    public void testSemver4j() throws Exception {
        performTest("JSemver", RUNS,
                () -> sortMe.stream()
                        .map(Semver::new)
                        .toArray(Semver[]::new),
                Arrays::sort);
    }
}
