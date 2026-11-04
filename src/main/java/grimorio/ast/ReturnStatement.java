package grimorio.ast;

import grimorio.token.Token;

/** {@code conjura valor;} termina el hechizo actual y entrega un valor. El valor es opcional. */
public final class ReturnStatement implements Statement {
    public final Token token;
    public final Expression value; // null si es "conjura;"

    public ReturnStatement(Token token, Expression value) {
        this.token = token;
        this.value = value;
    }

    @Override public Token token() { return token; }

    @Override
    public String toString() {
        return value == null ? "conjura;" : "conjura " + value + ";";
    }
}
