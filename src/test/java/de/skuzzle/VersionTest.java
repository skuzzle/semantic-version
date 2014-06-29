package de.skuzzle;

import org.junit.Assert;
import org.junit.Test;

import de.skuzzle.Version.VersionFormatException;

public class VersionTest {

    public VersionTest() {}

    @Test
    public void testSimpleVersion() {
        final Version v = Version.of("1.2.3");
        Assert.assertEquals(1, v.getMajor());
        Assert.assertEquals(2, v.getMinor());
        Assert.assertEquals(3, v.getPatch());
        Assert.assertEquals("", v.getPreRelease());
        Assert.assertEquals("", v.getBuildMetaData());
    }

    @Test
    public void testSemVerOrgPreReleaseSamples() {
        final Version v1 = Version.of("1.0.0-alpha");
        Assert.assertEquals("alpha", v1.getPreRelease());

        final Version v2 = Version.of("1.0.0-alpha.1");
        Assert.assertEquals("alpha.1", v2.getPreRelease());

        final Version v3 = Version.of("1.0.0-0.3.7");
        Assert.assertEquals("0.3.7", v3.getPreRelease());

        final Version v4 = Version.of("1.0.0-x.7.z.92");
        Assert.assertEquals("x.7.z.92", v4.getPreRelease());
    }

    @Test
    public void testSemVerOrgBuildMDSamples() {
        final Version v1 = Version.of("1.0.0-alpha+001");
        Assert.assertEquals("alpha", v1.getPreRelease());
        Assert.assertEquals("001", v1.getBuildMetaData());

        final Version v2 = Version.of("1.0.0+20130313144700");
        Assert.assertEquals("20130313144700", v2.getBuildMetaData());

        final Version v3 = Version.of("1.0.0-beta+exp.sha.5114f85");
        Assert.assertEquals("beta", v3.getPreRelease());
        Assert.assertEquals("exp.sha.5114f85", v3.getBuildMetaData());
    }

    @Test
    public void testVersionWithBuildMD() {
        final Version v = Version.of("1.2.3+some.id.foo");
        Assert.assertEquals("some.id.foo", v.getBuildMetaData());
    }

    @Test
    public void testVersionWithBuildMD2() {
        final Version v = Version.of(1, 2, 3, "", "some.id-1.foo");
        Assert.assertEquals("some.id-1.foo", v.getBuildMetaData());
    }

    @Test(expected = VersionFormatException.class)
    public void testVersionWithBuildMDEmptyLastPart() {
        Version.of(1, 2, 3, "", "some.id.");
    }

    @Test(expected = VersionFormatException.class)
    public void testVersionWithBuildMDEmptyMiddlePart() {
        Version.of(1, 2, 3, "", "some..id");
    }

    @Test
    public void testVersionWithPreRelease() {
        final Version v = Version.of("1.2.3-pre.release-foo.1");
        Assert.assertEquals("pre.release-foo.1", v.getPreRelease());
        final String[] expected = { "pre", "release-foo", "1" };
        Assert.assertArrayEquals(expected, v.getPreReleaseParts());
    }

    @Test
    public void testVersionWithPreReleaseAndBuildMD() {
        final Version v = Version.of("1.2.3-pre.release-foo.1+some.id-with-hyphen");
        Assert.assertEquals("pre.release-foo.1", v.getPreRelease());
        Assert.assertEquals("some.id-with-hyphen", v.getBuildMetaData());
    }

    @Test(expected = VersionFormatException.class)
    public void testPreReleaseWithLeadingZeroes() {
        Version.of("1.2.3-pre.001");
    }

    @Test(expected = VersionFormatException.class)
    public void testPreReleaseWithLeadingZeroes2() {
        Version.of(1, 2, 3, "pre.001");
    }

    @Test(expected = VersionFormatException.class)
    public void testPreReleaseWithLeadingZero() {
        Version.of("1.2.3-pre.01");
    }

    @Test(expected = VersionFormatException.class)
    public void testPreReleaseWithLeadingZero2() {
        Version.of(1, 2, 3, "pre.01");
    }

    @Test(expected = VersionFormatException.class)
    public void testPreReleaseMiddleEmptyIdentifier() {
        Version.of("1.2.3-pre..foo");
    }

    @Test(expected = VersionFormatException.class)
    public void testPreReleaseLastEmptyIdentifier() {
        Version.of("1.2.3-pre.foo.");
    }

