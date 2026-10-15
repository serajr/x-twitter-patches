package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val DEBUG = false

private const val TARGET_CLASS = "Lcom/x/ui/common/h2;"

private const val EXTENSION_CLASS =
    "Lapp/serajr/extension/navigationbar/PersistentNavigationHaze;"

private fun debugNavHaze(message: String) {
    if (DEBUG) println("FixNavigationBarHaze -> $message")
}

@Suppress("unused")
val fixNavigationBarHazePatch = bytecodePatch(
    name = "Fix Navigation Bar Haze",
    description = "Monitors the effective bottom tabs visibility for persistent navigation Haze.",
    default = false
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    extendWith("extensions/navigation.mpe")

    execute {
        var targetMethod: Method? = null

        classDefForEach { classDef ->
            if (classDef.type.toString() != TARGET_CLASS) {
                return@classDefForEach
            }

            targetMethod = classDef.methods.firstOrNull { method ->
                method.name == "a" &&
                    method.returnType == "V" &&
                    method.parameterTypes.map { it.toString() } == listOf(
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
            ?: error("FixNavigationBarHaze: target method not found.")

        val mutableClass = mutableClassDefBy(method.definingClass)

        val mutableMethod = mutableClass.methods.firstOrNull {
            it.toString() == method.toString()
        } ?: error("FixNavigationBarHaze: mutable method not found.")

        val implementation = mutableMethod.implementation
            ?: error("FixNavigationBarHaze: no implementation.")

        require(implementation.registerCount == 33) {
            "FixNavigationBarHaze: unexpected register layout."
        }

        val instructions = implementation.instructions.toList()

        // Effective visibility is held in v19 at the IF_EQZ
        // instruction following the convergence of both paths.
        val visibilityBranches = instructions.withIndex().filter { (_, instruction) ->
            instruction.opcode == Opcode.IF_EQZ &&
                (instruction as? OneRegisterInstruction)?.registerA == 19
        }

        require(visibilityBranches.size == 1) {
            "FixNavigationBarHaze: expected exactly one IF_EQZ v19, " +
                "found ${visibilityBranches.size}."
        }

        val visibilityIndex = visibilityBranches.single().index

        // Keep the original extension initialization hook.
        // Insert after finding the original instruction index,
        // so the index is not shifted before visibility injection.
        mutableMethod.addInstructions(
            visibilityIndex,
            """
                invoke-static/range {v19 .. v19}, ${EXTENSION_CLASS}->onVisibilityChanged(Z)V
            """.trimIndent()
        )

        mutableMethod.addInstructions(
            0,
            "invoke-static {}, ${EXTENSION_CLASS}->initialize()V"
        )

        debugNavHaze(
            "Visibility hook injected before IF_EQZ v19 " +
                "at original instruction index $visibilityIndex."
        )
    }
}
