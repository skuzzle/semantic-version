/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Simon Taddiken
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

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.*;

/**
 * This class is an implementation of the full <em>semantic version 2.0.0</em>
 * <a href="http://semver.org/">specification</a>. Instances can be obtained using the
 * static overloads of the <em>create</em> method or by {@link #parseVersion(String)
 * parsing} a String. This class implements {@link Comparable} to compare two versions by
 * following the specifications linked to above. The {@link #equals(Object)} method
 * conforms to the result of {@link #compareTo(Version)}, {@link #hashCode()} is
 * implemented appropriately. Neither method considers the {@link #getBuildMetaData()
 * build meta data} field for comparison.
 *
 * <p>
 * Instances of this class are immutable and thus thread safe. This also means that all
 * methods taking an array or other kind of modifiable objects as input, will first make a
 * copy before using it as internal state.
 *
 *
 * <p>
 * Note that unless stated otherwise, none of the public methods of this class accept
 * <code>null</code> values. Most methods will throw an {@link IllegalArgumentException}
 * when encountering a <code>null</code> argument. However, to comply with the
 * {@link Comparable} contract, the comparison methods will throw a
 * {@link NullPointerException} instead.
 *
 *
 * @author Simon Taddiken
 */
public final class Version implements Comparable<Version>, Serializable {

    /** Conforms to all Version implementations since 0.6.0 */
    private static final long serialVersionUID = 6034927062401119911L;

    private static final String[] EMPTY_ARRAY = new String[0];

    /**
     * The minimum value '0.0.0' for valid versions where all parts are 0 or empty.
     *
     * @since 2.1.0
     */
    public static final Version ZERO = Version.create(0, 0, 0);

