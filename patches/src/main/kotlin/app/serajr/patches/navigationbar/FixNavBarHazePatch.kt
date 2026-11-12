package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.serajr.patches.Constants
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val MAIN_ACTIVITY =
    "Lcom/x/android/main/MainActivity;"

private const val SOURCE_WRAPPER =
    "Lcom/x/compose/theme/j;"

private const val HAZE_STATE_HELPER =
    "Lcom/x/ui/common/post/b;"

private const val HAZE_EFFECT_OWNER =
    "Lcom/x/compose/core/o0;"

private const val HAZE_EFFECT_METHOD =
    "${HAZE_EFFECT_OWNER}->c(" +
        "Landroidx/compose/ui/Modifier;" +
        "Ldev/chrisbanes/haze/a1;" +
        "JLjava/util/List;FZFLdev/chrisbanes/haze/k0;" +
        "Lkotlin/jvm/functions/Function1;" +
        "Landroidx/compose/runtime/Composer;II" +
        ")Landroidx/compose/ui/Modifier;"

private const val ALIGNMENT_OWNER =
    "Landroidx/compose/ui/b;"

private const val BOTTOM_START =
    "${ALIGNMENT_OWNER}->g:Landroidx/compose/ui/i;"

private const val BOTTOM_CENTER =
    "${ALIGNMENT_OWNER}->h:Landroidx/compose/ui/i;"

private const val BOX_SCOPE =
    "Landroidx/compose/foundation/layout/x;"

private const val BOX_SCOPE_INSTANCE =
    "${BOX_SCOPE}->a:Landroidx/compose/foundation/layout/x;"

private const val BOX_ALIGN =
    "${BOX_SCOPE}->a(" +
        "Landroidx/compose/ui/Modifier;" +
        "Landroidx/compose/ui/d;" +
        ")Landroidx/compose/ui/Modifier;"

private const val MODIFIER_SINGLETON =
    "Landroidx/compose/ui/q;->a:Landroidx/compose/ui/q;"

private const val FILL_WIDTH =
    "Landroidx/compose/foundation/layout/m3;->g(" +
        "Landroidx/compose/ui/Modifier;F" +
        ")Landroidx/compose/ui/Modifier;"

private const val NAV_INSETS_FACTORY =
    "Landroidx/compose/foundation/layout/e;->u(" +
        "Landroidx/compose/runtime/Composer;" +
        ")Landroidx/compose/foundation/layout/z3;"

private const val NAV_INSETS_FIELD =
    "Landroidx/compose/foundation/layout/z3;->e:" +
        "Landroidx/compose/foundation/layout/c;"

private const val INSETS_PADDING =
    "Landroidx/compose/foundation/layout/e;->R(" +
        "Landroidx/compose/ui/Modifier;" +
        "Landroidx/compose/foundation/layout/y3;" +
        ")Landroidx/compose/ui/Modifier;"

private const val CONTENT_INVOKE =
    "Landroidx/compose/runtime/internal/g;->invoke(" +
        "Ljava/lang/Object;Ljava/lang/Object;" +
        ")Ljava/lang/Object;"

private const val SPACER_CALL =
    "Landroidx/compose/foundation/layout/e;->d(" +
        "Landroidx/compose/runtime/Composer;" +
        "Landroidx/compose/ui/Modifier;" +
        ")V"

private const val POINTER_INPUT =
    "Landroidx/compose/ui/input/pointer/g0;->b("

private const val NAV_HANDLER =
    "Lcom/x/compose/navbars/i;"

private const val STATE_FACTORY =
    "${HAZE_STATE_HELPER}->f(" +
        "Landroidx/compose/runtime/Composer;" +
        ")Ldev/chrisbanes/haze/a1;"

private const val SOURCE_MODIFIER =
    "${HAZE_STATE_HELPER}->e(" +
        "Landroidx/compose/ui/Modifier;" +
        "Ldev/chrisbanes/haze/a1;FI" +
        ")Landroidx/compose/ui/Modifier;"

