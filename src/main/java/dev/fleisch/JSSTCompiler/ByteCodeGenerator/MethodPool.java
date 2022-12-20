package dev.fleisch.JSSTCompiler.ByteCodeGenerator;

import dev.fleisch.JSSTCompiler.Node;
import dev.fleisch.JSSTCompiler.Objekt;
import dev.fleisch.JSSTCompiler.Operation;
import dev.fleisch.JSSTCompiler.Type;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;

/**
 * Pool containing methods.
 *
 * @author TillFleisch
 */
public class MethodPool extends Pool<Info.MethodInfo> {

    /**
     * constant pool used in this field pool for reference creation
     */
    ConstantPool constantPool;

    /**
     * Constructor with constant pool reference (used for constant pool entry generation)
     *
     * @param constantPool Constant pool used in the class file
     */
    MethodPool(ConstantPool constantPool) {
        this.constantPool = constantPool;
    }

    /**
     * Adds a method to the method pool
     *
     * @param procedure Method to add
     */
    public void add(Objekt.Procedure procedure) throws Exception {

        // Constant name
        constantPool.add(new Info.ConstantPoolInfo.UTF8Info(procedure.getName()));
        int nameIndex = constantPool.size();

        // Create a descriptor
        StringBuilder descriptorBuilder = new StringBuilder();
        descriptorBuilder.append("(");
        descriptorBuilder.append("I".repeat(procedure.getParameterList().size()));
        descriptorBuilder.append(")");
        if (procedure.getReturnType() == Type.VOID) descriptorBuilder.append("V");
        if (procedure.getReturnType() == Type.INT) descriptorBuilder.append("I");

        constantPool.add(new Info.ConstantPoolInfo.UTF8Info(descriptorBuilder.toString()));
        int descriptorIndex = constantPool.size();

        Info.AttributeInfo.CodeAttribute codeAttribute = generateCodeAttribute(procedure);

        // Array of attributes containing the constants value
        Info.AttributeInfo[] attributes = new Info.AttributeInfo[]{codeAttribute};

        add(new Info.MethodInfo(nameIndex, descriptorIndex, attributes));
        poolReference.put(procedure, size());
    }

    /**
     * Generates the Code attribute of a Method Info including the actual ByteCode used within the method
     *
     * @param procedure Method on which the CodeAttribute is based
     * @return Code attribute containing Bytecode for the given method.
     */
    Info.AttributeInfo.CodeAttribute generateCodeAttribute(Objekt.Procedure procedure) throws Exception {

        // Constant name
        constantPool.add(new Info.ConstantPoolInfo.UTF8Info("Code"));
        int nameIndex = constantPool.size();

        // Translate procedure ast/symbol-table into ByteCode

        ByteArrayOutputStream codeStream = new ByteArrayOutputStream();

        // Create a Procedure Translator
        ProcedureTranslator translator = new ProcedureTranslator(constantPool, procedure);

        // Write procedure byte code
        codeStream.write(translator.getBytecode());

        if (procedure.getReturnType() == Type.VOID) {
            // For void methods add a return just in case (implicit return)
            codeStream.write(ByteCode.RETURN.getCode());
        } else {
            // otherwise add a mock return (last statement must be return) even if it is not reachable
            codeStream.write(ByteCode.ICONST_0.getCode());
            codeStream.write(ByteCode.IRETURN.getCode());
        }

        return new Info.AttributeInfo.CodeAttribute(nameIndex, translator.getMaxStack(), translator.getMaxLocals(), codeStream.toByteArray());
    }

}

