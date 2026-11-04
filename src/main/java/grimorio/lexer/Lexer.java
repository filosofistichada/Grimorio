package grimorio.lexer;

import grimorio.Messages;
import grimorio.token.Token;
import grimorio.token.TokenType;

import java.util.ArrayList;
import java.util.List;

/**
 * Analizador léxico: recorre el código fuente carácter por carácter y lo
 * agrupa en tokens. Por ejemplo, {@code pocion x = 5;} produce:
 *
 * <pre>
 *   LET "pocion"  IDENT "x"  ASSIGN "="  INT "5"  SEMICOLON ";"
 * </pre>
 *
 * Ignora espacios, saltos de línea y comentarios ({@code // hasta el fin de la línea}).
 * Cada error léxico (un carácter desconocido, un texto sin cerrar) se guarda
 * en {@link #errors()} y se devuelve como un token ILLEGAL, para que el parser
 * pueda seguir y reportar todos los errores de una vez.
 */
public class Lexer {
    private final String input;
    private int position;      // índice del carácter actual (ch)
    private int readPosition;  // índice del siguiente carácter
    private char ch;           // carácter actual; '\0' al final de la entrada
    private int line = 1;      // línea del carácter actual
    private int column = 0;    // columna del carácter actual
    private final List<String> errors = new ArrayList<>();

    public Lexer(String input) {
        this.input = input;
        readChar();
    }

    public List<String> errors() {
        return errors;
    }

    /** Avanza un carácter, actualizando la línea y la columna. */
    private void readChar() {
        if (ch == '\n') {
            line++;
            column = 0;
        }
        ch = readPosition >= input.length() ? '\0' : input.charAt(readPosition);
        position = readPosition;
        readPosition++;
        column++;
    }

    /** Mira el siguiente carácter sin avanzar. */
    private char peekChar() {
        return readPosition >= input.length() ? '\0' : input.charAt(readPosition);
    }

    /** Devuelve el siguiente token de la entrada. Al final devuelve siempre EOF. */
    public Token nextToken() {
        skipWhitespaceAndComments();

        int startLine = line;
        int startColumn = column;

        switch (ch) {
            case '=': return twoCharOr('=', TokenType.EQ, TokenType.ASSIGN, startLine, startColumn);
            case '!': return twoCharOr('=', TokenType.NOT_EQ, TokenType.BANG, startLine, startColumn);
            case '<': return twoCharOr('=', TokenType.LTE, TokenType.LT, startLine, startColumn);
            case '>': return twoCharOr('=', TokenType.GTE, TokenType.GT, startLine, startColumn);
            case '*': return twoCharOr('*', TokenType.POWER, TokenType.ASTERISK, startLine, startColumn);
            case '&':
                if (peekChar() == '&') return twoCharOr('&', TokenType.AND, TokenType.ILLEGAL, startLine, startColumn);
                return illegal("se esperaba '&&' pero solo hay un '&'", startLine, startColumn);
            case '|':
                if (peekChar() == '|') return twoCharOr('|', TokenType.OR, TokenType.ILLEGAL, startLine, startColumn);
                return illegal("se esperaba '||' pero solo hay un '|'", startLine, startColumn);
            case '+': return single(TokenType.PLUS, startLine, startColumn);
            case '-': return single(TokenType.MINUS, startLine, startColumn);
            case '/': return single(TokenType.SLASH, startLine, startColumn);
            case '%': return single(TokenType.PERCENT, startLine, startColumn);
            case ',': return single(TokenType.COMMA, startLine, startColumn);
            case ';': return single(TokenType.SEMICOLON, startLine, startColumn);
            case ':': return single(TokenType.COLON, startLine, startColumn);
            case '(': return single(TokenType.LPAREN, startLine, startColumn);
            case ')': return single(TokenType.RPAREN, startLine, startColumn);
            case '{': return single(TokenType.LBRACE, startLine, startColumn);
            case '}': return single(TokenType.RBRACE, startLine, startColumn);
            case '[': return single(TokenType.LBRACKET, startLine, startColumn);
            case ']': return single(TokenType.RBRACKET, startLine, startColumn);
            case '"': return readString(startLine, startColumn);
            case '\0':
                if (position >= input.length()) return new Token(TokenType.EOF, "", startLine, startColumn);
                return illegal("carácter nulo no permitido", startLine, startColumn);
            default:
                if (isLetter(ch)) {
                    String word = readIdentifier();
                    return new Token(TokenType.lookupIdent(word), word, startLine, startColumn);
                }
                if (isDigit(ch)) {
                    return readNumber(startLine, startColumn);
                }
                return illegal("carácter '" + ch + "' no reconocido", startLine, startColumn);
        }
    }

