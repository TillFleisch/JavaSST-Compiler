package dev.fleisch.JSSTCompiler;

import java.io.EOFException;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        parserTest();
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
}