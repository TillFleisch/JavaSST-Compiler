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
    public void add(Objekt.Procedure procedure) throws IOException {

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
    Info.AttributeInfo.CodeAttribute generateCodeAttribute(Objekt.Procedure procedure) throws IOException {

        // Constant name
        constantPool.add(new Info.ConstantPoolInfo.UTF8Info("Code"));
        int nameIndex = constantPool.size();

        // Lists containing constants&variables to determine their indices
        LinkedList<Objekt.Parameter> localVariableIndices = new LinkedList<>();

        // Find all variables within the symbol table
        for (Objekt objekt : procedure.getSymbolTable()) {
            if (objekt instanceof Objekt.Parameter) {
                localVariableIndices.add((Objekt.Parameter) objekt);
            }
        }

        // Determine how many local variables exist
        int numberOfVariables = procedure.getParameterList().size() + localVariableIndices.size();

        // Translate procedure ast/symbol-table into ByteCode

        ByteArrayOutputStream codeStream = new ByteArrayOutputStream();
        codeStream.write(toByteCode(procedure.getAbstractSyntaxTree(), localVariableIndices));

        if (procedure.getReturnType() == Type.VOID) {
            // For void methods add a return just in case (implicit return)
            codeStream.write(ByteCode.RETURN.getCode());
        } else {
            // otherwise add a mock return (last statement must be return) even if it is not reachable
            codeStream.write(ByteCode.ICONST_0.getCode());
            codeStream.write(ByteCode.IRETURN.getCode());
        }

        // TODO: determine max stack size
        return new Info.AttributeInfo.CodeAttribute(nameIndex, 0xFFFF, numberOfVariables, codeStream.toByteArray());
    }


    /**
     * Translates an expression given by an AST into ByteCode
     *
     * @param node Expression to translate
     * @return Equivalent ByteCode
     */
    public byte[] toByteCode(Node node, LinkedList<Objekt.Parameter> localVariableIndices) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Translate sequences
        if (node instanceof Node.StatementSequenceNode) {
            Node.StatementSequenceNode statementSequenceNode = (Node.StatementSequenceNode) node;
            ByteArrayOutputStream sequenceStream = new ByteArrayOutputStream();

            // go through all statements and translate them
            for (Node statement : statementSequenceNode.getStatements()) {
                // Translate statement
                sequenceStream.write(toByteCode(statement, localVariableIndices));
            }

            // Write statements to output
            outputStream.write(sequenceStream.toByteArray());

            return outputStream.toByteArray();
        }

        // Translate unary operations
        if (node instanceof Node.UnaryOperationNode) {
            Node.UnaryOperationNode unaryOperationNode = (Node.UnaryOperationNode) node;

            // Translate return statements
            if (unaryOperationNode.getOperation() == Operation.Unary.RETURN) {
                if (unaryOperationNode.getLeft() == null) {
                    // Return void
                    outputStream.write(ByteCode.RETURN.getCode());
                } else {
                    // Return parameter int (load int recursively)
                    outputStream.write(toByteCode(unaryOperationNode.getLeft(), localVariableIndices));
                    // Return value
                    outputStream.write(ByteCode.IRETURN.getCode());
                }
            }
            return outputStream.toByteArray();
        }

        // Translate constant values
        if (node instanceof Node.ConstantNode) {
            Node.ConstantNode constantNode = (Node.ConstantNode) node;

            // Push the constant value onto the stack
            outputStream.write(ByteCode.BIPUSH.getCode());
            outputStream.write(constantNode.getValue());
            return outputStream.toByteArray();
            // TODO: figure out how constant larger than 1 byte work
        }

        // Translate identifiers (load operations)
        if (node instanceof Node.IdentifierNode) {
            return toByteCode((Node.IdentifierNode) node, localVariableIndices);
        }

        // Translate binary operation Nodes
        if (node instanceof Node.BinaryOperationNode) {
            return toByteCode((Node.BinaryOperationNode) node, localVariableIndices);
        }

        // Translate if/else statements
        if (node instanceof Node.IfNode) {
            return toByteCode((Node.IfNode) node, localVariableIndices);
        }

        // Translate while node
        if (node instanceof Node.WhileNode) {
            return toByteCode((Node.WhileNode) node, localVariableIndices);
        }

        // Translate procedure calls
        if (node instanceof Node.ProcedureCallNode) {
            return toByteCode((Node.ProcedureCallNode) node, localVariableIndices);
        }

        throw new UnsupportedOperationException(String.valueOf(node.getClass()));
    }

    /**
     * Translates a procedureCallNode into ByteCode
     *
     * @param procedureCallNode    ProcedureCallNode to translate
     * @param localVariableIndices LocalVariables available within the calling procedure
     * @return ByteCode representing the procedureCall node and subsequent nodes
     * @throws IOException on translations failure
     */
    byte[] toByteCode(Node.ProcedureCallNode procedureCallNode, LinkedList<Objekt.Parameter> localVariableIndices) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Node.StatementSequenceNode parameters = (Node.StatementSequenceNode) procedureCallNode.getLeft();

        // Translate all parameters and leave them on the stack
        for (Node statement : parameters.getStatements()) {
            outputStream.write(toByteCode(statement, localVariableIndices));
        }

        // Write invoke static (find index in constant Pool)
        outputStream.write(ByteCode.INVOKESTATIC.getCode());
        short methodIndex = (short) constantPool.getByReference(procedureCallNode.getSymbolTableEntry());
        outputStream.write(methodIndex >> 8);
        outputStream.write(methodIndex);

        return outputStream.toByteArray();
    }

    /**
     * Translates a whileNode into ByteCode
     * <p>
     *
     * @param whileNode            whileNode to translate
     * @param localVariableIndices LocalVariables available within the calling procedure
     * @return ByteCode representing the procedureCall node and subsequent nodes
     * @throws IOException on translations failure
     * @implNote for simplicity:<br>
     * Instead of resolving the condition we rely on binary-operation translation to take care of conditions (puts 1/0 on stack).<br>
     * We check if the stack holds value 1 and jump into the correct branch<br>
     * This approach also allows us to check the truthiness of a variable/value but adds unnecessary complexity for simple while conditions.<br>
     * </p>
     */
    byte[] toByteCode(Node.WhileNode whileNode, LinkedList<Objekt.Parameter> localVariableIndices) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Determine the conditioned branch to get it's size
        byte[] whileBranch = toByteCode(whileNode.getLeft(), localVariableIndices);

        // Add the constant 0 onto the stack (value we compare against, inverted -> if true we go out of the branch)
        outputStream.write(ByteCode.ICONST_0.getCode());

        // resolve the condition (this should put a value onto the stack)
        byte[] conditionCode = toByteCode(whileNode.getCondition(), localVariableIndices);
        outputStream.write(conditionCode);

        // compare value
        outputStream.write(ByteCode.IF_ICMPEQ.getCode());

        // Write jump offset (skip else branch)
        short elseBranchSize = (short) (whileBranch.length + 3 + 3); // + ifcmp + goto
        outputStream.write(elseBranchSize >> 8);
        outputStream.write(elseBranchSize);

        // Write the conditioned statements
        outputStream.write(whileBranch);

        // Write goto back to the if Statement (negative offset)
        outputStream.write(ByteCode.GOTO.getCode());
        short offset = (short) -(whileBranch.length + 3 + 1 + conditionCode.length); // ifcmp (+ 1 from incomplete goto)
        outputStream.write(offset >> 8);
        outputStream.write(offset);

        return outputStream.toByteArray();
    }

    /**
     * Translates a ifNode into ByteCode
     * <p>
     *
     * @param ifNode               ifNode to translate
     * @param localVariableIndices LocalVariables available within the calling procedure
     * @return ByteCode representing the procedureCall node and subsequent nodes
     * @throws IOException on translations failure
     * @implNote for simplicity:<br>
     * Instead of resolving the condition we rely on binary-operation translation to take care of conditions (puts 1/0 on stack).<br>
     * We check if the stack holds value 1 and jump into the correct branch<br>
     * This approach also allows us to check the truthiness of a variable/value but adds unnecessary complexity for simple if/else conditions.<br>
     * </p>
     */
    byte[] toByteCode(Node.IfNode ifNode, LinkedList<Objekt.Parameter> localVariableIndices) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Determines branches, such that sizes are known
        byte[] ifBranch = toByteCode(ifNode.getLeft(), localVariableIndices);
        byte[] elseBranch = toByteCode(ifNode.getRight(), localVariableIndices);

        // Add the constant 1 onto the stack (value we compare against)
        outputStream.write(ByteCode.ICONST_1.getCode());

        // resolve the condition (this should put a value onto the stack)
        outputStream.write(toByteCode(ifNode.getCondition(), localVariableIndices));

        // compare value
        outputStream.write(ByteCode.IF_ICMPEQ.getCode());

        // Write jump offset (skip else branch)
        short elseBranchSize = (short) (elseBranch.length + 3 + 3); // + ifcmp + goto
        outputStream.write(elseBranchSize >> 8);
        outputStream.write(elseBranchSize);

        // Write else branch
        outputStream.write(elseBranch);

        // Jump over if branch
        outputStream.write(ByteCode.GOTO.getCode());
        short ifBranchSize = (short) (ifBranch.length + 3); // goto
        outputStream.write(ifBranchSize >> 8);
        outputStream.write(ifBranchSize);

        // Write if branch
        outputStream.write(ifBranch);

        return outputStream.toByteArray();
    }

    /**
     * Translates a binaryOperationNode into ByteCode
     *
     * @param binaryOperationNode  binaryOperationNode to translate
     * @param localVariableIndices LocalVariables available within the calling procedure
     * @return ByteCode representing the procedureCall node and subsequent nodes
     * @throws IOException on translations failure
     * @implNote comparisons are implemented using ifcmp. They put the resulting value onto the stack.
     */
    byte[] toByteCode(Node.BinaryOperationNode binaryOperationNode, LinkedList<Objekt.Parameter> localVariableIndices) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Operation.Binary operation = binaryOperationNode.getOperation();

        // Translate assignment
        if (operation == Operation.Binary.ASSIGNMENT) {

            // Get variable being assigned
            Objekt.Parameter assignee = (Objekt.Parameter) ((Node.IdentifierNode) binaryOperationNode.getLeft()).getSymbolTableEntry();

            // resolve assignment
            outputStream.write(toByteCode(binaryOperationNode.getRight(), localVariableIndices));

            // Store value, differentiate between local variable and static class variable
            if (localVariableIndices.contains(assignee)) {
                // Reference local variable via index
                outputStream.write(ByteCode.ISTORE.getCode());
                outputStream.write(localVariableIndices.indexOf(assignee));
            } else {
                // put static class variable via put static and constant pool reference
                outputStream.write(ByteCode.PUTSTATIC.getCode());
                outputStream.write(constantPool.getByReference(assignee) >> 8);
                outputStream.write(constantPool.getByReference(assignee));
            }
            return outputStream.toByteArray();
        }


        Node left = binaryOperationNode.getLeft();
        Node right = binaryOperationNode.getRight();

        // Resolve left&right part of the expression (expression leave their value on the stack)
        outputStream.write(toByteCode(left, localVariableIndices));
        outputStream.write(toByteCode(right, localVariableIndices));

        // Write operation
        switch (operation) {
            case ADDITION -> outputStream.write(ByteCode.IADD.getCode());
            case SUBTRACTION -> outputStream.write(ByteCode.ISUB.getCode());
            case MULTIPLICATION -> outputStream.write(ByteCode.IMUL.getCode());
            case DIVISION -> outputStream.write(ByteCode.IDIV.getCode());
            case EQUAL -> outputStream.write(ByteCode.IF_ICMPEQ.getCode());
            case LESS -> outputStream.write(ByteCode.IF_ICMPLT.getCode());
            case LESS_EQUAL -> outputStream.write(ByteCode.IF_ICMPLE.getCode());
            case GREATER -> outputStream.write(ByteCode.IF_ICMPGT.getCode());
            case GREATER_EQUAL -> outputStream.write(ByteCode.IF_ICMPGE.getCode());
        }

        if (operation == Operation.Binary.EQUAL ||
                operation == Operation.Binary.LESS ||
                operation == Operation.Binary.LESS_EQUAL ||
                operation == Operation.Binary.GREATER ||
                operation == Operation.Binary.GREATER_EQUAL) {
            // Write the result to the stack

            // jump-offset 1 if condition true
            outputStream.write(0x00);
            outputStream.write(0x07);

            // default case (write 0) goto next (skip write 1)
            outputStream.write(ByteCode.ICONST_0.getCode());
            outputStream.write(ByteCode.GOTO.getCode());
            outputStream.write(0x00);
            outputStream.write(0x04);

            // jumped case (write 1)
            outputStream.write(ByteCode.ICONST_1.getCode());
        }

        return outputStream.toByteArray();
    }


    /**
     * Translates a identifierNode into ByteCode
     *
     * @param identifierNode       identifierNode to translate
     * @param localVariableIndices LocalVariables available within the calling procedure
     * @return ByteCode representing the procedureCall node and subsequent nodes
     */
    byte[] toByteCode(Node.IdentifierNode identifierNode, LinkedList<Objekt.Parameter> localVariableIndices) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Translate constant (constant values within the code)
        if (identifierNode.getSymbolTableEntry() instanceof Objekt.Constant) {
            Objekt.Constant constant = (Objekt.Constant) identifierNode.getSymbolTableEntry();

            // TODO: figure out how constant larger than 1 byte work

            // Push the constant value onto the stack
            outputStream.write(ByteCode.BIPUSH.getCode());
            outputStream.write(constant.getValue());
            return outputStream.toByteArray();
        }

        // Translate variables (load variable value onto stack)
        if (identifierNode.getSymbolTableEntry() instanceof Objekt.Parameter) {
            Objekt.Parameter variable = (Objekt.Parameter) identifierNode.getSymbolTableEntry();

            // Differentiate between local & static global variables
            if (localVariableIndices.contains(variable)) {
                // Reference local variable via index
                outputStream.write(ByteCode.ILOAD.getCode());
                outputStream.write(localVariableIndices.indexOf(variable));
            } else {
                // get static class variable via get static and constant pool reference
                outputStream.write(ByteCode.GETSTATIC.getCode());
                int staticIndex = constantPool.getByReference(variable); // get index from pool
                outputStream.write(staticIndex >> 8);
                outputStream.write(staticIndex);
            }
        }
        return outputStream.toByteArray();
    }
}

