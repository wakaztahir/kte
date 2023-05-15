package com.wakaztahir.kate.model

import com.wakaztahir.kate.parser.ParsedBlock

interface BlockContainer : AtDirective {

    override val expectSpaceOrNewLineWithIndentationAfterwards: Boolean get() = true

    val parsedBlock : ParsedBlock

}