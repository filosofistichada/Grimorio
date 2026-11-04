package grimorio.evaluator;

import grimorio.object.*;
import grimorio.token.Token;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Hechizos integrados. Se buscan después de las variables del usuario, así
 * que una pocion con el mismo nombre los oculta.
 *
 * <pre>
 *   revelar(a, b, ...)      imprime los valores separados por espacio
 *   medir(x)                longitud de un texto, lista o diccionario
 *   transmutar(x)           convierte a texto
 *   cristalizar(x)          convierte a entero (trunca decimales)
 *   destilar(x)             convierte a decimal
 *   esencia(x)              nombre del tipo: "entero", "texto", ...
 *   alCaldero(lista, x)     agrega x al final de la lista y la devuelve
 *   claves(diccionario)     lista con las claves
 *   encantar(lista, h)      nueva lista con h(x) para cada x       (map)
 *   tamizar(lista, h)       nueva lista con los x donde h(x) es luz (filter)
 * </pre>
 */
final class Builtins {
    private final Evaluator evaluator;
    private final PrintStream out;

    Builtins(Evaluator evaluator, PrintStream out) {
        this.evaluator = evaluator;
        this.out = out;
    }

    Map<String, BuiltinObj> all() {
        Map<String, BuiltinObj> map = new LinkedHashMap<>();
        add(map, "revelar", this::revelar);
        add(map, "medir", this::medir);
        add(map, "transmutar", this::transmutar);
        add(map, "cristalizar", this::cristalizar);
        add(map, "destilar", this::destilar);
        add(map, "esencia", this::esencia);
        add(map, "alCaldero", this::alCaldero);
        add(map, "claves", this::claves);
        add(map, "encantar", this::encantar);
        add(map, "tamizar", this::tamizar);
        return map;
    }

    private static void add(Map<String, BuiltinObj> map, String name, BuiltinObj.Implementation implementation) {
        map.put(name, new BuiltinObj(name, implementation));
    }

    private Obj revelar(List<Obj> args, Token at) {
        out.println(args.stream().map(Obj::inspect).collect(Collectors.joining(" ")));
        return NullObj.INSTANCE;
    }

    private Obj medir(List<Obj> args, Token at) {
        Obj arity = checkArity("medir", args, 1, at);
        if (arity != null) return arity;
        Obj x = args.get(0);
        if (x instanceof StringObj) return new IntegerObj(((StringObj) x).value.length());
        if (x instanceof ArrayObj) return new IntegerObj(((ArrayObj) x).elements.size());
        if (x instanceof HashObj) return new IntegerObj(((HashObj) x).pairs.size());
        return Evaluator.error(at, "medir() no sabe medir un " + x.type().displayName());
    }

    private Obj transmutar(List<Obj> args, Token at) {
        Obj arity = checkArity("transmutar", args, 1, at);
        if (arity != null) return arity;
        return new StringObj(args.get(0).inspect());
    }

    private Obj cristalizar(List<Obj> args, Token at) {
        Obj arity = checkArity("cristalizar", args, 1, at);
        if (arity != null) return arity;
        Obj x = args.get(0);
        if (x instanceof IntegerObj) return x;
        if (x instanceof FloatObj) {
            double value = ((FloatObj) x).value;
            if (Double.isNaN(value) || Double.isInfinite(value)) return Evaluator.error(at, "no se puede cristalizar " + x.inspect());
            return new IntegerObj((long) value);
        }
        if (x instanceof BooleanObj) return new IntegerObj(((BooleanObj) x).value ? 1 : 0);
        if (x instanceof StringObj) {
            String text = ((StringObj) x).value.trim();
            try {
                return new IntegerObj(Long.parseLong(text));
            } catch (NumberFormatException notInteger) {
                try {
                    return new IntegerObj((long) Double.parseDouble(text));
                } catch (NumberFormatException notNumber) {
                    return Evaluator.error(at, "no se puede cristalizar el texto \"" + text + "\" en un entero");
                }
            }
        }
        return Evaluator.error(at, "no se puede cristalizar un " + x.type().displayName());
    }

