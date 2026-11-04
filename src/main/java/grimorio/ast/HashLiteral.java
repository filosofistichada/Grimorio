package grimorio.ast;

import grimorio.token.Token;

import java.util.List;

/** Diccionario literal: {@code {"nombre": "Merlin", "edad": 900}}. Las claves se guardan en orden. */
public final class HashLiteral implements Expression {
    public final Token token;
    public final List<Expression> keys;
    public final List<Expression> values;

    public HashLiteral(Token token, List<Expression> keys, List<Expression> values) {
        this.token = token;
        this.keys = keys;
        this.values = values;
    }

    @Override public Token token() { return token; }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("{");
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) out.append(", ");
            out.append(keys.get(i)).append(": ").append(values.get(i));
        }
        return out.append("}").toString();
    }
}
