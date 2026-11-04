package grimorio.lexer;

import grimorio.token.Token;
import grimorio.token.TokenType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static grimorio.token.TokenType.*;
import static org.junit.jupiter.api.Assertions.*;

class LexerTest {

    private static void assertTypes(String source, TokenType... expected) {
        List<Token> tokens = Lexer.tokenize(source);
        assertEquals(expected.length + 1, tokens.size(), "cantidad de tokens (sin contar EOF) en: " + source);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], tokens.get(i).type(), "token " + i + " de: " + source);
        }
        assertEquals(EOF, tokens.get(tokens.size() - 1).type());
    }

    @Test
    void reconocePalabrasReservadas() {
        assertTypes("pocion hechizo conjura siProfecia otraProfecia maldicion amarre circulo luz sombra vacio",
                LET, FUNCTION, RETURN, IF, ELIF, ELSE, WHILE, FOR, TRUE, FALSE, NULL);
    }

    @Test
    void distingueIdentificadoresDePalabrasReservadas() {
        List<Token> tokens = Lexer.tokenize("pocion pociones _x x1");
        assertEquals(LET, tokens.get(0).type());
        assertEquals(IDENT, tokens.get(1).type());
        assertEquals("pociones", tokens.get(1).literal());
        assertEquals(IDENT, tokens.get(2).type());
        assertEquals(IDENT, tokens.get(3).type());
    }

    @Test
    void reconoceOperadoresDeUnoYDosCaracteres() {
        assertTypes("= == ! != < <= > >= * ** + - / % && ||",
                ASSIGN, EQ, BANG, NOT_EQ, LT, LTE, GT, GTE, ASTERISK, POWER, PLUS, MINUS, SLASH, PERCENT, AND, OR);
    }

    @Test
    void reconoceLiterales() {
        List<Token> tokens = Lexer.tokenize("42 3.14 \"hola\\n\"");
        assertEquals(INT, tokens.get(0).type());
        assertEquals(FLOAT, tokens.get(1).type());
        assertEquals("3.14", tokens.get(1).literal());
        assertEquals(STRING, tokens.get(2).type());
        assertEquals("hola\n", tokens.get(2).literal());
    }

    @Test
    void ignoraComentarios() {
        assertTypes("pocion x = 1; // esto se ignora\n// y esto también\nx", LET, IDENT, ASSIGN, INT, SEMICOLON, IDENT);
    }

    @Test
    void registraLineaYColumna() {
        List<Token> tokens = Lexer.tokenize("pocion x = 1;\n  revelar(x);");
        Token revelar = tokens.get(5);
        assertEquals("revelar", revelar.literal());
        assertEquals(2, revelar.line());
        assertEquals(3, revelar.column());
    }

    @Test
    void reportaCaracterDesconocido() {
        Lexer lexer = new Lexer("pocion x = 3 @ 2;");
        Token token;
        do { token = lexer.nextToken(); } while (token.type() != EOF);
        assertEquals(1, lexer.errors().size());
        assertTrue(lexer.errors().get(0).contains("'@'"));
        assertTrue(lexer.errors().get(0).contains("columna 14"));
    }

    @Test
    void reportaTextoSinCerrar() {
        Lexer lexer = new Lexer("\"sin cerrar");
        assertEquals(ILLEGAL, lexer.nextToken().type());
        assertTrue(lexer.errors().get(0).contains("texto sin cerrar"));
    }
}
