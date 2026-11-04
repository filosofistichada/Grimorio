package grimorio.ast;

import grimorio.token.Token;

import java.util.List;
import java.util.stream.Collectors;

/** {@code hechizo(a, b) { ... }}: define una función anónima; se nombra guardándola en una pocion. */
public final class FunctionLiteral implements Expression {
    public final Token token;
    public final List<Identifier> parameters;
    public final BlockStatement body;

    public FunctionLiteral(Token token, List<Identifier> parameters, BlockStatement body) {
        this.token = token;
        this.parameters = parameters;
        this.body = body;
    }

    @Override public Token token() { return token; }

    @Override
    public String toString() {
        return "hechizo(" + parameters.stream().map(Object::toString).collect(Collectors.joining(", ")) + ") " + body;
    }
}
