package com.wakaztahir.kate.parser

import com.wakaztahir.kate.dsl.ScopedModelObject
import com.wakaztahir.kate.model.*
import com.wakaztahir.kate.model.ConditionType
import com.wakaztahir.kate.model.model.*
import com.wakaztahir.kate.parser.stream.*
import com.wakaztahir.kate.parser.stream.increment
import com.wakaztahir.kate.parser.stream.parseTextWhile
import com.wakaztahir.kate.parser.variable.isVariableName
import com.wakaztahir.kate.parser.variable.parseVariableName
import com.wakaztahir.kate.parser.variable.parseVariableReference
import com.wakaztahir.kate.tokenizer.NodeTokenizer

sealed interface ForLoop : BlockContainer {

    val blockValue: ForLoopLazyBlockSlice

    val model: MutableKATEObject
        get() = blockValue.model

    override fun getBlockValue(model: KATEObject): LazyBlock {
        return blockValue
    }

    fun iterate(block: (iteration: Int) -> Unit)

    override fun generateTo(block: LazyBlock, destination: DestinationStream) {
        blockValue.hasBroken = false
        iterate {
            blockValue.generateTo(destination = destination)
        }
    }

    class ConditionalFor(
        val condition: ReferencedOrDirectValue,
        override val blockValue: ForLoopLazyBlockSlice
    ) : ForLoop {
        override fun <T> selectNode(tokenizer: NodeTokenizer<T>) = tokenizer.conditionalFor
        override fun iterate(block: (iteration: Int) -> Unit) {
            var i = 0
            while (!blockValue.hasBroken && (condition.asNullablePrimitive(model) as BooleanValue).value) {
                model.removeAll()
                block(i)
                i++
            }
        }
    }

    class IterableFor(
        val indexConstName: String?,
        val elementConstName: String,
        val listProperty: ReferencedOrDirectValue,
        override val blockValue: ForLoopLazyBlockSlice
    ) : ForLoop {

        override fun <T> selectNode(tokenizer: NodeTokenizer<T>): T = tokenizer.iterableFor

        private fun store(value: Int) {
            if (indexConstName != null) {
                model.insertValue(indexConstName, value)
            }
        }

        private fun store(value: KATEValue) {
            model.insertValue(elementConstName, value)
        }

        private fun remove() {
            if (indexConstName != null) {
                model.removeKey(indexConstName)
            }
            model.removeKey(elementConstName)
        }

        override fun iterate(block: (iteration: Int) -> Unit) {
            var index = 0
            val iterable = listProperty.asNullableList(model)
                ?: throw IllegalStateException("list property is not iterable in for loop")
            val total = iterable.collection.size
            while (!blockValue.hasBroken && index < total) {
                model.removeAll()
                store(index)
                store(iterable.collection.getOrElse(index) {
                    throw IllegalStateException("element at $index in for loop not found")
                })
                block(index)
                index++
            }
            remove()
        }
    }

    class NumberedFor(
        val variableName: String,
        val initializer: ReferencedOrDirectValue,
        val conditionType: ConditionType,
        val conditional: ReferencedOrDirectValue,
        val arithmeticOperatorType: ArithmeticOperatorType,
        val incrementer: ReferencedOrDirectValue,
        override val blockValue: ForLoopLazyBlockSlice
    ) : ForLoop {

        override fun <T> selectNode(tokenizer: NodeTokenizer<T>): T = tokenizer.numberedFor

        private fun ReferencedOrDirectValue.intVal(context: MutableKATEObject): Int {
            (asNullablePrimitive(context)?.value as? Int)?.let { return it }
                ?: throw IllegalStateException("for loop variable must be an integer")
        }

        private fun MutableKATEObject.storeIndex(value: Int) {
            insertValue(variableName, value)
        }

        private fun MutableKATEObject.removeIndex() {
            removeKey(variableName)
        }

        override fun iterate(block: (iteration: Int) -> Unit) {
            var i = initializer.intVal(model)
            val conditionValue = conditional.intVal(model)
            val incrementerValue = incrementer.intVal(model)
            while (!blockValue.hasBroken && conditionType.verifyCompare(i.compareTo(conditionValue))) {
                model.removeAll()
                model.storeIndex(i)
                block(i)
                i = arithmeticOperatorType.operate(i, incrementerValue)
            }
            model.removeIndex()
        }
    }

}

