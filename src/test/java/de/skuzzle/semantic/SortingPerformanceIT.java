package de.skuzzle.semantic;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vdurmont.semver4j.Semver;

public class SortingPerformanceIT extends AbstractVersionPerformanceTest {

    private String[] sortMe;

    @BeforeEach
    public void setup() {
        this.sortMe = new String[] {
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
                "2.1.1",
        };
    }

    @Test
    public void testOldVersion() throws Exception {
        performTest("Old Impl", RUN, () -> {
            return Arrays.stream(this.sortMe)
                    .map(VersionRegEx::parseVersion)
                    .toArray(length -> new VersionRegEx[length]);
        }, unsorted -> {
            Arrays.sort(unsorted);
        });
    }

    @Test
    public void testCurrentVersion() throws Exception {
        performTest("Current Impl", RUN, () -> {
            return Arrays.stream(this.sortMe)
                    .map(Version::parseVersion)
                    .toArray(length -> new Version[length]);
        }, unsorted -> {
            Arrays.sort(unsorted);
        });
    }

    @Test
    public void testJSemver() throws Exception {
        performTest("JSemver", RUN, () -> {
            return Arrays.stream(this.sortMe)
                    .map(com.github.zafarkhaja.semver.Version::valueOf)
                    .toArray(l -> new com.github.zafarkhaja.semver.Version[l]);
        }, unsorted -> {
            Arrays.sort(unsorted);
        });
    }

    @Test
    public void testSemver4j() throws Exception {
        performTest("JSemver", RUN, () -> {
            return Arrays.stream(this.sortMe)
                    .map(Semver::new)
                    .toArray(length -> new Semver[length]);
        }, unsorted -> {
            Arrays.sort(unsorted);
        });
    }
}
