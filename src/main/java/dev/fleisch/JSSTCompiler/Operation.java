package dev.fleisch.JSSTCompiler;

/**
 * Class containing operation ENUMS.
 *
 * @author TillFleisch
 */
public abstract class Operation {
    /**
     * ENUM for binary Operations
     *
     * @author TillFleisch
     */
    enum Binary {
        ASSIGNMENT {
            @Override
            public int apply(int lhs, int rhs) throws IllegalCallerException {
                throw new IllegalCallerException("Assignment doe not support apply operation");
            }
        },
        EQUAL {
            @Override
            public int apply(int lhs, int rhs) {
                return lhs == rhs ? 1 : 0;
            }
        },
        GREATER {
            @Override
            public int apply(int lhs, int rhs) {
                return lhs > rhs ? 1 : 0;
            }
        },
        GREATER_EQUAL {
            @Override
            public int apply(int lhs, int rhs) {
                return lhs >= rhs ? 1 : 0;
            }
        },
        LESS {
            @Override
            public int apply(int lhs, int rhs) {
                return lhs < rhs ? 1 : 0;
            }
        },
        LESS_EQUAL {
            @Override
            public int apply(int lhs, int rhs) {
                return lhs <= rhs ? 1 : 0;
            }
        },
        ADDITION {
            @Override
            public int apply(int lhs, int rhs) {
                return lhs + rhs;
            }
        },
        SUBTRACTION {
            @Override
            public int apply(int lhs, int rhs) {
                return lhs - rhs;
            }
        },
        MULTIPLICATION {
            @Override
            public int apply(int lhs, int rhs) {
                return lhs * rhs;
            }
        },
        DIVISION {
            @Override
            public int apply(int lhs, int rhs) {
                return lhs / rhs;
            }
        };

        /**
         * Apply this operator to arguments
         *
         * @param lhs Left hand side expression
         * @param rhs Right hand side expression
         * @return result of the given Operation
         * @throws IllegalAccessException If the Operation does not support apply operation
         */
        public abstract int apply(int lhs, int rhs) throws IllegalAccessException;

        /**
         * Convert a keyword into it's binary operation counterpart
         *
         * @param keyword The keywords to convert
         * @return The Binary operand encoded by the Keyword
         * @throws ParserException If the Keyword is not a valid operand
         */
        public static Binary toBinaryOperation(Keyword keyword) throws ParserException {
            if (keyword.ordinal() < 15 || keyword.ordinal() > 25) throw new ParserException("Invalid binary operand");

            // Map keyword to BinaryOperation
            return Binary.values()[keyword.ordinal() - 15];
        }
    }


    /**
     * ENUM for unary operations
     *
     * @author TillFleisch
     */
    enum Unary {
        RETURN
    }
}
