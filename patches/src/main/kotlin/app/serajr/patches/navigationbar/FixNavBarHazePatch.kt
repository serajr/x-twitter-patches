
package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val DEBUG = false

private const val TARGET_CLASS = "Lcom/x/ui/common/p2;"
private const val VISIBILITY_CLASS = "Lcom/x/ui/common/o2;"
private const val CONTENT_CLASS = "Lcom/x/ui/common/m2;"
private const val HAZE_NODE_CLASS = "Ldev/chrisbanes/haze/x;"

private const val EXTENSION_CLASS =
    "Lapp/serajr/extension/navigationbar/PersistentNavigationHaze;"

private const val GEOMETRY_LOG_TAG = "NavHazeGeometry"

private fun debugNavHaze(message: String) {
    if (DEBUG) println("FixNavigationBarHaze -> $message")
}

@Suppress("unused")
val fixNavigationBarHazePatch = bytecodePatch(
    name = "Fix Navigation Bar Haze",
    description = "Phase B2 diagnostics: monitors bottom bar visibility, composition, size, and HazeEffect geometry on X 12.35!",
    default = false
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    extendWith("extensions/navigation.mpe")

    execute {
        var targetMethod: Method? = null
        var visibilityMethod: Method? = null
        var contentMethod: Method? = null
        var sizeMethod: Method? = null

        var hazeSizeMethod: Method? = null
        var hazeOffsetMethod: Method? = null

        classDefForEach { classDef ->
            when (classDef.type.toString()) {
                TARGET_CLASS -> {
                    targetMethod = classDef.methods.firstOrNull { method ->
                        method.name == "a" &&
                            method.returnType == "V" &&
                            method.parameterTypes.map { it.toString() } == listOf(
                                "Landroidx/compose/runtime/internal/g;",
                                "Ldev/chrisbanes/haze/x0;",
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

                VISIBILITY_CLASS -> {
                    visibilityMethod = classDef.methods.firstOrNull { method ->
                        method.name == "setVisible" &&
                            method.returnType == "V" &&
                            method.parameterTypes.map { it.toString() } == listOf("Z")
                    }

                    sizeMethod = classDef.methods.firstOrNull { method ->
                        method.name == "getSize" &&
                            method.returnType == "I" &&
                            method.parameterTypes.isEmpty()
                    }
                }

                CONTENT_CLASS -> {
                    contentMethod = classDef.methods.firstOrNull { method ->
                        method.name == "invoke" &&
                            method.returnType == "Ljava/lang/Object;" &&
                            method.parameterTypes.map { it.toString() } == listOf(
                                "Ljava/lang/Object;",
                                "Ljava/lang/Object;",
                                "Ljava/lang/Object;"
                            )
                    }
                }

                HAZE_NODE_CLASS -> {
                    hazeSizeMethod = classDef.methods.firstOrNull { method ->
                        method.name == "X1" &&
                            method.returnType == "V" &&
                            method.parameterTypes.map { it.toString() } == listOf("J")
                    }

                    hazeOffsetMethod = classDef.methods.firstOrNull { method ->
                        method.name == "W1" &&
                            method.returnType == "V" &&
                            method.parameterTypes.map { it.toString() } == listOf("J")
                    }
                }
            }
        }

        // ============================================================
        // PHASE A — EXISTING VALIDATED DIAGNOSTICS
        // ============================================================

        // Hook 1: Initialize the navigation Haze extension.
        val originalTarget = targetMethod
            ?: error("FixNavigationBarHaze: p2.a() not found.")

        val mutableTarget = mutableClassDefBy(originalTarget.definingClass)
            .methods.firstOrNull { it.toString() == originalTarget.toString() }
            ?: error("FixNavigationBarHaze: mutable p2.a() not found.")

        require(mutableTarget.implementation?.registerCount == 37) {
            "FixNavigationBarHaze: unexpected p2.a() register layout."
        }

        mutableTarget.addInstructions(
            0,
            "invoke-static {}, ${EXTENSION_CLASS}->initialize()V"
        )

        // Hook 2: Monitor bottom bar visibility changes.
        val originalVisibility = visibilityMethod
            ?: error("FixNavigationBarHaze: o2.setVisible(Z) not found.")

        val mutableVisibility = mutableClassDefBy(originalVisibility.definingClass)
            .methods.firstOrNull { it.toString() == originalVisibility.toString() }
            ?: error("FixNavigationBarHaze: mutable o2.setVisible(Z) not found.")

        require(mutableVisibility.implementation?.registerCount == 2) {
            "FixNavigationBarHaze: unexpected o2.setVisible(Z) register layout."
        }

        mutableVisibility.addInstructions(
            0,
            "invoke-static {p1}, ${EXTENSION_CLASS}->onBottomBarVisibilityChanged(Z)V"
        )

        // Hook 3: Monitor bottom bar content composition.
        val originalContent = contentMethod
            ?: error("FixNavigationBarHaze: m2.invoke() not found.")

        val mutableContent = mutableClassDefBy(originalContent.definingClass)
            .methods.firstOrNull { it.toString() == originalContent.toString() }
            ?: error("FixNavigationBarHaze: mutable m2.invoke() not found.")

        require(mutableContent.implementation?.registerCount == 22) {
            "FixNavigationBarHaze: unexpected m2.invoke() register layout."
        }

        mutableContent.addInstructions(
            0,
            "invoke-static {}, ${EXTENSION_CLASS}->onBottomBarContentComposed()V"
        )

        // Hook 4: Monitor the animated bottom bar size.
        val originalSize = sizeMethod
            ?: error("FixNavigationBarHaze: o2.getSize() not found.")

        val mutableSize = mutableClassDefBy(originalSize.definingClass)
            .methods.firstOrNull { it.toString() == originalSize.toString() }
            ?: error("FixNavigationBarHaze: mutable o2.getSize() not found.")

        val sizeImplementation = mutableSize.implementation
            ?: error("FixNavigationBarHaze: o2.getSize() has no implementation.")

        debugNavHaze(
            "o2.getSize() registerCount=${sizeImplementation.registerCount}"
        )

        val sizeInstructions = sizeImplementation.instructions.toList()

        val returnIndex = sizeInstructions.indexOfLast {
            it.opcode == Opcode.RETURN
        }

        require(returnIndex >= 0) {
            "FixNavigationBarHaze: o2.getSize() return not found."
        }

        val returnInstruction = sizeInstructions[returnIndex]
            as? OneRegisterInstruction
            ?: error("FixNavigationBarHaze: unexpected getSize() return instruction.")

        val resultRegister = returnInstruction.registerA

        mutableSize.addInstructions(
            returnIndex,
            "invoke-static {v$resultRegister}, ${EXTENSION_CLASS}->onBottomBarSizeChanged(I)V"
        )

        debugNavHaze("Four Phase A diagnostic hooks installed.")

        // ============================================================
        // PHASE B2 — HAZE EFFECT GEOMETRY DIAGNOSTICS
        // ============================================================

        // These hooks observe geometry changes without modifying
        // the original values, HazeState, drawing, or animations.
        //
        // X1(J): stores the calculated effect size in field U.
        // W1(J): stores the calculated effect offset in field V.
        //
        // Original register layout:
        //   v0, v1 = local registers
        //   p0     = HazeEffect node
        //   p1, p2 = packed geometry value
        //
        // Logging is injected after the original geometry write.
        // No additional registers or extension methods are needed.

        fun installGeometryHook(
            original: Method?,
            methodName: String,
            fieldName: String,
            geometryLabel: String
        ) {
            val target = original
                ?: error(
                    "FixNavigationBarHaze: haze.x.$methodName(J) not found."
                )

            val mutable = mutableClassDefBy(target.definingClass)
                .methods.firstOrNull {
                    it.toString() == target.toString()
                }
                ?: error(
                    "FixNavigationBarHaze: mutable haze.x.$methodName(J) not found."
                )

            val implementation = mutable.implementation
                ?: error(
                    "FixNavigationBarHaze: haze.x.$methodName(J) has no implementation."
                )

            require(implementation.registerCount == 5) {
                "FixNavigationBarHaze: unexpected $methodName(J) register layout."
            }

            val instructions = implementation.instructions.toList()

            // Find the unique original write to the geometry field.
            // The supplied X 12.35 smali contains exactly one
            // IPUT_WIDE instruction in each target method.
            val writeIndices = instructions.indices.filter { index ->
                instructions[index].opcode == Opcode.IPUT_WIDE
            }

            require(writeIndices.size == 1) {
                "FixNavigationBarHaze: unexpected $methodName(J) geometry writes."
            }

            val writeIndex = writeIndices.single()

            // Confirm that the expected field is referenced.
            val fieldReference =
                (instructions[writeIndex] as?
                    com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction)
                    ?.reference
                    ?.toString()

            require(
                fieldReference == "$HAZE_NODE_CLASS->$fieldName:J"
            ) {
                "FixNavigationBarHaze: unexpected $methodName(J) field: $fieldReference"
            }

            // Preserve original execution. Logging occurs only when
            // the original comparison detected a geometry change.
            //
            // Format:
            // NavHazeGeometry: Size node=123456 value=4428000042fc0000
            //
            // Long.toHexString preserves the packed 64-bit value.
            //
            // The injected instructions use the original local
            // registers v0/v1 after the geometry write, when they
            // are no longer needed by the original method.

            mutable.addInstructions(
                writeIndex + 1,
                """
                const-string v1, "$GEOMETRY_LOG_TAG"

                new-instance v0, Ljava/lang/StringBuilder;
                invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

                const-string v1, "$geometryLabel node="
                invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

                invoke-static {p0}, Ljava/lang/System;->identityHashCode(Ljava/lang/Object;)I
                move-result v1

                invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

                const-string v1, " value="
                invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

                invoke-static {p1, p2}, Ljava/lang/Long;->toHexString(J)Ljava/lang/String;
                move-result-object v1

                invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

                invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
                move-result-object v0

                const-string v1, "$GEOMETRY_LOG_TAG"
                invoke-static {v1, v0}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
                """.trimIndent()
            )

            debugNavHaze(
                "$methodName(J) $geometryLabel diagnostic hook installed."
            )
        }

        // Hook 5: Track calculated HazeEffect size.
        installGeometryHook(
            original = hazeSizeMethod,
            methodName = "X1",
            fieldName = "U",
            geometryLabel = "Size"
        )

        // Hook 6: Track calculated HazeEffect offset.
        installGeometryHook(
            original = hazeOffsetMethod,
            methodName = "W1",
            fieldName = "V",
            geometryLabel = "Offset"
        )

        // Do not modify k2.a() or inject Compose nodes.
        // Preserve the original bottom bar animations and Haze rendering.
        debugNavHaze("Phase B2 geometry diagnostics installed.")
    }
}
