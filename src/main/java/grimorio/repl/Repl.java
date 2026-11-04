package grimorio.repl;

import grimorio.Interpreter;
import grimorio.lexer.Lexer;
import grimorio.object.ErrorObj;
import grimorio.object.NullObj;
import grimorio.object.Obj;
import grimorio.token.Token;
import grimorio.token.TokenType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Consola interactiva (Read-Eval-Print Loop). Tiene tres modos para ver cada
 * etapa del intérprete: lexer (tokens), parser (AST) y evaluar (resultado).
 *
 * Si una línea deja paréntesis, llaves o corchetes abiertos, el REPL sigue
 * leyendo líneas hasta que se cierren, así se pueden escribir hechizos de
 * varias líneas.
 */
public final class Repl {
    private enum Mode { LEXER, PARSER, EVALUAR }

    private static final String PROMPT = "grimorio>> ";
    private static final String CONTINUE = "       ... ";

    private final BufferedReader in;
    private final PrintStream out;
    private final Interpreter interpreter;
    private Mode mode = Mode.EVALUAR;

    public Repl(BufferedReader in, PrintStream out) {
        this.in = in;
        this.out = out;
        this.interpreter = new Interpreter(out);
    }

    public void start() throws IOException {
        out.println("Grimorio — escribe :ayuda para ver los comandos, :salir para terminar.");
        while (true) {
            out.print(promptForMode());
            String line = in.readLine();
            if (line == null) break; // Ctrl+D / Ctrl+Z
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.startsWith(":")) {
                if (!command(trimmed)) break;
                continue;
            }
            String source = readUntilBalanced(line);
            if (source == null) break;
            process(source);
        }
        out.println("¡Hasta la próxima luna!");
    }

    private String promptForMode() {
        switch (mode) {
            case LEXER: return "lexer>> ";
            case PARSER: return "parser>> ";
            default: return PROMPT;
        }
    }

    /** Ejecuta un comando del REPL. Devuelve false si hay que salir. */
    private boolean command(String text) throws IOException {
        String[] parts = text.split("\\s+", 2);
        switch (parts[0]) {
            case ":salir":
                return false;
            case ":lexer":
                mode = Mode.LEXER;
                out.println("Modo lexer: muestra los tokens.");
                break;
            case ":parser":
                mode = Mode.PARSER;
                out.println("Modo parser: muestra el AST.");
                break;
            case ":evaluar":
                mode = Mode.EVALUAR;
                out.println("Modo evaluar: ejecuta el código.");
                break;
            case ":cargar":
                if (parts.length < 2) {
                    out.println("Uso: :cargar ruta/al/archivo.grim");
                } else {
                    load(parts[1].trim());
                }
                break;
            case ":ayuda":
                help();
                break;
            default:
                out.println("Comando desconocido " + parts[0] + ". Escribe :ayuda.");
        }
        return true;
    }

    private void help() {
        out.println("Comandos:");
        out.println("  :lexer           ver los tokens de cada entrada");
        out.println("  :parser          ver el AST de cada entrada");
        out.println("  :evaluar         ejecutar el código (modo inicial)");
        out.println("  :cargar ruta     ejecutar un archivo .grim en esta sesión");
        out.println("  :ayuda           mostrar esta ayuda");
        out.println("  :salir           terminar");
        out.println("Las pociones se recuerdan entre líneas. Un bloque sin cerrar continúa en la línea siguiente.");
    }

    private void load(String path) {
        Path file = Paths.get(path);
        try {
            String source = Files.readString(file);
            if (interpreter.runProgram(source)) out.println("Grimorio cargado: " + file);
        } catch (IOException e) {
            out.println("No se pudo leer el archivo " + file + ": " + e.getMessage());
        }
    }

    /** Lee más líneas mientras haya delimitadores sin cerrar. */
    private String readUntilBalanced(String first) throws IOException {
        StringBuilder source = new StringBuilder(first);
        while (openDelimiters(source.toString()) > 0) {
            out.print(CONTINUE);
            String line = in.readLine();
            if (line == null) return null;
            if (line.trim().isEmpty()) break; // una línea vacía fuerza el fin de la entrada
            source.append('\n').append(line);
        }
        return source.toString();
    }

    private static int openDelimiters(String source) {
        int depth = 0;
        for (Token token : Lexer.tokenize(source)) {
            TokenType type = token.type();
            if (type == TokenType.LPAREN || type == TokenType.LBRACE || type == TokenType.LBRACKET) depth++;
            if (type == TokenType.RPAREN || type == TokenType.RBRACE || type == TokenType.RBRACKET) depth--;
        }
        return depth;
    }

    private void process(String source) {
        switch (mode) {
            case LEXER:
                Lexer lexer = new Lexer(source);
                Token token;
                while ((token = lexer.nextToken()).type() != TokenType.EOF) out.println(token);
                lexer.errors().forEach(out::println);
                break;
            case PARSER:
                out.print(Interpreter.describeAst(source));
                if (!source.isEmpty()) out.println();
                break;
            default:
                Obj result = interpreter.run(source);
                if (result instanceof ErrorObj) out.println(result.inspect());
                else if (result != NullObj.INSTANCE) out.println(result.repr());
        }
    }
}
