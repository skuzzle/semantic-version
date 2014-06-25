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
    public void testVersionWithBuildMD() {
        final Version v = Version.of("1.2.3+some.id.foo");
        Assert.assertEquals("some.id.foo", v.getBuildMetaData());
    }
    
    @Test
    public void testVersionWithBuildMD2() {
        final Version v = Version.of(1, 2, 3, "", "some.id.foo");
        Assert.assertEquals("some.id.foo", v.getBuildMetaData());
    }
    
    @Test
    public void testVersionWithPreRelease() {
        final Version v = Version.of("1.2.3-pre.release-foo.1");
        Assert.assertEquals("pre.release-foo.1", v.getPreRelease());
        final String[] expected = {"pre", "release-foo", "1" };
        Assert.assertArrayEquals(expected, v.getPreReleaseParts());
    }

    @Test
    public void testVersionWithPreReleaseAndBuildMD() {
        final Version v = Version.of("1.2.3-pre.release-foo.1+some.id");
        Assert.assertEquals("pre.release-foo.1", v.getPreRelease());
        Assert.assertEquals("some.id", v.getBuildMetaData());
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
    public void testPreReleaseWithEmptyIdentifier() {
        Version.of("1.2.3-pre..foo");
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
    public void testInitialDevelopment() {
        final Version v1 = Version.of(0, 1, 0);
        final Version v2 = Version.of(1, 1, 0);
        Assert.assertTrue(v1.isInitialDevelopment());
        Assert.assertFalse(v2.isInitialDevelopment());
    }
}
