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
        "Diagnoses the bottom navigation container using a solid salmon background.",
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

        val resultIndex = index + 2

        require(
            instructions.getOrNull(index + 1)?.opcode ==
                Opcode.MOVE_RESULT_OBJECT
        ) {
            "FixNavigationBarHaze: expected Modifier result."
        }

        /*
         * Diagnostic for X 12.31.0-prod.01.
         *
         * Apply a visible salmon background to
         * the outer bottom navigation container.
         *
         * v1     = resulting Modifier
         * v4/v5  = temporary Compose Color
         * v21    = Rectangle Shape
         * v22    = backup of the original v5 float
         *
         * Preserve the original bottom bar animation.
         *
         * Color: #FB9FA8 (opaque salmon)
         * Source: com.x.compose.core.r1.T0
         */

        mutableMethod.addInstructions(
            resultIndex,
            """
            move/from16 v22, v5

            sget-wide v4, Lcom/x/compose/core/r1;->T0:J

            sget-object v21, Landroidx/compose/ui/graphics/o0;->b:Landroidx/compose/ui/graphics/d1;

            move-object/from16 v9, v21

            invoke-static {v1, v4, v5, v9}, Landroidx/compose/foundation/p;->h(Landroidx/compose/ui/Modifier;JLandroidx/compose/ui/graphics/i1;)Landroidx/compose/ui/Modifier;

            move-result-object v1

            move/from16 v5, v22
            """.trimIndent()
        )

        debugNavHaze(
            "Diagnostic salmon background injected."
        )
    }
}
