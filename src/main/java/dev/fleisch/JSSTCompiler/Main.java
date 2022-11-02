package dev.fleisch.JSSTCompiler;

import java.io.EOFException;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            Input input = new Input("JavaSSTSourceCode/test.jsst");
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
}