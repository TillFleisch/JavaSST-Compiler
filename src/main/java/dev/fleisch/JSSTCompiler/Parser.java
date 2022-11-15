package dev.fleisch.JSSTCompiler;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Logger;

/**
 * Implements a basic parser which checks if a given program follow the given grammar.
 *
 * @author TillFleisch
 */
public class Parser {

    /**
     * The scanner used in this parser
     */
    Scanner scanner;

    /**
     * The current symbol read by the parser
     */
    Symbol<?> currentSymbol;

    /**
     * Create a parse for a given scanner
     *
     * @param scanner The scanner to use within this parser
     */
    public Parser(Scanner scanner) throws ScannerException, IOException, ParserException {
        this.scanner = scanner;
        try {
            next();
        } catch (EOFException e) {
            throw new ParserException("Unexpected EOF");
        }
    }

    /**
     * Parses a program and checks if it follows the given grammar.
     * <p>
     **/
    public void parse() throws ParserException, ScannerException, IOException {
        try {
            SymbolTable symbolTable = new SymbolTable();
            Objekt.Clasz clasz = parseClass();
            symbolTable.add(clasz);
        } catch (EOFException e) {
            throw new ParserException("Unexpected EOF");
        }
    }

    /**
     * Read the next symbol from the Scanner to cache.
     */
    private void next() throws ScannerException, IOException {
        currentSymbol = scanner.nextSymbol();
    }

    /**
     * Checks if the current Symbol is equal to Keyword <b>and read the next symbol</b>
     *
     * @param keyword The keyword used for comparison
     * @throws ParserException.ExpectedButFoundException if the symbol does not match the given keyword
     */
    private void assertKeyword(Keyword keyword) throws ParserException.ExpectedButFoundException, ScannerException,
            IOException {
        if (!currentSymbol.equals(new Symbol<>(keyword)))
            throw new ParserException.ExpectedButFoundException(keyword, currentSymbol, scanner);
        next();
    }

    /**
     * Parse arbitrary identifiers
     *
     * @return The parsed identifier
     */
    private String parseIdentifier() throws ParserException.ExpectedButFoundException, ScannerException, IOException {
        if (!currentSymbol.getType().equals(Symbol.Type.IDENTIFIER))
            throw new ParserException.ExpectedButFoundException(Symbol.Type.IDENTIFIER, currentSymbol, scanner);

        String identifier = (String) currentSymbol.content;
        next();
        return identifier;
    }

    /**
     * Parse arbitrary numbers
     */
    private void parseNumber() throws ParserException.ExpectedButFoundException, ScannerException, IOException {
        if (!currentSymbol.getType().equals(Symbol.Type.NUMBER))
            throw new ParserException.ExpectedButFoundException(Symbol.Type.NUMBER, currentSymbol, scanner);
        next();
    }

    /**
     * Parse non-terminal <i>factor</i>
     * <p>
     * identifier | number | "(" expression ")" | intern_procedure_call
     * </p>
     */
    private void parseFactor() throws ScannerException, IOException, ParserException {


        if (currentSymbol.getType() == Symbol.Type.NUMBER) {
            parseNumber();
            return;
        }

        if (currentSymbol.getType() == Symbol.Type.IDENTIFIER) {
            parseIdentifier();
            // differentiate stand-alone identifiers and procedure calls
            if (currentSymbol.equals(new Symbol<>(Keyword.ROUND_OPENING_BRACKET))) {
                parseActualParameters();
            }
            return;
        }

        if (currentSymbol.equals(new Symbol<>(Keyword.ROUND_OPENING_BRACKET))) {
            assertKeyword(Keyword.ROUND_OPENING_BRACKET);

            // Parse expression
            parseExpression();

            assertKeyword(Keyword.ROUND_CLOSING_BRACKET);
            return;
        }

        throw new ParserException("invalid factor", scanner);
    }

    /**
     * Parse non-terminal <i>actual_parameters
     * </i>
     * <p>
     * intern_procedure_call ";"
     * ->
     * identifier actualParameters ";"
     * </p>
     * <p>
     * Internal procedure calls are parsed directly
     */
    private void parseProcedureCall() throws ScannerException, ParserException, IOException {
        parseIdentifier();
        parseActualParameters();
        assertKeyword(Keyword.SEMICOLON);
    }

