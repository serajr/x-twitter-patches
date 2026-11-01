package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction

private const val REPLY_COMPOSER_CLASS =
    "Lcom/x/composer/minimal/b;"

private const val REPLY_CALLBACK_CLASS =
    "Lcom/x/composer/minimal/s0;"

private const val OUTER_SURFACE =
    "Landroidx/compose/foundation/p;->l("

private const val INNER_CLICKABLE =
    "Landroidx/compose/foundation/e0;->c("

private const val HAZE_MODIFIER =
    "Lcom/x/compose/core/m0;->e("

private const val CALLBACK_CONSTRUCTOR =
    "Lcom/x/composer/minimal/s0;-><init>("

private const val CALLBACK_COLOR =
    "Ldev/chrisbanes/haze/blur/m;->c("

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
    description = "Hides post reply controls and interaction while experimentally disabling the reply Haze tint.",
    default = false
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
        var replyOriginal: Method? = null
        var callbackOriginal: Method? = null

        classDefForEach { classDef ->
            when (classDef.type) {
                REPLY_COMPOSER_CLASS -> {
                    replyOriginal = classDef.methods.firstOrNull { method ->
                        method.name == "e" &&
                            method.returnType == "V" &&
                            method.parameterTypes.size == 23 &&
                            method.parameterTypes.firstOrNull()
                                ?.toString() ==
                                "Ldev/chrisbanes/haze/a1;" &&
                            method.parameterTypes.getOrNull(20)
                                ?.toString() ==
                                "Landroidx/compose/runtime/Composer;"
                    }
                }

                REPLY_CALLBACK_CLASS -> {
                    callbackOriginal = classDef.methods.firstOrNull { method ->
                        method.name == "invoke" &&
                            method.returnType == "Ljava/lang/Object;" &&
                            method.parameterTypes.size == 1 &&
                            method.parameterTypes[0].toString() ==
                                "Ljava/lang/Object;"
                    }
                }
            }
        }

        val replySource = requireNotNull(replyOriginal) {
            "D3-F1: reply composer method not found."
        }

        val callbackSource = requireNotNull(callbackOriginal) {
            "D3-F1: reply Haze callback not found."
        }

        val replyMethod = mutableClassDefBy(replySource.definingClass)
            .methods.first { it.toString() == replySource.toString() }

        val callbackMethod = mutableClassDefBy(callbackSource.definingClass)
            .methods.first { it.toString() == callbackSource.toString() }

        val replyInstructions =
            requireNotNull(replyMethod.implementation) {
                "D3-F1: reply method has no implementation."
            }.instructions.toList()

        val callbackInstructions =
            requireNotNull(callbackMethod.implementation) {
                "D3-F1: callback has no implementation."
            }.instructions.toList()

        fun referenceAt(
            instructions: List<com.android.tools.smali.dexlib2.iface.instruction.Instruction>,
            index: Int
        ): String? =
            (instructions[index] as? ReferenceInstruction)
                ?.reference?.toString()

        fun matchingInvokes(
            instructions: List<com.android.tools.smali.dexlib2.iface.instruction.Instruction>,
            prefix: String
        ): List<Int> =
            instructions.indices.filter { index ->
                val opcode = instructions[index].opcode

                (opcode == Opcode.INVOKE_STATIC ||
                    opcode == Opcode.INVOKE_STATIC_RANGE) &&
                    referenceAt(instructions, index)
                        ?.startsWith(prefix) == true
            }

        // --------------------------------------------------
        // D3-D: Remove the six reply controls.
        // --------------------------------------------------

        val renderIndices = replyInstructions.indices.filter { index ->
            val opcode = replyInstructions[index].opcode

            (opcode == Opcode.INVOKE_STATIC ||
                opcode == Opcode.INVOKE_STATIC_RANGE) &&
                RENDER_CALLS.any { prefix ->
                    referenceAt(replyInstructions, index)
                        ?.startsWith(prefix) == true
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
                referenceAt(replyInstructions, index)
                    ?.startsWith(prefix) == true
            }

            require(actual == expected) {
                "D3-F1: expected $expected calls for " +
                    "$prefix, found $actual."
            }
        }

        require(renderIndices.size == 6) {
            "D3-F1: unexpected renderer call count."
        }

        // --------------------------------------------------
        // D3-D: Preserve the original Haze modifier.
        // --------------------------------------------------

        val hazeIndices = matchingInvokes(
            replyInstructions,
            HAZE_MODIFIER
        )

        require(hazeIndices.size == 1) {
            "D3-F1: expected exactly one Haze modifier."
        }

        // --------------------------------------------------
        // D3-D: Remove the outer surface modifier.
        // --------------------------------------------------

        val outerCandidates = matchingInvokes(
            replyInstructions,
            OUTER_SURFACE
        ).filter { index ->
            val call =
                replyInstructions[index] as? FiveRegisterInstruction

            val result =
                replyInstructions.getOrNull(index + 1)
                    as? OneRegisterInstruction

            call != null &&
                call.registerC == 2 &&
                call.registerD == 3 &&
                call.registerE == 4 &&
                call.registerF == 5 &&
                call.registerG == 11 &&
                replyInstructions.getOrNull(index + 1)
                    ?.opcode == Opcode.MOVE_RESULT_OBJECT &&
                result?.registerA == 2
        }

        require(outerCandidates.size == 1) {
            "D3-F1: expected one outer surface, " +
                "found ${outerCandidates.size}."
        }

        val outerIndex = outerCandidates.single()

        // --------------------------------------------------
        // D3-D: Remove the clickable modifier.
        // --------------------------------------------------

        val clickableCandidates = matchingInvokes(
            replyInstructions,
            INNER_CLICKABLE
        ).filter { index ->
            val call =
                replyInstructions[index] as? RegisterRangeInstruction

            val result =
                replyInstructions.getOrNull(index + 1)
                    as? OneRegisterInstruction

            call?.startRegister == 16 &&
                call.registerCount == 7 &&
                replyInstructions.getOrNull(index + 1)
                    ?.opcode == Opcode.MOVE_RESULT_OBJECT &&
                result?.registerA == 6
        }

        require(clickableCandidates.size == 1) {
            "D3-F1: expected one inner clickable, " +
                "found ${clickableCandidates.size}."
        }

        val clickableIndex = clickableCandidates.single()

        // --------------------------------------------------
        // D3-F1: Identify the callback constructor used
        // exclusively by this reply composer.
        // --------------------------------------------------

        val callbackConstructors =
            replyInstructions.indices.filter { index ->
                val instruction = replyInstructions[index]

                instruction.opcode == Opcode.INVOKE_DIRECT &&
                    referenceAt(replyInstructions, index)
                        ?.startsWith(CALLBACK_CONSTRUCTOR) == true &&
                    index < hazeIndices.single()
            }

        require(callbackConstructors.size == 1) {
            "D3-F1: expected one reply callback constructor, " +
                "found ${callbackConstructors.size}."
        }

        val constructorIndex = callbackConstructors.single()

        val constructor =
            replyInstructions[constructorIndex] as?
                FiveRegisterInstruction

        require(
            constructor != null &&
                constructor.registerC == 2 &&
                constructor.registerD == 12 &&
                constructor.registerE == 9
        ) {
            "D3-F1: unexpected callback constructor registers."
        }

        val discriminatorIndex = constructorIndex - 1

        val discriminatorInstruction =
            replyInstructions[discriminatorIndex]

        val discriminatorRegister =
            discriminatorInstruction as? OneRegisterInstruction

        val discriminatorLiteral =
            discriminatorInstruction as? WideLiteralInstruction

        require(
            discriminatorInstruction.opcode == Opcode.CONST_4 &&
                discriminatorRegister?.registerA == 9 &&
                discriminatorLiteral?.wideLiteral == 0L
        ) {
            "D3-F1: expected const/4 v9, 0x0 before constructor."
        }

        // --------------------------------------------------
        // D3-F1: Validate the existing callback structure.
        // --------------------------------------------------

        val callbackColorCalls =
            callbackInstructions.indices.filter { index ->
                callbackInstructions[index].opcode ==
                    Opcode.INVOKE_INTERFACE &&
                    referenceAt(callbackInstructions, index)
                        ?.startsWith(CALLBACK_COLOR) == true
            }

        require(callbackColorCalls.size == 2) {
            "D3-F1: expected two original callback color calls, " +
                "found ${callbackColorCalls.size}."
        }

        val callbackReturns =
            callbackInstructions.indices.filter { index ->
                callbackInstructions[index].opcode ==
                    Opcode.RETURN_OBJECT
            }

        require(callbackReturns.size == 2) {
            "D3-F1: expected two callback return paths."
        }

        // The original method uses .registers 4:
        //
        // v0, v1, p0, p1
        //
        // The added instructions reuse v0 and v1.
        //
        // No register count change is necessary.

        val callbackRegisterCount =
            requireNotNull(callbackMethod.implementation)
                .registerCount

        require(callbackRegisterCount == 4) {
            "D3-F1: unexpected callback register count."
        }

        // --------------------------------------------------
        // Apply the D3-D changes plus the D3-F1
        // callback discriminator change.
        // --------------------------------------------------

        val replacements = mutableListOf<Pair<Int, String>>()

        renderIndices.forEach { index ->
            replacements += index to "nop"
        }

        replacements += outerIndex to "nop"
        replacements += (outerIndex + 1) to "nop"

        replacements += clickableIndex to "nop"
        replacements += (clickableIndex + 1) to
            "move-object/from16 v6, v16"

        // D3-F1: Mark only this callback instance.
        replacements += discriminatorIndex to
            "const/4 v9, 0x2"

        require(
            replacements.map { it.first }.distinct().size ==
                replacements.size
        ) {
            "D3-F1: overlapping reply replacements."
        }

        replacements.sortedByDescending { it.first }
            .forEach { (index, smali) ->
                replyMethod.replaceInstruction(index, smali)
            }

        // --------------------------------------------------
        // D3-F1: Extend the callback with a dedicated
        // discriminator-2 path.
        //
        // Preserve all existing paths unchanged.
        // --------------------------------------------------

        val callbackInsertIndex = callbackInstructions.indexOfFirst {
            it.opcode == Opcode.IGET_BYTE
        }

        require(callbackInsertIndex >= 0) {
            "D3-F1: callback discriminator read not found."
        }

        // The original discriminator is read into v0.
        // For value 2, run the original graphics callback,
        // then clear the Haze color-effect list.
        //
        // For every other value, execution falls through
        // to the original packed-switch.

        callbackMethod.addInstructions(
            callbackInsertIndex + 1,
            """
                const/4 v1, 0x2
                if-ne v0, v1, :d3f1_original

                iget-object v0, p0, Lcom/x/composer/minimal/s0;->b:Landroidx/compose/ui/graphics/u0;

                check-cast p1, Ldev/chrisbanes/haze/blur/m;

                invoke-interface {p1, v0}, Ldev/chrisbanes/haze/blur/m;->c(Landroidx/compose/ui/graphics/v;)V

                invoke-static {}, Lkotlin/collections/CollectionsKt;->emptyList()Ljava/util/List;

                move-result-object v0

                invoke-interface {p1, v0}, Ldev/chrisbanes/haze/blur/m;->h(Ljava/util/List;)V

                sget-object v0, Lkotlin/Unit;->a:Lkotlin/Unit;

                return-object v0

                :d3f1_original
                nop
            """.trimIndent()
        )
    }
}
