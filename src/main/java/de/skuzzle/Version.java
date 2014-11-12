/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Simon Taddiken
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.skuzzle;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is an implementation of the full <em>semantic version 2.0.0</em>
 * <a href="http://semver.org/">specification</a>. Instances can be obtained
 * using the static overloads of the <em>create</em> method or by
 * {@link #parseVersion(String) parsing} a String. This class implements
 * {@link Comparable} to compare two versions by following the specifications
 * linked to above. The {@link #equals(Object)} method conforms to the result of
 * {@link #compareTo(Version)}, {@link #hashCode()} is implemented
 * appropriately. Neither method considers the {@link #getBuildMetaData() build
 * meta data} field for comparison.
 *
 * @author Simon Taddiken
 * @version 0.4.0-SNAPSHOT
 */
public final class Version implements Comparable<Version>, Serializable {

    /** Conforms to all Version implementations since 0.1.0 */
    private static final long serialVersionUID = -7080189911455871050L;

    /**
     * Semantic Version Specification to which this class complies
     *
     * @since 0.2.0
     */
    public static final Version COMPLIANCE = Version.create(2, 0, 0);

    /**
     * This exception indicates that a version- or a part of a version string
     * could not be parsed according to the semantic version specification.
     *
     * @author Simon Taddiken
     */
    public static class VersionFormatException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        /**
         * Creates a new VersionFormatException with the given message.
         *
         * @param message The exception message.
         */
        public VersionFormatException(String message) {
            super(message);
        }
    }

    /**
     * Comparator for natural version ordering. See
     * {@link #compare(Version, Version)} for more information.
     *
     * @since 0.2.0
     */
    public static final Comparator<Version> NATURAL_ORDER = new Comparator<Version>() {
        @Override
        public int compare(Version o1, Version o2) {
            return Version.compare(o1, o2);
        }
    };

    /**
     * Comparator for ordering versions with additionally considering the build
     * meta data field when comparing versions.
     *
     * @since 0.3.0
     */
    public static final Comparator<Version> WITH_BUILD_META_DATA_ORDER =
            new Comparator<Version>() {

                @Override
                public int compare(Version o1, Version o2) {
                    return compareWithBuildMetaData(o1, o2);
                }
            };

    private static final Pattern PRE_RELEASE = Pattern.compile(
            "(?:(?:[0-9]+[a-zA-Z-][\\w-]*)|(?:[a-zA-Z][\\w-]*)|(?:[1-9]\\d*)|0)(?:\\.(?:(?:[0-9]+[a-zA-Z-][\\w-]*)|(?:[a-zA-Z][\\w-]*)|(?:[1-9]\\d*)|0))*");
    private static final Pattern BUILD_MD = Pattern.compile("[\\w-]+(\\.[\\w-]+)*");
    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:(?:[0-9]+[a-zA-Z-][\\w-]*)|(?:[a-zA-Z][\\w-]*)|(?:[1-9]\\d*)|0)(?:\\.(?:(?:[0-9]+[a-zA-Z-][\\w-]*)|(?:[a-zA-Z][\\w-]*)|(?:[1-9]\\d*)|0))*))?(?:\\+([\\w-]+(\\.[\\w-]+)*))?");

    // Match result group indices
    private static final int MAJOR_GROUP = 1;
    private static final int MINOR_GROUP = 2;
    private static final int PATCH_GROUP = 3;
    private static final int PRE_RELEASE_GROUP = 4;
    private static final int BUILD_MD_GROUP = 5;

    private static final int TO_STRING_ESTIMATE = 24;

    private final int major;
    private final int minor;
    private final int patch;
    private final String preRelease;
    private final String buildMetaData;

    private Version(int major, int minor, int patch, String preRelease, String buildMd) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preRelease = preRelease;
        this.buildMetaData = buildMd;
    }

    /**
     * Compares two versions, following the <em>semantic version</em>
     * specification. Here is a quote from <a href="http://semver.org/">semantic
     * version 2.0.0 specification</a>:
     *
     * <p>
     * <em> Precedence refers to how versions are compared to each other when
     * ordered. Precedence MUST be calculated by separating the version into
     * major, minor, patch and pre-release identifiers in that order (Build
     * metadata does not figure into precedence). Precedence is determined by
     * the first difference when comparing each of these identifiers from left
     * to right as follows: Major, minor, and patch versions are always compared
     * numerically. Example: 1.0.0 &lt; 2.0.0 &lt; 2.1.0 &lt; 2.1.1. When major, minor,
     * and patch are equal, a pre-release version has lower precedence than a
     * normal version. Example: 1.0.0-alpha &lt; 1.0.0. Precedence for two
     * pre-release versions with the same major, minor, and patch version MUST
     * be determined by comparing each dot separated identifier from left to
     * right until a difference is found as follows: identifiers consisting of
     * only digits are compared numerically and identifiers with letters or
     * hyphens are compared lexically in ASCII sort order. Numeric identifiers
     * always have lower precedence than non-numeric identifiers. A larger set
     * of pre-release fields has a higher precedence than a smaller set, if all
     * of the preceding identifiers are equal. Example: 1.0.0-alpha &lt;
     * 1.0.0-alpha.1 &lt; 1.0.0-alpha.beta &lt; 1.0.0-beta &lt; 1.0.0-beta.2 &lt;
     * 1.0.0-beta.11 &lt; 1.0.0-rc.1 &lt; 1.0.0.
     * </em>
     * </p>
     *
     * <p>
     * This method fulfills the general contract for Java's {@link Comparator
     * Comparators} and {@link Comparable Comparables}.
     * </p>
     *
     * @param v1 The first version for comparison.
     * @param v2 The second version for comparison.
     * @return A value below 0 iff {@code v1 &lt; v2}, a value above 0 iff
     *         {@code v1 &gt; v2</tt> and 0 iff <tt>v1 = v2}.
     * @throws NullPointerException If either parameter is null.
     * @since 0.2.0
     */
    public static int compare(Version v1, Version v2) {
        return compare(v1, v2, false);
    }

    /**
     * Compares two Versions with additionally considering the build meta data
     * field if all other parts are equal. Note: This is <em>not</em> part of
     * the semantic version specification.
     *
     * <p>
     * Comparison of the build meta data parts happens exactly as for pre
     * release identifiers. Considering of build meta data first kicks in if
     * both versions are equal when using their natural order.
     * </p>
     *
     * <p>
     * This method fulfills the general contract for Java's {@link Comparator
     * Comparators} and {@link Comparable Comparables}.
     * </p>
     *
     * @param v1 The first version for comparison.
     * @param v2 The second version for comparison.
     * @return A value below 0 iff {@code v1 &lt; v2}, a value above 0 iff
     *         {@code v1 &gt; v2</tt> and 0 iff <tt>v1 = v2}.
     * @throws NullPointerException If either parameter is null.
     * @since 0.3.0
     */
    public static int compareWithBuildMetaData(Version v1, Version v2) {
        return compare(v1, v2, true);
    }

    private static int compare(Version v1, Version v2, boolean withBuildMetaData) {
        if (v1 == null) {
            throw new NullPointerException("v1 is null");
        } else if (v2 == null) {
            throw new NullPointerException("v2 is null");
        } else if (v1 == v2) {
            return 0;
        }

        final int mc, mm, mp;
        final int result;
        if ((mc = Integer.compare(v1.major, v2.major)) == 0) {
            if ((mm = Integer.compare(v1.minor, v2.minor)) == 0) {
                if ((mp = Integer.compare(v1.patch, v2.patch)) == 0) {

                    if (v1.isPreRelease() && v2.isPreRelease()) {
                        // compare pre release parts
                        result = compareIdentifiers(v1.getPreReleaseParts(),
                                v2.getPreReleaseParts());
                    } else if (v1.isPreRelease()) {
                        // other is greater, because it is no pre release
                        result = -1;
                    } else if (v2.isPreRelease()) {
                        // this is greater because other is no pre release
                        result = 1;
                    } else {
                        // both are no pre releases
                        result = 0;
                    }
                } else {
                    // versions differ in patch
                    result = mp;
                }
            } else {
                // versions differ in minor
                result = mm;
            }
        } else {
            // versions differ in major
            result = mc;
        }

        if (withBuildMetaData && result == 0) {
            // compare build meta data if necessary. Apply same
            // logic as for pre release parts

            if (v1.hasBuildMetaData() && v2.hasBuildMetaData()) {
                return compareIdentifiers(v1.getBuildMetaDataParts(),
                        v2.getBuildMetaDataParts());
            } else if (v1.hasBuildMetaData()) {
                // other is greater because it has no build data
                return -1;
            } else {
                // this is greater because other has no build
                // data
                return 1;
            }
        }
        return result;
    }

    private static int compareIdentifiers(String[] parts1, String[] parts2) {
        int min = Math.min(parts1.length, parts2.length);
        for (int i = 0; i < min; ++i) {
            final int r = comparePreReleaseParts(parts1[i], parts2[i]);
            if (r != 0) {
                // versions differ in pre release part i
                return r;
            }
        }

        // all pre release id's are equal, so compare amount of
        // pre release id's
        return Integer.compare(parts1.length, parts2.length);
    }

    private static int comparePreReleaseParts(String p1, String p2) {
        final int num1 = isNumeric(p1);
        final int num2 = isNumeric(p2);

        if (num1 < 0 && num2 < 0) {
            // both are not numerical -> compare lexically
            return p1.compareTo(p2);
        } else if (num1 >= 0 && num2 >= 0) {
            // both are numerical
            return Integer.compare(num1, num2);
        } else if (num1 >= 0) {
            // only part1 is numerical -> p2 is greater
            return -1;
        } else {
            // only part2 is numerical -> p1 is greater
            return 1;
        }
    }

    /**
     * Determines whether s is a positive number. If it is, the number is
     * returned, otherwise the result is -1.
     *
     * @param s The String to check.
     * @return The positive number (incl. 0) if s a number, or -1 if it is not.
     */
    private static int isNumeric(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Creates a new Version from the provided components. Neither value of
     * {@code major, minor} or {@code patch} must be lower than 0 and at least
     * one must be greater than zero. {@code preRelease} or
     * {@code buildMetaData} may be the empty String. In this case, the created
     * {@code Version} will have no pre release resp. build meta data field. If
     * those parameters are not empty, they must conform to the semantic version
     * specification.
     *
     * @param major The major version.
     * @param minor The minor version.
     * @param patch The patch version.
     * @param preRelease The pre release version or the empty string.
     * @param buildMetaData The build meta data field or the empty string.
     * @return The version instance.
     * @throws VersionFormatException If {@code preRelease} or
     *             {@code buildMetaData} does not conform to the semantic
     *             version specification.
     */
    public static final Version create(int major, int minor, int patch,
            String preRelease,
            String buildMetaData) {
        checkParams(major, minor, patch);
        if (preRelease == null) {
            throw new IllegalArgumentException("preRelease is null");
        } else if (buildMetaData == null) {
            throw new IllegalArgumentException("buildMetaData is null");
        }
        if (!preRelease.isEmpty() && !PRE_RELEASE.matcher(preRelease).matches()) {
            throw new VersionFormatException(preRelease);
        }
        if (!buildMetaData.isEmpty() && !BUILD_MD.matcher(buildMetaData).matches()) {
            throw new VersionFormatException(buildMetaData);
        }
        return new Version(major, minor, patch, preRelease, buildMetaData);
    }

    /**
     * Creates a new Version from the provided components. The version's build
     * meta data field will be empty. Neither value of {@code major, minor} or
     * {@code patch} must be lower than 0 and at least one must be greater than
     * zero. {@code preRelease} may be the empty String. In this case, the
     * created version will have no pre release field. If it is not empty, it
     * must conform to the specifications of the semantic version.
     *
     * @param major The major version.
     * @param minor The minor version.
     * @param patch The patch version.
     * @param preRelease The pre release version or the empty string.
     * @return The version instance.
     * @throws VersionFormatException If {@code preRelease} is not empty and
     *             does not conform to the semantic versioning specification
     */
    public static final Version create(int major, int minor, int patch, String preRelease) {
        checkParams(major, minor, patch);
        if (preRelease == null) {
            throw new IllegalArgumentException("preRelease is null");
        }
        if (!PRE_RELEASE.matcher(preRelease).matches()) {
            throw new VersionFormatException(preRelease);
        }
        return new Version(major, minor, patch, preRelease, "");
    }

    /**
     * Creates a new Version from the three provided components. The version's
     * pre release and build meta data fields will be empty. Neither value must
     * be lower than 0 and at least one must be greater than zero
     *
     * @param major The major version.
     * @param minor The minor version.
     * @param patch The patch version.
     * @return The version instance.
     */
    public static final Version create(int major, int minor, int patch) {
        checkParams(major, minor, patch);
        return new Version(major, minor, patch, "", "");
    }

    private static void checkParams(int major, int minor, int patch) {
        if (major < 0) {
            throw new IllegalArgumentException("major < 0");
        } else if (minor < 0) {
            throw new IllegalArgumentException("minor < 0");
        } else if (patch < 0) {
            throw new IllegalArgumentException("patch < 0");
        } else if (major == 0 && minor == 0 && patch == 0) {
            throw new IllegalArgumentException("all parts are 0");
        }
    }

    /**
     * Tries to parse the provided String as a semantic version. If the string
     * does not conform to the semantic version specification, a
     * {@link VersionFormatException} will be thrown.
     *
     * @param versionString The String to parse.
     * @return The parsed version.
     * @throws VersionFormatException If the String is no valid version
     * @throws IllegalArgumentException If {@code versionString} is
     *             <code>null</code>.
     */
    public static final Version parseVersion(String versionString) {
        if (versionString == null) {
            throw new IllegalArgumentException("versionString is null");
        }
        final Matcher m = VERSION_PATTERN.matcher(versionString);
        if (!m.matches()) {
            throw new VersionFormatException(versionString);
        }

        final int major = Integer.parseInt(m.group(MAJOR_GROUP));
        final int minor = Integer.parseInt(m.group(MINOR_GROUP));
        final int patch = Integer.parseInt(m.group(PATCH_GROUP));

        checkParams(major, minor, patch);

        final String preRelease;
        if (m.group(PRE_RELEASE_GROUP) != null) {
            preRelease = m.group(PRE_RELEASE_GROUP);
        } else {
            preRelease = "";
        }

        final String buildMD;
        if (m.group(BUILD_MD_GROUP) != null) {
            buildMD = m.group(BUILD_MD_GROUP);
        } else {
            buildMD = "";
        }

        return new Version(major, minor, patch, preRelease, buildMD);
    }

    /**
     * Tries to parse the provided String as a semantic version. If
     * {@code allowPreRelease} is <code>false</code>, the String must have
     * neither a pre-release nor a build meta data part. Thus the given String
     * must have the format {@code X.Y.Z} where at least one part must be
     * greater than zero.
     *
     * <p>
     * If {@code allowPreRelease} is <code>true</code>, the String is parsed
     * accoring to the normal semantic version specification.
     * </p>
     *
     * @param versionString The String to parse.
     * @param allowPreRelease Whether pre-release and build meta data field are
     *            allowed.
     * @return The parsed version.
     * @throws VersionFormatException If the String is no valid version
     * @since 0.4.0
     */
    public static Version parseVersion(String versionString, boolean allowPreRelease) {
        final Version version = parseVersion(versionString);
        if (!allowPreRelease && (version.isPreRelease() || version.hasBuildMetaData())) {
            throw new VersionFormatException(String.format(
                    "Version is expected to have no pre-release or build meta data part"));
        }
        return version;
    }

    /**
     * Gets this version's major number.
     *
     * @return The major version.
     */
    public int getMajor() {
        return this.major;
    }

    /**
     * Gets this version's minor number.
     *
     * @return The minor version.
     */
    public int getMinor() {
        return this.minor;
    }

    /**
     * Gets this version's path number.
     *
     * @return The patch number.
     */
    public int getPatch() {
        return this.patch;
    }

    /**
     * Gets the pre release parts of this version as array by splitting the pre
     * result version string at the dots.
     *
     * @return Pre release version as array. Array is empty if this version has
     *         no pre release part.
     */
    public String[] getPreReleaseParts() {
        return this.preRelease.split("\\.");
    }

    /**
     * Gets the pre release identifier of this version. If this version has no
     * such identifier, an empty string is returned.
     *
     * @return This version's pre release identifier or an empty String if this
     *         version has no such identifier.
     */
    public String getPreRelease() {
        return this.preRelease;
    }

    /**
     * Gets this version's build meta data. If this version has no build meta
     * data, the returned string is empty.
     *
     * @return The build meta data or an empty String if this version has no
     *         build meta data.
     */
    public String getBuildMetaData() {
        return this.buildMetaData;
    }

    /**
     * Gets this version's build meta data as array by splitting the meta data
     * at dots. If this version has no build meta data, the result is an empty
     * array.
     *
     * @return The build meta data as array.
     */
    public String[] getBuildMetaDataParts() {
        return this.buildMetaData.split("\\.");
    }

    /**
     * Determines whether this version is still under initial development.
     *
     * @return <code>true</code> iff this version's major part is zero.
     */
    public boolean isInitialDevelopment() {
        return this.major == 0;
    }

    /**
     * Determines whether this is a pre release version.
     *
     * @return <code>true</code> iff {@link #getPreRelease()} is not empty.
     */
    public boolean isPreRelease() {
        return !this.preRelease.isEmpty();
    }

    /**
     * Determines whether this version has a build meta data field.
     *
     * @return <code>true</code> iff {@link #getBuildMetaData()} is not empty.
     */
    public boolean hasBuildMetaData() {
        return !this.buildMetaData.isEmpty();
    }

    /**
     * Creates a String representation of this version by joining its parts
     * together as by the semantic version specification.
     *
     * @return The version as a String.
     */
    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder(this.preRelease.length()
                + this.buildMetaData.length() + TO_STRING_ESTIMATE);
        b.append(this.major).append(".").append(this.minor).append(".")
                .append(this.patch);
        if (!this.preRelease.isEmpty()) {
            b.append("-").append(this.preRelease);
        }
        if (!this.buildMetaData.isEmpty()) {
            b.append("+").append(this.buildMetaData);
        }
        return b.toString();
    }

    /**
     * The hash code for a version instance is computed from the fields
     * {@link #getMajor() major}, {@link #getMinor() minor}, {@link #getPatch()
     * patch} and {@link #getPreRelease() pre-release}.
     *
     * @return A hash code for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.major, this.minor, this.patch, this.preRelease);
    }

    /**
     * Determines whether this version is equal to the passed object. This is
     * the case if the passed object is an instance of Version and this version
     * {@link #compareTo(Version) compared} to the provided one yields 0. Thus,
     * this method ignores the {@link #getBuildMetaData()} field.
     *
     * @param obj the object to compare with.
     * @return <code>true</code> iff {@code obj} is an instance of
     *         {@code Version} and {@code this.compareTo((Version) obj) == 0}
     * @see #compareTo(Version)
     */
    @Override
    public boolean equals(Object obj) {
        return testEquality(obj, false);
    }

    /**
     * Determines whether this version is equal to the passed object (as
     * determined by {@link #equals(Object)} and additionally considers the
     * build meta data part of both versions for equality.
     *
     * @param obj The object to compare with.
     * @return <code>true</code> iff {@code this.equals(obj)} and
     *         {@code this.getBuildMetaData().equals(((Version) obj).getBuildMetaData())}
     * @since 0.2.0
     * @deprecated Since 0.4.0 - use {@link #equalsWithBuildMetaData(Object)}
     *             instead.
     */
    @Deprecated
    public boolean equalsIncludeBuildMetaData(Object obj) {
        return testEquality(obj, true);
    }

    /**
     * Determines whether this version is equal to the passed object (as
     * determined by {@link #equals(Object)} and additionally considers the
     * build meta data part of both versions for equality.
     *
     * @param obj The object to compare with.
     * @return <code>true</code> iff {@code this.equals(obj)} and
     *         {@code this.getBuildMetaData().equals(((Version) obj).getBuildMetaData())}
     * @since 0.4.0
     */
    public boolean equalsWithBuildMetaData(Object obj) {
        return testEquality(obj, true);
    }

    private boolean testEquality(Object obj, boolean includeBuildMd) {
        return obj == this || obj != null && obj instanceof Version
                && compare(this, (Version) obj, includeBuildMd) == 0;
    }

    /**
     * Compares this version to the provided one, following the
     * <em>semantic versioning</em> specification. See
     * {@link #compare(Version, Version)} for more information.
     *
     * @param other The version to compare to.
     * @return A value lower than 0 if this &lt; other, a value greater than 0
     *         if this &gt; other and 0 if this == other. The absolute value of
     *         the result has no semantical interpretation.
     */
    @Override
    public int compareTo(Version other) {
        return compare(this, other);
    }

    /**
     * Compares this version to the provided one. Unlike the
     * {@link #compareTo(Version)} method, this one additionally considers the
     * build meta data field of both versions, if all other parts are equal.
     * Note: This is <em>not</em> part of the semantic version specification.
     *
     * <p>
     * Comparison of the build meta data parts happens exactly as for pre
     * release identifiers. Considering of build meta data first kicks in if
     * both versions are equal when using their natural order.
     * </p>
     *
     * @param other The version to compare to.
     * @return A value lower than 0 if this &lt; other, a value greater than 0
     *         if this &gt; other and 0 if this == other. The absolute value of
     *         the result has no semantical interpretation.
     * @since 0.3.0
     */
    public int compareToWithBuildMetaData(Version other) {
        return compareWithBuildMetaData(this, other);
    }
}