private const val LAMBDA_FACTORY =
    "Landroidx/compose/runtime/internal/h;->e(" +
        "ILkotlin/Function;" +
        "Landroidx/compose/runtime/Composer;" +
        ")Landroidx/compose/runtime/internal/g;"

private const val WRAPPER_CTOR =
    "${SOURCE_WRAPPER}-><init>(" +
        "Ljava/lang/Object;Ljava/lang/Object;B)V"

private const val HOOK_NAME =
    "serajrApplyNavigationHaze"

private const val HOOK_DESCRIPTOR =
    "${MAIN_ACTIVITY}->${HOOK_NAME}(" +
        "Landroidx/compose/ui/Modifier;" +
        "Ldev/chrisbanes/haze/a1;" +
        "Landroidx/compose/runtime/Composer;" +
        ")Landroidx/compose/ui/Modifier;"

// D3-K1A.10-T1: Native Bottom Bar visibility.

private const val BOTTOM_BAR_OWNER =
    "Lcom/x/ui/common/t2;"

private const val ANIMATED_VISIBILITY_PREFIX =
    "Landroidx/compose/animation/q;->h("

private const val BOTTOM_BAR_STATE_PROVIDER_PREFIX =
    "Lcom/x/ui/common/p2;->a("

// D3-K1A.10-T1: Compose snapshot state bridge.

private const val MUTABLE_STATE =
    "Landroidx/compose/runtime/w1;"

private const val MUTABLE_STATE_FACTORY_OWNER =
    "Landroidx/compose/runtime/i;"

private const val MUTABLE_STATE_FACTORY =
    "${MUTABLE_STATE_FACTORY_OWNER}->r(" +
        "Ljava/lang/Object;" +
        ")${MUTABLE_STATE}"

private const val MUTABLE_STATE_GET =
    "${MUTABLE_STATE}->getValue()Ljava/lang/Object;"

private const val MUTABLE_STATE_SET =
    "${MUTABLE_STATE}->setValue(Ljava/lang/Object;)V"

private const val VISIBILITY_FIELD_NAME =
    "serajrBottomBarVisibleState"

private const val VISIBILITY_FIELD =
    "${MAIN_ACTIVITY}->${VISIBILITY_FIELD_NAME}:${MUTABLE_STATE}"

private const val GET_STATE_NAME =
    "serajrGetBottomBarVisibleState"

private const val GET_STATE_DESCRIPTOR =
    "${MAIN_ACTIVITY}->${GET_STATE_NAME}()${MUTABLE_STATE}"

private const val WRITE_STATE_NAME =
    "serajrOnBottomBarVisible"

private const val WRITE_STATE_DESCRIPTOR =
    "${MAIN_ACTIVITY}->${WRITE_STATE_NAME}(Z)V"

private fun Method.refAt(index: Int): String? =
    (
        implementation
            ?.instructions
            ?.elementAtOrNull(index) as? ReferenceInstruction
    )?.reference?.toString()

