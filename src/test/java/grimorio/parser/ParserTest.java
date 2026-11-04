package grimorio.parser;

import grimorio.ast.*;
import grimorio.lexer.Lexer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    private static Program parse(String source) {
        Parser parser = new Parser(new Lexer(source));
        Program program = parser.parseProgram();
        assertTrue(parser.errors().isEmpty(), "errores inesperados: " + parser.errors());
        return program;
    }

    private static String firstError(String source) {
        Parser parser = new Parser(new Lexer(source));
        parser.parseProgram();
        assertFalse(parser.errors().isEmpty(), "se esperaba un error en: " + source);
        return parser.errors().get(0);
    }

    /** toString() del AST muestra los paréntesis que el parser decidió. */
    @ParameterizedTest
    @CsvSource(delimiterString = "=>", value = {
            "1 + 2 * 3 => (1 + (2 * 3))",
            "(1 + 2) * 3 => ((1 + 2) * 3)",
            "a - b - c => ((a - b) - c)",
            "2 ** 3 ** 2 => (2 ** (3 ** 2))",
            "-2 ** 2 => (-(2 ** 2))",
            "-a * b => ((-a) * b)",
            "!luz == sombra => ((!luz) == sombra)",
            "a < b == c > d => ((a < b) == (c > d))",
            "a || b && c => (a || (b && c))",
            "a && b || c => ((a && b) || c)",
            "a + b * c ** d => (a + (b * (c ** d)))",
            "x = y = 3 => (x = (y = 3))",
            "f(a + b, c)[0] => (f((a + b), c)[0])",
            "a + l[1] * 2 => (a + ((l[1]) * 2))"
    })
    void respetaPrecedenciaYAsociatividad(String source, String expected) {
        assertEquals(expected, parse(source).toString());
    }

    @Test
    void parseaDeclaracion() {
        Program program = parse("pocion x = 5;");
        LetStatement let = (LetStatement) program.statements.get(0);
        assertEquals("x", let.name.name);
        assertEquals(5, ((IntegerLiteral) let.value).value);
    }

    @Test
    void parseaCondicionalConVariasRamas() {
        Program program = parse("siProfecia (a) { 1 } otraProfecia (b) { 2 } maldicion { 3 }");
        IfExpression ifExpression = (IfExpression) ((ExpressionStatement) program.statements.get(0)).expression;
        assertEquals(2, ifExpression.branches.size());
        assertNotNull(ifExpression.alternative);
    }

    @Test
    void parseaCirculoYHechizo() {
        Program program = parse("pocion f = hechizo(a, b) { circulo (pocion i = 0; i < a; i = i + 1) { b } };");
        FunctionLiteral fn = (FunctionLiteral) ((LetStatement) program.statements.get(0)).value;
        assertEquals(2, fn.parameters.size());
        ForExpression loop = (ForExpression) ((ExpressionStatement) fn.body.statements.get(0)).expression;
        assertTrue(loop.init instanceof LetStatement);
        assertNotNull(loop.condition);
        assertNotNull(loop.update);
    }

    @Test
    void parseaListasYDiccionarios() {
        assertEquals("[1, (2 + 3), \"a\"]", parse("[1, 2 + 3, \"a\"]").toString());
        assertEquals("{\"a\": 1, \"b\": [2]}", parse("{\"a\": 1, \"b\": [2]}").toString());
    }

    @Test
    void reportaParentesisFaltante() {
        String error = firstError("siProfecia (x > 1 { revelar(x); }");
        assertTrue(error.contains("se esperaba ')'"), error);
        assertTrue(error.contains("línea 1"), error);
    }

    @Test
    void reportaAsignacionInvalida() {
        assertTrue(firstError("5 = 3;").contains("no se puede asignar"));
    }

    @Test
    void reportaBloqueSinCerrar() {
        assertTrue(firstError("amarre (luz) { revelar(1);").contains("falta '}'"));
    }

    @Test
    void noReportaErroresEnCascada() {
        Parser parser = new Parser(new Lexer("pocion f = hechizo(a, a) { conjura a; };\npocion ok = 1;"));
        parser.parseProgram();
        assertEquals(1, parser.errors().size(), parser.errors().toString());
    }
}
