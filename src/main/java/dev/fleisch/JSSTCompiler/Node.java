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
     * Traverse the tree starting from this node(root)
     */
    public void traverse(TraverseCallback traverseCallback) throws Exception {
        traverseCallback.onTraverse(this);
        if (left != null)
            left.traverse(traverseCallback);
        if (right != null)
            right.traverse(traverseCallback);
    }

    @Override
    public abstract String toString();

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

        @Override
        public String toString() {
            return "BinaryOperationNode:\\l\t" + operation.name();
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

        @Override
        public String toString() {
            return "UnaryOperationNode:\\l\t" + operation.name();
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

        @Override
        public String toString() {
            return "ConstantNode:\\l\t" + value;
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
         * Reference to symbol table entry
         */
        Objekt symbolTableEntry = null;

        /**
         * Constructor for identifier nodes
         *
         * @param identifier The identifiers name
         */
        public IdentifierNode(String identifier) {
            super(null, null);
            this.identifier = identifier;
        }

        /**
         * Set the symbol table reference of this object
         *
         * @param symbolTableEntry Parameter from the symbolTable
         */
        public void setSymbolTableEntry(Objekt symbolTableEntry) {
            this.symbolTableEntry = symbolTableEntry;
        }

        @Override
        public String toString() {
            return "IdentifierNode:\\l\t" + identifier;
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

        @Override
        public String toString() {
            return "WhileNode";
        }

        @Override
        public void traverse(TraverseCallback traverseCallback) throws Exception {
            condition.traverse(traverseCallback);
            super.traverse(traverseCallback);
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

        @Override
        public String toString() {
            return "IfNode";
        }

        @Override
        public void traverse(TraverseCallback traverseCallback) throws Exception {
            condition.traverse(traverseCallback);
            super.traverse(traverseCallback);
        }
    }

    /**
     * Node for procedure calls
     */
    public static class ProcedureCallNode extends Node {

        /**
         * Reference to symbol table entry
         */
        Objekt.Procedure symbolTableEntry = null;

        String identifier;

        /**
         * Constructor for Procedure call nodes
         *
         * @param identifier The procedures identifier
         * @param parameters The parameters to use for the procedure calls
         */
        public ProcedureCallNode(String identifier, StatementSequenceNode parameters) {
            super(parameters, null);
            this.identifier = identifier;
        }

        /**
         * Set the symbol table reference of this object
         *
         * @param symbolTableEntry Procedure from the symbolTable
         */
        public void setSymbolTableEntry(Objekt.Procedure symbolTableEntry) {
            this.symbolTableEntry = symbolTableEntry;
        }

        @Override
        public String toString() {
            return "ProcedureCallNode:\\l\t" + identifier;
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

        @Override
        public void traverse(TraverseCallback traverseCallback) throws Exception {
            traverseCallback.onTraverse(this);
            for (Node statement : statements) {
                statement.traverse(traverseCallback);
            }
        }

        @Override
        public String toString() {
            return "StatementSequenceNode";
        }

        /**
         * @return Hashcode of the fist child, if present, super.hashCode() otherwise
         */
        @Override
        public int hashCode() {
            if (statements.size() > 0) {
                return statements.get(0).hashCode();
            }
            return super.hashCode();
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

    /**
     * Graph traversal callback
     */
    public interface TraverseCallback {

        /**
         * Called upon traversal
         *
         * @param node The node begin traversed
         */
        void onTraverse(Node node) throws Exception;
    }

    /**
     * Determine the dot-representation for this node and it's children
     *
     * @return Dot representation
     */
    public String toDot() throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("subgraph ").append(hashCode()).append("{\n");

        // Traverse graph and retrieve Node information
        traverse(child -> {
            stringBuilder.append(child.hashCode()).append(" [label=\"").append(child).append("\"");
            // Shape nodes according to type
            if (child instanceof Node.IfNode || child instanceof Node.WhileNode) {
                stringBuilder.append("shape=diamond");
            } else if (child instanceof Node.ProcedureCallNode) {
                stringBuilder.append("shape=csd");
            } else if (child instanceof Node.UnaryOperationNode &&
                    ((Node.UnaryOperationNode) child).operation == Operation.Unary.RETURN) {
                stringBuilder.append("shape=oval");
            } else {
                stringBuilder.append("shape=box");
            }
            stringBuilder.append("]\n");
        });

        stringBuilder.append("\n\n");

        // Traverse graph and retrieve edges
        traverse(child -> {

            // Add left and right sub-graphs
            if (child.left != null) {
                stringBuilder.append(child.hashCode()).append("->").append(child.left.hashCode()).append("[color=green]\n");
            }

            if (child.right != null) {
                stringBuilder.append(child.hashCode()).append("->").append(child.right.hashCode()).append("[color=red]\n");
            }

            // Add condition sub-graph edges for while/if
            if (child instanceof Node.IfNode || child instanceof Node.WhileNode) {
                stringBuilder.append(child.hashCode());
                stringBuilder.append("->");
                if (child instanceof Node.IfNode) {
                    stringBuilder.append(((Node.IfNode) child).condition.hashCode());
                }
                if (child instanceof Node.WhileNode) {
                    stringBuilder.append(((Node.WhileNode) child).condition.hashCode());
                }
                stringBuilder.append("[color=blue]\n");
            }

            // add implicit edges from sequence nodes
            if (child instanceof Node.StatementSequenceNode) {
                List<Node> statements = ((Node.StatementSequenceNode) child).statements;

                // Add edges between statement of Statement Sequence Nodes
                for (int i = 0; i < statements.size(); i++) {
                    stringBuilder.append(statements.get(i).hashCode());
                    if (i < statements.size() - 1) {
                        stringBuilder.append("->");
                    }
                }

                if (statements.size() > 0)
                    stringBuilder.append("[color=black]\n");
            }

        });

        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}