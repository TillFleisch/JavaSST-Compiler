package dev.fleisch.JSSTCompiler;

import java.util.HashSet;

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
                Objekt.Procedure procedure = (Objekt.Procedure) objekt;
                Node.StatementSequenceNode ast = (Node.StatementSequenceNode) procedure.abstractSyntaxTree;

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

                        // Check for invalid assignments
                        if (node instanceof Node.BinaryOperationNode &&
                                ((Node.BinaryOperationNode) node).operation == Operation.Binary.ASSIGNMENT) {
                            Node.IdentifierNode identifier = (Node.IdentifierNode) node.left;

                            // Assignment to final variables
                            if (identifier.symbolTableEntry instanceof Objekt.Constant) {
                                throw new SemanticAnalysisException("Cannot assign final variable " +
                                        identifier.symbolTableEntry.name, identifier.getCodePosition());
                            }
                        }

                        // Assert that all return actually return a value if required
                        if (node instanceof Node.UnaryOperationNode) {
                            if (procedure.returnType != Type.VOID &&
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
                if (procedure.returnType != Type.VOID) {
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
                                + procedure);

                }


                // Check if variables are assigned before use

                // Set containing available variables
                // Add method arguments to the list as available parameters
                HashSet<Objekt> variables = new HashSet<>(procedure.parameterList);

                // Check if variables are used correctly within the procedure
                checkTemporalConsistency((Node.StatementSequenceNode) procedure.abstractSyntaxTree, variables);
            }
        }

    }


    /**
     * Recursively checks if variables are only used if they have been assigned before.
     *
     * @param statementSequenceNode statement sequence to check for validity
     * @param variables             Set of variables which are available due to prior statements
     * @return Set of variables available after all statements have been processed
     * @throws SemanticAnalysisException If variables have been used before assignment
     */
    static HashSet<Objekt> checkTemporalConsistency(Node.StatementSequenceNode statementSequenceNode, HashSet<Objekt> variables) throws SemanticAnalysisException {

        // Go through the statements and update entries within the variable map
        for (Node statement : statementSequenceNode.statements) {

            // Find all assignment statements
            if (statement instanceof Node.BinaryOperationNode &&
                    ((Node.BinaryOperationNode) statement).operation == Operation.Binary.ASSIGNMENT) {
                Node.IdentifierNode identifier = (Node.IdentifierNode) statement.left;

                // Check if assignment is valid and add it to set variables
                if (identifier.symbolTableEntry instanceof Objekt.Parameter) {

                    // Check if the assignment uses valid variables
                    assertValidVariablesUsed(statement.right, variables);

                    // Add the assigned variable to the set of assigned variables
                    variables.add(identifier.symbolTableEntry);
                }

            }

            // Find return statements
            if (statement instanceof Node.UnaryOperationNode &&
                    ((Node.UnaryOperationNode) statement).operation == Operation.Unary.RETURN) {
                // If the return statement does return a value check if it has been initialized;
                if (statement.left != null) {
                    // Check if the assignment uses valid variables
                    assertValidVariablesUsed(statement.left, variables);
                }
            }

            // Check loop content
            if (statement instanceof Node.WhileNode) {
                // Check if the condition uses valid variables
                assertValidVariablesUsed(((Node.WhileNode) statement).condition, variables);

                // Check contained statements
                checkTemporalConsistency((Node.StatementSequenceNode) statement.left,
                        new HashSet<>(variables));

                // Assume no variables are set during a loop (i.e. loop never entered)
            }

            // Check ifNode and includes variables which are available after both branches into the variable set
            if (statement instanceof Node.IfNode) {

                // Check if the condition uses valid variables
                assertValidVariablesUsed(((Node.IfNode) statement).condition, variables);

                // Check contained statements
                HashSet<Objekt> ifBranchVariables = checkTemporalConsistency((Node.StatementSequenceNode) statement.left,
                        new HashSet<>(variables));
                HashSet<Objekt> elseBranchVariables = checkTemporalConsistency((Node.StatementSequenceNode) statement.right,
                        new HashSet<>(variables));

                // intersect both sets
                ifBranchVariables.retainAll(elseBranchVariables);

                // Add all variables which are available after both branches to the variable set
                variables.addAll(ifBranchVariables);
            }

            // Check if parameters for procedure call are valid
            if (statement instanceof Node.ProcedureCallNode) {

                // Assert that all argument are valid
                for (Node argument : ((Node.StatementSequenceNode) statement.left).statements) {
                    assertValidVariablesUsed(argument, variables);
                }
            }


        }
        return variables;
    }

    /**
     * Checks if a given expression only uses variables contained within the provided set
     *
     * @param node      Expression
     * @param variables Set of valid variables to be used
     * @throws SemanticAnalysisException If the expression uses invalid variables
     */
    static void assertValidVariablesUsed(Node node, HashSet<Objekt> variables) throws SemanticAnalysisException {
        // Check statements used within condition
        HashSet<Objekt> usedVariables = variablesUsedInExpression(node);

        // Remove all valid variables
        usedVariables.removeAll(variables);

        // Throw exception for remaining non-final variables (have not been initialized)
        for (Objekt variable : usedVariables) {
            // Constant scope check not required for JavaSST
            if (!(variable instanceof Objekt.Constant))
                throw new SemanticAnalysisException("Variable " + variable.name + " might not have been initialized",
                        node.getCodePosition());
        }
    }

    /**
     * Determines which variables are used within an Expression recursively
     *
     * @return Set of Variables used within an expression
     */
    static HashSet<Objekt> variablesUsedInExpression(Node node) throws SemanticAnalysisException {
        HashSet<Objekt> variables = new HashSet<>();

        // Return identifier (variable/ final variable) as is
        if (node instanceof Node.IdentifierNode) {
            variables.add(((Node.IdentifierNode) node).symbolTableEntry);
            return variables;
        }

        // Return left/right branch recursively
        if (node instanceof Node.BinaryOperationNode) {
            variables.addAll(variablesUsedInExpression(node.left));
            variables.addAll(variablesUsedInExpression(node.right));
            return variables;
        }

        // Return branch recursively
        if (node instanceof Node.UnaryOperationNode) {
            variables.addAll(variablesUsedInExpression(node.left));
            return variables;
        }

        // Add parameters for procedure call to variable list
        if (node instanceof Node.ProcedureCallNode) {
            Node.ProcedureCallNode procedureCallNode = (Node.ProcedureCallNode) node;
            Node.StatementSequenceNode parameters = (Node.StatementSequenceNode) procedureCallNode.left;

            for (Node parameter : parameters.statements)
                variables.addAll(variablesUsedInExpression(parameter));
            return variables;
        }

        // Empty set for constants
        if (node instanceof Node.ConstantNode) {
            return variables;
        }

        throw new SemanticAnalysisException("Invalid expression supplied for variable check");
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