    /**
     * Parse non-terminal <i>actual_parameters
     * </i>
     * <p>
     * "(" [ expression {, expression}] ")"
     * </p>
     */
    private void parseActualParameters() throws ScannerException, IOException, ParserException {

        assertKeyword(Keyword.ROUND_OPENING_BRACKET);

        //Check for optional parameters
        if (!currentSymbol.equals(new Symbol<>(Keyword.ROUND_CLOSING_BRACKET))) {
            // must be expression potentially followed by more expressions
            try {
                parseExpression();

                // parse further comma separated paramters
                while (currentSymbol.equals(new Symbol<>(Keyword.COMMA))) {
                    assertKeyword(Keyword.COMMA);
                    parseExpression();
                }
            } catch (ParserException e) {
                throw new ParserException("Bad Procedure call parameters", scanner);
            }
        }

        assertKeyword(Keyword.ROUND_CLOSING_BRACKET);
    }

    /**
     * Parse non-terminal <i>expression</i>
     * <p>
     * simple_expression [("==" | "<" | "<=" | ">" | ">=") simple_expression]
     * </p>
     */
    private void parseExpression() throws ScannerException, IOException, ParserException {

        parseSimpleExpression();

        // Parse optional second simple expression
        if (currentSymbol.equals(new Symbol<>(Keyword.EQUAL)) ||
                currentSymbol.equals(new Symbol<>(Keyword.LESS)) ||
                currentSymbol.equals(new Symbol<>(Keyword.LESS_EQUAL)) ||
                currentSymbol.equals(new Symbol<>(Keyword.GREATER)) ||
                currentSymbol.equals(new Symbol<>(Keyword.GREATER_EQUAL))) {
            next();
            parseSimpleExpression();
        }
    }

    /**
     * Parse non-terminal <i>simple_expression</i>
     * <p>
     * term {("+"|"-") term}
     * </p>
     */
    private void parseSimpleExpression() throws ScannerException, IOException, ParserException {

        parseTerm();

        while (currentSymbol.equals(new Symbol<>(Keyword.PLUS)) ||
                currentSymbol.equals(new Symbol<>(Keyword.MINUS))) {
            next();
            parseTerm();
        }
    }

    /**
     * Parse non-terminal <i>term</i>
     * <p>
     * factor {("*"|"/") factor}
     * </p>
     */
    private void parseTerm() throws ScannerException, IOException, ParserException {

        parseFactor();

        while (currentSymbol.equals(new Symbol<>(Keyword.MULTIPLY)) ||
                currentSymbol.equals(new Symbol<>(Keyword.DIVIDE))) {
            next();
            parseFactor();
        }
    }

    /**
     * Parse non-terminal <i>statement</i>
     * <p>
     * assignment | procedure_call | if_statement | while_statement | return_statement
     * </p>
     * <p>
     * This method includes assignment and procedure call parsing, since they share their starting symbols
     */
    private void parseStatement() throws ScannerException, IOException, ParserException {
        // Check for if statement
        if (currentSymbol.equals(new Symbol<>(Keyword.IF))) {
            parseIf();
            return;
        }

        // Check for while statement
        if (currentSymbol.equals(new Symbol<>(Keyword.WHILE))) {
            parseWhile();
            return;
        }

        // Check for return statement
        if (currentSymbol.equals(new Symbol<>(Keyword.RETURN))) {
            parseReturn();
            return;
        }

        // Check for identifier (assignment and procedure call)
        if (currentSymbol.getType() == Symbol.Type.IDENTIFIER) {
            parseIdentifier();

            // Check for assignment
            // identifier "=" expression ";"
            if (currentSymbol.equals(new Symbol<>(Keyword.ASSIGN))) {
                assertKeyword(Keyword.ASSIGN);
                parseExpression();
                assertKeyword(Keyword.SEMICOLON);
                return;
            }

            // Check for procedure call
            // identifier actual_parameters ";"
            if (currentSymbol.equals(new Symbol<>(Keyword.ROUND_OPENING_BRACKET))) {
                parseActualParameters();
                assertKeyword(Keyword.SEMICOLON);
                return;
            }

            throw new ParserException("Expected assignment or procedure call!", scanner);
        }
        throw new ParserException("Expected statement!", scanner);
    }

    /**
     * Parse non-terminal <i>return_statement</i>
     * <p>
     * "return" [simple_expression] ";"
     * </p>
     */
    private void parseReturn() throws ScannerException, IOException, ParserException {
        assertKeyword(Keyword.RETURN);

        // Check for optional parameters
        if (!currentSymbol.equals(new Symbol<>(Keyword.SEMICOLON))) {
            parseSimpleExpression();
            assertKeyword(Keyword.SEMICOLON);
            return;
        }
        assertKeyword(Keyword.SEMICOLON);
    }

