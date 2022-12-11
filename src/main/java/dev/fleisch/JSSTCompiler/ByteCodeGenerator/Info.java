package dev.fleisch.JSSTCompiler.ByteCodeGenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Class containing Information classes used to translate into ByteCode
 *
 * @author TillFleisch
 */
public abstract class Info {

    /**
     * Class used for Field descriptions (i.e. variables)
     * <a href="https://docs.oracle.com/javase/specs/jvms/se6/html/ClassFile.doc.html#2877">Reference (field_info)</a>
     *
     * @author TillFleisch
     */
    public static class FieldInfo extends FieldBase {

        FieldInfo(int accessFlags, int nameIndex, int descriptorIndex, AttributeInfo[] attributes) {
            // field flags as public
            super(accessFlags, nameIndex, descriptorIndex, attributes);
        }
    }

    /**
     * Class used for Method description
     * <p>
     * Generated methods are public static by default (JavaSST does not allow customization)
     * <a href="https://docs.oracle.com/javase/specs/jvms/se6/html/ClassFile.doc.html#1513">Reference (method_info )</a>
     *
     * @author TillFleisch
     */
    public static class MethodInfo extends FieldBase {

        MethodInfo(int nameIndex, int descriptorIndex, AttributeInfo[] attributes) {
            // Method flags as public static
            super(0x0001 | 0x0008, nameIndex, descriptorIndex, attributes);
        }
    }

    /**
     * Base class for field Information.
     * FieldInfo and MethodInfo share the same structure and are derived from this class.
     *
     * @author TillFleisch
     */
    public static abstract class FieldBase implements Translation {

        /**
         * Access flags for this field
         */
        int accessFlags;

        /**
         * Index for the fields name within the constant pool
         */
        int nameIndex;

        /**
         * Index for the descriptor within the constant pool
         */
        int descriptorIndex;

        /**
         * The fields attributes
         */
        AttributeInfo[] attributes;


        /**
         * Constructor for FieldInformation objects
         *
         * @param accessFlags     Access flags for this field
         * @param nameIndex       Name index within the constant pool for this field
         * @param descriptorIndex Descriptor index within the constant pool for this field
         * @param attributes      Attributes used in this field
         */
        FieldBase(int accessFlags, int nameIndex, int descriptorIndex, AttributeInfo[] attributes) {
            this.accessFlags = accessFlags;
            this.nameIndex = nameIndex;
            this.descriptorIndex = descriptorIndex;
            this.attributes = attributes;
        }

        @Override
        public byte[] toByteCode() throws IOException {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // Write access flags (public)
            outputStream.write(accessFlags >> 8);
            outputStream.write(accessFlags);

            // Write name index
            outputStream.write(nameIndex >> 8);
            outputStream.write(nameIndex);

            // Write descriptor index
            outputStream.write(descriptorIndex >> 8);
            outputStream.write(descriptorIndex);

            // Write attribute count
            outputStream.write(attributes.length >> 8);
            outputStream.write(attributes.length);

            // Write attributes
            for (AttributeInfo attributeInfo : attributes) {
                outputStream.write(attributeInfo.toByteCode());
            }

            // return ByteCode array
            return outputStream.toByteArray();
        }
    }

    /**
     * Class containing different attributeInformation (i.e. used in field description)
     *
     * @author TillFleisch
     */
    public static abstract class AttributeInfo implements Translation {

        /**
         * Constant value attributes
         * <a href="https://docs.oracle.com/javase/specs/jvms/se6/html/ClassFile.doc.html#1405">Reference (ConstantValue_attribute)</a>
         *
         * @author TillFleisch
         */
        static class ConstantValueAttribute extends AttributeInfo implements Translation {

            /**
             * Index of the "ConstantValue" UTF8 entry within the constant pool
             */
            int attributeNameIndex;

            /**
             * Index of the constants value within the constant pool
             */
            int constantValueIndex;

            /**
             * Attribute length of ConstantValue Attributes
             */
            final int attributeLength = 2;

            /**
             * Constructor for constant value attributes
             *
             * @param attributeNameIndex index of the nameIndex within the constant pool
             * @param constantValueIndex index of the valueIndex within the constant pool
             */
            ConstantValueAttribute(int attributeNameIndex, int constantValueIndex) {
                this.attributeNameIndex = attributeNameIndex;
                this.constantValueIndex = constantValueIndex;
            }

            @Override
            public byte[] toByteCode() {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                // Write attributeNameIndex
                outputStream.write(attributeNameIndex >> 8);
                outputStream.write(attributeNameIndex);

                // Write attribute length
                outputStream.write(attributeLength >> 24);
                outputStream.write(attributeLength >> 16);
                outputStream.write(attributeLength >> 8);
                outputStream.write(attributeLength);

                // Write constant Value index
                outputStream.write(constantValueIndex >> 8);
                outputStream.write(constantValueIndex);

                return outputStream.toByteArray();
            }
        }

        /**
         * Code Attribute used in Method information
         * <a href="https://docs.oracle.com/javase/specs/jvms/se6/html/ClassFile.doc.html#1546">Reference(Code_attribute)</a>
         *
         * @author TillFleisch
         */
        static class CodeAttribute extends AttributeInfo {

