package dev.fleisch.JSSTCompiler;

import java.util.LinkedList;

/**
 * Simple Objects for describing parts of the language within the {@link SymbolTable}
 *
 * @author TillFleisch
 */
public abstract class Objekt {
    /**
     * The objects name
     */
    String name;

    /**
     * Basic constructor with name attribute
     *
     * @param name The objects name
     */
    public Objekt(String name) {
        this.name = name;
    }

    /**
     * Class describing classes.
     */
    static class Clasz extends Objekt {

        /**
         * The {@link SymbolTable} used within this class.
         */
        SymbolTable symbolTable;

        /**
         * Class-Constructor with a given SymbolTable
         *
         * @param name        The Classes name
         * @param symboltable The SymbolTable contained within the class
         */
        public Clasz(String name, SymbolTable symboltable) {
            super(name);
            this.symbolTable = symboltable;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Clasz)
                return ((Clasz) obj).name.equals(name);
            return super.equals(obj);
        }

        public String toDot(boolean showIdentifierReferences, boolean showProcedureCallReferences,
                            boolean linkProcedureASTs) throws Exception {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("digraph ").append(hashCode()).append(" {\n");
            stringBuilder.append("labelloc=\"top\";")
                    .append("\nlabel=\"").append(name).append("\";\n")
                    .append("compound=true;");

            stringBuilder.append("subgraph cluster_declarations_").append(name).append(" {\n");
            stringBuilder.append("label=\"Declarations ").append(name).append("\"\n");
            for (Objekt objekt : symbolTable) {

                // Add procedure as subgraph (scope)
                if (objekt instanceof Procedure) {
                    if (((Procedure) objekt).symbolTable.size() > 0) {
                        stringBuilder.append("subgraph cluster_declarations_").append(objekt.hashCode()).append("{\n");
                        stringBuilder.append("label=\"Declarations ").append(objekt).append("\";\n");

                        // Add entries from local SymbolTable within the subgraph
                        for (Objekt subObject : ((Procedure) objekt).symbolTable) {
                            stringBuilder.append(subObject.hashCode()).append("[label=\"").append(subObject).append("\"];\n");
                        }
                        stringBuilder.append("}\n");
                    } else {
                        stringBuilder.append(objekt.hashCode()).append("[label=\"").append(objekt).append("\" shape=box];\n");
                    }
                } else {
                    stringBuilder.append(objekt.hashCode()).append("[label=\"").append(objekt).append("\"];\n");
                }
            }
            stringBuilder.append("}\n");

            // generate basic AST for methods declared within the class
            for (Objekt objekt : symbolTable) {
                if (objekt instanceof Procedure) {
                    stringBuilder.append("subgraph cluster_").append(objekt.hashCode()).append("{\n");
                    stringBuilder.append("label=\"").append(objekt).append("\";\n");
                    stringBuilder.append("color=purple\n");

                    // generate AST graphs as usual
                    stringBuilder.append(((Procedure) objekt).abstractSyntaxTree.toDot()).append("\n");

                    stringBuilder.append("}\n");
                    // traverse each graph and add symbol table references

                    ((Procedure) objekt).abstractSyntaxTree.traverse(node -> {
                        // Find reference if present
                        if (showIdentifierReferences && node instanceof Node.IdentifierNode) {
                            Objekt reference = ((Node.IdentifierNode) node).symbolTableEntry;
                            // Add an edge from the current node to the referred declaration
                            stringBuilder.append(node.hashCode()).append("->").append(reference.hashCode())
                                    .append("[color=gray];\n");
                        }
                        if (showProcedureCallReferences && node instanceof Node.ProcedureCallNode) {
                            Procedure reference = ((Node.ProcedureCallNode) node).symbolTableEntry;
                            // Add an edge from the current node to the referred declaration
                            if (reference.symbolTable.size() > 0) {
                                // Point to first node within the methods declaration and cap the arrow at the
                                // cluster bounding box
                                stringBuilder.append(node.hashCode()).append("->")
                                        .append(reference.symbolTable.get(0).hashCode())
                                        .append("[color=orange ")
                                        .append("lhead=\"cluster_declarations_").append(reference.hashCode())
                                        .append("\"];\n");
                            } else {
                                stringBuilder.append(node.hashCode()).append("->").append(reference.hashCode())
                                        .append("[color=orange];\n");
                            }

                        }

                    });

                    // Add an edge from the procedure definition to the ast
                    if (linkProcedureASTs) {
                        // If there is no node for a procedure (implicit box from cluster) pick the first element
                        if (((Procedure) objekt).symbolTable.size() > 0) {
                            stringBuilder.append(((Procedure) objekt).symbolTable.get(0).hashCode());
                        } else {
                            stringBuilder.append(objekt.hashCode());
                        }
                        stringBuilder.append("->").append(((Procedure) objekt).abstractSyntaxTree.hashCode())
                                .append("[color=purple lhead=\"cluster_").append(objekt.hashCode());

                        // If there is no node for a procedure cap at cluster box
                        if (((Procedure) objekt).symbolTable.size() > 0) {
                            stringBuilder.append("\" ltail=\"cluster_declarations_").append(objekt.hashCode());
                        }
                        stringBuilder.append("\"];\n");
                    }
                }
            }

            stringBuilder.append("}");
            return stringBuilder.toString();
        }

