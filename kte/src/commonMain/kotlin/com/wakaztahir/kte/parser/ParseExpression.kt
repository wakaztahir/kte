package com.wakaztahir.kte.parser

import com.wakaztahir.kte.model.ReferencedValue
import com.wakaztahir.kte.parser.stream.SourceStream
import com.wakaztahir.kte.parser.stream.increment

enum class OperatorAssociativity {
    LeftToRight,
    RightToLeft
}

enum class ArithmeticOperatorType(val char: Char, val associativity: OperatorAssociativity, val precedence: Int) {

    Plus('+', associativity = OperatorAssociativity.LeftToRight, precedence = 6) {
        override fun operate(value1: Int, value2: Int): Int {
            return value1 + value2
        }

        override fun operate(value1: Double, value2: Double): Double {
            return value1 + value2
        }
    },
    Minus('-', associativity = OperatorAssociativity.LeftToRight, precedence = 6) {
        override fun operate(value1: Int, value2: Int): Int {
            return value1 - value2
        }

        override fun operate(value1: Double, value2: Double): Double {
            return value1 - value2
        }
    },
    Divide('/', associativity = OperatorAssociativity.LeftToRight, precedence = 4) {
        override fun operate(value1: Int, value2: Int): Int {
            return value1 / value2
        }

        override fun operate(value1: Double, value2: Double): Double {
            return value1 / value2
        }
    },
    Multiply('*', associativity = OperatorAssociativity.LeftToRight, precedence = 4) {
        override fun operate(value1: Int, value2: Int): Int {
            return value1 * value2
        }

        override fun operate(value1: Double, value2: Double): Double {
            return value1 * value2
        }
    },
    Mod('%', associativity = OperatorAssociativity.LeftToRight, precedence = 4) {
        override fun operate(value1: Int, value2: Int): Int {
            return value1 % value2
        }

        override fun operate(value1: Double, value2: Double): Double {
            return value1 % value2
        }
    };

    abstract fun operate(value1: Int, value2: Int): Int
    abstract fun operate(value1: Double, value2: Double): Double

}

internal fun SourceStream.parseArithmeticOperator(): ArithmeticOperatorType? {
    val result = when (currentChar) {
        '+' -> ArithmeticOperatorType.Plus
        '-' -> ArithmeticOperatorType.Minus
        '/' -> ArithmeticOperatorType.Divide
        '*' -> ArithmeticOperatorType.Multiply
        '%' -> ArithmeticOperatorType.Mod
        else -> null
    }
    if (result != null) incrementPointer()
    return result
}

private class ValueAndOperatorStack {

    private val container = mutableListOf<Any>()

    fun isEmpty(): Boolean = container.isEmpty()

    fun putAllInto(other: ValueAndOperatorStack) {
        for (i in container.size - 1 downTo 0) other.container.add(container[i])
    }

    fun putValue(value: ReferencedValue) {
        container.add(value)
    }

    fun putOperator(value: ArithmeticOperatorType) {
        container.add(value)
    }

    fun putCharacter(bracket: Char) {
        container.add(bracket)
    }

    fun peakOperator(): ArithmeticOperatorType? {
        return container.lastOrNull()?.let { it as? ArithmeticOperatorType }
    }

    fun peakValue(): ReferencedValue? {
        return container.lastOrNull()?.let { it as? ReferencedValue }
    }

    fun peakChar(): Char? {
        return container.lastOrNull()?.let { it as? Char }
    }

    fun popOperator(): ArithmeticOperatorType {
        return container.removeLast() as ArithmeticOperatorType
    }

    fun popValue(): ReferencedValue {
        return container.removeLast() as ReferencedValue
    }

    fun popChar(): Char {
        return container.removeLast() as Char
    }

