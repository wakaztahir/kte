import com.wakaztahir.kte.TemplateContext
import com.wakaztahir.kte.model.StringValue
import com.wakaztahir.kte.parser.parseConstantDeclaration
import com.wakaztahir.kte.parser.parseConstantReference
import com.wakaztahir.kte.parser.parseDynamicProperty
import com.wakaztahir.kte.parser.parseStringValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ConstantsTest {


    @Test
    fun testParseConstantReference() {
        val context = TemplateContext(("@const(myVar)"))
        val ref = context.stream.parseConstantReference()
        assertNotEquals(null, ref)
        assertEquals(ref!!.propertyPath[0].name, "myVar")
        context.stream.model.putValue("myVar", StringValue("someValue"))
        assertEquals("someValue", ref.getPrimitive(context.stream.model).value)
    }

    @Test
    fun testParseConstantDeclaration() {
        val context = TemplateContext(("@const myVar = \"someValue\""))
        val ref = context.stream.parseConstantDeclaration()
        assertNotEquals(null, ref)
        assertEquals("myVar", ref!!.variableName)
        assertEquals("someValue", ref.variableValue.getPrimitive(context.stream.model).value)
    }

    @Test
    fun testParseConstantGeneration() {
        val text = "@const myVar = \"someValue\"@const(myVar)"
        val context = TemplateContext(text)
        assertEquals("someValue", context.getDestinationAsString())
        assertEquals(text.length, context.stream.pointer)
    }

    @Test
    fun testParseStringValue() {
        val context = TemplateContext(("\"someValue\""))
        val value = context.stream.parseStringValue()!!.value
        assertEquals("someValue", value)
    }

    @Test
    fun testParseDynamicProperty() {
        val context = TemplateContext(("@const(myVar)"))
        context.stream.model.putValue("myVar", StringValue("someValue"))
        val property = context.stream.parseDynamicProperty()
        assertEquals(property!!.getPrimitive(context.stream.model).value, "someValue")
    }

    @Test
    fun testDeclarationAndReference() {
        val decContext = TemplateContext(("@const myVar = \"someValue\""))
        val dec = decContext.stream.parseConstantDeclaration()
        assertEquals("someValue", dec!!.variableValue.getPrimitive(decContext.stream.model).value)
        decContext.updateStream("@const myVar2 = @const(myVar)")
        dec!!.storeValue(decContext.stream.model)
        val refDec = decContext.stream.parseConstantDeclaration()
        assertEquals(
            dec.variableValue.getPrimitive(decContext.stream.model).value,
            refDec!!.variableValue.getPrimitive(decContext.stream.model).value
        )
    }

}