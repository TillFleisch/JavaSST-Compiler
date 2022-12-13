package dev.fleisch.JSSTCompiler.ByteCodeGenerator;

/**
 * ENUM for ByteCodes used in the JVM
 *
 * @author TillFleisch
 */
public enum ByteCode {
    ICONST_0(0x3),
    ICONST_1(0x4),
    IADD(0x60),
    ISUB(0x64),
    IMUL(0x68),
    IDIV(0x6c),
    IF_ICMPEQ(0x9f),
    IF_ICMPNE(0xa0),
    IF_ICMPLT(0xa1),
    IF_ICMPGE(0xa2),
    IF_ICMPGT(0xa3),
    IF_ICMPLE(0xa4),
    ILOAD(0x15),
    ISTORE(0x36),
    GETSTATIC(0xb2),
    PUTSTATIC(0xb3),
    BIPUSH(0x10),
    SIPUSH(0x11),
    LDC(0x12),
    GOTO(0xa7),
    INVOKESTATIC(0xb8),
    RETURN(0xb1),
    IRETURN(0xac);

    /**
     * Byte representing code
     */
    private final byte code;

    /**
     * Constructor with byte argument
     *
     * @param code byte representing this instruction
     */
    ByteCode(int code) {
        this.code = (byte) code;
    }

    /**
     * Get code representing this instruction
     *
     * @return byte code representing the enum
     */
    public byte getCode() {
        return code;
    }
}
