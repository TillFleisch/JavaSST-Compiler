package dev.fleisch.JSSTCompiler;

/**
 * ENUM class containing keywords from JavaSST
 *
 * @author TillFleisch
 */
public enum Keyword {
    CLASS("class"),
    PUBLIC("public"),
    VOID("void"),
    FINAL("final"),

    INT("int"),
    IF("if"),
    ELSE("else"),
    WHILE("while"),
    RETURN("return"),

    COMMA(","),
    SEMICOLON(";"),

    ROUND_OPENING_BRACKET("("),
    ROUND_CLOSING_BRACKET(")"),
    CURLY_OPENING_BRACKET("{"),
    CURLY_CLOSING_BRACKET("}"),

    ASSIGN("="),
    EQUAL("=="),
    GREATER(">"),
    GREATER_EQUAL(">="),
    LESS("<"),
    LESS_EQUAL("<="),

    PLUS("+"),
    MINUS("-"),
    MULTIPLY("*"),
    DIVIDE("/"),

    START_COMMENT("//"),
    START_COMMENT_MULTI_LINE("/*");
    
    /**
     * The string which this ENUM represents.
     */
    final String keyword;

    /**
     * Constructor for keywords with their string counterpart.
     *
     * @param keyword
     */
    Keyword(String keyword) {
        this.keyword = keyword;
    }

    /**
     * Determines the keyword a ENUM represents
     *
     * @return The string-sequence a keyword ENUM represents
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * Determines if a keywords represents a single character keyword
     *
     * @return True if the ENUM represents a single character keyword
     */
    public boolean isSingleCharacter() {
        return this.keyword.length() == 1;
    }
}
