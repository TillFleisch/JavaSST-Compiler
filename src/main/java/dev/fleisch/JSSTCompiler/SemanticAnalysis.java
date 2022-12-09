package dev.fleisch.JSSTCompiler;

/**
 * Class containing methods for semantic analysis of class objects.
 *
 * @author TillFleisch
 */
public class SemanticAnalysis {

    /**
     * Executes semantic analysis for the provided class object
     *
     * @param clasz Class to check
     * @throws Exception on semantic errors
     */
    public static void run(Objekt.Clasz clasz) throws Exception {

        // Check all methods contained within the class
        for (Objekt objekt : clasz.symbolTable) {
            if (objekt instanceof Objekt.Procedure) {
                Node.StatementSequenceNode ast = (Node.StatementSequenceNode) ((Objekt.Procedure) objekt).abstractSyntaxTree;

                // Check if all operations are type-compatible
                Node.TraverseCallback typeCompatibility = new Node.TraverseCallback() {
                    @Override
                    public void onTraverse(Node node) throws Exception {
                        // Check if all nodes return correctly (throws exception on fail)
                        doesReturnValue(node);

                        // Check if parameters for procedure call are valid
                        if (node instanceof Node.ProcedureCallNode) {
                            checkProcedureCallParameters((Node.ProcedureCallNode) node);
                        }

                        // Check if sequences contain unreachable statements
                        if (node instanceof Node.StatementSequenceNode) {
                            containsUnreachableCode((Node.StatementSequenceNode) node);
                        }

                        // Assert that all return actually return a value if required
                        if (node instanceof Node.UnaryOperationNode) {
                            if (((Objekt.Procedure) objekt).returnType != Type.VOID &&
                                    ((Node.UnaryOperationNode) node).operation == Operation.Unary.RETURN)
                                if (!doesReturnValue(node.left))
                                    throw new SemanticAnalysisException("Expected return value", node.getCodePosition());
                        }
                    }
                };

                // Traverse the ast
                ast.traverse(typeCompatibility);


                // Since we can't have unreachable code the last statement within a function must return a value
                // This is also true for branches in case of an if statement / loop
                if (((Objekt.Procedure) objekt).returnType != Type.VOID) {
                    Node lastStatement = ast.statements.get(ast.statements.size() - 1);
                    boolean returns = false;

                    // Check if the last statement is an If statement (return must be in if/else branch)
                    if (lastStatement instanceof Node.IfNode) {
                        // Make sure both branches only have paths returning a value
                        if (exitsWithReturnStatement((Node.StatementSequenceNode) lastStatement.left) &&
                                exitsWithReturnStatement((Node.StatementSequenceNode) lastStatement.right))
                            returns = true;
                    }

                    // Check if the last while statement contains a return statement
                    if (lastStatement instanceof Node.WhileNode) {
                        // Make sure the loop contains a return statement
                        if (exitsWithReturnStatement((Node.StatementSequenceNode) lastStatement.left)) {
                            returns = true;
                        }
                    }

                    // Assume last statement must be return statement
                    if (lastStatement instanceof Node.UnaryOperationNode) {
                        if (((Node.UnaryOperationNode) lastStatement).operation == Operation.Unary.RETURN) {
                            returns = true;
                        }
                    }

                    if (!returns)
                        throw new SemanticAnalysisException("Expected return statement with argument for procedure "
                                + objekt);

                }
            }
        }

        // TODO: check if identifier was assigned before use
    }