        @Override
        public String toString() {
            return "Class: " + name;
        }
    }

    /**
     * Class describing Parameters
     */
    static class Parameter extends Objekt {

        /**
         * The parameters name
         */
        Type type;

        /**
         * Constructor for parameters with type and name.
         *
         * @param name The parameter's name.
         * @param type The parameter's type.
         */
        public Parameter(String name, Type type) {
            super(name);
            this.type = type;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Parameter) {
                return ((Parameter) obj).name.equals(name) && ((Parameter) obj).type.equals(type);
            }
            return super.equals(obj);
        }

        @Override
        public String toString() {
            return "Variable/Parameter: " + name + "(" + type + ")";
        }
    }

    /**
     * Class describing Constants
     */
    static class Constant extends Parameter {

        /**
         * The constants value.
         * This is currently an integer, since the JavaSST is limited to a single type.
         * Otherwise, a generic or type-based implementation could be useful.
         */
        int value;

        /**
         * Constructor for Constants with a name,type and final value
         *
         * @param name  The constant's name
         * @param type  The constant's type
         * @param value The constant's value
         */
        public Constant(String name, Type type, int value) {
            super(name, type);
            this.value = value;
        }

        @Override
        public String toString() {
            return "Constant(final): " + name + "(" + type + "): " + value;
        }
    }

    /**
     * Class describing Procedures
     */
    static class Procedure extends Objekt {

        /**
         * The procedure's return type.
         */
        Type returnType;

        /**
         * The procedure's parameter list.
         */
        LinkedList<Parameter> parameterList;

        /**
         * The procedure's local SymbolTable.
         */
        SymbolTable symbolTable;

        /**
         * The AST contained within this Procedure call
         */
        Node abstractSyntaxTree;

        /**
         * Constructor for Procedure Objects.
         *
         * @param name          The procedure's name
         * @param parameterList The procedure's parameter list
         * @param returnType    The procedure's return type
         * @param symbolTable   The procedure's SymbolTable
         */
        public Procedure(String name, LinkedList<Parameter> parameterList, Type returnType, SymbolTable symbolTable) {
            super(name);
            this.parameterList = parameterList;
            this.symbolTable = symbolTable;
            this.returnType = returnType;

            // Add the parameters into the local SymbolTable
            symbolTable.addAll(parameterList);
        }

        /**
         * Constructor for Procedure Objects.
         *
         * @param name          The procedure's name
         * @param parameterList The procedure's parameter list
         * @param returnType    The procedure's return type
         */
        public Procedure(String name, LinkedList<Parameter> parameterList, Type returnType) {
            this(name, parameterList, returnType, new SymbolTable());
        }

        /**
         * Overrides the procedures SymbolTable
         *
         * @param symbolTable The new SymbolTable to be used
         */
        public void setSymbolTable(SymbolTable symbolTable) {
            this.symbolTable = symbolTable;
        }

        /**
         * Sets the syntax tree contained within this procedure
         *
         * @param abstractSyntaxTree the syntax tree contained within this procedure
         */
        public void setAbstractSyntaxTree(Node abstractSyntaxTree) {
            this.abstractSyntaxTree = abstractSyntaxTree;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Procedure)
                // We only need to check if the number of parameters is unique since JavaSST only supports a single type.
                // Check for same name and parameter list size
                return (((Procedure) obj).name.equals(name) &&
                        ((Objekt.Procedure) obj).parameterList.size() == parameterList.size());
            return super.equals(obj);
        }

        @Override
        public String toString() {
            // fancy exception with corr. nr of parameters (JavaSST only supports a single type "int")
            StringBuilder parameters = new StringBuilder("(");
            int nrParameter = parameterList.size();
            for (int i = 0; i < nrParameter; i++) {
                parameters.append("int");
                if (i < nrParameter - 1)
                    parameters.append(",");
            }
            parameters.append(")");
            return "Procedure: " + name + parameters;
        }
    }

}
