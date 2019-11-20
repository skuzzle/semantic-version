package de.skuzzle.semantic;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class IncrementationTest {
    
    @Test
    void testToStableIsAlreadyStable() throws Exception {
        final Version v = Version.create(1, 2, 3).withBuildMetaData("build");
        final Version stable = v.toStable();
        assertEquals(v, stable);
        assertFalse(stable.hasBuildMetaData());
    }
    
    @Test
    void testToStableDropIdentifiers() throws Exception {
        final Version v = Version.create(1, 2, 3).withPreRelease("SNAPSHOT").withBuildMetaData("build");
        final Version stable = v.toStable();
        final Version expected = Version.create(1, 2, 3);
        assertEquals(expected, stable);
        assertFalse(stable.hasBuildMetaData());
    }

    @Test
    public void testIncrementMajor() throws Exception {
        final Version v = Version.create(1, 2, 3, "pre-release", "build");
        assertEquals(Version.create(2, 0, 0), v.nextMajor());
    }

    @Test
    public void testIncrementMajorWithPreReleaseString() throws Exception {
        final Version v = Version.create(1, 2, 3, "pre-release", "build");
        assertEquals(Version.create(2, 0, 0, "new.pre.release"),
                v.nextMajor("new.pre.release"));
    }

    @Test
    public void testIncrementMajorWithPreReleaseArray() throws Exception {
        final Version v = Version.create(1, 2, 3, "pre-release", "build");
        assertEquals(Version.create(2, 0, 0, "new.pre.release"),
                v.nextMajor(new String[] { "new", "pre", "release" }));
    }

    @Test
    public void testIncrementMajorWithPreReleaseStringNull() throws Exception {
        final Version v = Version.create(1, 2, 3, "pre-release", "build");
        assertThrows(IllegalArgumentException.class, () -> v.nextMajor((String) null));
    }

    @Test
    public void testIncrementMajorWithPreReleaseArrayNull() throws Exception {
        final Version v = Version.create(1, 2, 3, "pre-release", "build");
        assertThrows(IllegalArgumentException.class, () -> v.nextMajor((String[]) null));
    }

    @Test
    public void testIncrementMinor() throws Exception {
        final Version v = Version.create(1, 2, 3, "pre-release", "build");
        assertEquals(Version.create(1, 3, 0), v.nextMinor());
    }

    @Test
    public void testIncrementMinorWithPreReleaseString() throws Exception {
        final Version v = Version.create(1, 2, 3, "pre-release", "build");
        assertEquals(Version.create(1, 3, 0, "new.pre.release"),
                v.nextMinor("new.pre.release"));
    }

    @Test
    public void testIncrementMinorWithPreReleaseArray() throws Exception {
        final Version v = Version.create(1, 2, 3, "pre-release", "build");
        assertEquals(Version.create(1, 3, 0, "new.pre.release"),
                v.nextMinor(new String[] { "new", "pre", "release" }));
    }

    @Test
    public void testIncrementMinorWithPreReleaseStringNull() throws Exception {
        final Version v = Version.create(1, 2, 3, "pre-release", "build");
        assertThrows(IllegalArgumentException.class, () -> v.nextMinor((String) null));
    }

    @Test
    public void testIncrementMinorWithPreReleaseArrayNull() throws Exception {
        final Version v = Version.create(1, 2, 3, "pre-release", "build");
        assertThrows(IllegalArgumentException.class, () -> v.nextMinor((String[]) null));
    }

    @Test
    public void testIncrementPatch() throws Exception {
        final Version v = Version.create(1, 2, 3, "pre-release", "build");
        assertEquals(Version.create(1, 2, 4), v.nextPatch());
    }

    @Test
    public void testIncrementPatchWithPreReleaseString() throws Exception {
        final Version v = Version.create(1, 2, 3, "pre-release", "build");
        assertEquals(Version.create(1, 2, 4, "new.pre.release"),
                v.nextPatch("new.pre.release"));
    }

    @Test
    public void testIncrementPatchWithPreReleaseArray() throws Exception {
        final Version v = Version.create(1, 2, 3, "pre-release", "build");
        assertEquals(Version.create(1, 2, 4, "new.pre.release"),
                v.nextPatch(new String[] { "new", "pre", "release" }));
    }

    @Test
    public void testIncrementPatchWithPreReleaseStringNull() throws Exception {
        final Version v = Version.create(1, 2, 3, "pre-release", "build");
        assertThrows(IllegalArgumentException.class, () -> v.nextPatch((String) null));
    }

    @Test
    public void testIncrementPatchWithPreReleaseArrayNull() throws Exception {
        final Version v = Version.create(1, 2, 3, "pre-release", "build");
        assertThrows(IllegalArgumentException.class, () -> v.nextPatch((String[]) null));
    }

    @Test
    public void testIncrementPreReleaseEmpty() throws Exception {
        final Version v = Version.create(1, 2, 3, "", "build");
        assertEquals(Version.create(1, 2, 3, "1"), v.nextPreRelease());
    }

    @Test
    public void testIncrementPreReleaseNumeric() throws Exception {
        final Version v = Version.create(1, 2, 3, "pre-release.1", "build");
        assertEquals(Version.create(1, 2, 3, "pre-release.2"), v.nextPreRelease());
    }

    @Test
    public void testIncrementPreReleaseNonNumeric() throws Exception {
        final Version v = Version.create(1, 2, 3, "pre-release", "build");
        assertEquals(Version.create(1, 2, 3, "pre-release.1"), v.nextPreRelease());
    }

    @Test
    public void testIncrementBuildMDEmpty() throws Exception {
        final Version v = Version.create(1, 2, 3, "pre-release", "");
        assertEquals(Version.create(1, 2, 3, "pre-release", "1"),
                v.nextBuildMetaData());
    }

    @Test
    public void testIncrementBuildMDNumeric() throws Exception {
        final Version v = Version.create(1, 2, 3, "pre-release", "build.1");
        assertEquals(Version.create(1, 2, 3, "pre-release", "build.2"),
                v.nextBuildMetaData());
    }

    @Test
    public void testIncrementBuildMDNonNumeric() throws Exception {
        final Version v = Version.create(1, 2, 3, "pre-release", "build");
        assertEquals(Version.create(1, 2, 3, "pre-release", "build.1"),
                v.nextBuildMetaData());
    }
}
