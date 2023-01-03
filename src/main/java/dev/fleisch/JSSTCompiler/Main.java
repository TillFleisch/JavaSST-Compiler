package dev.fleisch.JSSTCompiler;

import dev.fleisch.JSSTCompiler.ByteCodeGenerator.ByteCodeGenerator;

import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class Main {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Input file missing");
            return;
        }

        try {
            Parser parser = new Parser(new Scanner(new Input(args[0])));

            byteCodeGeneration(parser, args.length >= 2 && Boolean.parseBoolean(args[1]));
        } catch (ScannerException | IOException | ParserException e) {
            throw new RuntimeException(e);
        }
    }

    public static void scannerTest() {
        try {
            Input input = new Input("JavaSSTSourceCode/ScannerTest.jsst");
            Scanner scanner = new Scanner(input);
            while (true) {
                Symbol<?> symbol = scanner.nextSymbol();
                System.out.println(symbol);
            }
        } catch (EOFException e) {

        } catch (IOException | ScannerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void parserTest() {
        Parser.ParserTest.runTests();

        try {
            Parser parser = new Parser(new Scanner(new Input("JavaSSTSourceCode/ParserTest.jsst")));
            Objekt.Clasz clasz = parser.parse();

            SemanticAnalysis.run(clasz);

            System.out.println(clasz.toDot(true, true, true));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void byteCodeGeneration(Parser parser, boolean createDotFile) {
        try {
            Objekt.Clasz clasz = parser.parse();

            SemanticAnalysis.run(clasz);

            // Write dot file if required
            if (createDotFile) {
                try (PrintWriter fileOutputStream = new PrintWriter(new FileOutputStream(clasz.name + ".dot"))) {
                    fileOutputStream.write(clasz.toDot(true, true, true));
                }
            }

            // Create a bytecode generator
            ByteCodeGenerator byteCodeGenerator = new ByteCodeGenerator(clasz);

            // Generate ByteCode and write to file
            try (FileOutputStream fileOutputStream = new FileOutputStream(clasz.name + ".class")) {
                byteCodeGenerator.generate().writeTo(fileOutputStream);
            }

            System.out.println("\u001B[32mFile compiled successfully!\u001B[0m");
            System.out.println("Saved to: " + clasz.name + ".class");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}