    /** Token de un solo carácter. */
    private Token single(TokenType type, int line, int column) {
        Token token = new Token(type, String.valueOf(ch), line, column);
        readChar();
        return token;
    }

    /** Si el siguiente carácter es {@code next} forma el token doble; si no, el sencillo. */
    private Token twoCharOr(char next, TokenType doubleType, TokenType singleType, int line, int column) {
        if (peekChar() == next) {
            String literal = "" + ch + next;
            readChar();
            readChar();
            return new Token(doubleType, literal, line, column);
        }
        return single(singleType, line, column);
    }

    private Token illegal(String detail, int line, int column) {
        Token token = new Token(TokenType.ILLEGAL, String.valueOf(ch), line, column);
        errors.add(Messages.format(Messages.LEXICAL, detail, token));
        readChar();
        return token;
    }

    private String readIdentifier() {
        int start = position;
        while (isLetter(ch) || isDigit(ch)) readChar();
        return input.substring(start, position);
    }

    /** Lee un entero (123) o un decimal (3.14). Un punto debe ir seguido de un dígito. */
    private Token readNumber(int line, int column) {
        int start = position;
        while (isDigit(ch)) readChar();
        if (ch == '.' && isDigit(peekChar())) {
            readChar();
            while (isDigit(ch)) readChar();
            return new Token(TokenType.FLOAT, input.substring(start, position), line, column);
        }
        return new Token(TokenType.INT, input.substring(start, position), line, column);
    }

    /** Lee un texto entre comillas dobles. Admite los escapes \n, \t, \" y \\. */
    private Token readString(int line, int column) {
        StringBuilder value = new StringBuilder();
        readChar(); // salta la comilla de apertura
        while (ch != '"') {
            if (position >= input.length()) {
                Token token = new Token(TokenType.ILLEGAL, "\"" + value, line, column);
                errors.add(Messages.format(Messages.LEXICAL, "texto sin cerrar, falta la comilla final", token));
                return token;
            }
            if (ch == '\\') {
                readChar();
                switch (ch) {
                    case 'n': value.append('\n'); break;
                    case 't': value.append('\t'); break;
                    case '"': value.append('"'); break;
                    case '\\': value.append('\\'); break;
                    default:
                        Token bad = new Token(TokenType.ILLEGAL, "\\" + ch, this.line, this.column - 1);
                        errors.add(Messages.format(Messages.LEXICAL, "secuencia de escape '\\" + ch + "' no válida", bad));
                        value.append(ch);
                }
            } else {
                value.append(ch);
            }
            readChar();
        }
        readChar(); // salta la comilla de cierre
        return new Token(TokenType.STRING, value.toString(), line, column);
    }

    private void skipWhitespaceAndComments() {
        while (true) {
            if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
                readChar();
            } else if (ch == '/' && peekChar() == '/') {
                while (ch != '\n' && position < input.length()) readChar();
            } else {
                return;
            }
        }
    }

    /** Solo letras ASCII y guion bajo (decisión de diseño: evita problemas de codificación). */
    private static boolean isLetter(char c) {
        return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || c == '_';
    }

    private static boolean isDigit(char c) {
        return '0' <= c && c <= '9';
    }

    /** Convierte toda la entrada en una lista de tokens (incluye el EOF final). */
    public static List<Token> tokenize(String input) {
        Lexer lexer = new Lexer(input);
        List<Token> tokens = new ArrayList<>();
        Token token;
        do {
            token = lexer.nextToken();
            tokens.add(token);
        } while (token.type() != TokenType.EOF);
        return tokens;
    }
}