            /**
             * Constant pool index containing UTF8 "Code"
             */
            int nameIndex;

            /**
             * Largest stack size during code execution
             */
            int maxStack;

            /**
             * Largest number of variables used during code execution
             */
            int maxLocals;

            /**
             * ByteCode representing the code within the method
             */
            byte[] code;


            /**
             * Constructor for basic Code attributes
             *
             * @param nameIndex Index to UTF8 "Code" entry within the constant Pool
             * @param maxStack  Maximum size of the stack during execution
             * @param maxLocals Maximum number of variables used during execution
             * @param code      ByteCode representing the function
             */
            CodeAttribute(int nameIndex, int maxStack, int maxLocals, byte[] code) {
                this.nameIndex = nameIndex;
                this.maxStack = maxStack;
                this.maxLocals = maxLocals;
                this.code = code;
            }

            @Override
            public byte[] toByteCode() throws IOException {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                // Write nameIndex
                outputStream.write(nameIndex >> 8);
                outputStream.write(nameIndex);

                // Create separate stream for content to determine size
                ByteArrayOutputStream contentStream = new ByteArrayOutputStream();

                // Write max stack size
                contentStream.write(maxStack >> 8);
                contentStream.write(maxStack);

                // Write max local variables
                contentStream.write(maxLocals >> 8);
                contentStream.write(maxLocals);

                // Write code length
                contentStream.write(code.length >> 24);
                contentStream.write(code.length >> 16);
                contentStream.write(code.length >> 8);
                contentStream.write(code.length);

                // Write code content
                contentStream.write(code);

                // Write exception table length (JavaSST does not support exceptions)
                contentStream.write(0x00);
                contentStream.write(0x00);

                // Write attribute count (this simple compiler does not support attributes)
                contentStream.write(0x00);
                contentStream.write(0x00);

                // Don't write attributes since we don't have any

                // Write attributeLength
                outputStream.write(contentStream.size() >> 24);
                outputStream.write(contentStream.size() >> 16);
                outputStream.write(contentStream.size() >> 8);
                outputStream.write(contentStream.size());

                // Write the content to the output stream
                outputStream.write(contentStream.toByteArray());

                // Return generated bytecode describing Code attribute
                return outputStream.toByteArray();
            }
        }
    }

    /**
     * Basic constant pool info.
     *
     * <a href="https://docs.oracle.com/javase/specs/jvms/se6/html/ClassFile.doc.html#20080">Reference (cp_info)</a>
     *
     * @author TillFleisch
     */
    public abstract static class ConstantPoolInfo implements Translation {

        /**
         * Tag describing type of constant pool entry
         */
        byte tag;

        /**
         * Constructor for constant pool entries
         *
         * @param tag Constant pool entry tag
         */
        public ConstantPoolInfo(int tag) {
            this.tag = (byte) tag;
        }


        /**
         * Class describing a class
         *
         * <a href="https://docs.oracle.com/javase/specs/jvms/se6/html/ClassFile.doc.html#1221">Reference (CONSTANT_Class_info)</a>
         *
         * @author TillFleisch
         */
        static class ClaszInfo extends ConstantPoolInfo {

            /**
             * The classes name index within the constant pool
             */
            int nameIndex;

            /**
             * Constructor for class objects
             *
             * @param nameIndex The class name index within the constant pool
             */
            ClaszInfo(int nameIndex) {
                super(7);
                this.nameIndex = nameIndex;
            }

            @Override
            public byte[] toByteCode() {
                return new byte[]{tag, (byte) (nameIndex >> 8), (byte) nameIndex};
            }
        }

        /***
         *Class describing UTF8 information within the constant pool
         *
         * <a href="https://docs.oracle.com/javase/specs/jvms/se6/html/ClassFile.doc.html#7963">Reference (CONSTANT_Utf8_info)</a>
         *
         * @author TillFleisch
         */
        static class UTF8Info extends ConstantPoolInfo {

            /**
             * String to represent within the constant pool
             */
            String string;

            /**
             * Constructor for UTF8 constant pool entries
             *
             * @param string content
             */
            UTF8Info(String string) {
                super(1);
                this.string = string;
            }

            @Override
            public byte[] toByteCode() {

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                // Add all chars to the stream
                for (char c : string.toCharArray()) {
                    // Translate according to spec

                    if (c >= '\u0001' && c <= '\u007F') {
                        // (mask bits 0-6)
                        byteArrayOutputStream.write(c & 0b01111111);

                    } else if ((c >= (int) '\u0080' && c <= (int) '\u07FF') || c == '\u0000') {
                        // 2 byte representation
                        // x (mask bits 10-6)
                        byteArrayOutputStream.write(0b11000000 | ((c >> 6) & 0b00011111));

                        // y (mask bits 5-0)
                        byteArrayOutputStream.write(0b10000000 | (c & 0b00111111));
                    } else {
                        // 3 byte representation
                        // x (mask bits 15-12)
                        byteArrayOutputStream.write(0b11100000 | ((c >> 12) & 0b00001111));

                        // y (mask bits 10-6)
                        byteArrayOutputStream.write(0b10000000 | ((c >> 6) & 0b00011111));

                        // z (mask bits 5-0)
                        byteArrayOutputStream.write(0b10000000 | (c & 0b00111111));
                    }
                }
                byte[] bytes = byteArrayOutputStream.toByteArray();

                // Write tag and size followed by string content
                byte[] output = new byte[bytes.length + 3];
                output[0] = tag;
                output[1] = (byte) (bytes.length >> 8);
                output[2] = (byte) bytes.length;

                System.arraycopy(bytes, 0, output, 3, bytes.length);
                return output;
            }
        }

