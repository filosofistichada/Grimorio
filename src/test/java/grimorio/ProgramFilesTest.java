package grimorio;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ejecuta cada programa de tests/ y compara su salida con el archivo
 * .esperado del mismo nombre. También comprueba que los programas de
 * examples/ terminen sin errores.
 */
class ProgramFilesTest {

    private static String run(Path file, boolean[] ok) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Interpreter interpreter = new Interpreter(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        ok[0] = interpreter.runProgram(Files.readString(file));
        return normalize(buffer.toString(StandardCharsets.UTF_8));
    }

    private static String normalize(String text) {
        return text.replace("\r\n", "\n").stripTrailing();
    }

    private static List<Path> grimFiles(String dir) throws IOException {
        try (Stream<Path> files = Files.walk(Paths.get(dir))) {
            return files.filter(p -> p.toString().endsWith(".grim")).sorted().collect(Collectors.toList());
        }
    }

    @TestFactory
    Stream<DynamicTest> programasDePrueba() throws IOException {
        return grimFiles("tests").stream().map(file -> DynamicTest.dynamicTest(file.toString(), () -> {
            Path expectedFile = Paths.get(file.toString().replaceAll("\\.grim$", ".esperado"));
            boolean[] ok = new boolean[1];
            String actual = run(file, ok);
            assertEquals(normalize(Files.readString(expectedFile)), actual);
            boolean shouldFail = file.getParent().getFileName().toString().equals("errores");
            assertEquals(!shouldFail, ok[0], shouldFail ? "debía terminar con error" : "no debía terminar con error");
        }));
    }

    @TestFactory
    Stream<DynamicTest> ejemplosSinErrores() throws IOException {
        return grimFiles("examples").stream().map(file -> DynamicTest.dynamicTest(file.toString(), () -> {
            boolean[] ok = new boolean[1];
            String output = run(file, ok);
            assertTrue(ok[0], output);
        }));
    }
}