    /**
     * Parse non-terminal <i>while_statement</i>
     * <p>
     * "while" "(" expression ")" "{" statement_sequence "}"
     * </p>
     */
    private void parseWhile() throws ScannerException, IOException, ParserException {
        assertKeyword(Keyword.WHILE);
        assertKeyword(Keyword.ROUND_OPENING_BRACKET);
        try {
            parseExpression();
            assertKeyword(Keyword.ROUND_CLOSING_BRACKET);
        } catch (ParserException e) {
            throw new ParserException("Bad while-condition", scanner);
        }
        assertKeyword(Keyword.CURLY_OPENING_BRACKET);
        parseStatementSequence();
        assertKeyword(Keyword.CURLY_CLOSING_BRACKET);
    }

    /**
     * Parse non-terminal <i>if_statement</i>
     * <p>
     * "if" "(" expression ")" "{" statement_sequence "}" "else" "{" statement_sequence "}"
     * </p>
     */
    private void parseIf() throws ScannerException, IOException, ParserException {
        assertKeyword(Keyword.IF);
        assertKeyword(Keyword.ROUND_OPENING_BRACKET);
        try {
            parseExpression();
            assertKeyword(Keyword.ROUND_CLOSING_BRACKET);
        } catch (ParserException e) {
            throw new ParserException("Bad if-condition", scanner);
        }
        assertKeyword(Keyword.CURLY_OPENING_BRACKET);
        parseStatementSequence();
        assertKeyword(Keyword.CURLY_CLOSING_BRACKET);
        assertKeyword(Keyword.ELSE);
        assertKeyword(Keyword.CURLY_OPENING_BRACKET);
        parseStatementSequence();
        assertKeyword(Keyword.CURLY_CLOSING_BRACKET);
    }

    /**
     * Parse non-terminal <i>statement</i>
     * <p>
     * statement {statement}
     * </p>
     */
    private void parseStatementSequence() throws ScannerException, IOException, ParserException {
        parseStatement();

        // Check for possible statement
        // (assign, procedure-call) identifier | if | while | return
        while (isEligibleForStatement())
            parseStatement();
    }

    /**
     * Parse non-terminal <i>method_body</i>
     * <p>
     * "{" {local_declarations} statement_sequence "}"
     * </p>
     *
     * @return The SymbolTable used within this body
     */
    private SymbolTable parseMethodBody(SymbolTable enclosingTable) throws ScannerException, IOException, ParserException {
        SymbolTable symbolTable = new SymbolTable(enclosingTable);
        assertKeyword(Keyword.CURLY_OPENING_BRACKET);

        // Check for local declarations -> possible types -> not possible statement
        while (!isEligibleForStatement()) {
            symbolTable.add(parseLocalDeclaration());
        }

        parseStatementSequence();

        assertKeyword(Keyword.CURLY_CLOSING_BRACKET);
        return symbolTable;
    }

    /**
     * Parse non-terminal <i>local_declaration</i>
     * <p>
     * type identifier ;
     * </p>
     *
     * @return The parsed local declaration
     */
    private Objekt.Parameter parseLocalDeclaration() throws ScannerException, IOException,
            ParserException {
        Type type = parseType();
        String identifier = parseIdentifier();
        assertKeyword(Keyword.SEMICOLON);
        return new Objekt.Parameter(identifier, type);
    }

    /**
     * Parse non-terminal <i>type</i>
     * <p>
     * "int"
     * </p>
     *
     * @return The parsed Type
     */
    private Type parseType() throws ScannerException, IOException, ParserException {
        if (currentSymbol.equals(new Symbol<>(Keyword.INT))) {
            next();
            return Type.INT;
        }
        throw new ParserException("Expected type declaration", scanner);
    }


    /**
     * Parse non-terminal <i>method_declaration</i>
     * <p>
     * method_head method_body
     * </p>
     *
     * @return The parsed method declaration
     */
    private Objekt.Procedure parseMethodDeclaration(SymbolTable enclosingTable) throws ScannerException, IOException, ParserException {

        Objekt.Procedure procedure = parseMethodHead();
        procedure.setSymbolTable(parseMethodBody(enclosingTable));

        return procedure;
    }

    /**
     * Parse non-terminal <i>method_head</i>
     * <p>
     * "public" method_type identifier formal_parameters
     * </p>
     *
     * @return The Procedure described by this method head without a body
     */

    private Objekt.Procedure parseMethodHead() throws ScannerException, IOException, ParserException {
        assertKeyword(Keyword.PUBLIC);
        Type type = parseMethodType();
        String identifier = parseIdentifier();
        LinkedList<Objekt.Parameter> parameters = parseFormalParameters();

        return new Objekt.Procedure(identifier, parameters, type);
    }

