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
        SymbolTable symboltable;

        /**
         * Class-Constructor with a given SymbolTable
         *
         * @param name        The Classes name
         * @param symboltable The SymbolTable contained within the class
         */
        public Clasz(String name, SymbolTable symboltable) {
            super(name);
            this.symboltable = symboltable;
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
    }

    /**
     * Class describing Constants
     */
    static class Constant extends Objekt {

        /**
         * The constants Type
         */
        Type type;

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
            super(name);
            this.type = type;
            this.value = value;
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
    }

}
