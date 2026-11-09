package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

/**
 * D3-K1A.5 — experimental root-scoped navigation-bar Haze.
 *
 * X 12.40.1-prod.01 only. This patch uses the app's own Compose Haze helpers:
 * a remembered HazeState, a source wrapper around the navigation content,
 * and a sibling Haze effect on the existing bottom navigation-inset Spacer.
 *
 * No extension, new class, register allocation, or .locals modification.
 * Do not combine this source file with the obsolete C2.4 diagnostic patch.
 */
private const val MAIN_ACTIVITY = "Lcom/x/android/main/MainActivity;"
private const val SOURCE_WRAPPER = "Lcom/x/compose/theme/j;"
private const val HAZE_STATE_HELPER = "Lcom/x/ui/common/post/b;"
private const val HAZE_EFFECT_HELPER = "Lcom/x/compose/core/o0;"

private const val CONTENT_INVOKE =
    "Landroidx/compose/runtime/internal/g;->invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
private const val SPACER_CALL =
    "Landroidx/compose/foundation/layout/e;->d(Landroidx/compose/runtime/Composer;Landroidx/compose/ui/Modifier;)V"
private const val BOTTOM_CENTER = "Landroidx/compose/ui/b;->h:Landroidx/compose/ui/i;"
private const val POINTER_INPUT =
    "Landroidx/compose/ui/input/pointer/g0;->b(" // Verify by prefix: obfuscated signature may vary.
private const val NAV_HANDLER = "Lcom/x/compose/navbars/i;"
private const val STATE_FACTORY =
    "${HAZE_STATE_HELPER}->f(Landroidx/compose/runtime/Composer;)Ldev/chrisbanes/haze/a1;"
private const val EFFECT_FACTORY =
    "${HAZE_EFFECT_HELPER}->d(ILandroidx/compose/runtime/Composer;Landroidx/compose/ui/Modifier;Ldev/chrisbanes/haze/a1;)Landroidx/compose/ui/Modifier;"
private const val SOURCE_MODIFIER =
    "${HAZE_STATE_HELPER}->e(Landroidx/compose/ui/Modifier;Ldev/chrisbanes/haze/a1;FI)Landroidx/compose/ui/Modifier;"
private const val LAMBDA_FACTORY =
    "Landroidx/compose/runtime/internal/h;->e(ILkotlin/Function;Landroidx/compose/runtime/Composer;)Landroidx/compose/runtime/internal/g;"
private const val WRAPPER_CTOR =
    "${SOURCE_WRAPPER}-><init>(Ljava/lang/Object;Ljava/lang/Object;B)V"

private fun Method.refAt(index: Int): String? =
    (implementation?.instructions?.elementAtOrNull(index) as? ReferenceInstruction)
        ?.reference?.toString()

