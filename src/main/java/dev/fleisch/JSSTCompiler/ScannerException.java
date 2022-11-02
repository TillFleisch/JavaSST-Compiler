package dev.fleisch.JSSTCompiler;

/**
 * Class containing Scanner specific exceptions
 *
 * @author TillFleisch
 */
public class ScannerException extends Exception {

    /**
     * Basic general scanner exception
     */
    public ScannerException() {
    }

    /**
     * Basic general scanner exception with custom message
     *
     * @param message Exception message
     */
    public ScannerException(String message) {
        super(message);
    }

    /**
     * Unknown Sequence expression, may be thrown if the scanner encounters an unknown character sequence.
     *
     * @author TillFleisch
     */
    static class UnknownSequenceException extends ScannerException {

        /**
         * Unknown Sequence expression constructor with message parameter for further insight.
         *
         * @param message Information about the unknown sequence
         */
        public UnknownSequenceException(String message) {
            super(message);
        }
    }

}
