package grimorio.ast;

import grimorio.token.Token;

/** Entero literal, por ejemplo {@code 42}. */
public final class IntegerLiteral implements Expression {
    public final Token token;
    public final long value;

    public IntegerLiteral(Token token, long value) {
        this.token = token;
        this.value = value;
    }

    @Override public Token token() { return token; }

    @Override
    public String toString() {
        return token.literal();
    }
}
