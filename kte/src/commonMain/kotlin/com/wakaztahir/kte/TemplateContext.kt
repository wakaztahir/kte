package com.wakaztahir.kte

import com.wakaztahir.kte.dsl.ModelObjectImpl
import com.wakaztahir.kte.model.model.MutableTemplateModel
import com.wakaztahir.kte.model.model.TemplateModel
import com.wakaztahir.kte.parser.generateTo
import com.wakaztahir.kte.parser.stream.SourceStream
import com.wakaztahir.kte.parser.stream.TextDestinationStream
import com.wakaztahir.kte.parser.stream.TextSourceStream

class TemplateContext(stream: SourceStream){

    constructor(text: String, model: MutableTemplateModel = ModelObjectImpl()) : this(TextSourceStream(text, model))

    var stream: SourceStream = stream
        private set

    fun updateStream(stream: SourceStream) {
        this.stream = stream
    }

    fun updateStream(text: String) {
        this.stream = TextSourceStream(text, this.stream.model)
    }

    private val embedMap = hashMapOf<String, SourceStream>()

    fun embedStream(path: String, stream: SourceStream) {
        embedMap[path] = stream
    }

    fun getEmbeddedStream(path: String): SourceStream? {
        return embedMap[path]
    }

    @KTEDelicateFunction
    fun getDestinationAsString(): String {
        val destination = TextDestinationStream()
        generateTo(destination)
        return destination.getValue()
    }

}