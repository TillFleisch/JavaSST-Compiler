package dev.fleisch.JSSTCompiler;

/**
 * Class containing parser specific exceptions
 *
 * @author TillFleisch
 */
public class ParserException extends Exception {

    /**
     * Basic general Parser exception
     */
    public ParserException() {
    }

    /**
     * Basic general parser exception with custom message
     *
     * @param message Exception message
     */
    public ParserException(String message) {
        super(message);
    }

    /**
     * Basic general parser exception with custom message and scanner position
     *
     * @param message Exception message
     */
    public ParserException(String message, Scanner scanner) {
        super(message +
                " at Line " + scanner.getLine() + ":" + scanner.getPosition());
    }

    /**
     * Expected but found exception for discrepancies during parsing
     *
     * @author TillFleisch
     */
    static class ExpectedButFoundException extends ParserException {


        public ExpectedButFoundException(Symbol<?> expected, Symbol<?> found, Scanner scanner) {
            super("Expected '" +
                    ((expected.getType() != Symbol.Type.KEYWORD) ? expected.getType() : expected.content) +
                    "' but found " + found.toString() +
                    " at Line " + scanner.getLine() + ":" + scanner.getPosition());
        }

        public ExpectedButFoundException(Keyword expected, Symbol<?> found, Scanner scanner) {
            super("Expected '" +
                    expected +
                    "' but found " + found.toString() +
                    " at Line " + scanner.getLine() + ":" + scanner.getPosition());
        }

        public ExpectedButFoundException(Symbol.Type expected, Symbol<?> found, Scanner scanner) {
            super("Expected '" + expected +
                    "' but found " + found.toString() +
                    " at Line " + scanner.getLine() + ":" + scanner.getPosition());
        }

    }

}
