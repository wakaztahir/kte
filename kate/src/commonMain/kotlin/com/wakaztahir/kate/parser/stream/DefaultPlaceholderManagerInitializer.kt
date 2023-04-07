package com.wakaztahir.kate.parser.stream

import com.wakaztahir.kate.model.TextPlaceholderBlock

object DefaultPlaceholderManagerInitializer {

    private const val UnitPlaceholderName = "unit"
    private const val DoublePlaceholderName = "double"
    private const val IntPlaceholderName = "int"
    private const val LongPlaceholderName = "long"
    private const val StringPlaceholderName = "string"
    private const val CharPlaceholderName = "char"
    private const val BooleanPlaceholderName = "boolean"
    private const val ListPlaceholderName = "list"
    private const val MutableListPlaceholderName = "mutable_list"
    private const val ObjectPlaceholderName = "object"

    fun initializerDefaultPlaceholders(source: SourceStream) {
        source.placeholderManager.placeholders.addAll(
            listOf(
                TextPlaceholderBlock(
                    text = "",
                    parent = source,
                    placeholderName = UnitPlaceholderName,
                    definitionName = UnitPlaceholderName,
                    parameterName = null
                ),
                TextPlaceholderBlock(
                    text = "@runtime.print_string(@var(__param__.toString()))",
                    parent = source,
                    placeholderName = DoublePlaceholderName,
                    definitionName = DoublePlaceholderName,
                    parameterName = null
                ),
                TextPlaceholderBlock(
                    text = "@runtime.print_string(@var(__param__.toString()))",
                    parent = source,
                    placeholderName = IntPlaceholderName,
                    definitionName = IntPlaceholderName,
                    parameterName = null
                ),
                TextPlaceholderBlock(
                    text = "@runtime.print_string(@var(__param__.toString()))",
                    parent = source,
                    placeholderName = LongPlaceholderName,
                    definitionName = LongPlaceholderName,
                    parameterName = null
                ),
                TextPlaceholderBlock(
                    text = "@runtime.print_string(@var(__param__))",
                    parent = source,
                    placeholderName = StringPlaceholderName,
                    definitionName = StringPlaceholderName,
                    parameterName = null
                ),
                TextPlaceholderBlock(
                    text = "@runtime.print_char(@var(__param__))",
                    parent = source,
                    placeholderName = CharPlaceholderName,
                    definitionName = CharPlaceholderName,
                    parameterName = null
                ),
                TextPlaceholderBlock(
                    text = "@if(@var(__param__)) @runtime.print_string(\"true\") @else @runtime.print_string(\"false\") @endif",
                    parent = source,
                    placeholderName = BooleanPlaceholderName,
                    definitionName = BooleanPlaceholderName,
                    parameterName = null
                ),
                TextPlaceholderBlock(
                    text = "@runtime.print_string(@var(__param__.joinToString()))",
                    parent = source,
                    placeholderName = ListPlaceholderName,
                    definitionName = ListPlaceholderName,
                    parameterName = null
                ),
                TextPlaceholderBlock(
                    text = "@runtime.print_string(@var(__param__.joinToString()))",
                    parent = source,
                    placeholderName = MutableListPlaceholderName,
                    definitionName = MutableListPlaceholderName,
                    parameterName = null
                ),
                TextPlaceholderBlock(
                    text = "@runtime.print_string(@var(__param__.toString()))",
                    parent = source,
                    placeholderName = ObjectPlaceholderName,
                    definitionName = ObjectPlaceholderName,
                    parameterName = null
                )
            )
        )
    }

}