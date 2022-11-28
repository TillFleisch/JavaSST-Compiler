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
     * Parse arbitrary numbers.
     * Since JavaSST only uses integers we return integers.
     *
     * @return Number parsed
     */
    private int parseNumber() throws ParserException.ExpectedButFoundException, ScannerException, IOException {
        if (!currentSymbol.getType().equals(Symbol.Type.NUMBER))
            throw new ParserException.ExpectedButFoundException(Symbol.Type.NUMBER, currentSymbol, scanner);
        int number = (int) currentSymbol.content;
        next();
        return number;
    }

    /**
     * Parse non-terminal <i>factor</i>
     * <p>
     * identifier | number | "(" expression ")" | intern_procedure_call
     * </p>
     *
     * @return Syntax tree describing the parsed factor
     */
    private Node parseFactor() throws ScannerException, IOException, ParserException {

        if (currentSymbol.getType() == Symbol.Type.NUMBER) {
            return new Node.ConstantNode(parseNumber());
        }

        if (currentSymbol.getType() == Symbol.Type.IDENTIFIER) {
            String identifier = parseIdentifier();
            // differentiate stand-alone identifiers and procedure calls
            if (currentSymbol.equals(new Symbol<>(Keyword.ROUND_OPENING_BRACKET))) {
                Node.StatementSequenceNode parameters = parseActualParameters();
                return new Node.ProcedureCallNode(identifier, parameters);
            }
            return new Node.IdentifierNode(identifier);
        }

        if (currentSymbol.equals(new Symbol<>(Keyword.ROUND_OPENING_BRACKET))) {
            assertKeyword(Keyword.ROUND_OPENING_BRACKET);

            // Parse expression
            Node subExpression = parseExpression();

            assertKeyword(Keyword.ROUND_CLOSING_BRACKET);
            return subExpression;
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
     *
     * @return Procedure call Node with identifier and parameters
     */
    private Node.ProcedureCallNode parseProcedureCall() throws ScannerException, ParserException, IOException {
        String identifier = parseIdentifier();
        Node.StatementSequenceNode parameters = parseActualParameters();
        assertKeyword(Keyword.SEMICOLON);

        return new Node.ProcedureCallNode(identifier, parameters);
    }

    /**
     * Parse non-terminal <i>actual_parameters
     * </i>
     * <p>
     * "(" [ expression {, expression}] ")"
     * </p>
     *
     * @return Syntax tree describing the parsed parameters for method invocations
     */
    private Node.StatementSequenceNode parseActualParameters() throws ScannerException, IOException, ParserException {
        LinkedList<Node> parameters = new LinkedList<>();

        assertKeyword(Keyword.ROUND_OPENING_BRACKET);

        //Check for optional parameters
        if (!currentSymbol.equals(new Symbol<>(Keyword.ROUND_CLOSING_BRACKET))) {
            // must be expression potentially followed by more expressions
            try {
                parameters.add(parseExpression());

                // parse further comma separated parameters
                while (currentSymbol.equals(new Symbol<>(Keyword.COMMA))) {
                    assertKeyword(Keyword.COMMA);
                    parameters.add(parseExpression());
                }
            } catch (ParserException e) {
                throw new ParserException("Bad Procedure call parameters", scanner);
            }
        }

        assertKeyword(Keyword.ROUND_CLOSING_BRACKET);

        // Return the list of parameters
        return new Node.StatementSequenceNode(parameters);
    }

    /**
     * Parse non-terminal <i>expression</i>
     * <p>
     * simple_expression [("==" | "<" | "<=" | ">" | ">=") simple_expression]
     * </p>
     *
     * @return Syntax tree describing the parsed expression
     */
    private Node parseExpression() throws ScannerException, IOException, ParserException {

        Node firstExpression = parseSimpleExpression();

        // Parse optional second simple expression
        if (currentSymbol.equals(new Symbol<>(Keyword.EQUAL)) ||
                currentSymbol.equals(new Symbol<>(Keyword.LESS)) ||
                currentSymbol.equals(new Symbol<>(Keyword.LESS_EQUAL)) ||
                currentSymbol.equals(new Symbol<>(Keyword.GREATER)) ||
                currentSymbol.equals(new Symbol<>(Keyword.GREATER_EQUAL))) {
            Operation.Binary binaryOperation;
            try {
                // Determine the binary Operation
                binaryOperation = Operation.Binary.toBinaryOperation((Keyword) currentSymbol.content);
            } catch (ParserException e) {
                throw new ParserException(e.getMessage(), scanner);
            }
            next();

            Node secondExpression = parseSimpleExpression();

            // Return the simple expression as a binary operation with LHS and RHS subexpressions.
            return new Node.BinaryOperationNode(firstExpression, secondExpression, binaryOperation);
        }

        return firstExpression;
    }

    /**
     * Parse non-terminal <i>simple_expression</i>
     * <p>
     * term {("+"|"-") term}
     * </p>
     *
     * @return Syntax tree describing the parsed simple expression
     */
    private Node parseSimpleExpression() throws ScannerException, IOException, ParserException {

        Node firstTerm = parseTerm();
        if (currentSymbol.equals(new Symbol<>(Keyword.PLUS)) ||
                currentSymbol.equals(new Symbol<>(Keyword.MINUS))) {
            Operation.Binary binaryOperation;
            try {
                // Determine the binary Operation
                binaryOperation = Operation.Binary.toBinaryOperation((Keyword) currentSymbol.content);
            } catch (ParserException e) {
                throw new ParserException(e.getMessage(), scanner);
            }
            next();

            // Resolve subsequent operand recursively
            Node secondTerm = parseSimpleExpression();

            return new Node.BinaryOperationNode(firstTerm, secondTerm, binaryOperation);
        }
        return firstTerm;
    }

    /**
     * Parse non-terminal <i>term</i>
     * <p>
     * factor {("*"|"/") factor}
     * </p>
     *
     * @return Syntax tree describing the parsed term
     */
    private Node parseTerm() throws ScannerException, IOException, ParserException {

        Node firstFactor = parseFactor();

        if (currentSymbol.equals(new Symbol<>(Keyword.MULTIPLY)) ||
                currentSymbol.equals(new Symbol<>(Keyword.DIVIDE))) {
            Operation.Binary binaryOperation;
            try {
                // Determine the binary Operation
                binaryOperation =
                        Operation.Binary.toBinaryOperation((Keyword) currentSymbol.content);
            } catch (ParserException e) {
                throw new ParserException(e.getMessage(), scanner);
            }
            next();
            // Determine subsequent terms recursively
            Node secondFactor = parseTerm();
            return new Node.BinaryOperationNode(firstFactor, secondFactor, binaryOperation);
        }
        return firstFactor;
    }

    /**
     * Parse non-terminal <i>statement</i>
     * <p>
     * assignment | procedure_call | if_statement | while_statement | return_statement
     * </p>
     * <p>
     * This method includes assignment and procedure call parsing, since they share their starting symbols
     *
     * @return Syntax tree describing the parsed Statement
     */
    private Node parseStatement() throws ScannerException, IOException, ParserException {
        // Check for if statement
        if (currentSymbol.equals(new Symbol<>(Keyword.IF))) {
            return parseIf();
        }

        // Check for while statement
        if (currentSymbol.equals(new Symbol<>(Keyword.WHILE))) {
            return parseWhile();
        }

        // Check for return statement
        if (currentSymbol.equals(new Symbol<>(Keyword.RETURN))) {
            return parseReturn();
        }

        // Check for identifier (assignment and procedure call)
        if (currentSymbol.getType() == Symbol.Type.IDENTIFIER) {
            String identifier = parseIdentifier();

            // Check for assignment
            // identifier "=" expression ";"
            if (currentSymbol.equals(new Symbol<>(Keyword.ASSIGN))) {
                assertKeyword(Keyword.ASSIGN);
                Node assignment = parseExpression();
                assertKeyword(Keyword.SEMICOLON);
                return new Node.BinaryOperationNode(new Node.IdentifierNode(identifier), assignment, Operation.Binary.ASSIGNMENT);
            }

            // Check for procedure call
            // identifier actual_parameters ";"
            if (currentSymbol.equals(new Symbol<>(Keyword.ROUND_OPENING_BRACKET))) {
                Node.StatementSequenceNode parameters = parseActualParameters();
                assertKeyword(Keyword.SEMICOLON);
                return new Node.ProcedureCallNode(identifier, parameters);
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
     *
     * @return Syntax tree describing the parsed return statement
     */
    private Node parseReturn() throws ScannerException, IOException, ParserException {
        assertKeyword(Keyword.RETURN);

        // Check for optional parameters
        if (!currentSymbol.equals(new Symbol<>(Keyword.SEMICOLON))) {
            Node expression = parseSimpleExpression();
            assertKeyword(Keyword.SEMICOLON);
            return new Node.UnaryOperationNode(expression, Operation.Unary.RETURN);
        }
        assertKeyword(Keyword.SEMICOLON);
        return new Node.UnaryOperationNode(null, Operation.Unary.RETURN);
    }

    /**
     * Parse non-terminal <i>while_statement</i>
     * <p>
     * "while" "(" expression ")" "{" statement_sequence "}"
     * </p>
     *
     * @return Syntax tree describing the parsed if statement
     */
    private Node.WhileNode parseWhile() throws ScannerException, IOException, ParserException {
        assertKeyword(Keyword.WHILE);
        assertKeyword(Keyword.ROUND_OPENING_BRACKET);
        Node condition;
        try {
            condition = parseExpression();
            assertKeyword(Keyword.ROUND_CLOSING_BRACKET);
        } catch (ParserException e) {
            throw new ParserException("Bad while-condition", scanner);
        }
        assertKeyword(Keyword.CURLY_OPENING_BRACKET);
        Node.StatementSequenceNode node = parseStatementSequence();
        assertKeyword(Keyword.CURLY_CLOSING_BRACKET);

        return new Node.StatementSequenceNode.WhileNode(condition, node);
    }

    /**
     * Parse non-terminal <i>if_statement</i>
     * <p>
     * "if" "(" expression ")" "{" statement_sequence "}" "else" "{" statement_sequence "}"
     * </p>
     *
     * @return Syntax tree describing the parsed if statement
     */
    private Node.IfNode parseIf() throws ScannerException, IOException, ParserException {
        assertKeyword(Keyword.IF);
        assertKeyword(Keyword.ROUND_OPENING_BRACKET);
        Node condition;
        try {
            condition = parseExpression();
            assertKeyword(Keyword.ROUND_CLOSING_BRACKET);
        } catch (ParserException e) {
            throw new ParserException("Bad if-condition", scanner);
        }
        assertKeyword(Keyword.CURLY_OPENING_BRACKET);
        Node.StatementSequenceNode ifStatements = parseStatementSequence();
        assertKeyword(Keyword.CURLY_CLOSING_BRACKET);
        assertKeyword(Keyword.ELSE);
        assertKeyword(Keyword.CURLY_OPENING_BRACKET);
        Node.StatementSequenceNode elseStatements = parseStatementSequence();
        assertKeyword(Keyword.CURLY_CLOSING_BRACKET);

        return new Node.IfNode(condition, ifStatements, elseStatements);
    }

    /**
     * Parse non-terminal <i>statement</i>
     * <p>
     * statement {statement}
     * </p>
     *
     * @return Syntax tree describing the parsed if statement
     */
    private Node.StatementSequenceNode parseStatementSequence() throws ScannerException, IOException, ParserException {
        LinkedList<Node> statementNodes = new LinkedList<>();

        statementNodes.add(parseStatement());

        // Check for possible statement
        // (assign, procedure-call) identifier | if | while | return
        while (isEligibleForStatement())
            statementNodes.add(parseStatement());

        return new Node.StatementSequenceNode(statementNodes);
    }

    /**
     * Parse non-terminal <i>method_body</i>
     * <p>
     * "{" {local_declarations} statement_sequence "}"
     * </p>
     *
     * @return The SymbolTable used within this body
     */
    private Pair<SymbolTable, Node> parseMethodBody(SymbolTable enclosingTable) throws ScannerException, IOException, ParserException {
        SymbolTable symbolTable = new SymbolTable(enclosingTable);
        assertKeyword(Keyword.CURLY_OPENING_BRACKET);

        // Check for local declarations -> possible types -> not possible statement
        while (!isEligibleForStatement()) {
            Objekt.Parameter declaration = parseLocalDeclaration();
            // Check if the parameters is already present within the  local symbol-table(redefinition)
            for (Objekt obj : symbolTable) {
                if (declaration.equals(obj))
                    throw new ParserException("Variable redefinition: " + declaration.name, scanner);
            }
            symbolTable.add(declaration);
        }

        Node methodAST = parseStatementSequence();

        assertKeyword(Keyword.CURLY_CLOSING_BRACKET);
        return new Pair<>(symbolTable, methodAST);
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
        Pair<SymbolTable, Node> pair = parseMethodBody(enclosingTable);
        procedure.setSymbolTable(pair.first);
        procedure.setAbstractSyntaxTree(pair.second);

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
                Objekt.Parameter parameter = parseFormalParameterSection();

                // Check if the parameter-name is already used within this method declaration
                for (Objekt.Parameter p : parameters)
                    if (p.equals(parameter))
                        throw new ParserException("Variable name already used in this method declaration: " + parameter.name, scanner);

                parameters.add(parameter);
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

            // Assume the expression is a constant, try to evaluate it
            int value = parseExpression().evaluateConstantExpression();

            Objekt.Constant constant = new Objekt.Constant(identifier, type, value);

            // Check if the constants name is already present within the  local symbol-table(redefinition)
            for (Objekt obj : symbolTable) {
                // check final overlapping
                if (constant.equals(obj))
                    throw new ParserException("Variable redefinition: " + identifier, scanner);
            }

            symbolTable.push(constant);
            assertKeyword(Keyword.SEMICOLON);
        }

        // check for { type identifier ";" }
        // This checks for all possible types, not elegant
        while (currentSymbol.equals(new Symbol<>(Keyword.INT))) {
            Type type = parseType();
            String identifier = parseIdentifier();
            Objekt.Parameter parameter = new Objekt.Parameter(identifier, type);

            // Check if the parameters is already present within the  local symbol-table(redefinition)
            for (Objekt obj : symbolTable) {
                // check final overlapping
                if (parameter.equals(obj) || (obj instanceof Objekt.Constant && obj.name.equals(identifier)))
                    throw new ParserException("Variable redefinition: " + identifier, scanner);
            }

            symbolTable.push(parameter);
            assertKeyword(Keyword.SEMICOLON);
        }

        // { method_declaration }
        while (currentSymbol.equals(new Symbol<>(Keyword.PUBLIC))) {
            Objekt.Procedure procedure = parseMethodDeclaration(symbolTable);

            // Check if a procedure with same name and same parameter set is present
            for (Objekt obj : symbolTable) {
                if (procedure.equals(obj))
                    throw new ParserException("Unambiguous method declaration for: " + procedure.name, scanner);
            }

            symbolTable.push(procedure);
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

        return new Objekt.Clasz(identifier, symbolTable);
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
     * Generic pair of 2 objects
     *
     * @param <T1> Type of first object
     * @param <T2> Type of second object
     * @author TillFleisch
     */
    public static class Pair<T1, T2> {
        T1 first;
        T2 second;

        /**
         * Generic-pair constructor
         *
         * @param first  first object T1
         * @param second second object of type T2
         */
        public Pair(T1 first, T2 second) {
            this.first = first;
            this.second = second;
        }
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
                new Parser(temporaryScanner("(int i, int j, int k) ;")).parseFormalParameters();

                new Parser(temporaryScanner("public int meth() ;")).parseMethodHead();
                new Parser(temporaryScanner("public void meth(int i) ;")).parseMethodHead();

                new Parser(temporaryScanner("public int meth(){int i; int j; i=0; j=meth();} ;"))
                        .parseMethodDeclaration(new SymbolTable());
                new Parser(temporaryScanner("public void meth(){i=0;} ;")).parseMethodDeclaration(new SymbolTable());

                new Parser(temporaryScanner("final int i = 0; ;")).parseDeclarations();
                new Parser(temporaryScanner("final int i = 0; final int j = 0; ;")).parseDeclarations();
                new Parser(temporaryScanner("int i; int j; ;")).parseDeclarations();
                new Parser(temporaryScanner("int i; int j; int k; ;")).parseDeclarations();
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