    /**
     * Semantic Version Specification to which this class complies.
     *
     * @since 0.2.0
     */
    public static final Version COMPLIANCE = Version.create(2, 0, 0);

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
        private VersionFormatException(String message) {
            super(message);
        }
    }

    /**
     * Comparator for natural version ordering. See {@link #compare(Version, Version)} for
     * more information.
     * <p>
     * Instead of using this field, consider using a method reference like in
     * <code>Version::compare</code>.
     *
     * @since 0.2.0
     */
    public static final Comparator<Version> NATURAL_ORDER = new Comparator<Version>() {

        @Override
        public int compare(Version o1, Version o2) {
            return o1.compareTo(o2);
        }
    };

    /**
     * Comparator for ordering versions with additionally considering the build meta data
     * field when comparing versions.
     * <p>
     * Instead of using this field, consider using a method reference like in
     * <code>Version::compareWithBuildMetaData</code>.
     * <p>
     * Note: this comparator imposes orderings that are inconsistent with equals.
     *
     * @since 0.3.0
     */
    public static final Comparator<Version> WITH_BUILD_META_DATA_ORDER = new Comparator<Version>() {

        @Override
        public int compare(Version o1, Version o2) {
            return o1.compareToWithBuildMetaData(o2);
        }
    };

    private static final int TO_STRING_ESTIMATE = 16;

    // state machine states for parsing a version string
    private static final int STATE_MAJOR_INIT = 0;
    private static final int STATE_MAJOR_LEADING_ZERO = 1;
    private static final int STATE_MAJOR_DEFAULT = 2;
    private static final int STATE_MINOR_INIT = 3;
    private static final int STATE_MINOR_LEADING_ZERO = 4;
    private static final int STATE_MINOR_DEFAULT = 5;
    private static final int STATE_PATCH_INIT = 6;
    private static final int STATE_PATCH_LEADING_ZERO = 7;
    private static final int STATE_PATCH_DEFAULT = 8;
    private static final int STATE_PRERELEASE_INIT = 9;
    private static final int STATE_BUILDMD_INIT = 10;

    private static final int STATE_PART_INIT = 0;
    private static final int STATE_PART_LEADING_ZERO = 1;
    private static final int STATE_PART_NUMERIC = 2;
    private static final int STATE_PART_DEFAULT = 3;

    private static final int DECIMAL = 10;

    private static final int EOS = -1;
    private static final int FAILURE = -2;

    private final int major;
    private final int minor;
    private final int patch;
    private final String[] preReleaseParts;
    private final String[] buildMetaDataParts;

    // Since 1.1.0
    // these fields are only necessary for deserializing previous versions
    // see #readResolve method
    @Deprecated
    private String preRelease;
    @Deprecated
    private String buildMetaData;

    // store hash code once it has been calculated
    private static final int NOT_YET_CALCULATED = 2;
    private static final int HASH_PRIME = 31;
    private volatile int hash = NOT_YET_CALCULATED;

    private Version(int major, int minor, int patch, String[] preRelease,
            String[] buildMd) {
        checkParams(major, minor, patch);
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preReleaseParts = preRelease;
        this.buildMetaDataParts = buildMd;
    }

    private static Version parse(String s, boolean verifyOnly) {
        /*
         * Since 1.1.0:
         *
         * This huge and ugly inline parsing replaces the prior regex because it is way
         * faster. Besides that it also does provide better error messages in case a
         * String could not be parsed correctly. Condition and mutation coverage is
         * extremely high to ensure correctness.
         */

        // note: getting the char array once is faster than calling charAt multiple times
        final char[] stream = s.toCharArray();
        int major = 0;
        int minor = 0;
        int patch = 0;
        int state = STATE_MAJOR_INIT;

        List<String> preRelease = null;
        List<String> buildMd = null;
        loop: for (int i = 0; i <= stream.length; ++i) {
            final int c = i < stream.length ? stream[i] : EOS;

            switch (state) {

            // Parse major part
            case STATE_MAJOR_INIT:
                if (c == '0') {
                    state = STATE_MAJOR_LEADING_ZERO;
                } else if (c >= '1' && c <= '9') {
                    major = major * DECIMAL + Character.digit(c, DECIMAL);
                    state = STATE_MAJOR_DEFAULT;
                } else if (verifyOnly) {
                    return null;
                } else {
                    throw unexpectedChar(s, c);
                }
                break;
            case STATE_MAJOR_LEADING_ZERO:
                if (c == '.') {
                    // single 0 is allowed
                    state = STATE_MINOR_INIT;
                } else if (c >= '0' && c <= '9') {
                    if (verifyOnly) {
                        return null;
                    }
                    throw illegalLeadingChar(s, '0', "major");
                } else if (verifyOnly) {
                    return null;
                } else {
                    throw unexpectedChar(s, c);
                }
                break;
            case STATE_MAJOR_DEFAULT:
                if (c >= '0' && c <= '9') {
                    major = major * DECIMAL + Character.digit(c, DECIMAL);
                } else if (c == '.') {
                    state = STATE_MINOR_INIT;
                } else if (verifyOnly) {
                    return null;
                } else {
                    throw unexpectedChar(s, c);
                }
                break;

            // parse minor part
            case STATE_MINOR_INIT:
                if (c == '0') {
                    state = STATE_MINOR_LEADING_ZERO;
                } else if (c >= '1' && c <= '9') {
                    minor = minor * DECIMAL + Character.digit(c, DECIMAL);
                    state = STATE_MINOR_DEFAULT;
                } else if (verifyOnly) {
                    return null;
                } else {
                    throw unexpectedChar(s, c);
                }
                break;
            case STATE_MINOR_LEADING_ZERO:
                if (c == '.') {
                    // single 0 is allowed
                    state = STATE_PATCH_INIT;
                } else if (c >= '0' && c <= '9') {
                    if (verifyOnly) {
                        return null;
                    }
                    throw illegalLeadingChar(s, '0', "minor");
                } else if (verifyOnly) {
                    return null;
                } else {
                    throw unexpectedChar(s, c);
                }
                break;
            case STATE_MINOR_DEFAULT:
                if (c >= '0' && c <= '9') {
                    minor = minor * DECIMAL + Character.digit(c, DECIMAL);
                } else if (c == '.') {
                    state = STATE_PATCH_INIT;
                } else if (verifyOnly) {
                    return null;
                } else {
                    throw unexpectedChar(s, c);
                }
                break;

            // parse patch part
            case STATE_PATCH_INIT:
                if (c == '0') {
                    state = STATE_PATCH_LEADING_ZERO;
                } else if (c >= '1' && c <= '9') {
                    patch = patch * DECIMAL + Character.digit(c, DECIMAL);
                    state = STATE_PATCH_DEFAULT;
                } else if (verifyOnly) {
                    return null;
                } else {
                    throw unexpectedChar(s, c);
                }
                break;
            case STATE_PATCH_LEADING_ZERO:
                if (c == '-') {
                    // single 0 is allowed
                    state = STATE_PRERELEASE_INIT;
                } else if (c == '+') {
                    state = STATE_BUILDMD_INIT;
                } else if (c == EOS) {
                    break loop;
                } else if (c >= '0' && c <= '9') {
                    if (verifyOnly) {
                        return null;
                    }
                    throw illegalLeadingChar(s, '0', "patch");
                } else if (verifyOnly) {
                    return null;
                } else {
                    throw unexpectedChar(s, c);
                }
                break;
            case STATE_PATCH_DEFAULT:
                if (c >= '0' && c <= '9') {
                    patch = patch * DECIMAL + Character.digit(c, DECIMAL);
                } else if (c == '-') {
                    state = STATE_PRERELEASE_INIT;
                } else if (c == '+') {
                    state = STATE_BUILDMD_INIT;
                } else if (c != EOS) {
                    // eos is allowed here
                    if (verifyOnly) {
                        return null;
                    }
                    throw unexpectedChar(s, c);
                }
                break;
            case STATE_PRERELEASE_INIT:

                preRelease = verifyOnly ? null : new ArrayList<String>();
                i = parseID(stream, s, i, verifyOnly, false, true, preRelease,
                        "pre-release");
                if (i == FAILURE) {
                    // implies verifyOnly == true, otherwise exception would have been
                    // thrown
                    return null;
                }
                final int c1 = i < stream.length ? stream[i] : EOS;

                if (c1 == '+') {
                    state = STATE_BUILDMD_INIT;
                } else {
                    break loop;
                }
                break;

            case STATE_BUILDMD_INIT:
                buildMd = verifyOnly ? null : new ArrayList<String>();
                i = parseID(stream, s, i, verifyOnly, true, false, buildMd,
                        "build-meta-data");
                if (i == FAILURE) {
                    // implies verifyOnly == true, otherwise exception would have been
                    // thrown
                    return null;
                }

                break loop;
            default:
                throw new IllegalStateException("Illegal state: " + state);
            }
        }
        final String[] prerelease = preRelease == null ? EMPTY_ARRAY
                : preRelease.toArray(new String[preRelease.size()]);
        final String[] buildmetadata = buildMd == null ? EMPTY_ARRAY
                : buildMd.toArray(new String[buildMd.size()]);
        return new Version(major, minor, patch, prerelease, buildmetadata);
    }

    private static int parseID(char[] stream, String full, int start, boolean verifyOnly,
            boolean allowLeading0, boolean preRelease, List<String> parts,
            String partName) {

        assert verifyOnly || parts != null;

        final StringBuilder b = verifyOnly
                ? null
                : new StringBuilder(stream.length - start);

        int i = start;
        while (i <= stream.length) {

            i = parseIDPart(stream, full, i, verifyOnly, allowLeading0, preRelease, true,
                    b, partName);
            if (i == FAILURE) {
                // implies verifyOnly == true, otherwise exception would have been thrown
                return FAILURE;
            } else if (!verifyOnly) {
                parts.add(b.toString());
            }

            final int c = i < stream.length ? stream[i] : EOS;
            if (c == '.') {
                // keep looping
                ++i;
            } else {
                // identifier is done (hit EOS or '+')
                return i;
            }
        }
        throw new IllegalStateException();
    }

    private static int parseIDPart(char[] stream, String full, int start,
            boolean verifyOnly,
            boolean allowLeading0, boolean preRelease, boolean allowDot,
            StringBuilder b, String partName) {

        if (b != null) {
            b.setLength(0);
        }

        int state = STATE_PART_INIT;
        for (int i = start; i <= stream.length; ++i) {
            final int c = i < stream.length ? stream[i] : EOS;

            switch (state) {
            case STATE_PART_INIT:
                if (c == '0' && !allowLeading0) {
                    state = STATE_PART_LEADING_ZERO;
                    if (b != null) {
                        b.append('0');
                    }
                } else if (c == '-' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z'
                        || c >= '0' && c <= '9') {
                    if (b != null) {
                        b.appendCodePoint(c);
                    }
                    state = STATE_PART_DEFAULT;
                } else if (c == '.') {
                    if (verifyOnly) {
                        return FAILURE;
                    }
                    throw unexpectedChar(full, -1);
                } else {
                    if (verifyOnly) {
                        return FAILURE;
                    }
                    throw unexpectedChar(full, c);
                }
                break;
            case STATE_PART_LEADING_ZERO:
                // when in this state we consumed a single '0'
                if (c == '-' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z') {
                    if (b != null) {
                        b.appendCodePoint(c);
                    }
                    state = STATE_PART_DEFAULT;
                } else if (c >= '0' && c <= '9') {
                    if (b != null) {
                        b.appendCodePoint(c);
                    }
                    state = STATE_PART_NUMERIC;
                } else if (c == '.' && allowDot || c == EOS || c == '+' && preRelease) {
                    // if we are parsing a pre release part it can be terminated by a
                    // '+' in case a build meta data follows

                    // here, this part consist of a single '0'
                    return i;
                } else if (verifyOnly) {
                    return FAILURE;
                } else {
                    throw unexpectedChar(full, c);
                }
                break;
            case STATE_PART_NUMERIC:
                // when in this state, the part began with a '0' and we only consumed
                // numeric chars so far
                if (c == '-' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z') {
                    if (b != null) {
                        b.appendCodePoint(c);
                    }
                    state = STATE_PART_DEFAULT;
                } else if (c >= '0' && c <= '9') {
                    if (b != null) {
                        b.appendCodePoint(c);
                    }
                } else if (c == '.' || c == EOS || c == '+' && preRelease) {
                    // if we are parsing a pre release part it can be terminated by a
                    // '+' in case a build meta data follows

                    if (verifyOnly) {
                        return FAILURE;
                    }
                    throw illegalLeadingChar(full, '0', partName);
                } else if (verifyOnly) {
                    return FAILURE;
                } else {
                    throw unexpectedChar(full, c);
                }
                break;
            case STATE_PART_DEFAULT:
                if (c == '-' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z'
                        || c >= '0' && c <= '9') {
                    if (b != null) {
                        b.appendCodePoint(c);
                    }
                } else if (c == '.' && allowDot || c == EOS || c == '+' && preRelease) {
                    // if we are parsing a pre release part it can be terminated by a
                    // '+' in case a build meta data follows
                    return i;
                } else if (verifyOnly) {
                    return FAILURE;
                } else {
                    throw unexpectedChar(full, c);
                }
                break;

            }
        }

        throw new IllegalStateException();
    }

    private static VersionFormatException illegalLeadingChar(String v, int c,
            String part) {
        return new VersionFormatException(
                String.format("Illegal leading char '%c' in %s part of %s", c, part, v));
    }

    private static VersionFormatException unexpectedChar(String v, int c) {
        if (c == -1) {
            return new VersionFormatException(String.format(
                    "Incomplete version part in %s", v));
        }
        return new VersionFormatException(
                String.format("Unexpected char in %s: %c", v, c));
    }

    /**
     * Creates a new Version from this one, replacing only the major part with the given
     * one. All other parts will remain the same as in this Version.
     *
     * @param newMajor The new major version.
     * @return A new Version.
     * @throws IllegalArgumentException If all fields in the resulting Version are 0.
     * @since 1.1.0
     */
    public Version withMajor(int newMajor) {
        return new Version(newMajor, this.minor, this.patch, this.preReleaseParts,
                this.buildMetaDataParts);
    }

    /**
     * Creates a new Version from this one, replacing only the minor part with the given
     * one. All other parts will remain the same as in this Version.
     *
     * @param newMinor The new minor version.
     * @return A new Version.
     * @throws IllegalArgumentException If all fields in the resulting Version are 0.
     * @since 1.1.0
     */
    public Version withMinor(int newMinor) {
        return new Version(this.major, newMinor, this.patch, this.preReleaseParts,
                this.buildMetaDataParts);
    }

    /**
     * Creates a new Version from this one, replacing only the patch part with the given
     * one. All other parts will remain the same as in this Version.
     *
     * @param newPatch The new patch version.
     * @return A new Version.
     * @throws IllegalArgumentException If all fields in the resulting Version are 0.
     * @since 1.1.0
     */
    public Version withPatch(int newPatch) {
        return new Version(this.major, this.minor, newPatch, this.preReleaseParts,
                this.buildMetaDataParts);
    }

    /**
     * Creates a new Version from this one, replacing only the pre-release part with the
     * given String. All other parts will remain the same as in this Version. You can
     * remove the pre-release part by passing an empty String.
     *
     * @param newPreRelease The new pre-release identifier.
     * @return A new Version.
     * @throws VersionFormatException If the given String is not a valid pre-release
     *             identifier.
     * @throws IllegalArgumentException If preRelease is null.
     * @since 1.1.0
     */
    public Version withPreRelease(String newPreRelease) {
        require(newPreRelease != null, "newPreRelease is null");
        final String[] newPreReleaseParts = parsePreRelease(newPreRelease);
        return new Version(this.major, this.minor, this.patch, newPreReleaseParts,
                this.buildMetaDataParts);
    }

    /**
     * Creates a new Version from this one, replacing only the pre-release part with the
     * given array. All other parts will remain the same as in this Version. You can
     * remove the pre-release part by passing an empty array.
     * <p>
     * The passed array will be copied to not allow external modification to the new
     * Version's inner state.
     *
     * <p>
     * A single part within the array is allowed to contain a dot ('.'). Such parts will
     * be treated as if the array contained those parts as single elements.
     *
     *
     * <pre>
     * v.withPreRelease(new String[] { "a.b" })
     * &lt;=&gt;
     * v.withPreRelease(new String[] { "a", "b" })
     * </pre>
     *
     * @param newPreRelease the new pre release parts.
     * @return A new Version.
     * @throws VersionFormatException If the any element of the given array is not a valid
     *             pre release identifier part.
     * @throws IllegalArgumentException If newPreRelease is null.
     * @since 1.2.0
     */
    public Version withPreRelease(String[] newPreRelease) {
        require(newPreRelease != null, "newPreRelease is null");
        final String joined = join(newPreRelease);
        final String[] newPreReleaseParts = parsePreRelease(joined);
        return new Version(this.major, this.minor, this.patch, newPreReleaseParts,
                this.buildMetaDataParts);
    }

    /**
     * Creates a new Version from this one, replacing only the build-meta-data part with
     * the given String. All other parts will remain the same as in this Version. You can
     * remove the build-meta-data part by passing an empty String.
     *
     * @param newBuildMetaData The new build meta data identifier.
     * @return A new Version.
     * @throws VersionFormatException If the given String is not a valid build-meta-data
     *             identifier.
     * @throws IllegalArgumentException If newBuildMetaData is null.
     * @since 1.1.0
     */
    public Version withBuildMetaData(String newBuildMetaData) {
        require(newBuildMetaData != null, "newBuildMetaData is null");
        final String[] newBuildMdParts = parseBuildMd(newBuildMetaData);
        return new Version(this.major, this.minor, this.patch, this.preReleaseParts,
                newBuildMdParts);
    }

    /**
     * Creates a new Version from this one, replacing only the build-meta-data part with
     * the given array. All other parts will remain the same as in this Version. You can
     * remove the build-meta-data part by passing an empty array.
     * <p>
     * The passed array will be copied to not allow external modification to the new
     * Version's inner state.
     *
     * A single part within the array is allowed to contain a dot ('.'). Such parts will
     * be treated as if the array contained those parts as single elements.
     *
     *
     * <pre>
     * v.withBuildMetaData(new String[] { "a.b" })
     * &lt;=&gt;
     * v.withBuildMetaData(new String[] { "a", "b" })
     * </pre>
     *
     * @param newBuildMetaData the new build meta data parts.
     * @return A new Version.
     * @throws VersionFormatException If the any element of the given array is not a valid
     *             build meta data identifier part.
     * @throws IllegalArgumentException If newBuildMetaData is null.
     * @since 1.2.0
     */
    public Version withBuildMetaData(String[] newBuildMetaData) {
        require(newBuildMetaData != null, "newBuildMetaData is null");
        final String joined = join(newBuildMetaData);
        final String[] newBuildMdParts = parseBuildMd(joined);
        return new Version(this.major, this.minor, this.patch, this.preReleaseParts,
                newBuildMdParts);
    }

    private String[] verifyAndCopyArray(String parts[], boolean allowLeading0) {
        final String[] result = new String[parts.length];
        for (int i = 0; i < parts.length; ++i) {
            final String part = parts[i];
            require(part != null, "version part is null");
            if (part.isEmpty()) {
                throw new VersionFormatException(
                        "Incomplete version part in " + join(parts));
            }
            result[i] = part;

            // note: pass "pre-release" because this string will not be used when parsing
            // build-meta-data
            parseIDPart(part.toCharArray(), part, 0, false, allowLeading0, false, false,
                    null, "pre-release");
        }
        return result;
    }

    /**
     * Drops both the pre-release and the build meta data from this version.
     *
     * @return The nearest stable version.
     * @since 2.1.0
     */
    public Version toStable() {
        return new Version(this.major, this.minor, this.patch, EMPTY_ARRAY, EMPTY_ARRAY);
    }

    /**
     * Given this Version, returns the next major Version. That is, the major part is
     * incremented by 1 and the remaining parts are set to 0. This also drops the
     * pre-release and build-meta-data.
     *
     * @return The incremented version.
     * @see #nextMajor(String)
     * @see #nextMajor(String[])
     * @since 1.2.0
     */
    public Version nextMajor() {
        return new Version(this.major + 1, 0, 0, EMPTY_ARRAY, EMPTY_ARRAY);
    }

    /**
     * Given this Version, returns the next major Version. That is, the major part is
     * incremented by 1 and the remaining parts are set to 0. The pre-release part will be
     * set to the given identifier and the build-meta-data is dropped.
     *
     * @param newPrelease The pre-release part for the resulting Version.
     * @return The incremented version.
     * @throws VersionFormatException If the given String is not a valid pre-release
     *             identifier.
     * @throws IllegalArgumentException If newPreRelease is null.
     * @see #nextMajor()
     * @see #nextMajor(String[])
     * @since 1.2.0
     */
    public Version nextMajor(String newPrelease) {
        require(newPrelease != null, "newPreRelease is null");
        final String[] preReleaseParts = parsePreRelease(newPrelease);
        return new Version(this.major + 1, 0, 0, preReleaseParts, EMPTY_ARRAY);
    }

    /**
     * Given this Version, returns the next major Version. That is, the major part is
     * incremented by 1 and the remaining parts are set to 0. The pre-release part will be
     * set to the given identifier and the build-meta-data is dropped.
     *
     * @param newPrelease The pre-release part for the resulting Version.
     * @return The incremented version.
     * @throws VersionFormatException If the any element of the given array is not a valid
     *             pre release identifier part.
     * @throws IllegalArgumentException If newPreRelease is null.
     * @see #nextMajor()
     * @see #nextMajor(String)
     * @since 1.2.0
     */
    public Version nextMajor(String[] newPrelease) {
        require(newPrelease != null, "newPreRelease is null");
        final String[] newPreReleaseParts = verifyAndCopyArray(newPrelease, false);
        return new Version(this.major + 1, 0, 0, newPreReleaseParts, EMPTY_ARRAY);
    }

    /**
     * Given this version, returns the next minor version. That is, the major part remains
     * the same, the minor version is incremented and all other parts are reset/dropped.
     *
     * @return The incremented version.
     * @see #nextMinor(String)
     * @see #nextMinor(String[])
     * @since 1.2.0
     */
    public Version nextMinor() {
        return new Version(this.major, this.minor + 1, 0, EMPTY_ARRAY, EMPTY_ARRAY);
    }

    /**
     * Given this version, returns the next minor version. That is, the major part remains
     * the same and the minor version is incremented. The pre-release part will be set to
     * the given identifier and the build-meta-data is dropped.
     *
     * @param newPrelease The pre-release part for the resulting Version.
     * @return The incremented version.
     * @throws VersionFormatException If the given String is not a valid pre-release
     *             identifier.
     * @throws IllegalArgumentException If newPreRelease is null.
     * @see #nextMinor()
     * @see #nextMinor(String[])
     * @since 1.2.0
     */
    public Version nextMinor(String newPrelease) {
        require(newPrelease != null, "newPreRelease is null");
        final String[] preReleaseParts = parsePreRelease(newPrelease);
        return new Version(this.major, this.minor + 1, 0, preReleaseParts, EMPTY_ARRAY);
    }

    /**
     * Given this version, returns the next minor version. That is, the major part remains
     * the same and the minor version is incremented. The pre-release part will be set to
     * the given identifier and the build-meta-data is dropped.
     *
     * @param newPrelease The pre-release part for the resulting Version.
     * @return The incremented version.
     * @throws VersionFormatException If the any element of the given array is not a valid
     *             pre release identifier part.
     * @throws IllegalArgumentException If newPreRelease is null.
     * @see #nextMinor()
     * @see #nextMinor(String)
     * @since 1.2.0
     */
    public Version nextMinor(String[] newPrelease) {
        require(newPrelease != null, "newPreRelease is null");
        final String[] newPreReleaseParts = verifyAndCopyArray(newPrelease, false);
        return new Version(this.major, this.minor + 1, 0, newPreReleaseParts,
                EMPTY_ARRAY);
    }

    /**
     * Given this version, returns the next patch version. That is, the major and minor
     * parts remain the same, the patch version is incremented and all other parts are
     * reset/dropped.
     *
     * @return The incremented version.
     * @see #nextPatch(String)
     * @see #nextPatch(String[])
     * @since 1.2.0
     */
    public Version nextPatch() {
        return new Version(this.major, this.minor, this.patch + 1, EMPTY_ARRAY,
                EMPTY_ARRAY);
    }

    /**
     * Given this version, returns the next patch version. That is, the major and minor
     * parts remain the same and the patch version is incremented. The pre-release part
     * will be set to the given identifier and the build-meta-data is dropped.
     *
     * @param newPrelease The pre-release part for the resulting Version.
     * @return The incremented version.
     * @throws VersionFormatException If the given String is not a valid pre-release
     *             identifier.
     * @throws IllegalArgumentException If newPreRelease is null.
     * @see #nextPatch()
     * @see #nextPatch(String[])
     * @since 1.2.0
     */
    public Version nextPatch(String newPrelease) {
        require(newPrelease != null, "newPreRelease is null");
        final String[] preReleaseParts = parsePreRelease(newPrelease);
        return new Version(this.major, this.minor, this.patch + 1, preReleaseParts,
                EMPTY_ARRAY);
    }

    /**
     * Given this version, returns the next patch version. That is, the major and minor
     * parts remain the same and the patch version is incremented. The pre-release part
     * will be set to the given identifier and the build-meta-data is dropped.
     *
     * @param newPrelease The pre-release part for the resulting Version.
     * @return The incremented version.
     * @throws VersionFormatException If the any element of the given array is not a valid
     *             pre release identifier part.
     * @throws IllegalArgumentException If newPreRelease is null.
     * @see #nextPatch()
     * @see #nextPatch(String)
     * @since 1.2.0
     */
    public Version nextPatch(String[] newPrelease) {
        require(newPrelease != null, "newPreRelease is null");
        final String[] newPreReleaseParts = verifyAndCopyArray(newPrelease, false);
        return new Version(this.major, this.minor, this.patch + 1, newPreReleaseParts,
                EMPTY_ARRAY);
    }

    /**
     * Derives a new Version instance from this one by only incrementing the pre-release
     * identifier. The build-meta-data will be dropped, all other fields remain the same.
     *
     * <p>
     * The incrementation of the pre-release identifier behaves as follows:
     *
     * <ul>
     * <li>In case the identifier is currently empty, it becomes "1" in the result.</li>
     * <li>If the identifier's last part is numeric, that last part will be incremented in
     * the result.</li>
     * <li>If the last part is not numeric, the identifier is interpreted as
     * {@code identifier.0} which becomes {@code identifier.1} after increment.
     * </ul>
     * Examples:
     *
     * <table>
     * <caption>Pre-release identifier incrementation behavior</caption>
     * <tr>
     * <th>Version</th>
     * <th>After increment</th>
     * </tr>
     * <tr>
     * <td>1.2.3</td>
     * <td>1.2.3-1</td>
     * </tr>
     * <tr>
     * <td>1.2.3+build.meta.data</td>
     * <td>1.2.3-1</td>
     * </tr>
     * <tr>
     * <td>1.2.3-foo</td>
     * <td>1.2.3-foo.1</td>
     * </tr>
     * <tr>
     * <td>1.2.3-foo.1</td>
     * <td>1.2.3-foo.2</td>
     * </tr>
     * </table>
     *
     * @return The incremented Version.
     * @since 1.2.0
     */
    public Version nextPreRelease() {
        final String[] newPreReleaseParts = incrementIdentifier(this.preReleaseParts);
        return new Version(this.major, this.minor, this.patch, newPreReleaseParts,
                EMPTY_ARRAY);
    }

    /**
     * Derives a new Version instance from this one by only incrementing the
     * build-meta-data identifier. All other fields remain the same.
     *
     * <p>
     * The incrementation of the build-meta-data identifier behaves as follows:
     *
     * <ul>
     * <li>In case the identifier is currently empty, it becomes "1" in the result.</li>
     * <li>If the identifier's last part is numeric, that last part will be incremented in
     * the result. <b>Leading 0's will be removed</b>.</li>
     * <li>If the last part is not numeric, the identifier is interpreted as
     * {@code identifier.0} which becomes {@code identifier.1} after increment.
     * </ul>
     * Examples:
     *
     * <table>
     * <caption>Build meta data incrementation behavior</caption>
     * <tr>
     * <th>Version</th>
     * <th>After increment</th>
     * </tr>
     * <tr>
     * <td>1.2.3</td>
     * <td>1.2.3+1</td>
     * </tr>
     * <tr>
     * <td>1.2.3-pre.release</td>
     * <td>1.2.3-pre.release+1</td>
     * </tr>
     * <tr>
     * <td>1.2.3+foo</td>
     * <td>1.2.3+foo.1</td>
     * </tr>
     * <tr>
     * <td>1.2.3+foo.1</td>
     * <td>1.2.3+foo.2</td>
     * </tr>
     * </table>
     *
     * @return The incremented Version.
     * @since 1.2.0
     */
    public Version nextBuildMetaData() {
        final String[] newBuildMetaData = incrementIdentifier(this.buildMetaDataParts);
        return new Version(this.major, this.minor, this.patch, this.preReleaseParts,
                newBuildMetaData);
    }

    private String[] incrementIdentifier(String[] parts) {
        if (parts.length == 0) {
            return new String[] { "1" };
        }
        final int lastIdx = parts.length - 1;
        final String lastPart = parts[lastIdx];

        int num = isNumeric(lastPart);
        int newLength = parts.length;
        if (num >= 0) {
            num += 1;
        } else {
            newLength += 1;
            num = 1;
        }
        final String[] result = Arrays.copyOf(parts, newLength);
        result[newLength - 1] = String.valueOf(num);
        return result;
    }

    /**
     * Tries to parse the given String as a semantic version and returns whether the
     * String is properly formatted according to the semantic version specification.
     *
     * <p>
     * Note: this method does not throw an exception upon <code>null</code> input, but
     * returns <code>false</code> instead.
     *
     *
     * @param version The String to check.
     * @return Whether the given String is formatted as a semantic version.
     * @since 0.5.0
     */
    public static boolean isValidVersion(String version) {
        return version != null && !version.isEmpty() && parse(version, true) != null;
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
     *
     * @param preRelease The String to check.
     * @return Whether the given String is a valid pre-release identifier.
     * @since 0.5.0
     */
    public static boolean isValidPreRelease(String preRelease) {
        if (preRelease == null) {
            return false;
        } else if (preRelease.isEmpty()) {
            return true;
        }

        return parseID(preRelease.toCharArray(), preRelease, 0, true, false, false, null,
                "") != FAILURE;
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
     *
     *
     * @param buildMetaData The String to check.
     * @return Whether the given String is a valid build meta data identifier.
     * @since 0.5.0
     */
    public static boolean isValidBuildMetaData(String buildMetaData) {
        if (buildMetaData == null) {
            return false;
        } else if (buildMetaData.isEmpty()) {
            return true;
        }

        return parseID(buildMetaData.toCharArray(), buildMetaData, 0, true, true, false,
                null, "") != FAILURE;
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
    public static Version max(Version v1, Version v2) {
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
    public static Version min(Version v1, Version v2) {
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
     *
     *
     * <p>
     * This method fulfills the general contract for Java's {@link Comparator Comparators}
     * and {@link Comparable Comparables}.
     *
     *
     * @param v1 The first version for comparison.
     * @param v2 The second version for comparison.
     * @return A value below 0 iff {@code v1 &lt; v2}, a value above 0 iff
     *         {@code v1 &gt; v2</tt> and 0 iff <tt>v1 = v2}.
     * @throws NullPointerException If either parameter is null.
     * @since 0.2.0
     */
    public static int compare(Version v1, Version v2) {
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
     *
     *
     * <p>
     * This method fulfills the general contract for Java's {@link Comparator Comparators}
     * and {@link Comparable Comparables}.
     *
     *
     * @param v1 The first version for comparison.
     * @param v2 The second version for comparison.
     * @return A value below 0 iff {@code v1 &lt; v2}, a value above 0 iff
     *         {@code v1 &gt; v2</tt> and 0 iff <tt>v1 = v2}.
     * @throws NullPointerException If either parameter is null.
     * @since 0.3.0
     */
    public static int compareWithBuildMetaData(Version v1, Version v2) {
        // throw NPE to comply with Comparable specification
        if (v1 == null) {
            throw new NullPointerException("v1 is null");
        } else if (v2 == null) {
            throw new NullPointerException("v2 is null");
        }
        return compare(v1, v2, true);
    }

    private static int compare(Version v1, Version v2,
            boolean withBuildMetaData) {
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

    private static int comparePreRelease(Version v1, Version v2) {
        return compareLiterals(v1.preReleaseParts, v2.preReleaseParts);
    }

    private static int compareBuildMetaData(Version v1, Version v2) {
        return compareLiterals(v1.buildMetaDataParts, v2.buildMetaDataParts);
    }

    private static int compareLiterals(String[] v1Literal, String[] v2Literal) {
        final int result;
        if (v1Literal.length > 0 && v2Literal.length > 0) {
            // compare pre release parts
            result = compareIdentifiers(v1Literal, v2Literal);
        } else if (v1Literal.length > 0) {
            // other is greater, because it is no pre release
            result = -1;
        } else if (v2Literal.length > 0) {
            // this is greater because other is no pre release
            result = 1;
        } else {
            result = 0;
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
        final char[] chars = s.toCharArray();
        int num = 0;

        // note: this method does not account for leading zeroes as could occur in build
        // meta data parts. Leading zeroes are thus simply ignored when parsing the
        // number.
        for (int i = 0; i < chars.length; ++i) {
            final char c = chars[i];
            if (c >= '0' && c <= '9') {
                num = num * DECIMAL + Character.digit(c, DECIMAL);
            } else {
                return -1;
            }
        }
        return num;
    }

    private static String[] parsePreRelease(String preRelease) {
        if (preRelease != null && !preRelease.isEmpty()) {
            final List<String> parts = new ArrayList<String>();
            parseID(preRelease.toCharArray(), preRelease, 0, false, false, false, parts,
                    "pre-release");
            return parts.toArray(new String[parts.size()]);
        }
        return EMPTY_ARRAY;
    }

    private static String[] parseBuildMd(String buildMetaData) {
        if (buildMetaData != null && !buildMetaData.isEmpty()) {
            final List<String> parts = new ArrayList<String>();
            parseID(buildMetaData.toCharArray(), buildMetaData, 0, false, true, false,
                    parts, "build-meta-data");
            return parts.toArray(new String[parts.size()]);
        }
        return EMPTY_ARRAY;
    }

    private static final Version createInternal(int major, int minor, int patch,
            String preRelease, String buildMetaData) {

        final String[] preReleaseParts = parsePreRelease(preRelease);
        final String[] buildMdParts = parseBuildMd(buildMetaData);
        return new Version(major, minor, patch, preReleaseParts, buildMdParts);
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
    public static final Version create(int major, int minor, int patch,
            String preRelease,
            String buildMetaData) {
        require(preRelease != null, "preRelease is null");
        require(buildMetaData != null, "buildMetaData is null");
        return createInternal(major, minor, patch, preRelease, buildMetaData);
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
    public static final Version create(int major, int minor, int patch,
            String preRelease) {
        return create(major, minor, patch, preRelease, "");
    }

    /**
     * Creates a new Version from the three provided components. The version's pre release
     * and build meta data fields will be empty. Neither value must be lower than 0 and at
     * least one must be greater than zero.
     *
     * @param major The major version.
     * @param minor The minor version.
     * @param patch The patch version.
     * @return The version instance.
     */
    public static final Version create(int major, int minor, int patch) {
        return new Version(major, minor, patch, EMPTY_ARRAY, EMPTY_ARRAY);
    }

    /**
     * Creates a new Version from the two provided components, leaving the patch version
     * 0. The version's pre release and build meta data fields will be empty. Neither
     * value must be lower than 0 and at least one must be greater than zero.
     *
     * @param major The major version.
     * @param minor The minor version.
     * @return The version instance.
     * @since 2.1.0
     */
    public static final Version create(int major, int minor) {
        return new Version(major, minor, 0, EMPTY_ARRAY, EMPTY_ARRAY);
    }

    /**
     * Creates a new Version with the provided major version, leaving the minor and patch
     * version 0. The version's pre release and build meta data fields will be empty. The
     * major value must be lower than or equal to 0.
     *
     * @param major The major version.
     * @return The version instance.
     * @since 2.1.0
     */
    public static final Version create(int major) {
        return new Version(major, 0, 0, EMPTY_ARRAY, EMPTY_ARRAY);
    }

    private static void checkParams(int major, int minor, int patch) {
        require(major >= 0, "major < 0");
        require(minor >= 0, "minor < 0");
        require(patch >= 0, "patch < 0");
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
    public static final Version parseVersion(String versionString) {
        require(versionString != null, "versionString is null");
        return parse(versionString, false);
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
     *
     *
     * @param versionString The String to parse.
     * @param allowPreRelease Whether pre-release and build meta data field are allowed.
     * @return The parsed version.
     * @throws VersionFormatException If the String is no valid version
     * @since 0.4.0
     */
    public static Version parseVersion(String versionString,
            boolean allowPreRelease) {
        final Version version = parseVersion(versionString);
        if (!allowPreRelease && (version.isPreRelease() || version.hasBuildMetaData())) {
            throw new VersionFormatException(String.format(
                    "Version string '%s' is expected to have no pre-release or "
                            + "build meta data part",
                    versionString));
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
     * @see #min(Version, Version)
     */
    public Version min(Version other) {
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
     * @see #max(Version, Version)
     */
    public Version max(Version other) {
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
     * Gets the pre release identifier parts of this version as array. Modifying the
     * resulting array will have no influence on the internal state of this object.
     *
     * @return Pre release version as array. Array is empty if this version has no pre
     *         release part.
     */
    public String[] getPreReleaseParts() {
        if (this.preReleaseParts.length == 0) {
            return EMPTY_ARRAY;
        }
        return Arrays.copyOf(this.preReleaseParts, this.preReleaseParts.length);
    }

    /**
     * Gets the pre release identifier of this version. If this version has no such
     * identifier, an empty string is returned.
     *
     * <p>
     * Note: This method will always reconstruct a new String by joining the single
     * identifier parts.
     *
     *
     * @return This version's pre release identifier or an empty String if this version
     *         has no such identifier.
     */
    public String getPreRelease() {
        return join(this.preReleaseParts);
    }

    /**
     * Gets this version's build meta data. If this version has no build meta data, the
     * returned string is empty.
     *
     * <p>
     * Note: This method will always reconstruct a new String by joining the single
     * identifier parts.
     *
     *
     * @return The build meta data or an empty String if this version has no build meta
     *         data.
     */
    public String getBuildMetaData() {
        return join(this.buildMetaDataParts);
    }

    private static String join(String[] parts) {
        if (parts.length == 0) {
            return "";
        }
        final StringBuilder b = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            final String part = parts[i];
            b.append(part);
            if (i < parts.length - 1) {
                b.append(".");
            }
        }
        return b.toString();
    }

    /**
     * Gets the build meta data identifier parts of this version as array. Modifying the
     * resulting array will have no influence on the internal state of this object.
     *
     * @return Build meta data as array. Array is empty if this version has no build meta
     *         data part.
     */
    public String[] getBuildMetaDataParts() {
        if (this.buildMetaDataParts.length == 0) {
            return EMPTY_ARRAY;
        }
        return Arrays.copyOf(this.buildMetaDataParts, this.buildMetaDataParts.length);
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
     * Whether this is a 'stable' version. That is, it has no pre-release identifier.
     *
     * @return <code>true</code> iff {@link #getPreRelease()} is empty.
     * @see #isPreRelease()
     * @since 2.1.0
     */
    public boolean isStable() {
        return this.preReleaseParts.length == 0;
    }

    /**
     * Determines whether this is a pre release version.
     *
     * @return <code>true</code> iff {@link #getPreRelease()} is not empty.
     * @see #isStable()
     */
    public boolean isPreRelease() {
        return this.preReleaseParts.length > 0;
    }

    /**
     * Determines whether this version has a build meta data field.
     *
     * @return <code>true</code> iff {@link #getBuildMetaData()} is not empty.
     */
    public boolean hasBuildMetaData() {
        return this.buildMetaDataParts.length > 0;
    }

    /**
     * Creates a String representation of this version by joining its parts together as by
     * the semantic version specification.
     *
     * @return The version as a String.
     */
    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder(TO_STRING_ESTIMATE);
        b.append(this.major).append(".")
                .append(this.minor).append(".")
                .append(this.patch);

        if (isPreRelease()) {
            b.append("-").append(getPreRelease());
        }
        if (hasBuildMetaData()) {
            b.append("+").append(getBuildMetaData());
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
        final int h = this.hash;
        if (h == NOT_YET_CALCULATED) {
            this.hash = calculateHashCode();
        }
        return this.hash;
    }

    private int calculateHashCode() {
        int h = HASH_PRIME + this.major;
        h = HASH_PRIME * h + this.minor;
        h = HASH_PRIME * h + this.patch;
        h = HASH_PRIME * h + Arrays.hashCode(this.preReleaseParts);
        return h;
    }

    /**
     * Determines whether this version is equal to the passed object. This is the case if
     * the passed object is an instance of Version and this version
     * {@link #compareTo(Version) compared} to the provided one yields 0. Thus, this
     * method ignores the {@link #getBuildMetaData()} field.
     *
     * @param obj the object to compare with.
     * @return <code>true</code> iff {@code obj} is an instance of {@code Version} and
     *         {@code this.compareTo((Version) obj) == 0}
     * @see #compareTo(Version)
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
        return obj == this || obj instanceof Version
                && compare(this, (Version) obj, includeBuildMd) == 0;
    }

    /**
     * Compares this version to the provided one, following the <em>semantic
     * versioning</em> specification. See {@link #compare(Version, Version)} for more
     * information.
     *
     * @param other The version to compare to.
     * @return A value lower than 0 if this &lt; other, a value greater than 0 if this
     *         &gt; other and 0 if this == other. The absolute value of the result has no
     *         semantical interpretation.
     */
    @Override
    public int compareTo(Version other) {
        return compare(this, other);
    }

    /**
     * Compares this version to the provided one. Unlike the {@link #compareTo(Version)}
     * method, this one additionally considers the build meta data field of both versions,
     * if all other parts are equal. Note: This is <em>not</em> part of the semantic
     * version specification.
     *
     * <p>
     * Comparison of the build meta data parts happens exactly as for pre release
     * identifiers. Considering of build meta data first kicks in if both versions are
     * equal when using their natural order.
     *
     *
     * @param other The version to compare to.
     * @return A value lower than 0 if this &lt; other, a value greater than 0 if this
     *         &gt; other and 0 if this == other. The absolute value of the result has no
     *         semantical interpretation.
     * @since 0.3.0
     */
    public int compareToWithBuildMetaData(Version other) {
        return compareWithBuildMetaData(this, other);
    }

    /**
     * Returns a new Version where all identifiers are converted to upper case letters.
     *
     * @return A new version with upper case identifiers.
     * @since 1.1.0
     */
    public Version toUpperCase() {
        return new Version(this.major, this.minor, this.patch,
                copyCase(this.preReleaseParts, true),
                copyCase(this.buildMetaDataParts, true));
    }

    /**
     * Returns a new Version where all identifiers are converted to lower case letters.
     *
     * @return A new version with lower case identifiers.
     * @since 1.1.0
     */
    public Version toLowerCase() {
        return new Version(this.major, this.minor, this.patch,
                copyCase(this.preReleaseParts, false),
                copyCase(this.buildMetaDataParts, false));
    }

    private static String[] copyCase(String[] source, boolean toUpper) {
        final String[] result = new String[source.length];
        for (int i = 0; i < source.length; i++) {
            final String string = source[i];
            result[i] = toUpper ? string.toUpperCase(Locale.ROOT) : string.toLowerCase(Locale.ROOT);
        }
        return result;
    }

    /**
     * Tests whether this version is strictly greater than the given other version in
     * terms of precedence. Does not consider the build meta data part.
     * <p>
     * Convenience method for {@code this.compareTo(other) > 0} except that this method
     * throws an {@link IllegalArgumentException} if other is null.
     *
     *
     * @param other The version to compare to.
     * @return Whether this version is strictly greater.
     * @since 1.1.0
     */
    public boolean isGreaterThan(Version other) {
        require(other != null, "other must no be null");
        return compareTo(other) > 0;
    }

    /**
     * Tests whether this version is equal to or greater than the given other version in
     * terms of precedence. Does not consider the build meta data part.
     * <p>
     * Convenience method for {@code this.compareTo(other) >= 0} except that this method
     * throws an {@link IllegalArgumentException} if other is null.
     *
     * @param other The version to compare to.
     * @return Whether this version is greater or equal.
     * @since 2.1.0
     */
    public boolean isGreaterThanOrEqualTo(Version other) {
        require(other != null, "other must no be null");
        return compareTo(other) >= 0;
    }

    /**
     * Tests whether this version is strictly lower than the given other version in terms
     * of precedence. Does not consider the build meta data part.
     * <p>
     * Convenience method for {@code this.compareTo(other) < 0} except that this method
     * throws an {@link IllegalArgumentException} if other is null.
     *
     * @param other The version to compare to.
     * @return Whether this version is strictly lower.
     * @since 1.1.0
     */
    public boolean isLowerThan(Version other) {
        require(other != null, "other must no be null");
        return compareTo(other) < 0;
    }

    /**
     * Tests whether this version is equal to or lower than the given other version in
     * terms of precedence. Does not consider the build meta data part.
     * <p>
     * Convenience method for {@code this.compareTo(other) <= 0} except that this method
     * throws an {@link IllegalArgumentException} if other is null.
     *
     * @param other The version to compare to.
     * @return Whether this version is lower or equal.
     * @since 2.1.0
     */
    public boolean isLowerThanOrEqualTo(Version other) {
        require(other != null, "other must no be null");
        return compareTo(other) <= 0;
    }

    /**
     * Handles proper deserialization of objects serialized with a version prior to 1.1.0
     *
     * @return the deserialized object.
     * @throws ObjectStreamException If deserialization fails.
     * @since 1.1.0
     */
    private Object readResolve() throws ObjectStreamException {
        if (this.preRelease != null || this.buildMetaData != null) {
            return createInternal(this.major, this.minor, this.patch,
                    this.preRelease,
                    this.buildMetaData);
        }
        return this;
    }
}