    /**
     * Parse non-terminal <i>method_type</i>
     * <p>
     * "void" | type
     * </p>
     *
     * @return The methods type
     */
    private Type parseMethodType() throws ScannerException, IOException,
            ParserException {
        if (currentSymbol.equals(new Symbol<>(Keyword.VOID))) {
            assertKeyword(Keyword.VOID);
            return Type.VOID;
        }
        return parseType();
    }

    /**
     * Parse non-terminal <i>formal_parameter</i>
     * <p>
     * "(" [ fp_section {"," fp_section}] ")"
     * </p>
     *
     * @return List of formal parameters
     */
    private LinkedList<Objekt.Parameter> parseFormalParameters() throws ScannerException, IOException,
            ParserException {
        assertKeyword(Keyword.ROUND_OPENING_BRACKET);

        LinkedList<Objekt.Parameter> parameters = new LinkedList<>();
        // Check for formal parameter section
        if (!currentSymbol.equals(new Symbol<>(Keyword.ROUND_CLOSING_BRACKET))) {
            parameters.add(parseFormalParameterSection());

            // Parse further comma separated sections
            while (currentSymbol.equals(new Symbol<>(Keyword.COMMA))) {
                assertKeyword(Keyword.COMMA);
                parameters.add(parseFormalParameterSection());
            }
        }

        assertKeyword(Keyword.ROUND_CLOSING_BRACKET);

        return parameters;
    }

    /**
     * Parse non-terminal <i>formal_parameter_section</i>
     * <p>
     * type identifier
     * </p>
     *
     * @return The single formal parameter which was parsed
     */
    private Objekt.Parameter parseFormalParameterSection() throws ScannerException, IOException,
            ParserException {
        Type type = parseType();
        String identifier = parseIdentifier();
        return new Objekt.Parameter(identifier, type);
    }

    /**
     * Parse non-terminal <i>declarations</i>
     * <p>
     * { "final" type identifier "=" expression ";" }
     * { type identifier ";"}
     * { method_declaration }
     * </p>
     *
     * @return Symbol table containing all parsed CONSTANTS, PARAMETERS and METHODS
     */
    private SymbolTable parseDeclarations() throws ScannerException, IOException, ParserException {
        SymbolTable symbolTable = new SymbolTable();

        // check for { "final" type identifier "=" expression ";" }
        while (currentSymbol.equals(new Symbol<>(Keyword.FINAL))) {
            assertKeyword(Keyword.FINAL);

            Type type = parseType();
            String identifier = parseIdentifier();

            assertKeyword(Keyword.ASSIGN);
            parseExpression(); // TODO: determine expression value using AST

            symbolTable.push(new Objekt.Constant(identifier, type, 0));
            assertKeyword(Keyword.SEMICOLON);
        }

        // check for { type identifier ";" }
        // This checks for all possible types, not elegant
        while (currentSymbol.equals(new Symbol<>(Keyword.INT))) {
            Type type = parseType();
            String identifier = parseIdentifier();

            symbolTable.push(new Objekt.Parameter(identifier, type));
            assertKeyword(Keyword.SEMICOLON);
        }

        // { method_declaration }
        while (currentSymbol.equals(new Symbol<>(Keyword.PUBLIC))) {
            symbolTable.push(parseMethodDeclaration(symbolTable));
        }

        return symbolTable;
    }

    /**
     * Parse non-terminal <i>classbody</i>
     * <p>
     * "{" declarations "}"
     * </p>
     *
     * @return The symbolTable contained within this class body
     */
    private SymbolTable parseClassBody() throws ScannerException, IOException, ParserException {
        assertKeyword(Keyword.CURLY_OPENING_BRACKET);

        SymbolTable symbolTable = parseDeclarations();

        try {
            if (!currentSymbol.equals(new Symbol<>(Keyword.CURLY_CLOSING_BRACKET)))
                throw new ParserException.ExpectedButFoundException(Keyword.CURLY_CLOSING_BRACKET, currentSymbol, scanner);
            next();
        } catch (EOFException ignored) {
            // EOF is fine, since nothing is expected after curly closing bracket
        }

        return symbolTable;
    }

    /**
     * Parse non-terminal <i>class</i>
     * <p>
     * "class" identifier classbody
     * </p>
     */
    private Objekt.Clasz parseClass() throws ScannerException, IOException, ParserException {
        assertKeyword(Keyword.CLASS);
        String identifier = parseIdentifier();
        SymbolTable symbolTable = parseClassBody();

        return new Objekt.Clasz(identifier,symbolTable);
    }