@Suppress("unused")
val fixNavigationBarHazePatch = bytecodePatch(
    name = "Fix Navigation Bar Haze",
    description = "Experimental persistent native Haze behind the Android navigation bar using the root Compose content and navigation-inset overlay.",
    default = false
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
        var originalRoot: Method? = null
        var wrapperVerified = false
        var stateVerified = false
        var effectVerified = false

        classDefForEach { classDef ->
            when (classDef.type.toString()) {
                MAIN_ACTIVITY -> {
                    originalRoot = classDef.methods.firstOrNull { method ->
                        method.name == "P" && method.returnType == "V" &&
                            method.parameterTypes.map { it.toString() } == listOf(
                                "Z", "Landroidx/compose/runtime/internal/g;",
                                "Landroidx/compose/runtime/Composer;", "I"
                            )
                    }
                }
                SOURCE_WRAPPER -> {
                    val constructor = classDef.methods.any { method ->
                        method.name == "<init>" && method.returnType == "V" &&
                            method.parameterTypes.map { it.toString() } == listOf(
                                "Ljava/lang/Object;", "Ljava/lang/Object;", "B"
                            )
                    }
                    val sourceCalls = classDef.methods.any { method ->
                        method.name == "invoke" &&
                            method.implementation?.instructions?.any { instruction ->
                                (instruction as? ReferenceInstruction)?.reference?.toString() == SOURCE_MODIFIER
                            } == true
                    }
                    wrapperVerified = constructor && sourceCalls
                }
                HAZE_STATE_HELPER -> {
                    stateVerified = classDef.methods.any { method ->
                        method.name == "f" && method.returnType == "Ldev/chrisbanes/haze/a1;" &&
                            method.parameterTypes.map { it.toString() } ==
                            listOf("Landroidx/compose/runtime/Composer;")
                    }
                }
                HAZE_EFFECT_HELPER -> {
                    effectVerified = classDef.methods.any { method ->
                        method.name == "d" && method.returnType == "Landroidx/compose/ui/Modifier;" &&
                            method.parameterTypes.map { it.toString() } == listOf(
                                "I", "Landroidx/compose/runtime/Composer;",
                                "Landroidx/compose/ui/Modifier;", "Ldev/chrisbanes/haze/a1;"
                            )
                    }
                }
            }
        }

        require(wrapperVerified && stateVerified && effectVerified) {
            "FixNavigationBarHaze: native source wrapper/state/effect helpers do not match X 12.40.1."
        }

        val root = originalRoot ?: error("FixNavigationBarHaze: MainActivity.P not found.")
        val mutableRoot = mutableClassDefBy(root.definingClass)
            .methods.firstOrNull { it.toString() == root.toString() }
            ?: error("FixNavigationBarHaze: mutable MainActivity.P not found.")
        val implementation = mutableRoot.implementation
            ?: error("FixNavigationBarHaze: MainActivity.P has no implementation.")
        require(implementation.registerCount == 15) {
            "FixNavigationBarHaze: unexpected MainActivity.P register count."
        }

        val instructions = implementation.instructions.toList()
        val contentCandidates = instructions.indices.filter { index ->
            if (root.refAt(index) != CONTENT_INVOKE) return@filter false
            val call = instructions[index] as? FiveRegisterInstruction ?: return@filter false
            if (call.registerCount != 3 || call.registerC != 12 ||
                call.registerD != 4 || call.registerE != 13) return@filter false
            // This content call must immediately precede the bottom-center overlay.
            (index + 1 until minOf(index + 6, instructions.size)).any { nearby ->
                root.refAt(nearby) == BOTTOM_CENTER
            }
        }
        require(contentCandidates.size == 1) {
            "FixNavigationBarHaze: expected one root content invocation before BottomCenter."
        }
        val contentIndex = contentCandidates.single()

        val spacerCandidates = instructions.indices.filter { index ->
            if (root.refAt(index) != SPACER_CALL || index <= contentIndex) return@filter false
            val call = instructions[index] as? FiveRegisterInstruction ?: return@filter false
            if (call.registerCount != 2 || call.registerC != 4 || call.registerD != 13) {
                return@filter false
            }
            (contentIndex + 1 until index).any { nearby ->
                root.refAt(nearby)?.startsWith(POINTER_INPUT) == true
            } && (contentIndex + 1 until index).any { nearby ->
                root.refAt(nearby)?.contains(NAV_HANDLER) == true
            }
        }
        require(spacerCandidates.size == 1) {
            "FixNavigationBarHaze: expected one bottom navigation-inset Spacer."
        }
        val spacerIndex = spacerCandidates.single()
        require(spacerIndex > contentIndex && spacerIndex - contentIndex < 55) {
            "FixNavigationBarHaze: unexpected root content/Spacer structure."
        }

        // Apply from the later index backwards. The original content invoke is
        // removed and replaced as a single block; no jump targets are introduced.
        // v8 is dead in the verified window, v5/v6 are scratch; v4, v9 and p2
        // are preserved. p2 must survive for the restart lambda near method end.
        mutableRoot.addInstructions(
            spacerIndex,
            """
                const/4 v6, 0x6
                invoke-static {v6, v4, p3, v8}, $EFFECT_FACTORY
                move-result-object p3
            """.trimIndent()
        )

        mutableRoot.removeInstruction(contentIndex)
        mutableRoot.addInstructions(
            contentIndex,
            """
                invoke-static {v4}, $STATE_FACTORY
                move-result-object v8
                new-instance v5, $SOURCE_WRAPPER
                const/16 v6, 0x19
                invoke-direct {v5, v8, p2, v6}, $WRAPPER_CTOR
                const v6, 0x6e3a1b2c
                invoke-static {v6, v5, v4}, $LAMBDA_FACTORY
                move-result-object v5
                invoke-virtual {v5, v4, p3}, $CONTENT_INVOKE
            """.trimIndent()
        )
    }
}
