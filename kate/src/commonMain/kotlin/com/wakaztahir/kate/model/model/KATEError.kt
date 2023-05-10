package com.wakaztahir.kate.model.model

import com.wakaztahir.kate.model.CodeGen
import com.wakaztahir.kate.parser.stream.DestinationStream
import com.wakaztahir.kate.tokenizer.NodeTokenizer

class KATEParsingError(val throwable: Throwable) : CodeGen, Throwable(cause = throwable) {

    override fun <T> selectNode(tokenizer: NodeTokenizer<T>): T = tokenizer.kateParsingError

    override fun generateTo(model: MutableKATEObject, destination: DestinationStream) {
        throw throwable
    }
}