    /**
     * Checks if the current symbol is eligible to become a statement
     *
     * @return True if the current symbol ist the start of either and Identifier | IF | WHILE | RETURN
     */
    private boolean isEligibleForStatement() {
        return currentSymbol.getType() == (Symbol.Type.IDENTIFIER) ||
                currentSymbol.equals(new Symbol<>(Keyword.IF)) ||
                currentSymbol.equals(new Symbol<>(Keyword.WHILE)) ||
                currentSymbol.equals(new Symbol<>(Keyword.RETURN));
    }


    /**
     * Class for simple parser Tests
     *
     * @author TillFleisch
     */
    public static class ParserTest {

        /**
         * Run simple parser tests (no negatives).
         */
        public static void runTests() {
            try {
                // since inner functions expect more content a placeholder at the end is required (SEMICOLON)
                new Parser(temporaryScanner("ident ;")).parseIdentifier();

                new Parser(temporaryScanner("69 ;")).parseNumber();

                new Parser(temporaryScanner("ident  ;")).parseFactor(); // factor
                new Parser(temporaryScanner("69  ;")).parseFactor(); // factor(number)
                new Parser(temporaryScanner("meth() ;")).parseFactor(); // factor(function)
                new Parser(temporaryScanner("(5)  ;")).parseFactor(); // factor(expression)

                new Parser(temporaryScanner("4+20+6+9 ;")).parseExpression();
                new Parser(temporaryScanner("6==9 ;")).parseExpression();
                new Parser(temporaryScanner("6*9 <= 6+9*420+3 ;")).parseExpression();

                new Parser(temporaryScanner("meth(); ;")).parseProcedureCall();
                new Parser(temporaryScanner("meth(6); ;")).parseProcedureCall();
                new Parser(temporaryScanner("meth(3+1,4); ;")).parseProcedureCall();
                new Parser(temporaryScanner("meth(5+5,6*6*(6+6*5),meth(3)); ;")).parseProcedureCall();

                new Parser(temporaryScanner("i = 0; ;")).parseStatement();
                new Parser(temporaryScanner("""
                        i = 0;
                        i = meth();
                        meth(3);
                        return;
                        return 6;
                        while(1==1){
                        i=0;
                        }
                        if(1==1){
                        i=0;
                        }else{
                        i=1;
                        } ;""")).parseStatementSequence();

                new Parser(temporaryScanner("{i=0;} ;")).parseMethodBody(new SymbolTable());
                new Parser(temporaryScanner("{int i; i=0;} ;")).parseMethodBody(new SymbolTable());
                new Parser(temporaryScanner("{int i; int j; i=0; j=meth();} ;")).parseMethodBody(new SymbolTable());

                new Parser(temporaryScanner("() ;")).parseFormalParameters();
                new Parser(temporaryScanner("(int i) ;")).parseFormalParameters();
                new Parser(temporaryScanner("(int i, int i, int i) ;")).parseFormalParameters();

                new Parser(temporaryScanner("public int meth() ;")).parseMethodHead();
                new Parser(temporaryScanner("public void meth(int i) ;")).parseMethodHead();

                new Parser(temporaryScanner("public int meth(){int i; int j; i=0; j=meth();} ;"))
                        .parseMethodDeclaration(new SymbolTable());
                new Parser(temporaryScanner("public void meth(){i=0;} ;")).parseMethodDeclaration(new SymbolTable());

                new Parser(temporaryScanner("final int i = 0; ;")).parseDeclarations();
                new Parser(temporaryScanner("final int i = 0; final int i = 0; ;")).parseDeclarations();
                new Parser(temporaryScanner("int i; int j; ;")).parseDeclarations();
                new Parser(temporaryScanner("int i; int j; int j; ;")).parseDeclarations();
                new Parser(temporaryScanner("final int i = 0; int j; ;")).parseDeclarations();
                new Parser(temporaryScanner("final int i = 0; int j; public void meth(){i=0;};")).parseDeclarations();

                new Parser(temporaryScanner("{}")).parseClassBody();
                new Parser(temporaryScanner("{ final int i = 0; }")).parseClassBody();

                new Parser(temporaryScanner("class kek{}"));
                new Parser(temporaryScanner("class kek{int i;}"));

            } catch (Exception e) {
                Logger.getGlobal().info("\u001B[31mParser tests failed!\u001B[0m");
                throw new RuntimeException(e);
            }
            Logger.getGlobal().info("\u001B[32mAll parser tests passed!\u001B[0m");
        }

        /**
         * Creates a temporary Scanner based on a string
         *
         * @param fileContent jsst source code
         * @return A scanner for the given string
         */
        private static Scanner temporaryScanner(String fileContent) {
            return new Scanner(new Input(new ByteArrayInputStream(fileContent.getBytes())));
        }
    }
}
