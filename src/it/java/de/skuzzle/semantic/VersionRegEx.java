/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Simon Taddiken
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.skuzzle.semantic;

import java.io.Serializable;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is an implementation of the full <em>semantic version 2.0.0</em>
 * <a href="http://semver.org/">specification</a>. Instances can be obtained using the
 * static overloads of the <em>create</em> method or by {@link #parseVersion(String)
 * parsing} a String. This class implements {@link Comparable} to compare two versions by
 * following the specifications linked to above. The {@link #equals(Object)} method
 * conforms to the result of {@link #compareTo(VersionRegEx)}, {@link #hashCode()} is
 * implemented appropriately. Neither method considers the {@link #getBuildMetaData()
 * build meta data} field for comparison.
 *
 * <p>
 * Instances of this class are fully immutable.
 * </p>
 *
 * <p>
 * Note that unless stated otherwise, none of the public methods of this class accept
 * <code>null</code> values. Most methods will throw an {@link IllegalArgumentException}
 * when encountering a <code>null</code> argument. However, to comply with the
 * {@link Comparable} contract, the comparison methods will throw a
 * {@link NullPointerException} instead.
 * </p>
 *
 * @author Simon Taddiken
 */
public final class VersionRegEx implements Comparable<VersionRegEx>, Serializable {

    /** Conforms to all Version implementations since 0.6.0 */
    private static final long serialVersionUID = 6034927062401119911L;

    private static final String[] EMPTY_ARRAY = new String[0];

    /**
     * Semantic Version Specification to which this class complies
     *
     * @since 0.2.0
     */
    public static final VersionRegEx COMPLIANCE = VersionRegEx.create(2, 0, 0);

    /**
     * This exception indicates that a version- or a part of a version string could not be
     * parsed according to the semantic version specification.
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
     * Comparator for natural version ordering. See {@link #compare(VersionRegEx, VersionRegEx)} for
     * more information.
     *
     * @since 0.2.0
     */
    public static final Comparator<VersionRegEx> NATURAL_ORDER = new Comparator<VersionRegEx>() {
        @Override
        public int compare(VersionRegEx o1, VersionRegEx o2) {
            return VersionRegEx.compare(o1, o2);
        }
    };

    /**
     * Comparator for ordering versions with additionally considering the build meta data
     * field when comparing versions.
     *
     * <p>
     * Note: this comparator imposes orderings that are inconsistent with equals.
     * </p>
     *
     * @since 0.3.0
     */
    public static final Comparator<VersionRegEx> WITH_BUILD_META_DATA_ORDER = new Comparator<VersionRegEx>() {

        @Override
        public int compare(VersionRegEx o1, VersionRegEx o2) {
            return compareWithBuildMetaData(o1, o2);
        }
    };

    private static final Pattern PRE_RELEASE = Pattern.compile("" +
            "(?:(?:[0-9]+[a-zA-Z-][\\w-]*)|(?:[a-zA-Z][\\w-]*)|(?:[1-9]\\d*)|0)" +
            "(?:\\.(?:(?:[0-9]+[a-zA-Z-][\\w-]*)|(?:[a-zA-Z][\\w-]*)|(?:[1-9]\\d*)|0))*");

    private static final Pattern BUILD_MD = Pattern.compile("[\\w-]+(\\.[\\w-]+)*");
    private static final Pattern VERSION_PATTERN = Pattern.compile(""
            + "(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)"
            + "(?:-((?:(?:[0-9]+[a-zA-Z-][\\w-]*)|(?:[a-zA-Z][\\w-]*)|(?:[1-9]\\d*)|0)"
            + "(?:\\.(?:(?:[0-9]+[a-zA-Z-][\\w-]*)|(?:[a-zA-Z][\\w-]*)|(?:[1-9]\\d*)|0))*))?"
            + "(?:\\+([\\w-]+(\\.[\\w-]+)*))?");

    // Match result group indices
    private static final int MAJOR_GROUP = 1;
    private static final int MINOR_GROUP = 2;
    private static final int PATCH_GROUP = 3;
    private static final int PRE_RELEASE_GROUP = 4;
    private static final int BUILD_MD_GROUP = 5;

    private static final int TO_STRING_ESTIMATE = 12;
    private static final int HASH_PRIME = 31;

    private final int major;
    private final int minor;
    private final int patch;
    private final String preRelease;
    private final String buildMetaData;

    // store hash code once it has been calculated
    private volatile int hash;

    private VersionRegEx(int major, int minor, int patch, String preRelease, String buildMd) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preRelease = preRelease;
        this.buildMetaData = buildMd;
    }

    /**
     * Tries to parse the given String as a semantic version and returns whether the
     * String is properly formatted according to the semantic version specification.
     *
     * <p>
     * Note: this method does not throw an exception upon <code>null</code> input, but
     * returns <code>false</code> instead.
     * </p>
     *
     * @param version The String to check.
     * @return Whether the given String is formatted as a semantic version.
     * @since 0.5.0
     */
    public static boolean isValidVersion(String version) {
        if (version == null || version.isEmpty()) {
            return false;
        }
        return VERSION_PATTERN.matcher(version).matches();
    }

    /**
     * Returns whether the given String is a valid pre-release identifier. That is, this
     * method returns <code>true</code> if, and only if the {@code preRelease} parameter
     * is either the empty string or properly formatted as a pre-release identifier
     * according to the semantic version specification.
     *
     * <p>
     * Note: this method does not throw an exception upon <code>null</code> input, but
     * returns <code>false</code> instead.
     * </p>
     *
     * @param preRelease The String to check.
     * @return Whether the given String is a valid pre-release identifier.
     * @since 0.5.0
     */
    public static boolean isValidPreRelease(String preRelease) {
        return preRelease != null &&
                (preRelease.isEmpty() || PRE_RELEASE.matcher(preRelease).matches());
    }

    /**
     * Returns whether the given String is a valid build meta data identifier. That is,
     * this method returns <code>true</code> if, and only if the {@code buildMetaData}
     * parameter is either the empty string or properly formatted as a build meta data
     * identifier according to the semantic version specification.
     *
     * <p>
     * Note: this method does not throw an exception upon <code>null</code> input, but
     * returns <code>false</code> instead.
     * </p>
     *
     * @param buildMetaData The String to check.
     * @return Whether the given String is a valid build meta data identifier.
     * @since 0.5.0
     */
    public static boolean isValidBuildMetaData(String buildMetaData) {
        return buildMetaData != null &&
                (buildMetaData.isEmpty() || BUILD_MD.matcher(buildMetaData).matches());
    }

    /**
     * Returns the greater of the two given versions by comparing them using their natural
     * ordering. If both versions are equal, then the first argument is returned.
     *
     * @param v1 The first version.
     * @param v2 The second version.
     * @return The greater version.
     * @throws IllegalArgumentException If either argument is <code>null</code>.
     * @since 0.4.0
     */
    public static VersionRegEx max(VersionRegEx v1, VersionRegEx v2) {
        require(v1 != null, "v1 is null");
        require(v2 != null, "v2 is null");
        return compare(v1, v2, false) < 0
                ? v2
                : v1;
    }

    /**
     * Returns the lower of the two given versions by comparing them using their natural
     * ordering. If both versions are equal, then the first argument is returned.
     *
     * @param v1 The first version.
     * @param v2 The second version.
     * @return The lower version.
     * @throws IllegalArgumentException If either argument is <code>null</code>.
     * @since 0.4.0
     */
    public static VersionRegEx min(VersionRegEx v1, VersionRegEx v2) {
        require(v1 != null, "v1 is null");
        require(v2 != null, "v2 is null");
        return compare(v1, v2, false) <= 0
                ? v1
                : v2;
    }

    /**
     * Compares two versions, following the <em>semantic version</em> specification. Here
     * is a quote from <a href="http://semver.org/">semantic version 2.0.0
     * specification</a>:
     *
     * <p>
     * <em> Precedence refers to how versions are compared to each other when ordered.
     * Precedence MUST be calculated by separating the version into major, minor, patch
     * and pre-release identifiers in that order (Build metadata does not figure into
     * precedence). Precedence is determined by the first difference when comparing each
     * of these identifiers from left to right as follows: Major, minor, and patch
     * versions are always compared numerically. Example: 1.0.0 &lt; 2.0.0 &lt; 2.1.0 &lt;
     * 2.1.1. When major, minor, and patch are equal, a pre-release version has lower
     * precedence than a normal version. Example: 1.0.0-alpha &lt; 1.0.0. Precedence for
     * two pre-release versions with the same major, minor, and patch version MUST be
     * determined by comparing each dot separated identifier from left to right until a
     * difference is found as follows: identifiers consisting of only digits are compared
     * numerically and identifiers with letters or hyphens are compared lexically in ASCII
     * sort order. Numeric identifiers always have lower precedence than non-numeric
     * identifiers. A larger set of pre-release fields has a higher precedence than a
     * smaller set, if all of the preceding identifiers are equal. Example: 1.0.0-alpha
     * &lt; 1.0.0-alpha.1 &lt; 1.0.0-alpha.beta &lt; 1.0.0-beta &lt; 1.0.0-beta.2 &lt;
     * 1.0.0-beta.11 &lt; 1.0.0-rc.1 &lt; 1.0.0. </em>
     * </p>
     *
     * <p>
     * This method fulfills the general contract for Java's {@link Comparator Comparators}
     * and {@link Comparable Comparables}.
     * </p>
     *
     * @param v1 The first version for comparison.
     * @param v2 The second version for comparison.
     * @return A value below 0 iff {@code v1 &lt; v2}, a value above 0 iff
     *         {@code v1 &gt; v2</tt> and 0 iff <tt>v1 = v2}.
     * @throws NullPointerException If either parameter is null.
     * @since 0.2.0
     */
    public static int compare(VersionRegEx v1, VersionRegEx v2) {
        // throw NPE to comply with Comparable specification
        if (v1 == null) {
            throw new NullPointerException("v1 is null");
        } else if (v2 == null) {
            throw new NullPointerException("v2 is null");
        }
        return compare(v1, v2, false);
    }

    /**
     * Compares two Versions with additionally considering the build meta data field if
     * all other parts are equal. Note: This is <em>not</em> part of the semantic version
     * specification.
     *
     * <p>
     * Comparison of the build meta data parts happens exactly as for pre release
     * identifiers. Considering of build meta data first kicks in if both versions are
     * equal when using their natural order.
     * </p>
     *
     * <p>
     * This method fulfills the general contract for Java's {@link Comparator Comparators}
     * and {@link Comparable Comparables}.
     * </p>
     *
     * @param v1 The first version for comparison.
     * @param v2 The second version for comparison.
     * @return A value below 0 iff {@code v1 &lt; v2}, a value above 0 iff
     *         {@code v1 &gt; v2</tt> and 0 iff <tt>v1 = v2}.
     * @throws NullPointerException If either parameter is null.
     * @since 0.3.0
     */
    public static int compareWithBuildMetaData(VersionRegEx v1, VersionRegEx v2) {
        // throw NPE to comply with Comparable specification
        if (v1 == null) {
            throw new NullPointerException("v1 is null");
        } else if (v2 == null) {
            throw new NullPointerException("v2 is null");
        }
        return compare(v1, v2, true);
    }

    private static int compare(VersionRegEx v1, VersionRegEx v2, boolean withBuildMetaData) {
        int result = 0;
        if (v1 != v2) {
            final int mc, mm, mp, pr, md;
            if ((mc = compareInt(v1.major, v2.major)) != 0) {
                result = mc;
            } else if ((mm = compareInt(v1.minor, v2.minor)) != 0) {
                result = mm;
            } else if ((mp = compareInt(v1.patch, v2.patch)) != 0) {
                result = mp;
            } else if ((pr = comparePreRelease(v1, v2)) != 0) {
                result = pr;
            } else if (withBuildMetaData && ((md = compareBuildMetaData(v1, v2)) != 0)) {
                result = md;
            }
        }
        return result;
    }

    private static int compareInt(int a, int b) {
        return a - b;
    }

    private static int comparePreRelease(VersionRegEx v1, VersionRegEx v2) {
        return compareLiterals(v1.getPreRelease(), v2.getPreRelease());
    }

    private static int compareBuildMetaData(VersionRegEx v1, VersionRegEx v2) {
        return compareLiterals(v1.getBuildMetaData(), v2.getBuildMetaData());
    }

    private static int compareLiterals(String v1Literal, String v2Literal) {
        int result = 0;
        if (!v1Literal.isEmpty() && !v2Literal.isEmpty()) {
            // compare pre release parts
            result = compareIdentifiers(v1Literal.split("\\."), v2Literal.split("\\."));
        } else if (!v1Literal.isEmpty()) {
            // other is greater, because it is no pre release
            result = -1;
        } else if (!v2Literal.isEmpty()) {
            // this is greater because other is no pre release
            result = 1;
        }
        return result;
    }

    private static int compareIdentifiers(String[] parts1, String[] parts2) {
        final int min = Math.min(parts1.length, parts2.length);
        for (int i = 0; i < min; ++i) {
            final int r = compareIdentifierParts(parts1[i], parts2[i]);
            if (r != 0) {
                // versions differ in part i
                return r;
            }
        }

        // all id's are equal, so compare amount of id's
        return compareInt(parts1.length, parts2.length);
    }

    private static int compareIdentifierParts(String p1, String p2) {
        final int num1 = isNumeric(p1);
        final int num2 = isNumeric(p2);

        final int result;
        if (num1 < 0 && num2 < 0) {
            // both are not numerical -> compare lexically
            result = p1.compareTo(p2);
        } else if (num1 >= 0 && num2 >= 0) {
            // both are numerical
            result = compareInt(num1, num2);
        } else if (num1 >= 0) {
            // only part1 is numerical -> p2 is greater
            result = -1;
        } else {
            // only part2 is numerical -> p1 is greater
            result = 1;
        }
        return result;
    }

    /**
     * Determines whether s is a positive number. If it is, the number is returned,
     * otherwise the result is -1.
     *
     * @param s The String to check.
     * @return The positive number (incl. 0) if s a number, or -1 if it is not.
     */
    private static int isNumeric(String s) {
        try {
            return Integer.parseInt(s);
        } catch (final NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Creates a new Version from the provided components. Neither value of
     * {@code major, minor} or {@code patch} must be lower than 0 and at least one must be
     * greater than zero. {@code preRelease} or {@code buildMetaData} may be the empty
     * String. In this case, the created {@code Version} will have no pre release resp.
     * build meta data field. If those parameters are not empty, they must conform to the
     * semantic version specification.
     *
     * @param major The major version.
     * @param minor The minor version.
     * @param patch The patch version.
     * @param preRelease The pre release version or the empty string.
     * @param buildMetaData The build meta data field or the empty string.
     * @return The version instance.
     * @throws VersionFormatException If {@code preRelease} or {@code buildMetaData} does
     *             not conform to the semantic version specification.
     */
    public static final VersionRegEx create(int major, int minor, int patch,
            String preRelease,
            String buildMetaData) {
        checkParams(major, minor, patch);
        require(preRelease != null, "preRelease is null");
        require(buildMetaData != null, "buildMetaData is null");

        if (!isValidPreRelease(preRelease)) {
            throw new VersionFormatException(
                    String.format("Illegal pre-release part: %s", preRelease));
        } else if (!isValidBuildMetaData(buildMetaData)) {
            throw new VersionFormatException(
                    String.format("Illegal build-meta-data part: %s", buildMetaData));
        }
        return new VersionRegEx(major, minor, patch, preRelease, buildMetaData);
    }

    /**
     * Creates a new Version from the provided components. The version's build meta data
     * field will be empty. Neither value of {@code major, minor} or {@code patch} must be
     * lower than 0 and at least one must be greater than zero. {@code preRelease} may be
     * the empty String. In this case, the created version will have no pre release field.
     * If it is not empty, it must conform to the specifications of the semantic version.
     *
     * @param major The major version.
     * @param minor The minor version.
     * @param patch The patch version.
     * @param preRelease The pre release version or the empty string.
     * @return The version instance.
     * @throws VersionFormatException If {@code preRelease} is not empty and does not
     *             conform to the semantic versioning specification
     */
    public static final VersionRegEx create(int major, int minor, int patch,
            String preRelease) {
        checkParams(major, minor, patch);
        require(preRelease != null, "preRelease is null");

        if (!preRelease.isEmpty() && !PRE_RELEASE.matcher(preRelease).matches()) {
            throw new VersionFormatException(preRelease);
        }
        return new VersionRegEx(major, minor, patch, preRelease, "");
    }

    /**
     * Creates a new Version from the three provided components. The version's pre release
     * and build meta data fields will be empty. Neither value must be lower than 0 and at
     * least one must be greater than zero
     *
     * @param major The major version.
     * @param minor The minor version.
     * @param patch The patch version.
     * @return The version instance.
     */
    public static final VersionRegEx create(int major, int minor, int patch) {
        checkParams(major, minor, patch);
        return new VersionRegEx(major, minor, patch, "", "");
    }

    private static void checkParams(int major, int minor, int patch) {
        require(major >= 0, "major < 0");
        require(minor >= 0, "minor < 0");
        require(patch >= 0, "patch < 0");
        require(major != 0 || minor != 0 || patch != 0, "all parts are 0");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Tries to parse the provided String as a semantic version. If the string does not
     * conform to the semantic version specification, a {@link VersionFormatException}
     * will be thrown.
     *
     * @param versionString The String to parse.
     * @return The parsed version.
     * @throws VersionFormatException If the String is no valid version
     * @throws IllegalArgumentException If {@code versionString} is <code>null</code>.
     */
    public static final VersionRegEx parseVersion(String versionString) {
        require(versionString != null, "versionString is null");
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

        return new VersionRegEx(major, minor, patch, preRelease, buildMD);
    }

    /**
     * Tries to parse the provided String as a semantic version. If
     * {@code allowPreRelease} is <code>false</code>, the String must have neither a
     * pre-release nor a build meta data part. Thus the given String must have the format
     * {@code X.Y.Z} where at least one part must be greater than zero.
     *
     * <p>
     * If {@code allowPreRelease} is <code>true</code>, the String is parsed according to
     * the normal semantic version specification.
     * </p>
     *
     * @param versionString The String to parse.
     * @param allowPreRelease Whether pre-release and build meta data field are allowed.
     * @return The parsed version.
     * @throws VersionFormatException If the String is no valid version
     * @since 0.4.0
     */
    public static VersionRegEx parseVersion(String versionString, boolean allowPreRelease) {
        final VersionRegEx version = parseVersion(versionString);
        if (!allowPreRelease && (version.isPreRelease() || version.hasBuildMetaData())) {
            throw new VersionFormatException(String.format(
                    "Version is expected to have no pre-release or build meta data part"));
        }
        return version;
    }

    /**
     * Returns the lower of this version and the given version according to its natural
     * ordering. If versions are equal, {@code this} is returned.
     *
     * @param other The version to compare with.
     * @return The lower version.
     * @throws IllegalArgumentException If {@code other} is <code>null</code>.
     * @since 0.5.0
     * @see #min(VersionRegEx, VersionRegEx)
     */
    public VersionRegEx min(VersionRegEx other) {
        return min(this, other);
    }

    /**
     * Returns the greater of this version and the given version according to its natural
     * ordering. If versions are equal, {@code this} is returned.
     *
     * @param other The version to compare with.
     * @return The greater version.
     * @throws IllegalArgumentException If {@code other} is <code>null</code>.
     * @since 0.5.0
     * @see #max(VersionRegEx, VersionRegEx)
     */
    public VersionRegEx max(VersionRegEx other) {
        return max(this, other);
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
     * Gets the pre release parts of this version as array by splitting the pre result
     * version string at the dots.
     *
     * @return Pre release version as array. Array is empty if this version has no pre
     *         release part.
     */
    public String[] getPreReleaseParts() {
        return this.preRelease.isEmpty() ? EMPTY_ARRAY : this.preRelease.split("\\.");
    }

    /**
     * Gets the pre release identifier of this version. If this version has no such
     * identifier, an empty string is returned.
     *
     * @return This version's pre release identifier or an empty String if this version
     *         has no such identifier.
     */
    public String getPreRelease() {
        return this.preRelease;
    }

    /**
     * Gets this version's build meta data. If this version has no build meta data, the
     * returned string is empty.
     *
     * @return The build meta data or an empty String if this version has no build meta
     *         data.
     */
    public String getBuildMetaData() {
        return this.buildMetaData;
    }

    /**
     * Gets this version's build meta data as array by splitting the meta data at dots. If
     * this version has no build meta data, the result is an empty array.
     *
     * @return The build meta data as array.
     */
    public String[] getBuildMetaDataParts() {
        return this.buildMetaData.isEmpty() ? EMPTY_ARRAY
                : this.buildMetaData.split("\\.");
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
     * Creates a String representation of this version by joining its parts together as by
     * the semantic version specification.
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
     * The hash code for a version instance is computed from the fields {@link #getMajor()
     * major}, {@link #getMinor() minor}, {@link #getPatch() patch} and
     * {@link #getPreRelease() pre-release}.
     *
     * @return A hash code for this object.
     */
    @Override
    public int hashCode() {
        int h = this.hash;
        if (h == 0) {
            h = HASH_PRIME + this.major;
            h = HASH_PRIME * h + this.minor;
            h = HASH_PRIME * h + this.patch;
            h = HASH_PRIME * h + this.preRelease.hashCode();
            this.hash = h;
        }
        return this.hash;
    }

    /**
     * Determines whether this version is equal to the passed object. This is the case if
     * the passed object is an instance of Version and this version
     * {@link #compareTo(VersionRegEx) compared} to the provided one yields 0. Thus, this
     * method ignores the {@link #getBuildMetaData()} field.
     *
     * @param obj the object to compare with.
     * @return <code>true</code> iff {@code obj} is an instance of {@code Version} and
     *         {@code this.compareTo((Version) obj) == 0}
     * @see #compareTo(VersionRegEx)
     */
    @Override
    public boolean equals(Object obj) {
        return testEquality(obj, false);
    }

    /**
     * Determines whether this version is equal to the passed object (as determined by
     * {@link #equals(Object)} and additionally considers the build meta data part of both
     * versions for equality.
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
        return obj == this || obj instanceof VersionRegEx
                && compare(this, (VersionRegEx) obj, includeBuildMd) == 0;
    }

    /**
     * Compares this version to the provided one, following the <em>semantic
     * versioning</em> specification. See {@link #compare(VersionRegEx, VersionRegEx)} for more
     * information.
     *
     * @param other The version to compare to.
     * @return A value lower than 0 if this &lt; other, a value greater than 0 if this
     *         &gt; other and 0 if this == other. The absolute value of the result has no
     *         semantical interpretation.
     */
    @Override
    public int compareTo(VersionRegEx other) {
        return compare(this, other);
    }

    /**
     * Compares this version to the provided one. Unlike the {@link #compareTo(VersionRegEx)}
     * method, this one additionally considers the build meta data field of both versions,
     * if all other parts are equal. Note: This is <em>not</em> part of the semantic
     * version specification.
     *
     * <p>
     * Comparison of the build meta data parts happens exactly as for pre release
     * identifiers. Considering of build meta data first kicks in if both versions are
     * equal when using their natural order.
     * </p>
     *
     * @param other The version to compare to.
     * @return A value lower than 0 if this &lt; other, a value greater than 0 if this
     *         &gt; other and 0 if this == other. The absolute value of the result has no
     *         semantical interpretation.
     * @since 0.3.0
     */
    public int compareToWithBuildMetaData(VersionRegEx other) {
        return compareWithBuildMetaData(this, other);
    }
}
