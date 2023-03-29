package com.wakaztahir.kte

import com.wakaztahir.kte.dsl.ModelObjectImpl
import com.wakaztahir.kte.model.LazyBlock
import com.wakaztahir.kte.model.model.MutableKTEObject
import com.wakaztahir.kte.parser.stream.DefaultPlaceholderManagerInitializer
import com.wakaztahir.kte.parser.stream.EmbeddingManager
import com.wakaztahir.kte.parser.stream.PlaceholderManager
import com.wakaztahir.kte.parser.stream.SourceStream
import java.io.File
import java.io.InputStream
import java.net.URI
import java.net.URL

class InputSourceStream(
    private val inputStream: InputStream,
    override val model: MutableKTEObject = ModelObjectImpl("Global"),
    override val embeddingManager: EmbeddingManager = NoEmbeddings,
    override val placeholderManager: PlaceholderManager = EmptyPlaceholderManager()
) : SourceStream() {

    class RelativeResourceEmbeddingManager(
        private val basePath: String,
        private val classLoader: Class<Any> = object {}.javaClass
    ) : EmbeddingManager {
        override val embeddedStreams: MutableMap<String, Boolean> = mutableMapOf()
        override fun provideStream(block: LazyBlock, path: String): SourceStream? {
            val actualPath = "$basePath/${path.removePrefix("/").removePrefix("./")}"
            val file = classLoader.getResource(actualPath)
                ?: throw IllegalStateException("embedding with path not found $actualPath")
            return InputSourceStream(
                inputStream = file.openStream(),
                model = block.source.model,
                embeddingManager = block.source.embeddingManager,
                placeholderManager = block.source.placeholderManager
            )
        }
    }

    class RelativeFileEmbeddingManager(private val file: File) : EmbeddingManager {
        override val embeddedStreams: MutableMap<String, Boolean> = mutableMapOf()
        override fun provideStream(block: LazyBlock, path: String): SourceStream? {
            val resolved = file.resolve(path.removePrefix("./"))
            if (!resolved.exists()) throw IllegalStateException("file path doesn't exist ${resolved.absolutePath}")
            return InputSourceStream(
                inputStream = resolved.inputStream(),
                model = block.source.model,
                embeddingManager = RelativeFileEmbeddingManager(file.parentFile),
                placeholderManager = block.source.placeholderManager
            )
        }
    }

    init {
        DefaultPlaceholderManagerInitializer.initializerDefaultPlaceholders(this)
    }

    override var pointer: Int = 0

    private val currentInt: Int
        get() {
            inputStream.mark(pointer + 1)
            inputStream.skip(pointer.toLong())
            val character = inputStream.read()
            inputStream.reset()
            return character
        }

    override val currentChar: Char
        get() {
            return currentInt.toChar()
        }


    override val hasEnded: Boolean
        get() = currentInt == -1

    override fun incrementPointer(): Boolean {
        return setPointerAt(pointer + 1)
    }

    override fun decrementPointer(decrease: Int): Boolean {
        if (decrease == 0) return true
        return setPointerAt(pointer - decrease)
    }

    override fun setPointerAt(position: Int): Boolean {
        if (position >= 0) {
            pointer = position
            return true
        }
        return false
    }

}