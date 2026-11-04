package grimorio.parser;

import grimorio.Messages;
import grimorio.ast.*;
import grimorio.lexer.Lexer;
import grimorio.token.Token;
import grimorio.token.TokenType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Analizador sintáctico de Grimorio. Las instrucciones se leen por descenso
 * recursivo y las expresiones con la técnica de Pratt: cada tipo de token
 * tiene una función "prefija" (cómo empieza una expresión con ese token) y,
 * si es un operador, una función "infija" (cómo continúa una expresión que
 * ya tiene lado izquierdo). La tabla {@link #PRECEDENCES} decide cuánto
 * "agarra" cada operador.
 *
 * El parser mira siempre dos tokens: {@code cur} (el actual) y {@code peek}
 * (el siguiente). Los errores se acumulan en {@link #errors()}.
 */
public class Parser {
    private static final Map<TokenType, Precedence> PRECEDENCES = new EnumMap<>(TokenType.class);

    static {
        PRECEDENCES.put(TokenType.ASSIGN, Precedence.ASSIGN);
        PRECEDENCES.put(TokenType.OR, Precedence.OR);
        PRECEDENCES.put(TokenType.AND, Precedence.AND);
        PRECEDENCES.put(TokenType.EQ, Precedence.EQUALS);
        PRECEDENCES.put(TokenType.NOT_EQ, Precedence.EQUALS);
        PRECEDENCES.put(TokenType.LT, Precedence.LESSGREATER);
        PRECEDENCES.put(TokenType.GT, Precedence.LESSGREATER);
        PRECEDENCES.put(TokenType.LTE, Precedence.LESSGREATER);
        PRECEDENCES.put(TokenType.GTE, Precedence.LESSGREATER);
        PRECEDENCES.put(TokenType.PLUS, Precedence.SUM);
        PRECEDENCES.put(TokenType.MINUS, Precedence.SUM);
        PRECEDENCES.put(TokenType.ASTERISK, Precedence.PRODUCT);
        PRECEDENCES.put(TokenType.SLASH, Precedence.PRODUCT);
        PRECEDENCES.put(TokenType.PERCENT, Precedence.PRODUCT);
        PRECEDENCES.put(TokenType.POWER, Precedence.POWER);
        PRECEDENCES.put(TokenType.LPAREN, Precedence.CALL);
        PRECEDENCES.put(TokenType.LBRACKET, Precedence.INDEX);
    }

    private final Lexer lexer;
    private final List<String> errors = new ArrayList<>();
    private final Map<TokenType, Supplier<Expression>> prefixParseFns = new EnumMap<>(TokenType.class);
    private final Map<TokenType, Function<Expression, Expression>> infixParseFns = new EnumMap<>(TokenType.class);

    private Token cur;
    private Token peek;
    private int openBlocks; // bloques abiertos sin cerrar; lo usa synchronize()

    public Parser(Lexer lexer) {
        this.lexer = lexer;

        prefixParseFns.put(TokenType.IDENT, this::parseIdentifier);
        prefixParseFns.put(TokenType.INT, this::parseIntegerLiteral);
        prefixParseFns.put(TokenType.FLOAT, this::parseFloatLiteral);
        prefixParseFns.put(TokenType.STRING, () -> new StringLiteral(cur, cur.literal()));
        prefixParseFns.put(TokenType.TRUE, () -> new BooleanLiteral(cur, true));
        prefixParseFns.put(TokenType.FALSE, () -> new BooleanLiteral(cur, false));
        prefixParseFns.put(TokenType.NULL, () -> new NullLiteral(cur));
        prefixParseFns.put(TokenType.BANG, this::parsePrefixExpression);
        prefixParseFns.put(TokenType.MINUS, this::parsePrefixExpression);
        prefixParseFns.put(TokenType.LPAREN, this::parseGroupedExpression);
        prefixParseFns.put(TokenType.LBRACKET, this::parseArrayLiteral);
        prefixParseFns.put(TokenType.LBRACE, this::parseHashLiteral);
        prefixParseFns.put(TokenType.IF, this::parseIfExpression);
        prefixParseFns.put(TokenType.WHILE, this::parseWhileExpression);
        prefixParseFns.put(TokenType.FOR, this::parseForExpression);
        prefixParseFns.put(TokenType.FUNCTION, this::parseFunctionLiteral);

        for (TokenType op : new TokenType[] {TokenType.PLUS, TokenType.MINUS, TokenType.ASTERISK, TokenType.SLASH,
                TokenType.PERCENT, TokenType.EQ, TokenType.NOT_EQ, TokenType.LT, TokenType.GT, TokenType.LTE, TokenType.GTE}) {
            infixParseFns.put(op, this::parseInfixExpression);
        }
        infixParseFns.put(TokenType.POWER, this::parsePowerExpression);
        infixParseFns.put(TokenType.AND, this::parseLogicalExpression);
        infixParseFns.put(TokenType.OR, this::parseLogicalExpression);
        infixParseFns.put(TokenType.ASSIGN, this::parseAssignExpression);
        infixParseFns.put(TokenType.LPAREN, this::parseCallExpression);
        infixParseFns.put(TokenType.LBRACKET, this::parseIndexExpression);

        // Carga cur y peek.
        nextToken();
        nextToken();
    }

    public List<String> errors() {
        return errors;
    }

    // ------------------------------------------------------------------
    // Utilidades para moverse por los tokens
    // ------------------------------------------------------------------

    private void nextToken() {
        cur = peek;
        peek = lexer.nextToken();
    }

    private boolean curTokenIs(TokenType type) {
        return cur.type() == type;
    }

    private boolean peekTokenIs(TokenType type) {
        return peek.type() == type;
    }

    /** Si el siguiente token es del tipo esperado avanza; si no, registra un error. */
    private boolean expectPeek(TokenType type) {
        if (peekTokenIs(type)) {
            nextToken();
            return true;
        }
        error("se esperaba '" + type.description() + "' pero se encontró " + describe(peek), peek);
        return false;
    }

    private Precedence peekPrecedence() {
        return PRECEDENCES.getOrDefault(peek.type(), Precedence.LOWEST);
    }

    private Precedence curPrecedence() {
        return PRECEDENCES.getOrDefault(cur.type(), Precedence.LOWEST);
    }

    private void error(String detail, Token at) {
        // Un token ILLEGAL ya lo reportó el lexer; no se repite como error sintáctico.
        if (at.type() == TokenType.ILLEGAL) return;
        errors.add(Messages.format(Messages.SYNTAX, detail, at));
    }

    private static String describe(Token token) {
        if (token.type() == TokenType.EOF) return "el fin del archivo";
        return "'" + token.literal() + "'";
    }

    // ------------------------------------------------------------------
    // Instrucciones
    // ------------------------------------------------------------------

    /** programa := instruccion* EOF */
    public Program parseProgram() {
        List<Statement> statements = new ArrayList<>();
        while (!curTokenIs(TokenType.EOF)) {
            int errorsBefore = errors.size();
            Statement statement = parseStatement();
            if (statement != null && errors.size() == errorsBefore) {
                statements.add(statement);
            } else {
                synchronize();
            }
            nextToken();
        }
        return new Program(statements);
    }

    /**
     * Recuperación de errores: tras un error salta hasta el final de la
     * instrucción fallida (un ';' o la '}' que cierra sus bloques) para seguir
     * buscando errores sin reportar errores falsos en cascada.
     * {@code openBlocks} cuenta los bloques que la instrucción abrió y no
     * alcanzó a cerrar antes del error.
     */
    private void synchronize() {
        int depth = openBlocks;
        openBlocks = 0;
        while (!curTokenIs(TokenType.EOF)) {
            if (curTokenIs(TokenType.LBRACE)) {
                depth++;
            } else if (curTokenIs(TokenType.RBRACE)) {
                depth--;
                if (depth <= 0) {
                    if (peekTokenIs(TokenType.SEMICOLON)) nextToken();
                    return;
                }
            } else if (curTokenIs(TokenType.SEMICOLON) && depth <= 0) {
                return;
            }
            if (peekTokenIs(TokenType.EOF)) return;
            nextToken();
        }
    }

    /** instruccion := declaracion | retorno | expresion, con ';' opcional al final. */
    private Statement parseStatement() {
        Statement statement;
        switch (cur.type()) {
            case LET:
                statement = parseLetStatement();
                break;
            case RETURN:
                statement = parseReturnStatement();
                break;
            default:
                statement = parseExpressionStatement();
        }
        if (statement != null && peekTokenIs(TokenType.SEMICOLON)) nextToken();
        return statement;
    }

    /** declaracion := 'pocion' IDENT '=' expresion */
    private LetStatement parseLetStatement() {
        Token token = cur;
        if (!expectPeek(TokenType.IDENT)) return null;
        Identifier name = new Identifier(cur, cur.literal());
        if (!expectPeek(TokenType.ASSIGN)) return null;
        nextToken();
        Expression value = parseExpression(Precedence.LOWEST);
        if (value == null) return null;
        return new LetStatement(token, name, value);
    }

    /** retorno := 'conjura' expresion? */
    private ReturnStatement parseReturnStatement() {
        Token token = cur;
        if (peekTokenIs(TokenType.SEMICOLON) || peekTokenIs(TokenType.RBRACE) || peekTokenIs(TokenType.EOF)) {
            return new ReturnStatement(token, null);
        }
        nextToken();
        Expression value = parseExpression(Precedence.LOWEST);
        if (value == null) return null;
        return new ReturnStatement(token, value);
    }

    private ExpressionStatement parseExpressionStatement() {
        Token token = cur;
        Expression expression = parseExpression(Precedence.LOWEST);
        if (expression == null) return null;
        return new ExpressionStatement(token, expression);
    }

    /** bloque := '{' instruccion* '}'. Al entrar, cur es '{'; al salir, cur es '}'. */
    private BlockStatement parseBlockStatement() {
        Token token = cur;
        List<Statement> statements = new ArrayList<>();
        openBlocks++;
        nextToken();
        while (!curTokenIs(TokenType.RBRACE)) {
            if (curTokenIs(TokenType.EOF)) {
                error("falta '}': el bloque que empieza en la línea " + token.line() + ", columna "
                        + token.column() + " llega al fin del archivo sin cerrarse", cur);
                return null;
            }
            Statement statement = parseStatement();
            if (statement == null) return null;
            statements.add(statement);
            nextToken();
        }
        openBlocks--;
        return new BlockStatement(token, statements);
    }

    // ------------------------------------------------------------------
    // Expresiones (Pratt)
    // ------------------------------------------------------------------

    /**
     * Corazón del parser de Pratt. Lee una expresión prefija y luego, mientras
     * el operador que sigue tenga más precedencia que {@code precedence}, la
     * extiende hacia la derecha con su función infija.
     */
    private Expression parseExpression(Precedence precedence) {
        Supplier<Expression> prefix = prefixParseFns.get(cur.type());
        if (prefix == null) {
            error("no se esperaba " + describe(cur) + " aquí", cur);
            return null;
        }
        Expression left = prefix.get();

        while (left != null && !peekTokenIs(TokenType.SEMICOLON) && precedence.compareTo(peekPrecedence()) < 0) {
            Function<Expression, Expression> infix = infixParseFns.get(peek.type());
            if (infix == null) return left;
            nextToken();
            left = infix.apply(left);
        }
        return left;
    }

    private Expression parseIdentifier() {
        return new Identifier(cur, cur.literal());
    }

    private Expression parseIntegerLiteral() {
        try {
            return new IntegerLiteral(cur, Long.parseLong(cur.literal()));
        } catch (NumberFormatException e) {
            error("el entero " + cur.literal() + " es demasiado grande", cur);
            return null;
        }
    }

    private Expression parseFloatLiteral() {
        return new FloatLiteral(cur, Double.parseDouble(cur.literal()));
    }

    /** prefija := ('-' | '!') expresion */
    private Expression parsePrefixExpression() {
        Token token = cur;
        nextToken();
        Expression right = parseExpression(Precedence.PREFIX);
        if (right == null) return null;
        return new PrefixExpression(token, token.literal(), right);
    }

    /** Operadores binarios asociativos a la izquierda: el lado derecho se lee con la misma precedencia. */
    private Expression parseInfixExpression(Expression left) {
        Token token = cur;
        Precedence precedence = curPrecedence();
        nextToken();
        Expression right = parseExpression(precedence);
        if (right == null) return null;
        return new InfixExpression(token, left, token.literal(), right);
    }

    /** {@code **} es asociativo a la derecha: 2 ** 3 ** 2 = 2 ** (3 ** 2). */
    private Expression parsePowerExpression(Expression left) {
        Token token = cur;
        nextToken();
        Expression right = parseExpression(Precedence.PREFIX); // un nivel menos que POWER
        if (right == null) return null;
        return new InfixExpression(token, left, token.literal(), right);
    }

    private Expression parseLogicalExpression(Expression left) {
        Token token = cur;
        Precedence precedence = curPrecedence();
        nextToken();
        Expression right = parseExpression(precedence);
        if (right == null) return null;
        return new LogicalExpression(token, left, token.literal(), right);
    }

    /** asignacion := (IDENT | indexacion) '=' expresion. Asociativa a la derecha: a = b = 3. */
    private Expression parseAssignExpression(Expression target) {
        Token token = cur;
        if (!(target instanceof Identifier) && !(target instanceof IndexExpression)) {
            error("no se puede asignar a '" + target + "'; solo a una variable o a lista[i]", token);
            return null;
        }
        nextToken();
        Expression value = parseExpression(Precedence.LOWEST);
        if (value == null) return null;
        return new AssignExpression(token, target, value);
    }

    /** '(' expresion ')' */
    private Expression parseGroupedExpression() {
        nextToken();
        Expression expression = parseExpression(Precedence.LOWEST);
        if (expression == null || !expectPeek(TokenType.RPAREN)) return null;
        return expression;
    }

    /** '(' expresion ')' bloque, usado por siProfecia, otraProfecia y amarre. */
    private Expression parseCondition() {
        if (!expectPeek(TokenType.LPAREN)) return null;
        nextToken();
        Expression condition = parseExpression(Precedence.LOWEST);
        if (condition == null || !expectPeek(TokenType.RPAREN)) return null;
        return condition;
    }

    /** si := 'siProfecia' cond bloque ('otraProfecia' cond bloque)* ('maldicion' bloque)? */
    private Expression parseIfExpression() {
        Token token = cur;
        List<IfExpression.Branch> branches = new ArrayList<>();
        do {
            Expression condition = parseCondition();
            if (condition == null || !expectPeek(TokenType.LBRACE)) return null;
            BlockStatement block = parseBlockStatement();
            if (block == null) return null;
            branches.add(new IfExpression.Branch(condition, block));
        } while (peekTokenIs(TokenType.ELIF) && advance());

        BlockStatement alternative = null;
        if (peekTokenIs(TokenType.ELSE)) {
            nextToken();
            if (!expectPeek(TokenType.LBRACE)) return null;
            alternative = parseBlockStatement();
            if (alternative == null) return null;
        }
        return new IfExpression(token, branches, alternative);
    }

    /** Avanza un token y devuelve true; permite avanzar dentro de la condición de un ciclo. */
    private boolean advance() {
        nextToken();
        return true;
    }

    /** mientras := 'amarre' '(' expresion ')' bloque */
    private Expression parseWhileExpression() {
        Token token = cur;
        Expression condition = parseCondition();
        if (condition == null || !expectPeek(TokenType.LBRACE)) return null;
        BlockStatement body = parseBlockStatement();
        if (body == null) return null;
        return new WhileExpression(token, condition, body);
    }

    /** para := 'circulo' '(' inicio? ';' expresion? ';' expresion? ')' bloque */
    private Expression parseForExpression() {
        Token token = cur;
        if (!expectPeek(TokenType.LPAREN)) return null;
        nextToken();

        Statement init = null;
        if (!curTokenIs(TokenType.SEMICOLON)) {
            init = curTokenIs(TokenType.LET) ? parseLetStatement() : parseExpressionStatement();
            if (init == null || !expectPeek(TokenType.SEMICOLON)) return null;
        }

        Expression condition = null;
        if (peekTokenIs(TokenType.SEMICOLON)) {
            nextToken();
        } else {
            nextToken();
            condition = parseExpression(Precedence.LOWEST);
            if (condition == null || !expectPeek(TokenType.SEMICOLON)) return null;
        }

        Expression update = null;
        if (peekTokenIs(TokenType.RPAREN)) {
            nextToken();
        } else {
            nextToken();
            update = parseExpression(Precedence.LOWEST);
            if (update == null || !expectPeek(TokenType.RPAREN)) return null;
        }

        if (!expectPeek(TokenType.LBRACE)) return null;
        BlockStatement body = parseBlockStatement();
        if (body == null) return null;
        return new ForExpression(token, init, condition, update, body);
    }

    /** hechizo := 'hechizo' '(' (IDENT (',' IDENT)*)? ')' bloque */
    private Expression parseFunctionLiteral() {
        Token token = cur;
        if (!expectPeek(TokenType.LPAREN)) return null;
        List<Identifier> parameters = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (peekTokenIs(TokenType.RPAREN)) {
            nextToken();
        } else {
            do {
                nextToken();
                if (!curTokenIs(TokenType.IDENT)) {
                    error("se esperaba el nombre de un parámetro pero se encontró " + describe(cur), cur);
                    return null;
                }
                if (!seen.add(cur.literal())) {
                    error("el parámetro '" + cur.literal() + "' está repetido", cur);
                    return null;
                }
                parameters.add(new Identifier(cur, cur.literal()));
            } while (peekTokenIs(TokenType.COMMA) && advance());
            if (!expectPeek(TokenType.RPAREN)) return null;
        }
        if (!expectPeek(TokenType.LBRACE)) return null;
        BlockStatement body = parseBlockStatement();
        if (body == null) return null;
        return new FunctionLiteral(token, parameters, body);
    }

    /** llamado := expresion '(' argumentos ')' */
    private Expression parseCallExpression(Expression function) {
        Token token = cur;
        List<Expression> arguments = parseExpressionList(TokenType.RPAREN);
        if (arguments == null) return null;
        return new CallExpression(token, function, arguments);
    }

    /** indexacion := expresion '[' expresion ']' */
    private Expression parseIndexExpression(Expression left) {
        Token token = cur;
        nextToken();
        Expression index = parseExpression(Precedence.LOWEST);
        if (index == null || !expectPeek(TokenType.RBRACKET)) return null;
        return new IndexExpression(token, left, index);
    }

    /** lista := '[' (expresion (',' expresion)*)? ']' */
    private Expression parseArrayLiteral() {
        Token token = cur;
        List<Expression> elements = parseExpressionList(TokenType.RBRACKET);
        if (elements == null) return null;
        return new ArrayLiteral(token, elements);
    }

    /** diccionario := '{' (expresion ':' expresion (',' expresion ':' expresion)*)? '}' */
    private Expression parseHashLiteral() {
        Token token = cur;
        List<Expression> keys = new ArrayList<>();
        List<Expression> values = new ArrayList<>();
        while (!peekTokenIs(TokenType.RBRACE)) {
            nextToken();
            Expression key = parseExpression(Precedence.LOWEST);
            if (key == null || !expectPeek(TokenType.COLON)) return null;
            nextToken();
            Expression value = parseExpression(Precedence.LOWEST);
            if (value == null) return null;
            keys.add(key);
            values.add(value);
            if (!peekTokenIs(TokenType.RBRACE) && !expectPeek(TokenType.COMMA)) return null;
        }
        if (!expectPeek(TokenType.RBRACE)) return null;
        return new HashLiteral(token, keys, values);
    }

    /** Lista de expresiones separadas por comas hasta {@code end}. Al entrar, cur es el delimitador de apertura. */
    private List<Expression> parseExpressionList(TokenType end) {
        List<Expression> list = new ArrayList<>();
        if (peekTokenIs(end)) {
            nextToken();
            return list;
        }
        nextToken();
        Expression first = parseExpression(Precedence.LOWEST);
        if (first == null) return null;
        list.add(first);
        while (peekTokenIs(TokenType.COMMA)) {
            nextToken();
            nextToken();
            Expression next = parseExpression(Precedence.LOWEST);
            if (next == null) return null;
            list.add(next);
        }
        if (!expectPeek(end)) return null;
        return list;
    }
}
