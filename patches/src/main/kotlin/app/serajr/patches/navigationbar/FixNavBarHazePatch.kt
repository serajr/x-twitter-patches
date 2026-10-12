package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val DEBUG = false

private const val TARGET_CLASS =
    "Lcom/x/ui/common/h2;"

private fun debugNavHaze(message: String) {
    if (DEBUG) {
        println("FixNavigationBarHaze -> $message")
    }
}

@Suppress("unused")
val fixNavigationBarHazePatch = bytecodePatch(
    name = "Fix Navigation Bar Haze",
    description =
        "Diagnoses the bottom navigation container " +
        "using a solid red background.",
    default = false
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
        var targetMethod: Method? = null

        classDefForEach { classDef ->
            if (classDef.type.toString() != TARGET_CLASS) {
                return@classDefForEach
            }

            targetMethod = classDef.methods.firstOrNull { method ->
                method.name == "a" &&
                    method.returnType == "V" &&
                    method.parameterTypes.map { it.toString() } ==
                    listOf(
                        "Landroidx/compose/runtime/internal/f;",
                        "Ldev/chrisbanes/haze/w0;",
                        "Lkotlin/jvm/functions/Function1;",
                        "Landroidx/compose/ui/Modifier;",
                        "Landroidx/compose/runtime/internal/f;",
                        "Z",
                        "Z",
                        "Landroidx/compose/runtime/internal/f;",
                        "Landroidx/compose/runtime/Composer;",
                        "I"
                    )
            }
        }

        val method = targetMethod
            ?: error(
                "FixNavigationBarHaze: target method not found."
            )

        val mutableClass =
            mutableClassDefBy(method.definingClass)

        val mutableMethod =
            mutableClass.methods.firstOrNull {
                it.toString() == method.toString()
            } ?: error(
                "FixNavigationBarHaze: mutable method not found."
            )

        val implementation =
            mutableMethod.implementation
                ?: error(
                    "FixNavigationBarHaze: no implementation."
                )

        require(implementation.registerCount == 33) {
            "FixNavigationBarHaze: unexpected register layout."
        }

        val instructions =
            implementation.instructions.toList()

        val matches = instructions.withIndex().filter {
            (_, instruction) ->

            if (instruction.opcode != Opcode.INVOKE_STATIC) {
                return@filter false
            }

            val reference =
                (instruction as? ReferenceInstruction)?.reference
                    as? MethodReference
                    ?: return@filter false

            val invoke =
                instruction as? Instruction35c
                    ?: return@filter false

            reference.definingClass.toString() ==
                "Landroidx/compose/ui/a;" &&
                reference.name == "d" &&
                reference.returnType ==
                    "Landroidx/compose/ui/Modifier;" &&
                reference.parameterTypes.map { it.toString() } ==
                    listOf(
                        "Landroidx/compose/runtime/Composer;",
                        "Landroidx/compose/ui/Modifier;"
                    ) &&
                invoke.registerCount == 2 &&
                invoke.registerC == 11 &&
                invoke.registerD == 15
        }

        require(matches.size == 1) {
            "FixNavigationBarHaze: expected one outer " +
                "Modifier call, found ${matches.size}."
        }

        val index = matches.single().index

        debugNavHaze("Target method: $method")
        debugNavHaze("Insertion index: $index")

        /*
         * Diagnostic for X 12.31.0-prod.01.
         *
         * v15 = outer container Modifier
         * v9/v10 = temporary Compose Color (Long)
         * v4 = temporary Rectangle Shape
         *
         * Opaque red: ARGB #FFFF0000
         * Packed Compose Color:
         * 0xFFFF000000000000L
         *
         * Apply Modifier.background() before the
         * outer Modifier is passed to Compose.
         *
         * AnimatedVisibility remains unchanged.
         */

        mutableMethod.addInstructions(
            index,
            """
            const-wide v9, 0xffff000000000000L

            sget-object v4, Landroidx/compose/ui/graphics/o0;->b:Landroidx/compose/ui/graphics/d1;

            invoke-static {v15, v9, v10, v4}, Landroidx/compose/foundation/p;->h(Landroidx/compose/ui/Modifier;JLandroidx/compose/ui/graphics/i1;)Landroidx/compose/ui/Modifier;

            move-result-object v15
            """.trimIndent()
        )

        debugNavHaze(
            "Diagnostic red background injected."
        )
    }
}
