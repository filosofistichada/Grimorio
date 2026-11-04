package grimorio.ast;

import grimorio.token.Token;

/** Texto literal entre comillas dobles. */
public final class StringLiteral implements Expression {
    public final Token token;
    public final String value;

    public StringLiteral(Token token, String value) {
        this.token = token;
        this.value = value;
    }

    @Override public Token token() { return token; }

    @Override
    public String toString() {
        return "\"" + value + "\"";
    }
}