    fun toExpression(): ExpressionValue? {
        val stack = ValueAndOperatorStack()
        while (container.isNotEmpty()) {
            when (val item = container.removeFirst()) {
                is ArithmeticOperatorType -> {
                    val second = stack.container.removeLast() as ReferencedValue
                    val first = stack.container.removeLast() as ReferencedValue
                    stack.putValue(
                        ExpressionValue(
                            first = first,
                            operatorType = item,
                            second = second,
                        )
                    )
                }

                is Char -> {

                }

                is ReferencedValue -> {
                    stack.putValue(item)
                }

                else -> {
                    throw IllegalStateException("Unknown value type found in stack")
                }
            }
        }
        return stack.peakValue()?.let { it as? ExpressionValue }?.also {
            println("Expression : $it")
        }
    }

}

private fun SourceStream.parseValueAndOperator(): Pair<ReferencedValue, ArithmeticOperatorType?>? {
    val firstValue = parseDynamicProperty()
    if (firstValue != null) {
        val pointerAfterFirstValue = pointer
        increment(' ')
        return if (increment('@')) {
            val arithmeticOperator = parseArithmeticOperator()
            if (arithmeticOperator != null) {
                increment(' ')
                Pair(firstValue, arithmeticOperator)
            } else {
                setPointerAt(pointerAfterFirstValue)
                Pair(firstValue, null)
            }
        } else {
            setPointerAt(pointerAfterFirstValue)
            Pair(firstValue, null)
        }
    }
    return null
}

private fun SourceStream.parseExpressionWith(
    stack: ValueAndOperatorStack,
    final: ValueAndOperatorStack
) {
    while (!hasEnded) {
        val valueAndOp = parseValueAndOperator()
        if (valueAndOp != null) {
            final.putValue(valueAndOp.first)
            val operator = valueAndOp.second
            if (operator != null) {
                if (stack.isEmpty() || stack.peakChar() == '(') {
                    stack.putOperator(operator)
                } else if (stack.peakOperator() == null || operator.precedence > stack.peakOperator()!!.precedence) {
                    while (!stack.isEmpty() && stack.peakOperator() != null) {
                        if (operator.precedence > stack.peakOperator()!!.precedence) {
                            final.putOperator(stack.popOperator())
                        } else if (operator.precedence == stack.peakOperator()!!.precedence) {
                            when (operator.associativity) {
                                OperatorAssociativity.LeftToRight -> final.putOperator(stack.popOperator())
                                OperatorAssociativity.RightToLeft -> {
                                    // do nothing
                                }
                            }
                        }
                    }
                    stack.putOperator(operator)
                } else if (operator.precedence < stack.peakOperator()!!.precedence) {
                    stack.putOperator(operator)
                } else if (operator.precedence == stack.peakOperator()!!.precedence) {
                    when (operator.associativity) {
                        OperatorAssociativity.LeftToRight -> final.putOperator(stack.popOperator())
                        OperatorAssociativity.RightToLeft -> {
                            // do nothing
                        }
                    }
                    stack.putOperator(operator)
                } else {
                    throw IllegalStateException("no condition satisfied")
                }
            } else {
                break
            }
        } else {
            if (currentChar == '(') {
                stack.putCharacter(currentChar)
                incrementPointer()
            } else if (currentChar == ')') {
                var found = false
                while (!found) {
                    if (stack.peakOperator() != null) {
                        final.putOperator(stack.popOperator())
                    } else if (stack.peakChar() != null) {
                        if (stack.peakChar() == '(') found = true
                        final.putCharacter(stack.popChar())
                    }
                }
                incrementPointer()
            } else {
                break
            }
        }
    }
    stack.putAllInto(final)
}

internal fun SourceStream.parseExpression(): ReferencedValue? {
    val valueAndOp = parseValueAndOperator()
    if (valueAndOp != null) {
        return if (valueAndOp.second != null) {

            val stack = ValueAndOperatorStack()
            stack.putOperator(valueAndOp.second!!)
            val final = ValueAndOperatorStack()
            final.putValue(valueAndOp.first)
            parseExpressionWith(stack = stack, final = final)
            final.toExpression()

        } else {
            valueAndOp.first
        }
    }
    return null
}