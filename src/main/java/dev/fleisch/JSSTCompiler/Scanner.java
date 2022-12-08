package dev.fleisch.JSSTCompiler;

import java.io.EOFException;
import java.io.IOException;

/**
 * Basic scanner for JavaSST source Code.
 *
 * @author TillFleisch
 */
public class Scanner {

    /**
     * Input object to process the input file character by character.
     */
    private final Input input;

    /**
     * The current character being processed by the Scanner
     */
    char currentCharacter = ' ';


    /**
     * Creates a Scanner for a given file.
     *
     * @param input Input object for a given File
     */
    public Scanner(Input input) {
        this.input = input;
    }

    /**
     * Tries to read the next valid symbol from the given input
     */
    public Symbol<?> nextSymbol() throws ScannerException, IOException {

        if (!input.available())
            throw new EOFException();

        skipIrrelevant();

        // Check if the current character is a digit -> start of a number
        if (isDigit(currentCharacter)) {
            int number = 0;
            while (isDigit(currentCharacter)) {
                // Convert character to
                number = number * 10 + currentCharacter - '0';
                try {
                    currentCharacter = input.next();
                } catch (EOFException e) {
                    break;
                }
            }
            return new Symbol<>(number);
        }

        // Check if the current character is a letter -> identifier, variable or keyword
        if (isLetter(currentCharacter)) {

            // Create a substring and read valid variable names and keywords
            StringBuilder substringBuilder = new StringBuilder();
            while (isLetter(currentCharacter) || isDigit(currentCharacter)) {
                substringBuilder.append(currentCharacter);
                try {
                    currentCharacter = input.next();
                } catch (EOFException e) {
                    break;
                }
            }

            String substring = substringBuilder.toString();

            // Check if the identifier matches any reserved keyword
            for (Keyword keyword : Keyword.values())
                if (keyword.getKeyword().equals(substring))
                    return new Symbol<>(keyword);

            // Assume the substring is a variable identifier
            return new Symbol<>(substring);
        }

        // From here on only keywords are possible
        // We use the following heuristic: read until the input is no longer a keyword, then return the last known keyword
        if (!isLetter(currentCharacter) && !isDigit(currentCharacter) && !isWhitespace(currentCharacter)) {
            StringBuilder substringBuilder = new StringBuilder();

            Keyword validKeyword = null;
            // Iterate until the current sequence is not valid anymore
            while (!isLetter(currentCharacter) && !isDigit(currentCharacter) && !isWhitespace(currentCharacter)) {
                substringBuilder.append(currentCharacter);

                // Check if the substring is still a valid keyword
                boolean valid = false;
                for (Keyword keyword : Keyword.values())
                    if (keyword.getKeyword().equals(substringBuilder.toString())) {
                        valid = true;
                        validKeyword = keyword;
                    }

                // Return the last valid keyword
                if (!valid) {
                    break;
                }

                // Continue with the next character
                try {
                    currentCharacter = input.next();
                } catch (EOFException e) {
                    break;
                }
            }

            // Check if a valid keyword was retrieved, throw error otherwise
            if (validKeyword != null) {
                // Skip comments multiline, catch trailing ends

                // Skip comments (multiline)
                if (validKeyword == Keyword.START_COMMENT_MULTI_LINE) {
                    try {
                        // Read symbol until *\
                        while (true) {
                            currentCharacter = input.next();

                            // Detect /*
                            if (currentCharacter == '*') {
                                currentCharacter = input.next();
                                if (currentCharacter == '/') {
                                    break;
                                }
                            }
                        }

                    } catch (EOFException e) {
                        throw new ScannerException("Unclosed comment at " + input.getPosition());
                    }
                    currentCharacter = input.next();
                    // Return symbol after comment
                    return nextSymbol();
                }

                // Skip comments (single line)
                if (validKeyword == Keyword.START_COMMENT) {
                    // Read symbol until \n
                    while (true) {
                        currentCharacter = input.next();

                        // Detect \n
                        if (currentCharacter == '\n') {
                            currentCharacter = input.next();
                            break;
                        }
                    }
                    // Return symbol after comment
                    return nextSymbol();
                }

                return new Symbol<>(validKeyword);
            } else {
                // Throw an exception for unknown keywords
                throw new ScannerException.UnknownSequenceException("Unknown character sequence encountered: " +
                        substringBuilder + " at " + input.getPosition());
            }

        }

        // Throw an exception if we've encountered an unknown sequence
        throw new ScannerException.UnknownSequenceException("Unknown character sequence encountered: " +
                currentCharacter + " at " + input.getPosition());
    }

    /**
     * Skips irrelevant characters on the input.
     *
     * @throws IOException on input exception
     */
    private void skipIrrelevant() throws IOException {
        while (isWhitespace(currentCharacter)) {
            currentCharacter = input.next();
        }
    }

    /**
     * Determines if the character is considered a whitespace character.
     *
     * @param c The character to check
     * @return True if the character is considered a whitespace
     */
    private boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    /**
     * Determines if a given character is a digit
     *
     * @param c The character to check
     * @return true if the character contains a digit from 0..9
     */
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Determines if a given character is a letter
     *
     * @param c The character to check
     * @return true if the character contains a letter from a..z or A..Z
     */
    private boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z');
    }

    /**
     * Passes the input's code position
     *
     * @return The scanners position within the source file
     */
    public CodePosition getPosition() {
        return input.getPosition();
    }

}

/**
 * Basic Symbols which the Scanner may return
 *
 * @param <T> Symbol content type
 * @author TillFleisch
 */
class Symbol<T> {

    /**
     * The symbols content
     */
    T content;

    /**
     * Create a symbol with a given content
     *
     * @param content The symbols content
     */
    public Symbol(T content) {
        this.content = content;
    }

    @Override
    public String toString() {
        if (content != null)
            return getType() + ": " + content.toString();
        else
            return "Type: " + getType();
    }

    /**
     * Determines the Symbols type
     *
     * @return This symbol's type
     */
    public Type getType() {
        if (this.content instanceof Keyword)
            return Type.KEYWORD;
        if (this.content instanceof String)
            return Type.IDENTIFIER;
        if (this.content instanceof Integer)
            return Type.NUMBER;
        return Type.UNKNOWN;
    }

    /**
     * Check if two Symbols are equivalent
     *
     * @param other Object
     * @return True if other is an equivalent symbol to this
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof Symbol<?>) {
            if (((Symbol<?>) other).getType().equals(getType())) {
                return ((Symbol<?>) other).content.equals(content);
            } else {
                return false;
            }
        }
        return false;
    }

    /**
     * Enum containing different types of symbols
     *
     * @author TillFleisch
     */
    enum Type {
        KEYWORD,
        IDENTIFIER,
        NUMBER,
        UNKNOWN
    }
}

