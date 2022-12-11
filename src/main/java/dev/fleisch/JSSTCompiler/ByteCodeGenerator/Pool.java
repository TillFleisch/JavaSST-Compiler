package dev.fleisch.JSSTCompiler.ByteCodeGenerator;

import dev.fleisch.JSSTCompiler.Objekt;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Class for easy constant pool management
 *
 * @author TillFleisch
 */

public abstract class Pool<T> extends LinkedList<T> {
    /**
     * Map for easy objekt to pool reference
     */
    final HashMap<Objekt, Integer> poolReference = new HashMap<>();

    /**
     * Retrieve a pool index based on an Object
     *
     * @param objekt The objekt to find within the pool
     * @return Pool index for the given objekt
     */
    public int getByReference(Objekt objekt) {
        return poolReference.get(objekt);
    }

}