@Suppress("unused")
val fixNavigationBarHazePatch = bytecodePatch(
    name = "Fix Navigation Bar Haze",
    description =
        "D3-K1A.10-T1: synchronizes native solid Navigation Bar " +
        "Haze with Bottom Bar visibility using Compose snapshot state.",
    default = false
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
        var originalRoot: Method? = null
        var originalBottomBar: Method? = null

        var wrapperVerified = false
        var stateVerified = false
        var effectVerified = false
        var alignmentVerified = false
        var boxScopeVerified = false
        var modifierVerified = false
        var widthVerified = false
        var insetsVerified = false
        var spacerVerified = false

        var snapshotFactoryVerified = false
        var snapshotGetVerified = false
        var snapshotSetVerified = false

        classDefForEach { classDef ->
            when (classDef.type.toString()) {
                MAIN_ACTIVITY -> {
                    originalRoot =
                        classDef.methods.firstOrNull { method ->
                            method.name == "P" &&
                                method.returnType == "V" &&
                                method.parameterTypes.map {
                                    it.toString()
                                } == listOf(
                                    "Z",
                                    "Landroidx/compose/runtime/internal/g;",
                                    "Landroidx/compose/runtime/Composer;",
                                    "I"
                                )
                        }
                }

                SOURCE_WRAPPER -> {
                    val constructor =
                        classDef.methods.any { method ->
                            method.name == "<init>" &&
                                method.returnType == "V" &&
                                method.parameterTypes.map {
                                    it.toString()
                                } == listOf(
                                    "Ljava/lang/Object;",
                                    "Ljava/lang/Object;",
                                    "B"
                                )
                        }

                    val sourceCalls =
                        classDef.methods.any { method ->
                            method.name == "invoke" &&
                                method.implementation
                                    ?.instructions
                                    ?.any { instruction ->
                                        (
                                            instruction as?
                                                ReferenceInstruction
                                        )?.reference?.toString() ==
                                            SOURCE_MODIFIER
                                    } == true
                        }

                    wrapperVerified = constructor && sourceCalls
                }

                HAZE_STATE_HELPER -> {
                    stateVerified =
                        classDef.methods.any { method ->
                            method.name == "f" &&
                                method.returnType ==
                                    "Ldev/chrisbanes/haze/a1;" &&
                                method.parameterTypes.map {
                                    it.toString()
                                } == listOf(
                                    "Landroidx/compose/runtime/Composer;"
                                )
                        }
                }

                HAZE_EFFECT_OWNER -> {
                    effectVerified =
                        classDef.methods.any { method ->
                            method.name == "c" &&
                                method.returnType ==
                                    "Landroidx/compose/ui/Modifier;" &&
                                method.parameterTypes.map {
                                    it.toString()
                                } == listOf(
                                    "Landroidx/compose/ui/Modifier;",
                                    "Ldev/chrisbanes/haze/a1;",
                                    "J",
                                    "Ljava/util/List;",
                                    "F",
                                    "Z",
                                    "F",
                                    "Ldev/chrisbanes/haze/k0;",
                                    "Lkotlin/jvm/functions/Function1;",
                                    "Landroidx/compose/runtime/Composer;",
                                    "I",
                                    "I"
                                )
                        }
                }

                ALIGNMENT_OWNER -> {
                    alignmentVerified =
                        classDef.fields.any { field ->
                            field.name == "g" &&
                                field.type == "Landroidx/compose/ui/i;"
                        } &&
                        classDef.fields.any { field ->
                            field.name == "h" &&
                                field.type == "Landroidx/compose/ui/i;"
                        }
                }

                BOX_SCOPE -> {
                    val singleton =
                        classDef.fields.any { field ->
                            field.name == "a" &&
                                field.type == BOX_SCOPE
                        }

                    val alignMethod =
                        classDef.methods.any { method ->
                            method.name == "a" &&
                                method.returnType ==
                                    "Landroidx/compose/ui/Modifier;" &&
                                method.parameterTypes.map {
                                    it.toString()
                                } == listOf(
                                    "Landroidx/compose/ui/Modifier;",
                                    "Landroidx/compose/ui/d;"
                                )
                        }

                    boxScopeVerified = singleton && alignMethod
                }

                "Landroidx/compose/ui/q;" -> {
                    modifierVerified =
                        classDef.fields.any { field ->
                            field.name == "a" &&
                                field.type == "Landroidx/compose/ui/q;"
                        }
                }

                "Landroidx/compose/foundation/layout/m3;" -> {
                    widthVerified =
                        classDef.methods.any { method ->
                            method.name == "g" &&
                                method.returnType ==
                                    "Landroidx/compose/ui/Modifier;" &&
                                method.parameterTypes.map {
                                    it.toString()
                                } == listOf(
                                    "Landroidx/compose/ui/Modifier;",
                                    "F"
                                )
                        }
                }

                "Landroidx/compose/foundation/layout/e;" -> {
                    val insetFactory =
                        classDef.methods.any { method ->
                            method.name == "u" &&
                                method.returnType ==
                                    "Landroidx/compose/foundation/layout/z3;" &&
                                method.parameterTypes.map {
                                    it.toString()
                                } == listOf(
                                    "Landroidx/compose/runtime/Composer;"
                                )
                        }

                    val insetPadding =
                        classDef.methods.any { method ->
                            method.name == "R" &&
                                method.returnType ==
                                    "Landroidx/compose/ui/Modifier;" &&
                                method.parameterTypes.map {
                                    it.toString()
                                } == listOf(
                                    "Landroidx/compose/ui/Modifier;",
                                    "Landroidx/compose/foundation/layout/y3;"
                                )
                        }

                    spacerVerified =
                        classDef.methods.any { method ->
                            method.name == "d" &&
                                method.returnType == "V" &&
                                method.parameterTypes.map {
                                    it.toString()
                                } == listOf(
                                    "Landroidx/compose/runtime/Composer;",
                                    "Landroidx/compose/ui/Modifier;"
                                )
                        }

                    insetsVerified = insetFactory && insetPadding
                }

                // D3-K1A.10-T1:
                // Resolve the native Bottom Bar composable.

                BOTTOM_BAR_OWNER -> {
                    originalBottomBar =
                        classDef.methods.firstOrNull { method ->
                            method.name == "a" &&
                                method.returnType == "V" &&
                                method.parameterTypes.map {
                                    it.toString()
                                } == listOf(
                                    "Landroidx/compose/runtime/internal/g;",
                                    "Ldev/chrisbanes/haze/a1;",
                                    "Lkotlin/jvm/functions/Function1;",
                                    "Landroidx/compose/ui/Modifier;",
                                    "Landroidx/compose/runtime/internal/g;",
                                    "Z",
                                    "Z",
                                    "Z",
                                    "Landroidx/compose/runtime/internal/g;",
                                    "Landroidx/compose/runtime/Composer;",
                                    "I"
                                )
                        }
                }

                // Verify the native Compose snapshot API.

                MUTABLE_STATE_FACTORY_OWNER -> {
                    snapshotFactoryVerified =
                        classDef.methods.any { method ->
                            method.name == "r" &&
                                method.returnType == MUTABLE_STATE &&
                                method.parameterTypes.map {
                                    it.toString()
                                } == listOf("Ljava/lang/Object;") &&
                                (
                                    method.accessFlags and
                                        AccessFlags.STATIC.value
                                ) != 0
                        }
                }

                MUTABLE_STATE -> {
                    snapshotGetVerified =
                        classDef.methods.any { method ->
                            method.name == "getValue" &&
                                method.returnType ==
                                    "Ljava/lang/Object;" &&
                                method.parameterTypes.isEmpty()
                        }

                    snapshotSetVerified =
                        classDef.methods.any { method ->
                            method.name == "setValue" &&
                                method.returnType == "V" &&
                                method.parameterTypes.map {
                                    it.toString()
                                } == listOf("Ljava/lang/Object;")
                        }
                }
            }
        }

        // Validate the original D3-K1A.9C-T1 infrastructure.

        require(
            wrapperVerified &&
                stateVerified &&
                effectVerified &&
                alignmentVerified &&
                boxScopeVerified &&
                modifierVerified &&
                widthVerified &&
                insetsVerified &&
                spacerVerified
        ) {
            "FixNavigationBarHaze: D3-K1A.9C-T1 helpers " +
                "do not match X 12.40.1."
        }

        // Validate the new Compose state infrastructure.

        require(
            snapshotFactoryVerified &&
                snapshotGetVerified &&
                snapshotSetVerified
        ) {
            "FixNavigationBarHaze: D3-K1A.10-T1 " +
                "Compose snapshot API does not match."
        }

        val root =
            originalRoot
                ?: error(
                    "FixNavigationBarHaze: MainActivity.P not found."
                )

        val bottomBar =
            originalBottomBar
                ?: error(
                    "FixNavigationBarHaze: t2.a not found."
                )

        val mutableClass =
            mutableClassDefBy(root.definingClass)

        require(
            mutableClass.methods.none {
                it.name == HOOK_NAME ||
                    it.name == GET_STATE_NAME ||
                    it.name == WRITE_STATE_NAME
            }
        ) {
            "FixNavigationBarHaze: helper method already exists."
        }

        require(
            mutableClass.fields.none {
                it.name == VISIBILITY_FIELD_NAME
            }
        ) {
            "FixNavigationBarHaze: visibility field already exists."
        }

        val mutableRoot =
            mutableClass.methods.firstOrNull {
                it.toString() == root.toString()
            } ?: error(
                "FixNavigationBarHaze: mutable MainActivity.P not found."
            )

        val implementation =
            mutableRoot.implementation
                ?: error(
                    "FixNavigationBarHaze: MainActivity.P " +
                        "has no implementation."
                )

        require(implementation.registerCount == 15) {
            "FixNavigationBarHaze: unexpected " +
                "MainActivity.P register count."
        }

        val instructions =
            implementation.instructions.toList()

        // Original root content fingerprint.

        val contentCandidates =
            instructions.indices.filter { index ->
                if (root.refAt(index) != CONTENT_INVOKE)
                    return@filter false

                val call =
                    instructions[index] as? FiveRegisterInstruction
                        ?: return@filter false

                if (
                    call.registerCount != 3 ||
                    call.registerC != 12 ||
                    call.registerD != 4 ||
                    call.registerE != 13
                ) return@filter false

                (
                    index + 1 until
                        minOf(index + 6, instructions.size)
                ).any { nearby ->
                    root.refAt(nearby) == BOTTOM_CENTER
                }
            }

        require(contentCandidates.size == 1) {
            "FixNavigationBarHaze: expected one " +
                "root content invocation."
        }

        val contentIndex = contentCandidates.single()

        // Original navigation-inset Spacer fingerprint.

        val spacerCandidates =
            instructions.indices.filter { index ->
                if (
                    root.refAt(index) != SPACER_CALL ||
                    index <= contentIndex
                ) return@filter false

                val call =
                    instructions[index] as? FiveRegisterInstruction
                        ?: return@filter false

                if (
                    call.registerCount != 2 ||
                    call.registerC != 4 ||
                    call.registerD != 13
                ) return@filter false

                (contentIndex + 1 until index).any { nearby ->
                    root.refAt(nearby)
                        ?.startsWith(POINTER_INPUT) == true
                } &&
                (contentIndex + 1 until index).any { nearby ->
                    root.refAt(nearby)
                        ?.contains(NAV_HANDLER) == true
                }
            }

        require(spacerCandidates.size == 1) {
            "FixNavigationBarHaze: expected one " +
                "bottom navigation-inset Spacer."
        }

        val spacerIndex = spacerCandidates.single()

        require(
            spacerIndex > contentIndex &&
                spacerIndex - contentIndex < 55
        ) {
            "FixNavigationBarHaze: unexpected " +
                "root content/Spacer structure."
        }

        require(
            root.refAt(spacerIndex + 1) ==
                "Landroidx/compose/runtime/o0;->u(Z)V"
        ) {
            "FixNavigationBarHaze: unexpected " +
                "root Box closing structure."
        }

        // D3-K1A.10-T1:
        // Locate the native AnimatedVisibility invocation.
        //
        // REA verified:
        //   const/4 v5, 0x1
        //   invoke-static/range {v9 .. v18}, q.h(...)
        //
        // v10 is the final resolved Bottom Bar visibility.
        // No Smali line numbers are used.

        val bottomBarClass =
            mutableClassDefBy(bottomBar.definingClass)

        val mutableBottomBar =
            bottomBarClass.methods.firstOrNull {
                it.toString() == bottomBar.toString()
            } ?: error(
                "FixNavigationBarHaze: mutable t2.a not found."
            )

        val bottomBarImplementation =
            mutableBottomBar.implementation
                ?: error(
                    "FixNavigationBarHaze: t2.a " +
                        "has no implementation."
                )

        val bottomBarInstructions =
            bottomBarImplementation.instructions.toList()

        val visibilityCandidates =
            bottomBarInstructions.indices.filter { index ->
                if (index == 0)
                    return@filter false

                val reference =
                    bottomBar.refAt(index)
                        ?: return@filter false

                if (
                    !reference.startsWith(
                        ANIMATED_VISIBILITY_PREFIX
                    )
                ) return@filter false

                val instruction =
                    bottomBarInstructions[index]

                if (
                    instruction.opcode !=
                        Opcode.INVOKE_STATIC_RANGE
                ) return@filter false

                val range =
                    instruction as? RegisterRangeInstruction
                        ?: return@filter false

                if (
                    range.startRegister != 9 ||
                    range.registerCount != 10
                ) return@filter false

                val previous =
                    bottomBarInstructions[index - 1]

                if (previous.opcode != Opcode.CONST_4)
                    return@filter false

                val previousRegister =
                    previous as? OneRegisterInstruction
                        ?: return@filter false

                val previousLiteral =
                    previous as? NarrowLiteralInstruction
                        ?: return@filter false

                if (
                    previousRegister.registerA != 5 ||
                    previousLiteral.narrowLiteral != 1
                ) return@filter false

                (
                    index + 1 until
                        minOf(
                            index + 32,
                            bottomBarInstructions.size
                        )
                ).any { nearby ->
                    bottomBar.refAt(nearby)
                        ?.startsWith(
                            BOTTOM_BAR_STATE_PROVIDER_PREFIX
                        ) == true
                }
            }

        require(visibilityCandidates.size == 1) {
            "FixNavigationBarHaze: expected one " +
                "native Bottom Bar AnimatedVisibility anchor."
        }

        val visibilityIndex =
            visibilityCandidates.single()

        // All structural checks have passed.
        // Begin modifications.

        // ---------------------------------------------------------
        // 1. Shared Compose MutableState<Boolean> field.
        // ---------------------------------------------------------

        val visibilityField =
            ImmutableField(
                MAIN_ACTIVITY,
                VISIBILITY_FIELD_NAME,
                MUTABLE_STATE,
                AccessFlags.PRIVATE.value or
                    AccessFlags.STATIC.value,
                null,
                null,
                null
            ).toMutable()

        mutableClass.fields.add(visibilityField)

        // ---------------------------------------------------------
        // 2. Shared lazy state initializer.
        //
        // Both reader and writer use the same state instance.
        // Initial value TRUE means the Bottom Bar is visible.
        // ---------------------------------------------------------

        val getStateMethod =
            ImmutableMethod(
                MAIN_ACTIVITY,
                GET_STATE_NAME,
                emptyList(),
                MUTABLE_STATE,
                AccessFlags.PRIVATE.value or
                    AccessFlags.STATIC.value,
                null,
                null,
                MutableMethodImplementation(2)
            ).toMutable()

        getStateMethod.addInstructions(
            0,
            """
                sget-object v0, $VISIBILITY_FIELD
                if-nez v0, :state_ready

                sget-object v1, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;

                invoke-static {v1}, $MUTABLE_STATE_FACTORY
                move-result-object v0

                sput-object v0, $VISIBILITY_FIELD

                :state_ready
                return-object v0
            """.trimIndent()
        )

        mutableClass.methods.add(getStateMethod)

        // ---------------------------------------------------------
        // 3. Native Bottom Bar visibility writer.
        //
        // PUBLIC is required because t2.a belongs to another package.
        // Structural equality suppresses redundant state updates.
        // ---------------------------------------------------------

        val writeStateMethod =
            ImmutableMethod(
                MAIN_ACTIVITY,
                WRITE_STATE_NAME,
                listOf(
                    ImmutableMethodParameter(
                        "Z",
                        null,
                        null
                    )
                ),
                "V",
                AccessFlags.PUBLIC.value or
                    AccessFlags.STATIC.value,
                null,
                null,
                MutableMethodImplementation(3)
            ).toMutable()

        writeStateMethod.addInstructions(
            0,
            """
                invoke-static {}, $GET_STATE_DESCRIPTOR
                move-result-object v0

                invoke-static {p0}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
                move-result-object v1

                invoke-virtual {v0, v1}, $MUTABLE_STATE_SET

                return-void
            """.trimIndent()
        )

        mutableClass.methods.add(writeStateMethod)

        // ---------------------------------------------------------
        // 4. Original native Haze hook.
        //
        // D3-K1A.9C-T1 rendering parameters remain unchanged.
        //
        // D3-K1A.10-T1:
        //   enabled = !bottomBarVisible
        //
        // 16 registers:
        //   v0..v12 = locals
        //   p0..p2  = v13..v15
        // ---------------------------------------------------------

        val hook =
            ImmutableMethod(
                MAIN_ACTIVITY,
                HOOK_NAME,
                listOf(
                    ImmutableMethodParameter(
                        "Landroidx/compose/ui/Modifier;",
                        null,
                        null
                    ),
                    ImmutableMethodParameter(
                        "Ldev/chrisbanes/haze/a1;",
                        null,
                        null
                    ),
                    ImmutableMethodParameter(
                        "Landroidx/compose/runtime/Composer;",
                        null,
                        null
                    )
                ),
                "Landroidx/compose/ui/Modifier;",
                AccessFlags.PRIVATE.value or
                    AccessFlags.STATIC.value,
                null,
                null,
                MutableMethodImplementation(16)
            ).toMutable()

        hook.addInstructions(
            0,
            """
                move-object/from16 v0, p0
                move-object/from16 v1, p1

                const-wide/16 v2, 0x0

                invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;
                move-result-object v4

                const/high16 v5, 0x42000000

                invoke-static {}, $GET_STATE_DESCRIPTOR
                move-result-object v6

                invoke-virtual {v6}, $MUTABLE_STATE_GET
                move-result-object v6

                check-cast v6, Ljava/lang/Boolean;

                invoke-virtual {v6}, Ljava/lang/Boolean;->booleanValue()Z
                move-result v6

                xor-int/lit8 v6, v6, 0x1

                const v7, 0x3d4ccccd

                const/4 v8, 0x0
                const/4 v9, 0x0

                move-object/from16 v10, p2

                const/4 v11, 0x0
                const/4 v12, 0x0

                invoke-static/range {v0 .. v12}, $HAZE_EFFECT_METHOD
                move-result-object v0

                return-object v0
            """.trimIndent()
        )

        mutableClass.methods.add(hook)

        // ---------------------------------------------------------
        // 5. Native Bottom Bar visibility publication.
        //
        // Read-only use of v10.
        // Original register count and values remain unchanged.
        // ---------------------------------------------------------

        mutableBottomBar.addInstructions(
            visibilityIndex,
            """
                invoke-static {v10}, $WRITE_STATE_DESCRIPTOR
            """.trimIndent()
        )

        // ---------------------------------------------------------
        // 6. Original D3-K1A.9C-T1 navigation-inset Haze Spacer.
        //
        // Geometry and modifier order remain unchanged.
        // ---------------------------------------------------------

        mutableRoot.addInstructions(
            spacerIndex + 1,
            """
                sget-object v0, $MODIFIER_SINGLETON
                sget-object v1, $BOTTOM_START
                sget-object v2, $BOX_SCOPE_INSTANCE

                invoke-virtual {v2, v0, v1}, $BOX_ALIGN
                move-result-object v0

                const/high16 v1, 0x3f800000

                invoke-static {v0, v1}, $FILL_WIDTH
                move-result-object v0

                invoke-static {v4}, $NAV_INSETS_FACTORY
                move-result-object v3

                iget-object v3, v3, $NAV_INSETS_FIELD

                invoke-static {v0, v3}, $INSETS_PADDING
                move-result-object v0

                invoke-static {v0, v8, v4}, $HOOK_DESCRIPTOR
                move-result-object p3

                invoke-static {v4, p3}, $SPACER_CALL
            """.trimIndent()
        )

        // ---------------------------------------------------------
        // 7. Original D3-K1A.9C-T1 HazeState and source wrapper.
        //
        // Shared native HazeState remains in v8.
        // ---------------------------------------------------------

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
