package grimorio;

import grimorio.lexer.Lexer;
import grimorio.repl.Repl;
import grimorio.token.Token;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Punto de entrada.
 *
 * <pre>
 *   java -jar grimorio.jar                     abre el REPL
 *   java -jar grimorio.jar programa.grim       ejecuta un archivo
 *   java -jar grimorio.jar --tokens prog.grim  muestra los tokens del archivo
 *   java -jar grimorio.jar --ast prog.grim     muestra el AST del archivo
 * </pre>
 *
 * Código de salida: 0 si todo salió bien, 1 si el programa tuvo errores, 2 si
 * no se pudo leer el archivo o los argumentos son inválidos.
 */
public final class Main {
    private Main() {}

    public static void main(String[] args) throws IOException {
        // UTF-8 explícito para que las tildes se vean bien (en Windows: ejecutar antes "chcp 65001").
        PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);

        if (args.length == 0) {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            new Repl(in, out).start();
            return;
        }

        String option = args.length == 2 ? args[0] : null;
        String path = args[args.length - 1];
        if (args.length > 2 || path.equals("--ayuda") || (option != null && !option.equals("--tokens") && !option.equals("--ast"))) {
            out.println("Uso: grimorio [--tokens | --ast] [archivo.grim]");
            System.exit(2);
        }

        String source;
        Path file = Paths.get(path);
        try {
            source = Files.readString(file);
        } catch (IOException e) {
            out.println("No se pudo leer el archivo " + file + ": " + e.getMessage());
            System.exit(2);
            return;
        }

        if ("--tokens".equals(option)) {
            Lexer lexer = new Lexer(source);
            Token token;
            do {
                token = lexer.nextToken();
                out.println(token);
            } while (token.type() != grimorio.token.TokenType.EOF);
            lexer.errors().forEach(out::println);
            return;
        }
        if ("--ast".equals(option)) {
            out.print(Interpreter.describeAst(source));
            out.println();
            return;
        }

        boolean ok = new Interpreter(out).runProgram(source);
        System.exit(ok ? 0 : 1);
    }
}
