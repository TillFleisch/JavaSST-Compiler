package dev.fleisch.JSSTCompiler;

/**
 * Basic class describing positions within the source code.
 *
 * @author TillFleisch
 */
public class CodePosition {

    /**
     * Line number within the Source file
     */
    private int line;

    /**
     * Column number within the Source file
     */
    private int column;

    /**
     * Code Position constructor for line and column
     *
     * @param line   The line number within the source file
     * @param column The column number within the source file
     */
    public CodePosition(int line, int column) {
        this.line = line;
        this.column = column;
    }

    /**
     * Copy Constructor
     *
     * @param codePosition CodePosition to copy
     */
    public CodePosition(CodePosition codePosition) {
        this.column = codePosition.getColumn();
        this.line = codePosition.getLine();
    }

    /**
     * @return The column number within the source file
     */
    public int getColumn() {
        return column;
    }

    /**
     * @return The line number within the source file
     */
    public int getLine() {
        return line;
    }

    /**
     * @param column New column to be set
     */
    public void setColumn(int column) {
        this.column = column;
    }

    /**
     * @param line New line to be set
     */
    public void setLine(int line) {
        this.line = line;
    }

    
    @Override
    public String toString() {
        return "Line: " + line + " Column: " + column;
    }

}