    private Obj destilar(List<Obj> args, Token at) {
        Obj arity = checkArity("destilar", args, 1, at);
        if (arity != null) return arity;
        Obj x = args.get(0);
        if (x instanceof FloatObj) return x;
        if (x instanceof IntegerObj) return new FloatObj(((IntegerObj) x).value);
        if (x instanceof StringObj) {
            try {
                return new FloatObj(Double.parseDouble(((StringObj) x).value.trim()));
            } catch (NumberFormatException e) {
                return Evaluator.error(at, "no se puede destilar el texto \"" + ((StringObj) x).value + "\" en un decimal");
            }
        }
        return Evaluator.error(at, "no se puede destilar un " + x.type().displayName());
    }

    private Obj esencia(List<Obj> args, Token at) {
        Obj arity = checkArity("esencia", args, 1, at);
        if (arity != null) return arity;
        return new StringObj(args.get(0).type().displayName());
    }

    private Obj alCaldero(List<Obj> args, Token at) {
        Obj arity = checkArity("alCaldero", args, 2, at);
        if (arity != null) return arity;
        if (!(args.get(0) instanceof ArrayObj)) {
            return Evaluator.error(at, "alCaldero() necesita una lista como primer argumento, no un " + args.get(0).type().displayName());
        }
        ArrayObj array = (ArrayObj) args.get(0);
        array.elements.add(args.get(1));
        return array;
    }

    private Obj claves(List<Obj> args, Token at) {
        Obj arity = checkArity("claves", args, 1, at);
        if (arity != null) return arity;
        if (!(args.get(0) instanceof HashObj)) {
            return Evaluator.error(at, "claves() necesita un diccionario, no un " + args.get(0).type().displayName());
        }
        List<Obj> keys = new ArrayList<>();
        for (HashObj.Pair pair : ((HashObj) args.get(0)).pairs.values()) keys.add(pair.key);
        return new ArrayObj(keys);
    }

    /** Hechizo de orden superior: recibe otro hechizo y lo aplica a cada elemento. */
    private Obj encantar(List<Obj> args, Token at) {
        Obj check = checkListAndFunction("encantar", args, at);
        if (check != null) return check;
        List<Obj> result = new ArrayList<>();
        for (Obj element : ((ArrayObj) args.get(0)).elements) {
            Obj value = evaluator.applyFunction(args.get(1), List.of(element), at);
            if (Evaluator.isError(value)) return value;
            result.add(value);
        }
        return new ArrayObj(result);
    }

    private Obj tamizar(List<Obj> args, Token at) {
        Obj check = checkListAndFunction("tamizar", args, at);
        if (check != null) return check;
        List<Obj> result = new ArrayList<>();
        for (Obj element : ((ArrayObj) args.get(0)).elements) {
            Obj keep = evaluator.applyFunction(args.get(1), List.of(element), at);
            if (Evaluator.isError(keep)) return keep;
            if (Evaluator.isTruthy(keep)) result.add(element);
        }
        return new ArrayObj(result);
    }

    private static Obj checkListAndFunction(String name, List<Obj> args, Token at) {
        Obj arity = checkArity(name, args, 2, at);
        if (arity != null) return arity;
        if (!(args.get(0) instanceof ArrayObj)) {
            return Evaluator.error(at, name + "() necesita una lista como primer argumento");
        }
        if (!(args.get(1) instanceof FunctionObj) && !(args.get(1) instanceof BuiltinObj)) {
            return Evaluator.error(at, name + "() necesita un hechizo como segundo argumento");
        }
        return null;
    }

    /** Devuelve un error si la cantidad de argumentos no es la esperada, o null si está bien. */
    private static Obj checkArity(String name, List<Obj> args, int expected, Token at) {
        if (args.size() == expected) return null;
        return Evaluator.error(at, name + "() espera " + expected + " argumento(s) y recibió " + args.size());
    }
}
