package dev.fleisch.JSSTCompiler.ByteCodeGenerator;

import dev.fleisch.JSSTCompiler.Objekt;
import dev.fleisch.JSSTCompiler.Type;

import java.util.HashMap;

/**
 * Class describing a constant pool which holds ConstantPoolInformation
 *
 * @author TillFleisch
 */
public class ConstantPool extends Pool<Info.ConstantPoolInfo> {

    /**
     * Map for easy integer constant to pool reference
     */
    final HashMap<Integer, Integer> constantReference = new HashMap<>();

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

    /**
     * Creates a method reference within the constant pool.
     * This does not create the actual method info. Method info \w code is stored in the Method pool
     *
     * @param procedure Procedure to create a reference for
     * @param clasz     Class in which the procedure is contained
     */
    public void add(Objekt.Procedure procedure, Objekt.Clasz clasz) {
        // Add CONSTANT_Methodref_info used for static retrieval

        // TODO: this contains Code clone from MethodPool (also creates duplicate entries within the constant pool)
        // Constant name
        add(new Info.ConstantPoolInfo.UTF8Info(procedure.getName()));
        int nameIndex = size();

        // Create a descriptor
        StringBuilder descriptorBuilder = new StringBuilder();
        descriptorBuilder.append("(");
        descriptorBuilder.append("I".repeat(procedure.getParameterList().size()));
        descriptorBuilder.append(")");
        if (procedure.getReturnType() == Type.VOID)
            descriptorBuilder.append("V");
        if (procedure.getReturnType() == Type.INT)
            descriptorBuilder.append("I");

        // Add the descriptor to the pool
        add(new Info.ConstantPoolInfo.UTF8Info(descriptorBuilder.toString()));
        int descriptorIndex = size();

        // Retrieve constant pool
        int classIndex = getByReference(clasz);

        // Add NameAndType info to the constant pool
        add(new Info.ConstantPoolInfo.NameAndTypeInfo(nameIndex, descriptorIndex));
        int nameAndTypeIndex = size();

        // add a CONSTANT_Methodref_info to the constant pool
        add(new Info.ConstantPoolInfo.MethodReferenceInfo(classIndex, nameAndTypeIndex));
        poolReference.put(procedure, size());
    }

    /**
     * Add constant ints to the constant pool
     *
     * @param value constant to add
     */
    public void add(int value) {

        // Check if constant already present
        if (!constantReference.containsKey(value)) {
            // Add the constant to the constant pool
            add(new Info.ConstantPoolInfo.IntegerInfo(value));
            constantReference.put(value, size());
        }
    }
}