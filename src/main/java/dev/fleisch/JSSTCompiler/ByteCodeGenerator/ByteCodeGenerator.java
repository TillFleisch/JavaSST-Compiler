package dev.fleisch.JSSTCompiler.ByteCodeGenerator;

import dev.fleisch.JSSTCompiler.Objekt;
import dev.fleisch.JSSTCompiler.SymbolTable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


/**
 * Class used for ClassFileGeneration using ByteCode
 *
 * @author TillFleisch
 */
public class ByteCodeGenerator {


    /**
     * Clasz to generate bytecode for
     */
    Objekt.Clasz clasz;

    /**
     * Mock Class for Object as superclass
     */
    Objekt.Clasz objectClass = new Objekt.Clasz("java/lang/Object", new SymbolTable());

    /**
     * The constant pool used in this class File
     */
    ConstantPool constantPool = new ConstantPool();

    /**
     * The fields used in this class File
     */
    FieldPool fieldPool = new FieldPool(constantPool);

    /**
     * The methods used in this File
     */
    MethodPool methodPool = new MethodPool(constantPool);


    /**
     * Byte output stream containing the class file
     */
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    /**
     * Creates a ByteCode generator for a given class.
     * <p>
     * Determines variable and procedure call pool
     *
     * @param clasz Class on which the generator is based
     */
    public ByteCodeGenerator(Objekt.Clasz clasz) throws IOException {
        this.clasz = clasz;

        // Add the class to the pool
        constantPool.add(clasz);

        // add object class to be used as superclass
        constantPool.add(objectClass);

        // Add elements from the symbolTable
        for (Objekt objekt : clasz.getSymbolTable()) {

            // Add constants to the field pool
            if (objekt instanceof Objekt.Constant) {
                fieldPool.add((Objekt.Constant) objekt);
                continue;
            }

            // Add global variables to the field pool
            if (objekt instanceof Objekt.Parameter) {
                fieldPool.add((Objekt.Parameter) objekt, clasz);
            }

            // add methods-references to the method Pool
            // Method information is generated afterwards, since references might be required
            if (objekt instanceof Objekt.Procedure) {
                constantPool.add((Objekt.Procedure) objekt, clasz);
            }
        }

        // Add methods (method information \w code)
        for (Objekt objekt : clasz.getSymbolTable()) {
            // add methods to the method Pool
            if (objekt instanceof Objekt.Procedure) {
                methodPool.add((Objekt.Procedure) objekt);
            }
        }

    }

    /**
     * Generates the class file containing fields, methods and their code
     *
     * @return OutputStream containing the generated class file
     * @throws IOException if generation fails
     */
    public ByteArrayOutputStream generate() throws IOException {

        // Write magic to output
        outputStream.write(new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE});

        // Write minor version
        outputStream.write(new byte[]{(byte) 0x00, (byte) 0x00});

        // Write major version (JDK 1.1)
        outputStream.write(new byte[]{(byte) 0x00, (byte) 0x3B});//2D

        // Write constant pool count
        int constantPoolSize = constantPool.size() + 1;
        outputStream.write(constantPoolSize >> 8);
        outputStream.write(constantPoolSize);

        // Write constant pool
        for (Info.ConstantPoolInfo info : constantPool) {
            outputStream.write(info.toByteCode());
        }

        // Write access flags (public class)
        outputStream.write(new byte[]{(byte) 0x00, (byte) (0b0001)});

        // Write this_class (reference to class info in constant pool)
        int classIndex = constantPool.getByReference(clasz);
        outputStream.write(classIndex >> 8);
        outputStream.write(classIndex);

        // Write super class (mock Object class reference) (JavaSST does not support inheritance)
        int objectClassIndex = constantPool.getByReference(objectClass);
        outputStream.write(objectClassIndex >> 8);
        outputStream.write(objectClassIndex);

        // Write interfaces count (JavaSST does not support interfaces)
        outputStream.write(new byte[]{(byte) 0x00, (byte) 0x00});

        // Don't write interfaces since we don't have any

        // Write fields count
        outputStream.write(fieldPool.size() >> 8);
        outputStream.write(fieldPool.size());

        // Write field pool
        for (Info.FieldInfo info : fieldPool) {
            outputStream.write(info.toByteCode());
        }

        // Write method count
        outputStream.write(methodPool.size() >> 8);
        outputStream.write(methodPool.size());

        // Write methods
        for (Info.MethodInfo info : methodPool) {
            outputStream.write(info.toByteCode());
        }

        // Write class attributes count
        outputStream.write(new byte[]{(byte) 0x00, (byte) 0x00});

        // Don't write attributes since JavaSST does not support class attributes

        return outputStream;
    }
}
