package de.skuzzle.semantic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

public class Java6CompatibilityTest {

    @Test
    void testAllClassFilesAreCompatibleWithJava6() throws Exception {
        final int java6 = 0x32;
        allClassFiles()
                .filter(cf -> !isModuleInfo(cf))
                .forEach(cf -> testClassFileForJavaVersion(cf, java6));
    }

    @Test
    void testModuleInfoIsCompatibleWithJava9() throws Exception {
        final int java9 = 0x35;
        allClassFiles()
                .filter(this::isModuleInfo)
                .forEach(mi -> testClassFileForJavaVersion(mi, java9));
    }

    private boolean isModuleInfo(Path path) {
        return path.getFileName().toString().equals("module-info.class");
    }

    private Stream<Path> allClassFiles() throws IOException {
        final Path targetFolder = Paths.get("./target/classes");
        return Files.find(targetFolder, Integer.MAX_VALUE,
                (path, bfa) -> path.getFileName().toString().endsWith(".class"));
    }

    private void testClassFileForJavaVersion(Path path, int expectedClassFileVersion) {
        final int[] expectedSequence = { 0xCA, 0xFE, 0xBA, 0xBE, 0, 0, 0, expectedClassFileVersion };

        try (InputStream in = Files.newInputStream(path, StandardOpenOption.READ)) {
            for (int i = 0; i < expectedSequence.length; ++i) {
                final int expected = expectedSequence[i];
                final int actual = in.read();
                assertEquals(expected, actual,
                        String.format("Class file %s: Expected byte %d at index %d but found %d", path, expected, i,
                                actual));
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
