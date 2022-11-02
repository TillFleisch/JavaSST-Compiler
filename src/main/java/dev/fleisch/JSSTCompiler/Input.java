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
     * The input stream's position within the file. (Line within the file)
     */
    private int line = 1;

    /**
     * The input stream's position within the file. (Char on line)
     */
    private int position = 0;

    /**
     * Creates a Input object for a given JavaSST source file
     *
     * @param filePath Path pointing to a JavaSST source file
     * @throws FileNotFoundException
     */
    public Input(String filePath) throws FileNotFoundException {
        inputStream = new FileInputStream(filePath);
    }

    /**
     * Reads the next character available on the input file.
     *
     * @return Next file
     * @throws IOException If reading fails, or EOF has been reached.
     */
    public char next() throws IOException {
        if (inputStream.available() != 0) {
            char c = (char) inputStream.read();

            // Increase position and line to keep track of the streams position
            position++;
            if (c == '\n') {
                position = 0;
                line++;
            }

            return c;
        } else {
            inputStream.close();
            throw new EOFException();
        }
    }

    /**
     * @return The current position at which the input helper is within the file.
     */
    public int getPosition() {
        return position;
    }

    /**
     * @return The current line at which the input helper is within the file.
     */
    public int getLine() {
        return line;
    }
}
