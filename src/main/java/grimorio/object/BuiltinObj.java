package grimorio.object;

import grimorio.token.Token;

import java.util.List;

/** Hechizo integrado del lenguaje (revelar, medir, ...), implementado en Java. */
public final class BuiltinObj implements Obj {
    @FunctionalInterface
    public interface Implementation {
        /** @param at token del llamado, para ubicar los errores */
        Obj apply(List<Obj> args, Token at);
    }

    public final String name;
    public final Implementation implementation;

    public BuiltinObj(String name, Implementation implementation) {
        this.name = name;
        this.implementation = implementation;
    }

    @Override public ObjType type() { return ObjType.BUILTIN; }
    @Override public String inspect() { return "hechizo integrado " + name; }
}
