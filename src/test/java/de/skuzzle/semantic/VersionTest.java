package de.skuzzle.semantic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Assert;
import org.junit.Test;

import de.skuzzle.semantic.Version.VersionFormatException;

public class VersionTest {

    private static final Version[] SEMVER_ORG_VERSIONS = new Version[] {
            Version.parseVersion("1.0.0-alpha"),
            Version.parseVersion("1.0.0-alpha.1"),
            Version.parseVersion("1.0.0-alpha.beta"),
            Version.parseVersion("1.0.0-beta"),
            Version.parseVersion("1.0.0-beta.2"),
            Version.parseVersion("1.0.0-beta.11"),
            Version.parseVersion("1.0.0-rc.1"),
            Version.parseVersion("1.0.0"),
            Version.parseVersion("2.0.0"),
            Version.parseVersion("2.1.0"),
            Version.parseVersion("2.1.1")
    };

    // same as above, but exchanged pre release and build meta data
    private static final Version[] SEMVER_ORG_BMD_VERSIONS = new Version[] {
            Version.parseVersion("1.0.0-rc.1+alpha"),
            Version.parseVersion("1.0.0-rc.1+alpha.1"),
            Version.parseVersion("1.0.0-rc.1+alpha.beta"),
            Version.parseVersion("1.0.0-rc.1+beta"),
            Version.parseVersion("1.0.0-rc.1+beta.2"),
            Version.parseVersion("1.0.0-rc.1+beta.11"),
            Version.parseVersion("1.0.0-rc.1+rc.1"),
            Version.parseVersion("1.0.0-rc.1"),
            Version.parseVersion("2.0.0-rc.1"),
            Version.parseVersion("2.1.0-rc.1"),
            Version.parseVersion("2.1.1")
    };

    public static void main(String[] args) throws IOException {
        new VersionTest().writeBinFile();
    }

    public void writeBinFile() throws IOException {
        final FileOutputStream out = new FileOutputStream("versions.bin");
        final ObjectOutputStream oout = new ObjectOutputStream(out);
        for (final Version v : SEMVER_ORG_VERSIONS) {
            oout.writeObject(v);
        }
        for (final Version v : SEMVER_ORG_BMD_VERSIONS) {
            oout.writeObject(v);
        }
        oout.close();
    }