        /**
         * Class describing Integer Information within the constant pool
         * <p>
         * <a href="https://docs.oracle.com/javase/specs/jvms/se6/html/ClassFile.doc.html#1221">Reference (CONSTANT_Integer_info)</a>
         *
         * @author TillFleisch
         */
        static class IntegerInfo extends ConstantPoolInfo {

            /**
             * The integers value
             */
            int value;

            /**
             * Constructor for integer entries within the constant pool
             *
             * @param value The integers value
             */
            public IntegerInfo(int value) {
                super(3);
                this.value = value;
            }

            @Override
            public byte[] toByteCode() {
                return new byte[]{tag, (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value};
            }

        }

        /**
         * Constructor for field reference information
         * <p>
         * CONSTANT_Fieldref_info
         */
        public static class FieldReferenceInfo extends ConstantPoolInfo {

            /**
             * Index of the Class containing the field within the constant pool
             */
            int classIndex;

            /**
             * Index of the nameAndType entry within the constant pool
             */
            int nameAndTypeIndex;

            /**
             * Constructor for field reference information
             *
             * @param classIndex       Index of the Class containing the field within the constant pool
             * @param nameAndTypeIndex Index of the nameAndType entry within the constant pool
             */
            FieldReferenceInfo(int classIndex, int nameAndTypeIndex) {
                super(9);
                this.classIndex = classIndex;
                this.nameAndTypeIndex = nameAndTypeIndex;
            }

            @Override
            public byte[] toByteCode() {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                // Write the tag
                outputStream.write(tag);

                // Write the class index
                outputStream.write(classIndex >> 8);
                outputStream.write(classIndex);

                // Write name and type index
                outputStream.write(nameAndTypeIndex >> 8);
                outputStream.write(nameAndTypeIndex);

                return outputStream.toByteArray();
            }
        }

        /**
         * Constructor for method reference information
         * <p>
         * CONSTANT_Fieldref_info
         */
        public static class MethodReferenceInfo extends ConstantPoolInfo {

            /**
             * Index of the Class containing the field within the constant pool
             */
            int classIndex;

            /**
             * Index of the nameAndType entry within the constant pool
             */
            int nameAndTypeIndex;

            /**
             * Constructor for field reference information
             *
             * @param classIndex       Index of the Class containing the field within the constant pool
             * @param nameAndTypeIndex Index of the nameAndType entry within the constant pool
             */
            MethodReferenceInfo(int classIndex, int nameAndTypeIndex) {
                super(10);
                this.classIndex = classIndex;
                this.nameAndTypeIndex = nameAndTypeIndex;
            }

            @Override
            public byte[] toByteCode() {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                // Write the tag
                outputStream.write(tag);

                // Write the class index
                outputStream.write(classIndex >> 8);
                outputStream.write(classIndex);

                // Write name and type index
                outputStream.write(nameAndTypeIndex >> 8);
                outputStream.write(nameAndTypeIndex);

                return outputStream.toByteArray();
            }
        }

        /**
         * Class describing Name and Type info
         *
         * <a href="https://docs.oracle.com/javase/specs/jvms/se6/html/ClassFile.doc.html#1327">CONSTANT_NameAndType_info</a>
         *
         * @author TillFleisch
         */
        public static class NameAndTypeInfo extends ConstantPoolInfo {

            /**
             * NameInfo for NameAndType info
             */
            int nameIndex;

            /**
             * Descriptor for NameAndType info
             */
            int descriptorIndex;

            /**
             * Constructor for NameAndTypeInfo
             */
            public NameAndTypeInfo(int nameIndex, int descriptorIndex) {
                super(12);
                this.nameIndex = nameIndex;
                this.descriptorIndex = descriptorIndex;
            }

            @Override
            public byte[] toByteCode() {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                // Write tag
                outputStream.write(tag);

                // Write nameIndex
                outputStream.write(nameIndex >> 8);
                outputStream.write(nameIndex);

                // Write descriptorIndex
                outputStream.write(descriptorIndex >> 8);
                outputStream.write(descriptorIndex);

                return outputStream.toByteArray();
            }
        }
    }


    /**
     * Interface for ByteCode Translation
     *
     * @author TillFleisch
     */
    interface Translation {

        /**
         * Generates a ByteCode representation.
         *
         * @return ByteCode representation of the Information
         * @throws IOException On translation failure
         */
        byte[] toByteCode() throws IOException;
    }
}
