package grimorio.ast;

import grimorio.token.Token;

import java.util.List;
import java.util.stream.Collectors;

/** Lista literal: {@code [1, 2, 3]}. */
public final class ArrayLiteral implements Expression {
    public final Token token;
    public final List<Expression> elements;

    public ArrayLiteral(Token token, List<Expression> elements) {
        this.token = token;
        this.elements = elements;
    }

    @Override public Token token() { return token; }

    @Override
    public String toString() {
        return "[" + elements.stream().map(Object::toString).collect(Collectors.joining(", ")) + "]";
    }
}
