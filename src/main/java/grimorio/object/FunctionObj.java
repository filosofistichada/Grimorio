package grimorio.object;

import grimorio.ast.BlockStatement;
import grimorio.ast.Identifier;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Hechizo definido por el usuario. Guarda el entorno donde se creó
 * ({@code env}): por eso puede seguir usando esas variables después
 * (closures).
 */
public final class FunctionObj implements Obj {
    public final List<Identifier> parameters;
    public final BlockStatement body;
    public final Environment env;

    public FunctionObj(List<Identifier> parameters, BlockStatement body, Environment env) {
        this.parameters = parameters;
        this.body = body;
        this.env = env;
    }

    @Override public ObjType type() { return ObjType.FUNCTION; }

    @Override
    public String inspect() {
        return "hechizo(" + parameters.stream().map(p -> p.name).collect(Collectors.joining(", ")) + ") {...}";
    }
}