    @Test
    public void testPreReleaseEmptyString() {
        final Version v = Version.create(1, 1, 1, "");
        assertEquals("", v.getPreRelease());
        assertEquals("", v.getBuildMetaData());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPreReleaseNull() {
        Version.create(1, 1, 1, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildMDNull() {
        Version.create(1, 1, 1, "", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativePatch() {
        Version.create(1, 1, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeMinor() {
        Version.create(1, -1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeMajor() {
        Version.create(-1, 1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseVersionNull() {
        Version.parseVersion(null);
    }

    @Test
    public void testSimpleVersion() {
        final Version v = Version.parseVersion("1.2.3");
        Assert.assertEquals(1, v.getMajor());
        Assert.assertEquals(2, v.getMinor());
        Assert.assertEquals(3, v.getPatch());
        Assert.assertEquals("", v.getPreRelease());
        Assert.assertEquals("", v.getBuildMetaData());
    }

    @Test
    public void testSemVerOrgPreReleaseSamples() {
        final Version v1 = Version.parseVersion("1.0.0-alpha");
        Assert.assertEquals("alpha", v1.getPreRelease());

        final Version v2 = Version.parseVersion("1.0.0-alpha.1");
        Assert.assertEquals("alpha.1", v2.getPreRelease());

        final Version v3 = Version.parseVersion("1.0.0-0.3.7");
        Assert.assertEquals("0.3.7", v3.getPreRelease());

        final Version v4 = Version.parseVersion("1.0.0-x.7.z.92");
        Assert.assertEquals("x.7.z.92", v4.getPreRelease());
    }

    @Test
    public void testSemVerOrgBuildMDSamples() {
        final Version v1 = Version.parseVersion("1.0.0-alpha+001");
        Assert.assertEquals("alpha", v1.getPreRelease());
        Assert.assertEquals("001", v1.getBuildMetaData());

        final Version v2 = Version.parseVersion("1.0.0+20130313144700");
        Assert.assertEquals("20130313144700", v2.getBuildMetaData());

        final Version v3 = Version.parseVersion("1.0.0-beta+exp.sha.5114f85");
        Assert.assertEquals("beta", v3.getPreRelease());
        Assert.assertEquals("exp.sha.5114f85", v3.getBuildMetaData());
    }

    @Test
    public void testVersionWithBuildMD() {
        final Version v = Version.parseVersion("1.2.3+some.id.foo");
        Assert.assertEquals("some.id.foo", v.getBuildMetaData());
    }

    @Test
    public void testVersionWithBuildMD2() {
        final Version v = Version.create(1, 2, 3, "", "some.id-1.foo");
        Assert.assertEquals("some.id-1.foo", v.getBuildMetaData());
    }

    @Test(expected = VersionFormatException.class)
    public void testVersionWithBuildMDEmptyLastPart() {
        Version.create(1, 2, 3, "", "some.id.");
    }

    @Test(expected = VersionFormatException.class)
    public void testVersionWithBuildMDEmptyMiddlePart() {
        Version.create(1, 2, 3, "", "some..id");
    }

    @Test
    public void testVersionWithPreRelease() {
        final Version v = Version.parseVersion("1.2.3-pre.release-foo.1");
        Assert.assertEquals("pre.release-foo.1", v.getPreRelease());
        final String[] expected = { "pre", "release-foo", "1" };
        Assert.assertArrayEquals(expected, v.getPreReleaseParts());
    }

    @Test
    public void testVersionWithPreReleaseAndBuildMD() {
        final Version v = Version.parseVersion("1.2.3-pre.release-foo.1+some.id-with-hyphen");
        Assert.assertEquals("pre.release-foo.1", v.getPreRelease());
        Assert.assertEquals("some.id-with-hyphen", v.getBuildMetaData());
    }

    @Test(expected = VersionFormatException.class)
    public void testPreReleaseWithLeadingZeroes() {
        Version.parseVersion("1.2.3-pre.001");
    }

    @Test(expected = VersionFormatException.class)
    public void testPreReleaseWithLeadingZeroes2() {
        Version.create(1, 2, 3, "pre.001");
    }

    @Test(expected = VersionFormatException.class)
    public void testPreReleaseWithLeadingZero() {
        Version.parseVersion("1.2.3-pre.01");
    }

    @Test(expected = VersionFormatException.class)
    public void testPreReleaseWithLeadingZero2() {
        Version.create(1, 2, 3, "pre.01");
    }

    @Test(expected = VersionFormatException.class)
    public void testPreReleaseMiddleEmptyIdentifier() {
        Version.parseVersion("1.2.3-pre..foo");
    }

    @Test(expected = VersionFormatException.class)
    public void testPreReleaseLastEmptyIdentifier() {
        Version.parseVersion("1.2.3-pre.foo.");
    }

    @Test(expected = VersionFormatException.class)
    public void testParseExpectNoPrelease() {
        Version.parseVersion("1.2.3-foo", false);
    }

    @Test(expected = VersionFormatException.class)
    public void testParseExpectNoBuildMetaData() {
        Version.parseVersion("1.2.3+foo", false);
    }

    @Test(expected = VersionFormatException.class)
    public void testParseExpectNoPreReleaseAndBuildMetaData() {
        Version.parseVersion("1.2.3-foo+foo", false);
    }

    @Test
    public void testParseVersionSuccessExpectNoPreRelease() {
        Version.parseVersion("1.2.3", false);
    }

    @Test
    public void testParseVersionSuccess() {
        final Version version = Version.parseVersion("1.2.3-foo+bar", true);
        Assert.assertEquals("foo", version.getPreRelease());
        Assert.assertEquals("bar", version.getBuildMetaData());
    }

    @Test(expected = VersionFormatException.class)
    public void testPreReleaseLastEmptyIdentifier2() {
        Version.create(1, 2, 3, "pre.foo.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVersionAll0() {
        Version.parseVersion("0.0.0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVersionAll02() {
        Version.create(0, 0, 0);
    }

    @Test(expected = VersionFormatException.class)
    public void testPreReleaseInvalid() {
        Version.create(1, 2, 3, "pre.", "build");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPreReleaseNullAndBuildMDGiven() {
        Version.create(1, 2, 3, null, "build");
    }

    @Test
    public void testOnlyBuildMdEmpty() {
        Version.create(1, 2, 3, "pre", "");
    }

    @Test
    public void testPreReleaseWithLeadingZeroesIdentifier() {
        // leading zeroes allowed in string identifiers
        final Version v = Version.parseVersion("1.2.3-001abc");
        Assert.assertEquals("001abc", v.getPreRelease());
    }

    @Test
    public void testPreReleaseWithLeadingZeroesIdentifier2() {
        // leading zeroes allowed in string identifiers
        final Version v = Version.create(1, 2, 3, "001abc");
        Assert.assertEquals("001abc", v.getPreRelease());
    }

    @Test
    public void testNoPrecedenceChangeByBuildMD() {
        final Version v1 = Version.parseVersion("1.2.3+1.0");
        final Version v2 = Version.parseVersion("1.2.3+2.0");
        Assert.assertEquals(0, v1.compareTo(v2));
    }

    @Test
    public void testSimplePrecedence() {
        final Version v1 = Version.parseVersion("1.0.0");
        final Version v2 = Version.parseVersion("1.0.1");
        final Version v3 = Version.parseVersion("1.1.0");
        final Version v4 = Version.parseVersion("2.0.0");

        Assert.assertTrue(v1.compareTo(v2) < 0);
        Assert.assertTrue(v2.compareTo(v3) < 0);
        Assert.assertTrue(v3.compareTo(v4) < 0);
        Assert.assertTrue(v2.compareTo(v1) > 0);
        Assert.assertTrue(v3.compareTo(v2) > 0);
        Assert.assertTrue(v4.compareTo(v3) > 0);
    }

    @Test
    public void testPrecedencePreRelease() {
        final Version v1 = Version.parseVersion("1.0.0");
        final Version v2 = Version.parseVersion("1.0.0-rc1");
        Assert.assertTrue(v1.compareTo(v2) > 0);
        Assert.assertTrue(v2.compareTo(v1) < 0);
    }

    @Test
    public void testPrecedencePreRelease2() {
        final Version v1 = Version.parseVersion("1.0.0-rc1");
        final Version v2 = Version.parseVersion("1.0.0-rc1");
        Assert.assertTrue(v1.compareTo(v2) == 0);
    }

    @Test
    public void testPrecedencePreRelease3() {
        final Version v1 = Version.parseVersion("1.0.0-rc1");
        final Version v2 = Version.parseVersion("1.0.0-rc1.5");
        // the one with longer list is greater
        Assert.assertTrue(v1.compareTo(v2) < 0);
        Assert.assertTrue(v2.compareTo(v1) > 0);
    }

    @Test
    public void testPrecedencePreRelease4() {
        final Version v1 = Version.parseVersion("1.0.0-a");
        final Version v2 = Version.parseVersion("1.0.0-b");
        Assert.assertTrue(v1.compareTo(v2) < 0);
        Assert.assertTrue(v2.compareTo(v1) > 0);
    }

    @Test
    public void testPrecedencePreRelease5() {
        final Version v1 = Version.parseVersion("1.0.0-1");
        final Version v2 = Version.parseVersion("1.0.0-2");
        Assert.assertTrue(v1.compareTo(v2) < 0);
        Assert.assertTrue(v2.compareTo(v1) > 0);
    }

    @Test
    public void testPrecedencePreRelease6() {
        final Version v1 = Version.parseVersion("1.0.0-1.some.id-with-hyphen.a");
        final Version v2 = Version.parseVersion("1.0.0-1.some.id-with-hyphen.b");
        Assert.assertTrue(v1.compareTo(v2) < 0);
        Assert.assertTrue(v2.compareTo(v1) > 0);
    }

    @Test
    public void testInitialDevelopment() {
        final Version v1 = Version.create(0, 1, 0);
        final Version v2 = Version.create(1, 1, 0);
        Assert.assertTrue(v1.isInitialDevelopment());
        Assert.assertFalse(v2.isInitialDevelopment());
    }

    @Test
    public void testSemVerOrgPrecedenceSample() {
        for (int i = 1; i < SEMVER_ORG_VERSIONS.length; ++i) {
            final Version v1 = SEMVER_ORG_VERSIONS[i - 1];
            final Version v2 = SEMVER_ORG_VERSIONS[i];
            final int c = v1.compareTo(v2);
            Assert.assertTrue(v1 + " is not lower than " + v2, c < 0);
        }
    }

    @Test
    public void testSemVerOrgPrecedenceSampleComparator() {
        for (int i = 1; i < SEMVER_ORG_VERSIONS.length; ++i) {
            final Version v1 = SEMVER_ORG_VERSIONS[i - 1];
            final Version v2 = SEMVER_ORG_VERSIONS[i];
            final int c = Version.NATURAL_ORDER.compare(v1, v2);
            Assert.assertTrue(v1 + " is not lower than " + v2, c < 0);
        }
    }

    @Test
    public void testBuildMetaDataEquality() {
        final Version v1 = Version.create(0, 0, 1, "", "some.build-meta.data");
        final Version v2 = Version.create(0, 0, 1, "", "some.different.build-meta.data");
        Assert.assertFalse(v1.equalsWithBuildMetaData(v2));
    }

    @Test
    public void testBuildMDPrecedence() {
        for (int i = 1; i < SEMVER_ORG_BMD_VERSIONS.length; ++i) {
            final Version v1 = SEMVER_ORG_BMD_VERSIONS[i - 1];
            final Version v2 = SEMVER_ORG_BMD_VERSIONS[i];
            final int c = v1.compareToWithBuildMetaData(v2);
            Assert.assertTrue(v1 + " is not lower than " + v2, c < 0);
        }
    }

    @Test
    public void testBuildMDPrecedenceComparator() {
        for (int i = 1; i < SEMVER_ORG_BMD_VERSIONS.length; ++i) {
            final Version v1 = SEMVER_ORG_BMD_VERSIONS[i - 1];
            final Version v2 = SEMVER_ORG_BMD_VERSIONS[i];
            final int c = Version.WITH_BUILD_META_DATA_ORDER.compare(v1, v2);
            Assert.assertTrue(v1 + " is not lower than " + v2, c < 0);
        }
    }

    @Test
    public void testBuildMDPrecedenceReverse() {
        for (int i = 1; i < SEMVER_ORG_BMD_VERSIONS.length; ++i) {
            final Version v1 = SEMVER_ORG_BMD_VERSIONS[i - 1];
            final Version v2 = SEMVER_ORG_BMD_VERSIONS[i];
            final int c = v2.compareToWithBuildMetaData(v1);
            Assert.assertTrue(v2 + " is not greater than " + v1, c > 0);
        }
    }

    @Test
    public void testPreReleaseEquality() throws Exception {
        for (final Version version : SEMVER_ORG_VERSIONS) {
            final Version copy = Version.create(version.getMajor(), version.getMinor(),
                    version.getPatch(), version.getPreRelease(),
                    version.getBuildMetaData());
            assertEquals(version, copy);
            assertTrue(version.equalsWithBuildMetaData(copy));
            assertTrue(version.compareTo(copy) == 0);
            assertTrue(version.compareToWithBuildMetaData(copy) == 0);
        }
    }

    @Test
    public void testBuildMDEquality() throws Exception {
        for (final Version version : SEMVER_ORG_BMD_VERSIONS) {
            final Version copy = Version.create(version.getMajor(), version.getMinor(),
                    version.getPatch(), version.getPreRelease(),
                    version.getBuildMetaData());
            assertEquals(version, copy);
            assertTrue(version.equalsWithBuildMetaData(copy));
            assertTrue(version.compareTo(copy) == 0);
            assertTrue(version.compareToWithBuildMetaData(copy) == 0);
        }
    }

    @Test(expected = NullPointerException.class)
    public void testCompareNull1() {
        Version.compare(null, Version.create(1, 1, 1));
    }

    @Test(expected = NullPointerException.class)
    public void testCompareNull2() {
        Version.compare(Version.create(1, 1, 1), null);
    }

    @Test
    public void testCompareIdentical() {
        final Version v = Version.create(1, 1, 1);
        Assert.assertEquals(0, Version.compare(v, v));
    }

    @Test
    public void testNotEqualsNull() {
        final Version v = Version.create(1, 1, 1);
        Assert.assertFalse(v.equals(null));
    }

    @Test
    public void testNotEqualsForeign() {
        final Version v = Version.create(1, 1, 1);
        Assert.assertFalse(v.equals(new Object()));
    }

    @Test
    public void testEqualsIdentity() {
        final Version v = Version.create(1, 2, 3);
        Assert.assertEquals(v, v);
    }

    @Test
    public void testNotEqualsTrivial() {
        final Version v1 = Version.create(1, 1, 1);
        final Version v2 = Version.create(1, 1, 2);
        Assert.assertFalse(v1.equals(v2));
    }

    @Test
    public void testParseToString() {
        for (final Version v1 : SEMVER_ORG_VERSIONS) {
            final Version v2 = Version.parseVersion(v1.toString());
            Assert.assertEquals(v1, v2);
            Assert.assertEquals(v1.hashCode(), v2.hashCode());
        }
    }

    @Test
    public void testMin() throws Exception {
        final Version v1 = Version.create(1, 0, 0);
        final Version v2 = Version.create(0, 1, 0);

        Assert.assertSame(Version.min(v1, v2), Version.min(v2, v1));
        Assert.assertSame(v2, Version.min(v1, v2));
    }

    @Test
    public void testMinEquals() throws Exception {
        final Version v1 = Version.create(1, 0, 0);
        final Version v2 = Version.create(1, 0, 0);

        final Version min = Version.min(v1, v2);
        Assert.assertSame(v1, min);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinNullV1() throws Exception {
        Version.min(null, Version.create(1, 0, 0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinNullV2() throws Exception {
        Version.min(Version.create(1, 0, 0), null);
    }

    @Test
    public void testMax() throws Exception {
        final Version v1 = Version.create(1, 0, 0);
        final Version v2 = Version.create(0, 1, 0);

        Assert.assertSame(Version.max(v1, v2), Version.max(v2, v1));
        Assert.assertSame(v1, Version.max(v1, v2));
    }

    @Test
    public void testMaxEquals() throws Exception {
        final Version v1 = Version.create(1, 0, 0);
        final Version v2 = Version.create(1, 0, 0);

        final Version max = Version.max(v1, v2);
        Assert.assertSame(v1, max);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaxNullV1() throws Exception {
        Version.max(null, Version.create(1, 0, 0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaxNullV2() throws Exception {
        Version.max(Version.create(1, 0, 0), null);
    }

    @Test
    public void testSamePrereleaseAndWithBuildMD() throws Exception {
        final Version v1 = Version.parseVersion("1.0.0-a.b+a");
        final Version v2 = Version.parseVersion("1.0.0-a.b+b");

        assertTrue(v1.compareToWithBuildMetaData(v2) < 0);
    }

    @Test
    public void testIsNoPreReleaseIdentifierNull() throws Exception {
        assertFalse(Version.isValidPreRelease(null));
    }

    @Test
    public void testIsPreReleaseIdentifierEmptyString() throws Exception {
        assertTrue(Version.isValidPreRelease(""));
    }

    @Test
    public void testIsValidPreReleaseIdentifier() throws Exception {
        for (final Version v : SEMVER_ORG_VERSIONS) {
            assertTrue(Version.isValidPreRelease(v.getPreRelease()));
        }
    }

    @Test
    public void testIsNoBuildMDIdentifierNull() throws Exception {
        assertFalse(Version.isValidBuildMetaData(null));
    }

    @Test
    public void testIsBuildMDIdentifierEmptyString() throws Exception {
        assertTrue(Version.isValidBuildMetaData(""));
    }

    @Test
    public void testIsValidBuildMDIdentifier() throws Exception {
        for (final Version v : SEMVER_ORG_BMD_VERSIONS) {
            assertTrue(v.toString(), Version.isValidBuildMetaData(v.getBuildMetaData()));
        }
    }

    @Test
    public void testNullIsNoVersion() throws Exception {
        assertFalse(Version.isValidVersion(null));
    }

    @Test
    public void testEmptyStringIsNoVersion() throws Exception {
        assertFalse(Version.isValidVersion(""));
    }

    @Test
    public void testIsValidVersion() throws Exception {
        for (final Version v : SEMVER_ORG_VERSIONS) {
            assertTrue(Version.isValidVersion(v.toString()));
        }
    }

    @Test
    public void testSerialize() throws Exception {
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final ObjectOutputStream out = new ObjectOutputStream(bout);
        for (final Version v : SEMVER_ORG_VERSIONS) {
            out.writeObject(v);
        }
        out.close();
        final InputStream bin = new ByteArrayInputStream(bout.toByteArray());
        final ObjectInputStream in = new ObjectInputStream(bin);
        for (final Version v : SEMVER_ORG_VERSIONS) {
            assertEquals(v, in.readObject());
        }
        in.close();
    }

    @Test
    public void testDeserialize05() throws Exception {
        // Deserialize a file which has been written by version 0.6.0
        final ClassLoader cl = getClass().getClassLoader();
        final InputStream inp = cl.getResourceAsStream("versions_0.6.bin");
        final ObjectInputStream oin = new ObjectInputStream(inp);
        for (final Version v : SEMVER_ORG_VERSIONS) {
            assertEquals(v, oin.readObject());
        }

        for (final Version v : SEMVER_ORG_BMD_VERSIONS) {
            assertEquals(v, oin.readObject());
        }
        oin.close();
    }
}
