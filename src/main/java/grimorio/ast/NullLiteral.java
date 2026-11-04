package grimorio.ast;

import grimorio.token.Token;

/** {@code vacio}: la ausencia de valor. */
public final class NullLiteral implements Expression {
    public final Token token;

    public NullLiteral(Token token) {
        this.token = token;
    }

    @Override public Token token() { return token; }

    @Override
    public String toString() {
        return "vacio";
    }
}
