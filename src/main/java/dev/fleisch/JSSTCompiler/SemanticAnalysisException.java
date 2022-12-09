package dev.fleisch.JSSTCompiler;

/**
 * Class containing parser specific exceptions
 *
 * @author TillFleisch
 */
public class SemanticAnalysisException extends Exception {

    /**
     * Basic general semantic analysis exception
     */
    public SemanticAnalysisException() {
    }

    /**
     * Basic general semantic analysis exception with custom message
     *
     * @param message Exception message
     */
    public SemanticAnalysisException(String message) {
        super(message);
    }

    /**
     * Basic general semantic analysis exception with custom message and code position
     *
     * @param message Exception message
     */
    public SemanticAnalysisException(String message, CodePosition codePosition) {
        super(message + " at " + codePosition);
    }


}
