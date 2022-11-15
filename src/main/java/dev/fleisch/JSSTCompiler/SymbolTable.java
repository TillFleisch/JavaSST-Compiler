package dev.fleisch.JSSTCompiler;

import java.util.LinkedList;

/**
 * Basic SymbolTable implementation using {@link LinkedList}.
 * Simple nesting is possible by referencing the enclosing SymbolTable
 *
 * @author TillFleisch
 */
public class SymbolTable extends LinkedList<Objekt> {

    /**
     * The enclosing SymbolTable, if present
     */
    SymbolTable enclosingTable = null;

    /**
     * Create an empty SymbolTable
     */
    public SymbolTable() {
    }

    /**
     * Create an empty SymbolTable with an enclosing Table reference.
     *
     * @param enclosingTable A reference to the enclosing SymbolTable
     */
    public SymbolTable(SymbolTable enclosingTable) {
        this.enclosingTable = enclosingTable;
    }
}