    /**
     * Determines AST starting from the ast does return a expression value
     *
     * @param node A Node representing the root of a AST
     * @return True this node returns a valid expression
     */
    static boolean doesReturnValue(Node node) throws Exception {

        if (node instanceof Node.ConstantNode) {
            // Constants must have a value so they return something
            return true;
        }

        if (node instanceof Node.IdentifierNode) {
            // An identifier can be null, if no value has been assigned yet
            return true;
        }

        if (node instanceof Node.BinaryOperationNode) {
            Node.BinaryOperationNode binaryOperationNode = (Node.BinaryOperationNode) node;
            if (binaryOperationNode.operation == Operation.Binary.ASSIGNMENT) {
                return false;
            }

            // Check if binary operations have matching types
            if (!(doesReturnValue(binaryOperationNode.left) && doesReturnValue(binaryOperationNode.right)))
                throw new SemanticAnalysisException("Invalid operation: " +
                        binaryOperationNode.operation + " at " + binaryOperationNode.getCodePosition());
            return true;
        }
        if (node instanceof Node.UnaryOperationNode) {
            Node.UnaryOperationNode unaryOperationNode = (Node.UnaryOperationNode) node;
            return doesReturnValue(unaryOperationNode.left);
        }

        if (node instanceof Node.ProcedureCallNode) {
            Node.ProcedureCallNode procedureCallNode = (Node.ProcedureCallNode) node;

            // Return if per def. a procedure call does returns a value
            return procedureCallNode.symbolTableEntry.returnType != Type.VOID;
        }

        return false;
    }

    /**
     * Checks if all parameters passed to the function return the correct value.
     * Since JavaSST only has a single return type, checking if anything is return is sufficient
     *
     * @param procedureCallNode procedure Call to check
     */
    static void checkProcedureCallParameters(Node.ProcedureCallNode procedureCallNode) throws Exception {
        Node.StatementSequenceNode parameterSequence = (Node.StatementSequenceNode) procedureCallNode.left;

        for (Node parameter : parameterSequence.statements) {
            if (!doesReturnValue(parameter))
                throw new SemanticAnalysisException("Expected parameter to return value at procedure call " +
                        procedureCallNode.symbolTableEntry + " at " + procedureCallNode.getCodePosition());
        }
    }


    /**
     * Checks if a statement sequence contains unreachable code
     *
     * @param statementSequenceNode Statement sequence to check
     * @throws SemanticAnalysisException If the code contain unreachable code
     */
    static void containsUnreachableCode(Node.StatementSequenceNode statementSequenceNode) throws SemanticAnalysisException {

        for (int i = 0; i < statementSequenceNode.statements.size(); i++) {
            Node node = statementSequenceNode.statements.get(i);

            // Check if a while node contains a blocking return
            if (node instanceof Node.WhileNode) {
                if (exitsWithReturnStatement((Node.StatementSequenceNode) node.left)) {
                    if (i < statementSequenceNode.statements.size() - 1)
                        throw new SemanticAnalysisException("Unreachable code",
                                statementSequenceNode.statements.get(i + 1).getCodePosition());
                }
            }

            // Check if an if statement contains a blocking return
            if (node instanceof Node.IfNode) {
                if (exitsWithReturnStatement((Node.StatementSequenceNode) node.left) &&
                        exitsWithReturnStatement((Node.StatementSequenceNode) node.right)) {
                    if (i < statementSequenceNode.statements.size() - 1)
                        throw new SemanticAnalysisException("Unreachable code",
                                statementSequenceNode.statements.get(i + 1).getCodePosition());
                }
            }

            // Check for return statements
            if (node instanceof Node.UnaryOperationNode &&
                    ((Node.UnaryOperationNode) node).operation == Operation.Unary.RETURN) {
                if (i < statementSequenceNode.statements.size() - 1)
                    throw new SemanticAnalysisException("Unreachable code",
                            statementSequenceNode.statements.get(i + 1).getCodePosition());
            }
        }
    }

    /**
     * Checks if a statement sequence exits with return statement
     *
     * @param statementSequenceNode statementSequence to check
     * @return true if all paths exit with a return statement
     */
    static boolean exitsWithReturnStatement(Node.StatementSequenceNode statementSequenceNode) {
        boolean returns = false;
        for (Node node : statementSequenceNode.statements) {

            // Check if ifNodes have return statements in both branches
            if (node instanceof Node.IfNode) {
                returns |= exitsWithReturnStatement((Node.StatementSequenceNode) node.left) &&
                        exitsWithReturnStatement((Node.StatementSequenceNode) node.right);
            }

            // Check if While statements contain returns
            if (node instanceof Node.WhileNode) {
                returns |= exitsWithReturnStatement((Node.StatementSequenceNode) node.left);
            }

            // Check for return statements
            if (node instanceof Node.UnaryOperationNode &&
                    ((Node.UnaryOperationNode) node).operation == Operation.Unary.RETURN) {
                returns = true;
            }
        }

        return returns;
    }

}
