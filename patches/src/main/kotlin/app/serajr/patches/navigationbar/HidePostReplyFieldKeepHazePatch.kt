package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction

private const val REPLY_COMPOSER_CLASS =
    "Lcom/x/composer/minimal/b;"

private const val OUTER_SURFACE =
    "Landroidx/compose/foundation/p;->l("

private const val INNER_CLICKABLE =
    "Landroidx/compose/foundation/e0;->c("

private const val HAZE_MODIFIER =
    "Lcom/x/compose/core/m0;->e("

private const val CLIP_MODIFIER =
    "Landroidx/compose/ui/draw/j;->c("

private val RENDER_CALLS = listOf(
    "Lcom/x/ui/common/user/z;->l(",
    "Lcom/x/composer/minimal/b;->d(",
    "Landroidx/compose/animation/g;->i(",
    "Landroidx/compose/material3/x;->h(",
    "Landroidx/compose/animation/g;->h("
)

@Suppress("unused")
val hidePostReplyBarKeepHazePatch = bytecodePatch(
    name = "Hide Post Reply Bar (Keep Haze)",
    description = "Hides post reply controls and removes the container clip and interaction while preserving Haze.",
    default = false
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
        var targetMethod: Method? = null

        classDefForEach { classDef ->
            if (classDef.type != REPLY_COMPOSER_CLASS) {
                return@classDefForEach
            }

            targetMethod = classDef.methods.firstOrNull { method ->
                method.name == "e" &&
                    method.returnType == "V" &&
                    method.parameterTypes.size == 23 &&
                    method.parameterTypes.firstOrNull()
                        ?.toString() == "Ldev/chrisbanes/haze/a1;" &&
                    method.parameterTypes.getOrNull(20)
                        ?.toString() == "Landroidx/compose/runtime/Composer;"
            }
        }

        val original = requireNotNull(targetMethod) {
            "D3-E: reply composer method not found."
        }

        val method = mutableClassDefBy(original.definingClass)
            .methods.first { it.toString() == original.toString() }

        val implementation = requireNotNull(method.implementation) {
            "D3-E: method has no implementation."
        }

        val instructions = implementation.instructions.toList()

        fun referenceAt(index: Int): String? =
            (instructions[index] as? ReferenceInstruction)
                ?.reference?.toString()

        fun matchingInvokes(prefix: String): List<Int> =
            instructions.indices.filter { index ->
                val opcode = instructions[index].opcode

                (opcode == Opcode.INVOKE_STATIC ||
                    opcode == Opcode.INVOKE_STATIC_RANGE) &&
                    referenceAt(index)?.startsWith(prefix) == true
            }

        // D3-B: Preserve all six renderer removals.
        val renderIndices = instructions.indices.filter { index ->
            val opcode = instructions[index].opcode

            (opcode == Opcode.INVOKE_STATIC ||
                opcode == Opcode.INVOKE_STATIC_RANGE) &&
                RENDER_CALLS.any { prefix ->
                    referenceAt(index)?.startsWith(prefix) == true
                }
        }

        val expectedCounts = mapOf(
            "Lcom/x/ui/common/user/z;->l(" to 1,
            "Lcom/x/composer/minimal/b;->d(" to 1,
            "Landroidx/compose/animation/g;->i(" to 2,
            "Landroidx/compose/material3/x;->h(" to 1,
            "Landroidx/compose/animation/g;->h(" to 1
        )

        expectedCounts.forEach { (prefix, expected) ->
            val actual = renderIndices.count { index ->
                referenceAt(index)?.startsWith(prefix) == true
            }

            require(actual == expected) {
                "D3-E: expected $expected calls for $prefix, found $actual."
            }
        }

        require(renderIndices.size == 6) {
            "D3-E: unexpected renderer call count."
        }

        // Verify that the original Haze call is present.
        val hazeIndices = matchingInvokes(HAZE_MODIFIER)

        require(hazeIndices.size == 1) {
            "D3-E: expected exactly one Haze modifier."
        }

        // D3-D: Remove the outer surface modifier.
        val outerCandidates = matchingInvokes(OUTER_SURFACE)
            .filter { index ->
                val call = instructions[index] as? FiveRegisterInstruction
                val result = instructions.getOrNull(index + 1)
                    as? OneRegisterInstruction

                call != null &&
                    call.registerC == 2 &&
                    call.registerD == 3 &&
                    call.registerE == 4 &&
                    call.registerF == 5 &&
                    call.registerG == 11 &&
                    instructions.getOrNull(index + 1)
                        ?.opcode == Opcode.MOVE_RESULT_OBJECT &&
                    result?.registerA == 2
            }

        require(outerCandidates.size == 1) {
            "D3-E: expected one outer surface, " +
                "found ${outerCandidates.size}."
        }

        val outerIndex = outerCandidates.single()

        // D3-D: Remove the clickable modifier.
        val clickableCandidates = matchingInvokes(INNER_CLICKABLE)
            .filter { index ->
                val call = instructions[index] as? RegisterRangeInstruction
                val result = instructions.getOrNull(index + 1)
                    as? OneRegisterInstruction

                call?.startRegister == 16 &&
                    call.registerCount == 7 &&
                    instructions.getOrNull(index + 1)
                        ?.opcode == Opcode.MOVE_RESULT_OBJECT &&
                    result?.registerA == 6
            }

        require(clickableCandidates.size == 1) {
            "D3-E: expected one inner clickable, " +
                "found ${clickableCandidates.size}."
        }

        val clickableIndex = clickableCandidates.single()

        // D3-E: Locate the clip immediately preceding Haze.
        //
        // Original:
        // invoke-static {v1, v11}, draw/j;->c(...)
        // move-result-object v16
        //
        // Replacement:
        // nop
        // move-object/from16 v16, v1
        //
        // This preserves the incoming Modifier without
        // the clip and keeps the subsequent Haze call.
        val clipCandidates = matchingInvokes(CLIP_MODIFIER)
            .filter { index ->
                val call = instructions[index] as? FiveRegisterInstruction
                val result = instructions.getOrNull(index + 1)
                    as? OneRegisterInstruction

                call != null &&
                    call.registerC == 1 &&
                    call.registerD == 11 &&
                    instructions.getOrNull(index + 1)
                        ?.opcode == Opcode.MOVE_RESULT_OBJECT &&
                    result?.registerA == 16 &&
                    index < hazeIndices.single()
            }

        require(clipCandidates.size == 1) {
            "D3-E: expected one clip before Haze, " +
                "found ${clipCandidates.size}."
        }

        val clipIndex = clipCandidates.single()

        val replacements = mutableListOf<Pair<Int, String>>()

        renderIndices.forEach { index ->
            replacements += index to "nop"
        }

        // D3-D: Preserve the original outer Modifier.
        replacements += outerIndex to "nop"
        replacements += (outerIndex + 1) to "nop"

        // D3-D: Preserve the original clickable input
        // Modifier, removing the touch interaction.
        replacements += clickableIndex to "nop"
        replacements += (clickableIndex + 1) to
            "move-object/from16 v6, v16"

        // D3-E: Remove only the rounded clip.
        replacements += clipIndex to "nop"
        replacements += (clipIndex + 1) to
            "move-object/from16 v16, v1"

        require(
            replacements.map { it.first }.distinct().size ==
                replacements.size
        ) {
            "D3-E: overlapping replacement indices."
        }

        replacements.sortedByDescending { it.first }
            .forEach { (index, smali) ->
                method.replaceInstruction(index, smali)
            }
    }
}
