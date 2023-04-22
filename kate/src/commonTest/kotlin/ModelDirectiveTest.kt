import com.wakaztahir.kate.TemplateContext
import com.wakaztahir.kate.model.*
import com.wakaztahir.kate.model.model.*
import com.wakaztahir.kate.parser.function.KATEInvocation
import com.wakaztahir.kate.parser.function.KATEParsedFunction
import com.wakaztahir.kate.parser.variable.parseVariableReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModelDirectiveTest {

    private inline fun TemplateContext.testDirective(block: (ModelDirective) -> Unit) {
        val previous = stream.pointer
        block(stream.parseVariableReference(true)!!)
        stream.setPointerAt(previous)
    }

    @Test
    fun testStringFunctions() {
        assertEquals("s", GenerateCode("@var myStr = \"0ishere\" @var(myStr[2])"))
        assertEquals("5", GenerateCode("@var myStr = \"hello\" @var(myStr.size())"))
    }

    @Test
    fun testObjectGeneration() {
        val context = TemplateContext("@var(MyObject)")
        context.stream.model.apply {
            this.putObject("MyObject") {
                this.setValue("myInt", 15)
                this.setValue("myDouble", 16.000)
                this.setValue("myStr", "something is here")
                this.setValue(key = "myList", value = listOf(10, 20, 30, 40))
                this.putObject(key = "MyNestedObject") {
                    this.setValue("myInt", 15)
                    this.setValue("myDouble", 16.000)
                    this.setValue("myStr", "something is here")
                    this.putObject(key = "MoreNestedObject") {
                        this.setValue("myInt", 15)
                        this.setValue("myDouble", 16.000)
                        this.setValue("myStr", "something is here")
                    }
                }
            }
        }
        assertEquals(
            """{
                |	myDouble : 16.0
                |	myList : 10,20,30,40
                |	MyNestedObject : {
                |		myDouble : 16.0
                |		MoreNestedObject : {
                |			myDouble : 16.0
                |			myInt : 15
                |			myStr : something is here
                |		}
                |		myInt : 15
                |		myStr : something is here
                |	}
                |	myInt : 15
                |	myStr : something is here
            |}""".trimMargin("|"), context.getDestinationAsString()
        )
    }

    @Test
    fun testParseModelDirective() {
        val context = TemplateContext("@var(firstProp.secondProp.thirdCall().fourthProp.fifthProp(true,false))")
        context.testDirective { directive ->
            assertEquals("firstProp", directive.propertyPath[0].name)
            assertEquals("secondProp", directive.propertyPath[1].name)
            assertEquals("thirdCall", directive.propertyPath[2].name)
            assertEquals("fourthProp", directive.propertyPath[3].name)
            val call = directive.propertyPath[4] as ModelReference.FunctionCall
            assertEquals("fifthProp", call.name)
            assertEquals(true, call.parametersList[0].asPrimitive(context.stream.model).value)
            assertEquals(false, call.parametersList[1].asPrimitive(context.stream.model).value)
        }
    }

    @Test
    fun testModelFunctionCallWithParameters() {
        val context = TemplateContext(text = "@var(callSum(1,2))")
        val directive = context.stream.parseVariableReference(true)!!
        assertTrue(directive.propertyPath[0] is ModelReference.FunctionCall)
    }

    @Test
    fun testFunction() {
        var invocations = 0
        val context = TemplateContext(
            "@var(funName())@var(propName)()@partial_raw funName() @end_partial_raw",
            MutableKATEObject {
                insertValue(
                    "funName",
                    KATEParsedFunction("funName ()->string") { model, path, pathIndex, invokedOn, parameters ->
                        invocations++
                        StringValue("funVal")
                    })
                setValue("propName", "propVal")
            })
        assertEquals("funValpropVal()", context.getDestinationAsString())
        assertEquals(2, invocations)
    }

    @Test
    fun testParseModelDirectiveCodeGen() {
        val model = MutableKATEObject {
            setValue("property1", true)
            putObject("property2") {
                setValue("property3", "123")
            }
            insertValue(
                "callSum",
                KATEParsedFunction("callSum ()->string") { model, path, pathIndex, invokedOn, parameters ->
                    IntValue(parameters.map { it.asPrimitive(model) }.sumOf { it.value as Int })
                })
        }
        assertEquals("true1233", GenerateCode("@var(property1)@var(property2.property3)@var(callSum(1,2))", model))
        assertEquals("3", GenerateCode("@var sum = @var(callSum(1,2)) @var(sum)", model))
        assertEquals("16", GenerateCode("@var(callSum(9 + 3,2 * 2))", model))
    }

}