package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private const val DEBUG = false

private const val TARGET_CLASS = "Lcom/x/ui/common/p2;"
private const val VISIBILITY_CLASS = "Lcom/x/ui/common/o2;"
private const val CONTENT_CLASS = "Lcom/x/ui/common/m2;"
private const val HAZE_NODE_CLASS = "Ldev/chrisbanes/haze/x;"

private const val EXTENSION_CLASS =
    "Lapp/serajr/extension/navigationbar/PersistentNavigationHaze;"

private const val GEOMETRY_LOG_TAG = "NavHazeGeometry"
private const val DRAW_LOG_TAG = "NavHazeDraw"

private fun debugNavHaze(message: String) {
    if (DEBUG) println("FixNavigationBarHaze -> $message")
}

private fun logNode(
    event: String,
    includeAttached: Boolean = false
): String {
    val attached = if (includeAttached) {
        """
        iget-boolean v0, p0, Landroidx/compose/ui/s;->n:Z
        const-string v1, " attached="
        invoke-virtual {v2, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
        invoke-virtual {v2, v0}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;
        """.trimIndent()
    } else {
        ""
    }

    return """
        new-instance v2, Ljava/lang/StringBuilder;
        invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V
        const-string v1, "$event node="
        invoke-virtual {v2, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
        invoke-static {p0}, Ljava/lang/System;->identityHashCode(Ljava/lang/Object;)I
        move-result v0
        invoke-virtual {v2, v0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
        $attached
        invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
        move-result-object v2
        const-string v1, "$DRAW_LOG_TAG"
        invoke-static {v1, v2}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
    """.trimIndent()
}

