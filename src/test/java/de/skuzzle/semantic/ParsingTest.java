package de.skuzzle.semantic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import de.skuzzle.semantic.Version.VersionFormatException;

@RunWith(JUnitPlatform.class)
public class ParsingTest {

    private static final String INCOMPLETE_VERSION_PART = "Incomplete version part in %s";

    private static String unexpectedChar(char c) {
        return "Unexpected char in %s: " + Character.toString(c);
    }

    private static final String[] LEGAL_VERSIONS = {
            "123.456.789",
            "0.1.0",
            "0.0.1",
            "0.0.1-0a",
            "0.0.1-0a+a0",
            "1.1.0-a",
            "1.1.0+a",
            "1.1.0+012",
            "1.1.0-112",
            "1.1.0-0",
            "1.1.0-0123a",
            "1.1.0-0123a+0012",
    };

    private static final String[][] ILLEGAL_PRE_RELEASES = {
            { "01.1", "Illegal leading char '0' in pre-release part of %s" },
            { "1.01", "Illegal leading char '0' in pre-release part of %s" },
            { "pre.001", "Illegal leading char '0' in pre-release part of %s" },
            { "pre.01", "Illegal leading char '0' in pre-release part of %s" },
            { "pre..foo", INCOMPLETE_VERSION_PART },
            { "$", unexpectedChar('$') }
    };

    private static final String[][] ILLEGAL_VERSIONS = {
            { "1.", INCOMPLETE_VERSION_PART },
            { "1.1.", INCOMPLETE_VERSION_PART },
            { "1.0", INCOMPLETE_VERSION_PART },
            { "1.2.3-pre.foo.", INCOMPLETE_VERSION_PART },
            { "1", INCOMPLETE_VERSION_PART },
            { "1$.1.0", unexpectedChar('$') },
            { "1.1$.0", unexpectedChar('$') },
            { "1.1.1$", unexpectedChar('$') },
            { "$.1.1", unexpectedChar('$') },
            { "1.$.1", unexpectedChar('$') },
            { "1.1.$", unexpectedChar('$') },
            { "0$.1.1", unexpectedChar('$') },
            { "1.0$.1", unexpectedChar('$') },
            { "1.1.0$", unexpectedChar('$') },
            { "01.1.0", "Illegal leading char '0' in major part of %s" },
            { "1.01.0", "Illegal leading char '0' in minor part of %s" },
            { "1.1.01", "Illegal leading char '0' in patch part of %s" },
            { "1.2.3-01.1", "Illegal leading char '0' in pre-release part of %s" },
            { "1.2.3-1.01+abc", "Illegal leading char '0' in pre-release part of %s" },
            { "1.2.3-1.01", "Illegal leading char '0' in pre-release part of %s" },
            { "1.2.3-pre.001", "Illegal leading char '0' in pre-release part of %s" },
            { "1.2.3-pre.01", "Illegal leading char '0' in pre-release part of %s" },
            { "1.2.3-pre.01+a.b", "Illegal leading char '0' in pre-release part of %s" },
            { "1.2.3-pre..foo", INCOMPLETE_VERSION_PART },
            { "1.2.3+pre..foo", INCOMPLETE_VERSION_PART },
            { "1.2.3+pre.foo.", INCOMPLETE_VERSION_PART },
            { "1.2.3-$+foo", unexpectedChar('$') },
            { "1.2.3+$", unexpectedChar('$') },
            { "1.2.3-foo$+foo", unexpectedChar('$') },
            { "1.2.3-1$+foo", unexpectedChar('$') },
            { "1.2.3-1+1$", unexpectedChar('$') },
            { "1.2.3-foo+foo$", unexpectedChar('$') },
            { "1.2.3-1+1$", unexpectedChar('$') },
            { "1.2.3-0$", unexpectedChar('$') },
            { "1.2.3+0$", unexpectedChar('$') },
            { "1.2.3-0123$", unexpectedChar('$') },
            { "1.2.3-+", unexpectedChar('+') },

    };

    @TestFactory
    Collection<DynamicTest> testParseWithException() {
        final List<DynamicTest> results = new ArrayList<>(
                ILLEGAL_VERSIONS.length);

        for (final String[] input : ILLEGAL_VERSIONS) {
            results.add(dynamicTest("Parse " + input[0],
                    () -> {
                        final VersionFormatException e = assertThrows(
                                VersionFormatException.class,
                                () -> Version.parseVersion(input[0]));

                        final String expectedMessage = String.format(input[1], input[0]);
                        assertEquals(expectedMessage, e.getMessage());
                    }));
        }

        return results;
    }

    @TestFactory
    Collection<DynamicTest> testWithPreReleaseException() {
        final List<DynamicTest> results = new ArrayList<>(
                ILLEGAL_PRE_RELEASES.length);

        for (final String input[] : ILLEGAL_PRE_RELEASES) {
            results.add(dynamicTest("Pre release " + input[0],
                    () -> {
                        final VersionFormatException e = assertThrows(
                                VersionFormatException.class,
                                () -> Version.create(1, 2, 3).withPreRelease(input[0]));

                        final String expectedMessage = String.format(input[1], input[0]);
                        assertEquals(expectedMessage, e.getMessage());
                    }));
        }

        return results;
    }

    @TestFactory
    Collection<DynamicTest> testWithPreReleaseArrayException() {
        final List<DynamicTest> results = new ArrayList<>(
                ILLEGAL_PRE_RELEASES.length);

        for (final String input[] : ILLEGAL_PRE_RELEASES) {
            results.add(dynamicTest("Pre release as array: " + input[0],
                    () -> {
                        final VersionFormatException e = assertThrows(
                                VersionFormatException.class,
                                () -> Version.create(1, 2, 3).withPreRelease(
                                        input[0].split("\\.")));

                        final String expectedMessage = String.format(input[1], input[0]);
                        assertEquals(expectedMessage, e.getMessage());
                    }));
        }

        return results;
    }

    @TestFactory
    Collection<DynamicTest> testParseVerifyOnly() {
        final List<DynamicTest> results = new ArrayList<>();

        for (final String[] input : ILLEGAL_VERSIONS) {
            results.add(dynamicTest(input[0],
                    () -> assertFalse(Version.isValidVersion(input[0]))));
        }
        return results;
    }

    @TestFactory
    Collection<DynamicTest> testParseLegalVersions() {
        final List<DynamicTest> results = new ArrayList<>();

        for (final String input : LEGAL_VERSIONS) {
            final DynamicTest test = dynamicTest("Parse " + input, () -> {
                final Version parsed = Version.parseVersion(input);
                assertEquals(input, parsed.toString());
            });
            results.add(test);
        }
        return results;
    }

}
