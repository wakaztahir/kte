package com.wakaztahir.kte.dsl

import com.wakaztahir.kte.model.KTEValue
import com.wakaztahir.kte.model.ModelDirective
import com.wakaztahir.kte.model.ModelReference
import com.wakaztahir.kte.model.model.MutableTemplateModel
import com.wakaztahir.kte.model.model.TemplateModel

class ScopedModelObject(private val parent: MutableTemplateModel) : ModelObjectImpl() {

    override fun getModelReference(reference: ModelReference): KTEValue? {
        return super.getModelReference(reference) ?: parent.getModelReference(reference)
    }

    override fun putValue(key: String, value: KTEValue) {
        if (parent.contains(key)) {
            parent.putValue(key, value)
        } else {
            super.putValue(key, value)
        }
    }

    override fun toString(): String {
        return stringValue(0)
    }

    override fun stringValue(indentationLevel: Int): String {
        return super.stringValue(indentationLevel) + " extends " + parent.toString()
    }

}