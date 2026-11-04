package grimorio.ast;

import grimorio.token.Token;

import java.util.List;
import java.util.stream.Collectors;

/** Llamado a un hechizo: {@code fib(10)}. {@code function} puede ser cualquier expresión que produzca un hechizo. */
public final class CallExpression implements Expression {
    public final Token token;
    public final Expression function;
    public final List<Expression> arguments;

    public CallExpression(Token token, Expression function, List<Expression> arguments) {
        this.token = token;
        this.function = function;
        this.arguments = arguments;
    }

    @Override public Token token() { return token; }

    @Override
    public String toString() {
        return function + "(" + arguments.stream().map(Object::toString).collect(Collectors.joining(", ")) + ")";
    }
}
