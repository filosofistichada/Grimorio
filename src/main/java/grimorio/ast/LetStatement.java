package grimorio.ast;

import grimorio.token.Token;

/** {@code pocion nombre = valor;} declara una variable en el ámbito actual. */
public final class LetStatement implements Statement {
    public final Token token;
    public final Identifier name;
    public final Expression value;

    public LetStatement(Token token, Identifier name, Expression value) {
        this.token = token;
        this.name = name;
        this.value = value;
    }

    @Override public Token token() { return token; }

    @Override
    public String toString() {
        return "pocion " + name + " = " + value + ";";
    }
}
