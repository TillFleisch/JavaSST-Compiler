package dev.fleisch.JSSTCompiler;

import dev.fleisch.JSSTCompiler.ByteCodeGenerator.ByteCodeGenerator;

import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        byteCodeGenerationTest();
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

    public static void byteCodeGenerationTest() {
        try {
            Parser parser = new Parser(new Scanner(new Input("JavaSSTSourceCode/ParserTest.jsst")));
            Objekt.Clasz clasz = parser.parse();

            SemanticAnalysis.run(clasz);

            // Create a bytecode generator
            ByteCodeGenerator byteCodeGenerator = new ByteCodeGenerator(clasz);

            // Generate ByteCode and write to file
            try (FileOutputStream fileOutputStream = new FileOutputStream(clasz.name + ".class")) {
                byteCodeGenerator.generate().writeTo(fileOutputStream);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}