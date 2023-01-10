package dev.fleisch.JSSTCompiler.ByteCodeGenerator;

import dev.fleisch.JSSTCompiler.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;


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
    public ByteCodeGenerator(Objekt.Clasz clasz) throws Exception {
        this.clasz = clasz;

        // Add the class to the pool
        constantPool.add(clasz);

        // add object class to be used as superclass
        constantPool.add(objectClass);

        // Add elements from the symbolTable
        for (Objekt objekt : clasz.getSymbolTable()) {

            // Add constants to the field pool
            if (objekt instanceof Objekt.Constant) {
                fieldPool.add((Objekt.Constant) objekt, clasz);

                // Add it to the constant pool if it's larger than 2 bytes
                int constant = ((Objekt.Constant) objekt).getValue();
                if (constant > 32767 || constant < -32767)
                    constantPool.add(constant);
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

        // Add all variables into the constant pool
        for (Objekt objekt : clasz.getSymbolTable()) {
            // add methods to the method Pool
            if (objekt instanceof Objekt.Procedure) {
                Node ast = ((Objekt.Procedure) objekt).getAbstractSyntaxTree();

                ast.traverse(new Node.TraverseCallback() {
                    @Override
                    public void onTraverse(Node node) {
                        if (node instanceof Node.ConstantNode) {
                            int constant = ((Node.ConstantNode) node).getValue();

                            // add the constant if it's to large for SIPUSH
                            if (constant > 32767 || constant < -32767)
                                constantPool.add(constant);
                        }
                    }
                });
            }
        }

        // Add methods (method information \w code)
        for (Objekt objekt : clasz.getSymbolTable()) {
            // add methods to the method Pool
            if (objekt instanceof Objekt.Procedure) {
                methodPool.add((Objekt.Procedure) objekt);
            }
        }

        // Create mock default constructor
        Objekt.Procedure defaultConstructor = new Objekt.Procedure("<init>", new LinkedList<>(), Type.VOID);

        // Create a mock super procedure with class object class reference
        Objekt.Procedure mockProcedure = new Objekt.Procedure("<init>", new LinkedList<>(), Type.VOID);
        constantPool.add(mockProcedure, objectClass);

        // Create mock procedure call
        Node.ProcedureCallNode mockCall = new Node.ProcedureCallNode("<init>", new Node.StatementSequenceNode(new LinkedList<>()), new CodePosition(0, 0));
        mockCall.setSymbolTableEntry(mockProcedure);

        // Add final variable initialization
        Node.StatementSequenceNode statementSequence = initializeGlobals(clasz);

        // Add mock super call at start of procedure
        statementSequence.getStatements().add(0, mockCall);

        // Add default constructor
        defaultConstructor.setAbstractSyntaxTree(statementSequence);
        methodPool.add(defaultConstructor);
    }

    /**
     * Generates an AST which initializes non-static final variables.
     *
     * @param clasz Class for which finals will be initialized
     * @return StatementSequenceNode containing assignments
     */

    Node.StatementSequenceNode initializeGlobals(Objekt.Clasz clasz) {
        LinkedList<Node> output = new LinkedList<>();

        // Find all final variables and create mock assignments
        for (Objekt objekt : clasz.getSymbolTable()) {
            // Add constants to the field pool
            if (objekt instanceof Objekt.Constant) {
                Objekt.Constant constant = (Objekt.Constant) objekt;

                // Mock identifier for variable being assigned
                Node.IdentifierNode assignee = new Node.IdentifierNode(constant.getName(), new CodePosition(0, 0));
                assignee.setSymbolTableEntry(constant);

                // Create mock variable assignment
                output.add(new Node.BinaryOperationNode(assignee,
                        new Node.ConstantNode(constant.getValue(), new CodePosition(0, 0)),
                        Operation.Binary.ASSIGNMENT,
                        new CodePosition(0, 0)));
            }
        }

        return new Node.StatementSequenceNode(output);
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