@Suppress("unused")
val fixNavigationBarHazePatch = bytecodePatch(
    name = "Fix Navigation Bar Haze",
    description = "Phase B3 diagnostics: tracks HazeEffect geometry, draw paths, and blur activation on X 12.35!",
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
        var hazeDrawMethod: Method? = null
        var hazeRenderMethod: Method? = null

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
                                "Z", "Z", "Z",
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

                    hazeDrawMethod = classDef.methods.firstOrNull { method ->
                        method.name == "W" &&
                            method.returnType == "V" &&
                            method.parameterTypes.map { it.toString() } == listOf(
                                "Landroidx/compose/ui/node/k0;"
                            )
                    }

                    hazeRenderMethod = classDef.methods.firstOrNull { method ->
                        method.name == "G1" &&
                            method.returnType == "V" &&
                            method.parameterTypes.map { it.toString() } == listOf(
                                "Landroidx/compose/ui/node/k0;",
                                "Z"
                            )
                    }
                }
            }
        }

        fun mutableMethod(original: Method?, label: String): Method {
            val method = original ?: error(
                "FixNavigationBarHaze: $label not found."
            )
            return mutableClassDefBy(method.definingClass)
                .methods.firstOrNull { it.toString() == method.toString() }
                ?: error("FixNavigationBarHaze: mutable $label not found.")
        }

        // PHASE A

        val mutableTarget = mutableMethod(targetMethod, "p2.a()")
        require(mutableTarget.implementation?.registerCount == 37)
        mutableTarget.addInstructions(
            0,
            "invoke-static {}, ${EXTENSION_CLASS}->initialize()V"
        )

        val mutableVisibility = mutableMethod(
            visibilityMethod, "o2.setVisible(Z)"
        )
        require(mutableVisibility.implementation?.registerCount == 2)
        mutableVisibility.addInstructions(
            0,
            "invoke-static {p1}, ${EXTENSION_CLASS}->onBottomBarVisibilityChanged(Z)V"
        )

        val mutableContent = mutableMethod(
            contentMethod, "m2.invoke()"
        )
        require(mutableContent.implementation?.registerCount == 22)
        mutableContent.addInstructions(
            0,
            "invoke-static {}, ${EXTENSION_CLASS}->onBottomBarContentComposed()V"
        )

        val mutableSize = mutableMethod(sizeMethod, "o2.getSize()")
        val sizeInstructions = mutableSize.implementation
            ?.instructions?.toList()
            ?: error("FixNavigationBarHaze: missing getSize() instructions.")

        val returnIndex = sizeInstructions.indexOfLast {
            it.opcode == Opcode.RETURN
        }
        require(returnIndex >= 0)

        val returnInstruction = sizeInstructions[returnIndex]
            as? OneRegisterInstruction
            ?: error("FixNavigationBarHaze: unexpected getSize() return.")

        mutableSize.addInstructions(
            returnIndex,
            "invoke-static {v${returnInstruction.registerA}}, " +
                "${EXTENSION_CLASS}->onBottomBarSizeChanged(I)V"
        )

        // PHASE B2

        fun installGeometryHook(
            original: Method?,
            methodName: String,
            fieldName: String,
            geometryLabel: String
        ) {
            val mutable = mutableMethod(
                original, "haze.x.$methodName(J)"
            )
            val implementation = mutable.implementation
                ?: error("Missing $methodName implementation.")

            require(implementation.registerCount == 5)

            val instructions = implementation.instructions.toList()
            val writeIndices = instructions.indices.filter {
                instructions[it].opcode == Opcode.IPUT_WIDE
            }
            require(writeIndices.size == 1)

            val writeIndex = writeIndices.single()
            val reference = (instructions[writeIndex]
                as? ReferenceInstruction)?.reference?.toString()

            require(reference == "$HAZE_NODE_CLASS->$fieldName:J") {
                "Unexpected $methodName field: $reference"
            }

            mutable.addInstructions(
                writeIndex + 1,
                """
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
        }

        installGeometryHook(
            hazeSizeMethod, "X1", "U", "Size"
        )
        installGeometryHook(
            hazeOffsetMethod, "W1", "V", "Offset"
        )

        // PHASE B3 — DRAW PATH DIAGNOSTICS

        val mutableDraw = mutableMethod(
            hazeDrawMethod, "haze.x.W(k0)"
        )
        val drawImplementation = mutableDraw.implementation
            ?: error("Missing haze draw implementation.")

        require(drawImplementation.registerCount == 10)

        // W() has 10 registers; v0-v6 are local registers.
        // Insert only at verified method locations.

        mutableDraw.addInstructions(
            0,
            logNode("DRAW_ENTER", includeAttached = true)
        )

        // Log immediately before G1 invocation.
        val drawInstructions = mutableDraw.implementation!!
            .instructions.toList()

        val g1Index = drawInstructions.indexOfFirst {
            it.opcode == Opcode.INVOKE_VIRTUAL &&
                (it as? ReferenceInstruction)
                    ?.reference?.toString()?.contains(
                        "Ldev/chrisbanes/haze/x;->G1("
                    ) == true
        }

        require(g1Index >= 0) {
            "FixNavigationBarHaze: G1 call not found in W()."
        }

        mutableDraw.addInstructions(
            g1Index,
            logNode("DRAW_G1")
        )

        // PHASE B3 — G1 BLUR ACTIVATION

        val mutableRender = mutableMethod(
            hazeRenderMethod, "haze.x.G1(k0,Z)"
        )
        val renderImplementation = mutableRender.implementation
            ?: error("Missing G1 implementation.")

        require(renderImplementation.registerCount == 12)

        val renderInstructions = renderImplementation.instructions.toList()

        val p1CallIndex = renderInstructions.indexOfFirst {
            it.opcode == Opcode.INVOKE_VIRTUAL &&
                (it as? ReferenceInstruction)
                    ?.reference?.toString()?.contains(
                        "Ldev/chrisbanes/haze/x;->P1("
                    ) == true
        }

        require(p1CallIndex >= 0) {
            "FixNavigationBarHaze: P1 call not found in G1()."
        }

        mutableRender.addInstructions(
            p1CallIndex,
            logNode("G1_BLUR_ACTIVE")
        )

        debugNavHaze("Phase B3 draw diagnostics installed.")
    }
}
