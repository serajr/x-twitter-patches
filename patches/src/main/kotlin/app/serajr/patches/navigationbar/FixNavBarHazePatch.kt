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

private const val TARGET_CLASS = "Lcom/x/ui/common/h2;"

private const val HAZE_STATE =
    "Ldev/chrisbanes/haze/w0;"

private fun debugNavHaze(message: String) {
    if (DEBUG) {
        println("FixNavigationBarHaze -> $message")
    }
}

@Suppress("unused")
val fixNavigationBarHazePatch = bytecodePatch(
    name = "Fix Navigation Bar Haze",
    description =
        "Experimentally applies the existing Haze effect " +
        "to the bottom navigation container.",
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
                    method.parameterTypes.size == 10 &&
                    method.parameterTypes[0].toString() ==
                        "Landroidx/compose/runtime/internal/f;" &&
                    method.parameterTypes[1].toString() ==
                        HAZE_STATE &&
                    method.parameterTypes[2].toString() ==
                        "Lkotlin/jvm/functions/Function1;" &&
                    method.parameterTypes[3].toString() ==
                        "Landroidx/compose/ui/Modifier;" &&
                    method.parameterTypes[4].toString() ==
                        "Landroidx/compose/runtime/internal/f;" &&
                    method.parameterTypes[5].toString() == "Z" &&
                    method.parameterTypes[6].toString() == "Z" &&
                    method.parameterTypes[7].toString() ==
                        "Landroidx/compose/runtime/internal/f;" &&
                    method.parameterTypes[8].toString() ==
                        "Landroidx/compose/runtime/Composer;" &&
                    method.parameterTypes[9].toString() == "I"
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

        val instructions =
            mutableMethod.implementation?.instructions?.toList()
                ?: error(
                    "FixNavigationBarHaze: no method implementation."
                )

        // Locate the outer Compose Modifier application.
        val matches = instructions.withIndex().filter { (_, instruction ->
            if (instruction.opcode != Opcode.INVOKE_STATIC) {
                return@filter false
            }

            val reference =
                (instruction as? ReferenceInstruction)?.reference
                    as? MethodReference
                    ?: return@filter false

            val invoke = instruction as? Instruction35c
                ?: return@filter false

            reference.definingClass.toString() ==
                "Landroidx/compose/ui/a;" &&
                reference.name == "d" &&
                reference.returnType ==
                    "Landroidx/compose/ui/Modifier;" &&
                reference.parameterTypes.size == 2 &&
                invoke.registerCount == 2 &&
                invoke.registerC == 11 &&
                invoke.registerD == 15
        }

        require(matches.size == 1) {
            "FixNavigationBarHaze: expected one outer " +
                "Modifier call, found ${matches.size}."
        }

        val index = matches.single().index

        debugNavHaze("Target: $method")
        debugNavHaze("Insertion index: $index")

        /*
         * X 12.31.0-prod.01 register layout:
         *
         * v15 = outer Modifier
         * v11 = Composer
         * p1  = existing HazeState
         *
         * v4 and v9 can be used temporarily here.
         *
         * No register count changes.
         * No changes to AnimatedVisibility.
         * No changes to the original e2 lambda.
         */

        mutableMethod.addInstructions(
            index,
            """
            move-object/from16 v4, p1
            const/4 v9, 0x6

            invoke-static {v15, v4, v11, v9}, Lcom/x/compose/core/e0;->c(Landroidx/compose/ui/Modifier;Ldev/chrisbanes/haze/w0;Landroidx/compose/runtime/Composer;I)Landroidx/compose/ui/Modifier;

            move-result-object v15
            """.trimIndent()
        )

        debugNavHaze("Outer container Haze injected.")
    }
}
