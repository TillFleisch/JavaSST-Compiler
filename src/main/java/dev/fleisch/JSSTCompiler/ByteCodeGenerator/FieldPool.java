package dev.fleisch.JSSTCompiler.ByteCodeGenerator;

import dev.fleisch.JSSTCompiler.Objekt;

/**
 * Pool containing Field entries
 *
 * @author TillFleisch
 */
public class FieldPool extends Pool<Info.FieldInfo> {

    /**
     * constant pool used in this field pool for reference creation
     */
    ConstantPool constantPool;

    /**
     * Constructor with constant pool reference (used for constant pool entry generation)
     *
     * @param constantPool Constant pool used in the class file
     */
    FieldPool(ConstantPool constantPool) {
        this.constantPool = constantPool;
    }

    /**
     * Adds a constant (final variable) to the field pool
     *
     * @param constant The constant to add
     */
    public void add(Objekt.Constant constant) {

        // Constant name
        constantPool.add(new Info.ConstantPoolInfo.UTF8Info(constant.getName()));
        int nameIndex = constantPool.size();

        // Create a descriptor
        String descriptor = "I";
        constantPool.add(new Info.ConstantPoolInfo.UTF8Info(descriptor));
        int descriptorIndex = constantPool.size();

        // Add the constants value to the constantPool
        constantPool.add(new Info.ConstantPoolInfo.IntegerInfo(constant.getValue()));
        int constantIndex = constantPool.size();

        // TODO: don't create new entries for each constant
        // "Constant value" entry
        constantPool.add(new Info.ConstantPoolInfo.UTF8Info("ConstantValue"));
        int constantValueIndex = constantPool.size();

        // Array of attributes containing the constants value
        Info.AttributeInfo[] attributes = new Info.AttributeInfo[]{
                new Info.AttributeInfo.ConstantValueAttribute(constantValueIndex, constantIndex)};

        // (public final static)
        add(new Info.FieldInfo(0x0001 | 0x0010 | 0x0008, nameIndex, descriptorIndex, attributes));
        poolReference.put(constant, size());
    }

    /**
     * Adds a variable to the field pool
     *
     * @param parameter variable to add
     * @param clasz     Class in which the parameter is encapsulated
     */
    public void add(Objekt.Parameter parameter, Objekt.Clasz clasz) {
        // Constant name
        constantPool.add(new Info.ConstantPoolInfo.UTF8Info(parameter.getName()));
        int nameIndex = constantPool.size();

        // Create a descriptor
        String descriptor = "I";
        constantPool.add(new Info.ConstantPoolInfo.UTF8Info(descriptor));
        int descriptorIndex = constantPool.size();

        // (public)
        add(new Info.FieldInfo(0x0001 | 0x0008, nameIndex, descriptorIndex, new Info.AttributeInfo[]{}));
        poolReference.put(parameter, size());

        // Add CONSTANT_Fieldref_info used for static retrieval

        // Retrieve constant pool
        int classIndex = constantPool.getByReference(clasz);

        // Add NameAndType info to the constant pool
        constantPool.add(new Info.ConstantPoolInfo.NameAndTypeInfo(nameIndex, descriptorIndex));
        int nameAndTypeIndex = constantPool.size();

        // add a CONSTANT_Fieldref_info to the constant pool
        constantPool.add(new Info.ConstantPoolInfo.FieldReferenceInfo(classIndex, nameAndTypeIndex));
        constantPool.poolReference.put(parameter, constantPool.size());
    }

}
