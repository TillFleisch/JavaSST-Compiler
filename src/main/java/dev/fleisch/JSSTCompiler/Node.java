package dev.fleisch.JSSTCompiler;

import java.util.List;

/**
 * Class containing Nodes making up an abstract syntax Tree.
 * Basic binary tree node.
 *
 * @author TillFleisch
 */
public abstract class Node {
    Node left;

    Node right;

    /**
     * Constructor for basic binary tree Notes.
     *
     * @param left  Left node
     * @param right Right node
     */
    public Node(Node left, Node right) {
        this.left = left;
        this.right = right;
    }

    /**
     * Node for binary operations.
     */
    public static class BinaryOperationNode extends Node {

        /**
         * The binary operator used in this Node
         */
        Operation.Binary operation;

        /**
         * Constructor for a binary operation node
         *
         * @param left      LHS expression
         * @param right     RHS expression
         * @param operation The Operator for this Node
         */
        public BinaryOperationNode(Node left, Node right, Operation.Binary operation) {
            super(left, right);
            this.operation = operation;
        }
    }

    /**
     * Node for unary operations.
     */
    public static class UnaryOperationNode extends Node {

        /**
         * The unary operation used int this Node
         */
        Operation.Unary operation;


        /**
         * Constructor for a unary operation node
         *
         * @param left      Node to which the unary operator is applied
         * @param operation Unary operation
         */
        public UnaryOperationNode(Node left, Operation.Unary operation) {
            super(left, null);
            this.operation = operation;
        }
    }


    /**
     * Node for constant expressions
     */
    public static class ConstantNode extends Node {

        /**
         * The value of this constant.
         */
        int value;

        /**
         * Constructor for a constant
         *
         * @param value The constant Nodes value
         */
        public ConstantNode(int value) {
            super(null, null);
            this.value = value;
        }
    }

    /**
     * Node for identifiers
     */
    public static class IdentifierNode extends Node {

        /**
         * The string identifier
         */
        String identifier;

        /**
         * Constructor for identifier nodes
         *
         * @param identifier The identifiers name
         */
        public IdentifierNode(String identifier) {
            super(null, null);
            this.identifier = identifier;
        }
    }

    /**
     * Node for while constructs
     */
    public static class WhileNode extends Node {

        /**
         * The condition for this while construct
         */
        Node condition;

        /**
         * Constructor for while Nodes
         *
         * @param condition  The condition for this while Node
         * @param statements The StatementSequence contained within the while loop
         */
        public WhileNode(Node condition, StatementSequenceNode statements) {
            super(statements, null);
            this.condition = condition;
        }
    }

    /**
     * Node for if constructs
     */
    public static class IfNode extends Node {

        /**
         * The condition for this if construct
         */
        Node condition;

        /**
         * Constructor for if constructs
         *
         * @param condition      The condition used in this if construct
         * @param ifStatements   The statement sequence to execute if the condition evaluates to true
         * @param elseStatements The statement sequence to execute of the condition evaluates to false
         */
        public IfNode(Node condition, StatementSequenceNode ifStatements, StatementSequenceNode elseStatements) {
            super(ifStatements, elseStatements);
            this.condition = condition;
        }
    }

    /**
     * Node for procedure calls
     */
    public static class ProcedureCallNode extends Node {


        /**
         * Constructor for Procedure call nodes
         *
         * @param identifier The procedures identifier
         * @param parameters The parameters to use for the procedure calls
         */
        public ProcedureCallNode(String identifier, StatementSequenceNode parameters) {
            super(new IdentifierNode(identifier), parameters);
        }
    }

    /**
     * Node for StatementSequenceNodes
     * <p>
     * This node acts as a wrapper. It does not have children.
     * Subsequent expression are stored, in order, within the list.
     */
    public static class StatementSequenceNode extends Node {

        /**
         * The statements contained within this Node
         */
        List<Node> statements;

        /**
         * Constructor for statement sequence Nodes
         *
         * @param statements The statement sequence contained within this Node
         */
        public StatementSequenceNode(List<Node> statements) {
            super(null, null);
            this.statements = statements;
        }
    }

    /**
     * Evaluate this Node under the assumption that it's content make up a constant expression
     *
     * @return The value of the expression
     * @throws ParserException If the contents are not static and cannot be parsed into a constant expression
     */
    public int evaluateConstantExpression() throws ParserException {

        // Return value of constant nodes
        if (this instanceof ConstantNode) {
            return ((ConstantNode) this).value;
        }

        // resolve valid binary operations recursively
        if (this instanceof BinaryOperationNode) {
            BinaryOperationNode node = (BinaryOperationNode) this;

            if (node.operation == Operation.Binary.ASSIGNMENT) {
                throw new ParserException("Evaluating constant failed! Non-constant found!");
            }

            try {
                return node.operation.apply(node.left.evaluateConstantExpression(), node.right.evaluateConstantExpression());
            } catch (IllegalAccessException e) {
                throw new ParserException(e.getMessage());
            }
        }

        throw new ParserException("Evaluating constant failed! Non-constant found " + this.getClass());
    }
}