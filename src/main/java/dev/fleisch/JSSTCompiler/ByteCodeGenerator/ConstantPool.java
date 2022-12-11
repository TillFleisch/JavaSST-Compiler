package dev.fleisch.JSSTCompiler.ByteCodeGenerator;

import dev.fleisch.JSSTCompiler.Objekt;

/**
 * Class describing a constant pool which holds ConstantPoolInformation
 *
 * @author TillFleisch
 */
public class ConstantPool extends Pool<Info.ConstantPoolInfo> {

    /**
     * Adds a clasz object to the Constant pool table
     *
     * @param clasz The clasz object to add
     */
    public void add(Objekt.Clasz clasz) {

        // Create class info with name reference
        add(new Info.ConstantPoolInfo.ClaszInfo(size() + 2)); // Index ahead

        // Add object to reference
        poolReference.put(clasz, size());

        // Create name info
        add(new Info.ConstantPoolInfo.UTF8Info(clasz.getName()));
    }
}