class ForLoopBreak(val slice : ForLoopLazyBlockSlice) : CodeGen{
    override fun <T> selectNode(tokenizer: NodeTokenizer<T>): T = tokenizer.forLoopBreak
    override fun generateTo(block: LazyBlock, destination: DestinationStream) {
        slice.hasBroken = true
    }
}

class ForLoopLazyBlockSlice(
    parentBlock: LazyBlock,
    startPointer: Int,
    length: Int,
    blockEndPointer: Int,
    parent: ScopedModelObject,
    isDefaultNoRaw: Boolean,
    indentationLevel: Int
) : LazyBlockSlice(
    parentBlock = parentBlock,
    startPointer = startPointer,
    length = length,
    model = parent,
    blockEndPointer = blockEndPointer,
    isDefaultNoRaw = isDefaultNoRaw,
    indentationLevel = indentationLevel
) {

    var hasBroken = false

    fun SourceStream.parseBreakForAtDirective(): ForLoopBreak? {
        return if (currentChar == '@' && increment("@breakfor")) {
            ForLoopBreak(this@ForLoopLazyBlockSlice)
        } else {
            null
        }
    }

    override fun generateTo(destination: DestinationStream) {
        source.setPointerAt(startPointer)
        for(gen in parsedBlock.codeGens){
            if(hasBroken) break
            gen.gen.generateTo(this,destination)
        }
        source.setPointerAt(blockEndPointer)
    }

    override fun parseNestedAtDirective(block: LazyBlock): CodeGen? {
        source.parseBreakForAtDirective()?.let { return it }
        return super.parseNestedAtDirective(block)
    }

}

private fun LazyBlock.parseForBlockValue(): ForLoopLazyBlockSlice {
    val slice = parseBlockSlice(
        startsWith = "@for",
        endsWith = "@endfor",
        isDefaultNoRaw = isDefaultNoRaw,
        inheritModel = false
    )
    return ForLoopLazyBlockSlice(
        parentBlock = this,
        startPointer = slice.startPointer,
        length = slice.length,
        blockEndPointer = slice.blockEndPointer,
        parent = slice.model as ScopedModelObject,
        isDefaultNoRaw = slice.isDefaultNoRaw,
        indentationLevel = indentationLevel + 1
    ).also { it.prepare() }
}

private fun LazyBlock.parseConditionalFor(): ForLoop.ConditionalFor? {
    val condition = parseCondition(parseDirectRefs = false)
    if (condition != null) {
        source.increment(')')
        source.increment(' ')
        val blockValue = this@parseConditionalFor.parseForBlockValue()
        return ForLoop.ConditionalFor(
            condition = condition,
            blockValue = blockValue
        )
    }
    return null
}

private fun LazyBlock.parseListReferencedValue(parseDirectRefs: Boolean): ReferencedOrDirectValue? {
    parseListDefinition()?.let { return it }
    parseMutableListDefinition()?.let { return it }
    parseVariableReference(parseDirectRefs = parseDirectRefs)?.let { return it }
    return null
}

private fun LazyBlock.parseIterableForLoopAfterVariable(variableName: String): ForLoop.IterableFor? {
    var secondVariableName: String? = null
    if (source.increment(',')) {
        secondVariableName = source.parseTextWhile { currentChar.isVariableName() }
    }
    source.escapeSpaces()
    if (source.increment(':')) {
        source.escapeSpaces()
        val referencedValue = parseListReferencedValue(parseDirectRefs = true)
        source.escapeSpaces()
        if (!source.increment(')')) {
            throw IllegalStateException("expected ) , got ${source.currentChar}")
        }
        source.increment(' ')
        val blockValue = this@parseIterableForLoopAfterVariable.parseForBlockValue()
        if (referencedValue != null) {
            return ForLoop.IterableFor(
                indexConstName = secondVariableName,
                elementConstName = variableName,
                listProperty = referencedValue,
                blockValue = blockValue
            )
        }
    }
    return null
}

