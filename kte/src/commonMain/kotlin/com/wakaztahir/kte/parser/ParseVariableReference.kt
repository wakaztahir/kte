package com.wakaztahir.kte.parser

import com.wakaztahir.kte.model.ModelDirective
import com.wakaztahir.kte.model.ModelReference
import com.wakaztahir.kte.model.model.ReferencedValue
import com.wakaztahir.kte.parser.stream.SourceStream
import com.wakaztahir.kte.parser.stream.increment
import com.wakaztahir.kte.parser.stream.parseTextWhile

internal fun SourceStream.parseFunctionParameters(): List<ReferencedValue>? {
    if (increment('(')) {
        if (increment(')')) {
            return emptyList()
        }
        val parameters = mutableListOf<ReferencedValue>()
        do {
            val parameter = this.parseAnyExpressionOrValue()
            if (parameter != null) {
                parameters.add(parameter)
            } else {
                break
            }
        } while (increment(','))
        if (!increment(')')) {
            throw IllegalStateException("a function call must end with ')'")
        }
        return parameters
    }
    return null
}

internal fun SourceStream.parseIndexingOperatorCall(invokeOnly: Boolean): ModelReference.FunctionCall? {
    if (increment('[')) {
        val indexingValue = parseNumberReference()
            ?: throw IllegalStateException("couldn't get indexing value inside indexing operator")
        if (increment(']')) {
            return ModelReference.FunctionCall(
                name = "get",
                invokeOnly = invokeOnly,
                parametersList = listOf(indexingValue)
            )
        } else {
            throw IllegalStateException("indexing operator must end with ']'")
        }
    }
    return null
}

internal fun SourceStream.parseDotReferencesInto(propertyPath: MutableList<ModelReference>) {
    do {
        val invokeOnly = increment('@')
        val propertyName = parseTextWhile { currentChar.isVariableName() }
        val parameters = parseFunctionParameters()
        if (parameters != null) {
            propertyPath.add(ModelReference.FunctionCall(propertyName, invokeOnly = invokeOnly, parameters))
        } else {
            propertyPath.add(ModelReference.Property(propertyName))
        }
        parseIndexingOperatorCall(invokeOnly)?.let { propertyPath.add(it) }
    } while (increment('.'))
}

class VariableReferenceParseException(message: String) : Exception(message)

internal fun SourceStream.parseVariableReference(): ModelDirective? {
    if (currentChar == '@' && increment("@var(")) {
        val propertyPath = mutableListOf<ModelReference>()
        parseDotReferencesInto(propertyPath)
        if (!increment(')')) {
            throw VariableReferenceParseException("expected ) got $currentChar at $pointer")
        }
        return ModelDirective(propertyPath)
    }
    return null
}
