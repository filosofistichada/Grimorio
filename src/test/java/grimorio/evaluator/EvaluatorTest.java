package grimorio.evaluator;

import grimorio.Interpreter;
import grimorio.object.ErrorObj;
import grimorio.object.Obj;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class EvaluatorTest {

    private static Obj eval(String source) {
        return new Interpreter(new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8)).run(source);
    }

    private static String value(String source) {
        Obj result = eval(source);
        assertFalse(result instanceof ErrorObj, () -> "error inesperado: " + result.inspect());
        return result.repr();
    }

    private static String error(String source) {
        Obj result = eval(source);
        assertTrue(result instanceof ErrorObj, () -> "se esperaba un error y se obtuvo " + result.repr());
        return result.inspect();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "1 + 2 * 3           | 7",
            "7 / 2               | 3",
            "7 / 2.0             | 3.5",
            "-7 % 3              | -1",
            "2 ** 10             | 1024",
            "2 ** -1             | 0.5",
            "1 == 1.0            | luz",
            "\"a\" + \"b\"       | \"ab\"",
            "[1] + [2]           | [1, 2]",
            "[1, [2]] == [1, [2]] | luz",
            "!vacio              | luz",
            "!0                  | sombra"
    })
    void evaluaExpresiones(String source, String expected) {
        assertEquals(expected, value(source));
    }

    @Test
    void declaraYReasignaVariables() {
        assertEquals("6", value("pocion x = 5; x = x + 1; x"));
    }

    @Test
    void condicionalesAnidados() {
        assertEquals("\"b\"", value("pocion n = 3; siProfecia (n > 5) { \"a\" } otraProfecia (n > 1) { siProfecia (luz) { \"b\" } } maldicion { \"c\" }"));
        assertEquals("vacio", value("siProfecia (sombra) { 1 }"));
    }

    @Test
    void ciclos() {
        assertEquals("10", value("pocion s = 0; pocion i = 0; amarre (i < 5) { s = s + i; i = i + 1; } s"));
        assertEquals("15", value("pocion s = 0; circulo (pocion i = 1; i <= 5; i = i + 1) { s = s + i; } s"));
    }

    @Test
    void recursion() {
        assertEquals("55", value("pocion fib = hechizo(n) { siProfecia (n < 2) { conjura n; } conjura fib(n - 1) + fib(n - 2); }; fib(10)"));
    }

    @Test
    void conjuraSaleDeCiclosAnidados() {
        assertEquals("3", value("pocion f = hechizo() { circulo (pocion i = 0; luz; i = i + 1) { amarre (luz) { siProfecia (i == 3) { conjura i; } i = i + 1; } } }; f()"));
    }

    @Test
    void ambitosConMismoNombre() {
        assertEquals("\"global\"", value("pocion x = \"global\"; pocion f = hechizo() { pocion x = \"local\"; x }; f(); x"));
        assertEquals("\"global\"", value("pocion x = \"global\"; siProfecia (luz) { pocion x = \"bloque\"; } x"));
        assertEquals("2", value("pocion c = 0; pocion inc = hechizo() { c = c + 1; }; inc(); inc(); c"));
    }

    @Test
    void closures() {
        assertEquals("15", value("pocion sumador = hechizo(a) { hechizo(b) { a + b } }; pocion mas5 = sumador(5); mas5(10)"));
    }

    @Test
    void cortocircuitoNoEvaluaElLadoDerecho() {
        assertEquals("sombra", value("sombra && noExiste()"));
        assertEquals("luz", value("luz || noExiste()"));
    }

    @Test
    void estructurasDeDatos() {
        assertEquals("[1, 9, 3]", value("pocion l = [1, 2, 3]; l[1] = 9; l"));
        assertEquals("[1, 2]", value("alCaldero([1], 2)"));
        assertEquals("2", value("pocion d = {\"a\": 1}; d[\"b\"] = 2; d[\"b\"]"));
        assertEquals("vacio", value("{\"a\": 1}[\"z\"]"));
    }

    @Test
    void hechizosIntegrados() {
        assertEquals("3", value("medir([1, 2, 3])"));
        assertEquals("\"42\"", value("transmutar(42)"));
        assertEquals("7", value("cristalizar(\"7\")"));
        assertEquals("2.0", value("destilar(2)"));
        assertEquals("\"lista\"", value("esencia([])"));
        assertEquals("[2, 4]", value("encantar([1, 2], hechizo(x) { x * 2 })"));
        assertEquals("[2]", value("tamizar([1, 2, 3], hechizo(x) { x == 2 })"));
    }

    @Test
    void erroresDeEjecucion() {
        assertTrue(error("1 / 0").contains("división entre cero"));
        assertTrue(error("noExiste").contains("'noExiste' no existe"));
        assertTrue(error("\"a\" + 1").contains("transmutar"));
        assertTrue(error("[1][5]").contains("fuera de rango"));
        assertTrue(error("pocion f = hechizo(a) { a }; f()").contains("espera 1"));
        assertTrue(error("5()").contains("no es un hechizo"));
        assertTrue(error("9223372036854775807 + 1").contains("desbordamiento"));
        assertTrue(error("pocion r = hechizo() { r() }; r()").contains("recursión demasiado profunda"));
    }

    @Test
    void elErrorIndicaLineaYColumna() {
        assertTrue(error("pocion a = 1;\npocion b = a / 0;").contains("(línea 2, columna 14)"));
    }
}