private class NumberedForLoopIncrementer(
    val operatorType: ArithmeticOperatorType,
    val incrementerValue: ReferencedOrDirectValue
)

private fun LazyBlock.parseNumberOrReference(parseDirectRefs: Boolean): ReferencedOrDirectValue? {
    source.parseNumberValue()?.let { return it }
    parseVariableReference(parseDirectRefs = parseDirectRefs)?.let { return it }
    return null
}

private fun LazyBlock.parseNumberedForLoopIncrementer(variableName: String): NumberedForLoopIncrementer {
    val incrementalConst = source.parseTextWhile { currentChar.isVariableName() }
    if (incrementalConst == variableName) {
        val operator = source.parseArithmeticOperator()
        if (operator != null) {
            val singleIncrement =
                if (operator == ArithmeticOperatorType.Plus || operator == ArithmeticOperatorType.Minus) operator else null
            val singleIncrementerChar =
                if (singleIncrement == ArithmeticOperatorType.Plus) '+' else if (singleIncrement == ArithmeticOperatorType.Minus) '-' else null
            val incrementer = if (singleIncrement != null && source.increment(singleIncrementerChar!!)) {
                IntValue(1)
            } else {
                parseNumberOrReference(parseDirectRefs = true)
            }
                ?: throw IllegalStateException("expected number property or value or '+' or '-' , got ${source.currentChar} in for loop incrementer")
            return NumberedForLoopIncrementer(
                operatorType = operator,
                incrementerValue = incrementer
            )
        } else {
            throw IllegalStateException("expected '+','-','/','*','%' , got ${source.currentChar} in for loop condition")
        }
    } else {
        throw IllegalStateException("incremental variable is different : $incrementalConst != $variableName")
    }
}

private fun LazyBlock.parseNumberedForLoopAfterVariable(variableName: String): ForLoop.NumberedFor? {
    if (source.increment('=')) {
        source.escapeSpaces()
        val initializer = parseAnyExpressionOrValue()
            ?: throw IllegalStateException("unexpected ${source.currentChar} , expected a number or property")
        if (source.increment(';')) {
            val conditionalConst = source.parseTextWhile { currentChar.isVariableName() }
            if (conditionalConst == variableName) {
                val conditionType = source.parseConditionType()
                    ?: throw IllegalStateException("expected conditional operator , got ${source.currentChar}")
                val conditional = parseAnyExpressionOrValue(
                    parseDirectRefs = true
                ) ?: throw IllegalStateException("expected number property of value got ${source.currentChar}")
                if (source.increment(';')) {
                    val incrementer = parseNumberedForLoopIncrementer(variableName)
                    if (!source.increment(')')) {
                        throw IllegalStateException("expected ) , got ${source.currentChar}")
                    }
                    source.increment(' ')
                    val blockValue = this@parseNumberedForLoopAfterVariable.parseForBlockValue()
                    return ForLoop.NumberedFor(
                        variableName = variableName,
                        initializer = initializer,
                        conditionType = conditionType,
                        conditional = conditional,
                        arithmeticOperatorType = incrementer.operatorType,
                        incrementer = incrementer.incrementerValue,
                        blockValue = blockValue
                    )
                } else {
                    throw IllegalStateException("unexpected ${source.currentChar} , expected ';'")
                }
            } else {
                throw IllegalStateException("conditional variable is different : $conditionalConst != $variableName")
            }
        } else {
            throw IllegalStateException("unexpected ${source.currentChar} , expected ';'")
        }
    }
    return null
}

internal fun LazyBlock.parseForLoop(): ForLoop? {
    if (source.currentChar == '@' && source.increment("@for(")) {

        parseConditionalFor()?.let { return it }

        val variableName = source.parseVariableName()
        if (variableName != null) {

            source.escapeSpaces()

            // Parsing the numbered loop
            parseNumberedForLoopAfterVariable(variableName)?.let { return it }

            // Parsing the iterable loop
            parseIterableForLoopAfterVariable(variableName)?.let { return it }

        }

    }
    return null
}