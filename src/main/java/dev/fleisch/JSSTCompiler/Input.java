package dev.fleisch.JSSTCompiler;


import java.io.*;

/**
 * Input class which manages basic file processing of JavaSST source files.
 *
 * @author TillFleisch
 */
public class Input {

    /**
     * Inputsteam
     */
    private final InputStream inputStream;

    /**
     * The input stream's position within the file.
     */
    private final CodePosition codePosition = new CodePosition(1, 0);

    /**
     * Determines if input is available
     */
    private boolean available = true;

    /**
     * Creates a Input object for a given JavaSST source file
     *
     * @param filePath Path pointing to a JavaSST source file
     * @throws FileNotFoundException If the source file is not available
     */
    public Input(String filePath) throws FileNotFoundException {
        inputStream = new FileInputStream(filePath);
    }

    /**
     * Creates a Input object for a given JavaSST input stream
     */
    public Input(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * Reads the next character available on the input file.
     *
     * @return Next file
     * @throws IOException If reading fails, or EOF has been reached.
     */
    public char next() throws IOException {
        if (available && inputStream.available() != 0) {
            char c = (char) inputStream.read();

            // Increase position and line to keep track of the streams position
            codePosition.setColumn(codePosition.getColumn() + 1);
            if (c == '\n') {
                codePosition.setColumn(0);
                codePosition.setLine(codePosition.getLine() + 1);
            }

            return c;
        } else {
            available = false;
            inputStream.close();
            throw new EOFException();
        }
    }

    /**
     * Determines if the input is available
     *
     * @return true if the input is still available
     */
    public boolean available() {
        return available;
    }

    /**
     * @return The current position at which the input helper is within the file.
     */
    public CodePosition getPosition() {
        return codePosition;
    }

}
