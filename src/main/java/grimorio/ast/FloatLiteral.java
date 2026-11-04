package grimorio.ast;

import grimorio.token.Token;

/** Decimal literal, por ejemplo {@code 3.14}. */
public final class FloatLiteral implements Expression {
    public final Token token;
    public final double value;

    public FloatLiteral(Token token, double value) {
        this.token = token;
        this.value = value;
    }

    @Override public Token token() { return token; }

    @Override
    public String toString() {
        return token.literal();
    }
}