    @Test(expected = VersionFormatException.class)
    public void testPreReleaseLastEmptyIdentifier2() {
        Version.of(1, 2, 3, "pre.foo.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVersionAll0() {
        Version.of("0.0.0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVersionAll02() {
        Version.of(0, 0, 0);
    }

    @Test
    public void testPreReleaseWithLeadingZeroesIdentifier() {
        // leading zeroes allowed in string identifiers
        final Version v = Version.of("1.2.3-001abc");
        Assert.assertEquals("001abc", v.getPreRelease());
    }

    @Test
    public void testPreReleaseWithLeadingZeroesIdentifier2() {
        // leading zeroes allowed in string identifiers
        final Version v = Version.of(1, 2, 3, "001abc");
        Assert.assertEquals("001abc", v.getPreRelease());
    }

    @Test
    public void testNoPrecedenceChangeByBuildMD() {
        final Version v1 = Version.of("1.2.3+1.0");
        final Version v2 = Version.of("1.2.3+2.0");
        Assert.assertEquals(0, v1.compareTo(v2));
    }

    @Test
    public void testSimplePrecedence() {
        final Version v1 = Version.of("1.0.0");
        final Version v2 = Version.of("1.0.1");
        final Version v3 = Version.of("1.1.0");
        final Version v4 = Version.of("2.0.0");

        Assert.assertTrue(v1.compareTo(v2) < 0);
        Assert.assertTrue(v2.compareTo(v3) < 0);
        Assert.assertTrue(v3.compareTo(v4) < 0);
        Assert.assertTrue(v2.compareTo(v1) > 0);
        Assert.assertTrue(v3.compareTo(v2) > 0);
        Assert.assertTrue(v4.compareTo(v3) > 0);
    }

    @Test
    public void testPrecedencePreRelease() {
        final Version v1 = Version.of("1.0.0");
        final Version v2 = Version.of("1.0.0-rc1");
        Assert.assertTrue(v1.compareTo(v2) > 0);
        Assert.assertTrue(v2.compareTo(v1) < 0);
    }

    @Test
    public void testPrecedencePreRelease2() {
        final Version v1 = Version.of("1.0.0-rc1");
        final Version v2 = Version.of("1.0.0-rc1");
        Assert.assertTrue(v1.compareTo(v2) == 0);
    }

    @Test
    public void testPrecedencePreRelease3() {
        final Version v1 = Version.of("1.0.0-rc1");
        final Version v2 = Version.of("1.0.0-rc1.5");
        // the one with longer list is greater
        Assert.assertTrue(v1.compareTo(v2) < 0);
        Assert.assertTrue(v2.compareTo(v1) > 0);
    }

    @Test
    public void testPrecedencePreRelease4() {
        final Version v1 = Version.of("1.0.0-a");
        final Version v2 = Version.of("1.0.0-b");
        Assert.assertTrue(v1.compareTo(v2) < 0);
        Assert.assertTrue(v2.compareTo(v1) > 0);
    }

    @Test
    public void testPrecedencePreRelease5() {
        final Version v1 = Version.of("1.0.0-1");
        final Version v2 = Version.of("1.0.0-2");
        Assert.assertTrue(v1.compareTo(v2) < 0);
        Assert.assertTrue(v2.compareTo(v1) > 0);
    }

    @Test
    public void testPrecedencePreRelease6() {
        final Version v1 = Version.of("1.0.0-1.some.id-with-hyphen.a");
        final Version v2 = Version.of("1.0.0-1.some.id-with-hyphen.b");
        Assert.assertTrue(v1.compareTo(v2) < 0);
        Assert.assertTrue(v2.compareTo(v1) > 0);
    }

    @Test
    public void testInitialDevelopment() {
        final Version v1 = Version.of(0, 1, 0);
        final Version v2 = Version.of(1, 1, 0);
        Assert.assertTrue(v1.isInitialDevelopment());
        Assert.assertFalse(v2.isInitialDevelopment());
    }

    @Test
    public void testSemVerOrgPrecedenceSample() {
        final Version[] versions = {
                Version.of("1.0.0-alpha"),
                Version.of("1.0.0-alpha.1"),
                Version.of("1.0.0-alpha.beta"),
                Version.of("1.0.0-beta"),
                Version.of("1.0.0-beta.2"),
                Version.of("1.0.0-beta.11"),
                Version.of("1.0.0-rc.1"),
                Version.of("1.0.0"),
                Version.of("2.0.0"),
                Version.of("2.1.0"),
                Version.of("2.1.1")
        };

        for (int i = 1; i < versions.length; ++i) {
            final Version v1 = versions[i - 1];
            final Version v2 = versions[i];
            final int c = v1.compareTo(v2);
            Assert.assertTrue(v1 + " is not lower than " + v2, c < 0);
        }
    }
}
