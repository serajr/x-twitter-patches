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

private fun drawLog(event: String): String = """
    new-instance v2, Ljava/lang/StringBuilder;
    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V
    const-string v1, "$event node="
    invoke-virtual {v2, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-static {p0}, Ljava/lang/System;->identityHashCode(Ljava/lang/Object;)I
    move-result v0
    invoke-virtual {v2, v0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v2
    const-string v1, "$DRAW_LOG_TAG"
    invoke-static {v1, v2}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
""".trimIndent()

@Suppress("unused")
val fixNavigationBarHazePatch = bytecodePatch(
    name = "Fix Navigation Bar Haze",
    description = "Phase B3 diagnostics: monitors bottom bar animation, HazeEffect geometry, and draw paths on X 12.35!",
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

        // PHASE A

        val originalTarget = targetMethod
            ?: error("FixNavigationBarHaze: p2.a() not found.")

        val mutableTarget = mutableClassDefBy(originalTarget.definingClass)
            .methods.firstOrNull { it.toString() == originalTarget.toString() }
            ?: error("FixNavigationBarHaze: mutable p2.a() not found.")

        require(mutableTarget.implementation?.registerCount == 37)
        mutableTarget.addInstructions(
            0,
            "invoke-static {}, ${EXTENSION_CLASS}->initialize()V"
        )

        val originalVisibility = visibilityMethod
            ?: error("FixNavigationBarHaze: o2.setVisible(Z) not found.")

        val mutableVisibility = mutableClassDefBy(originalVisibility.definingClass)
            .methods.firstOrNull { it.toString() == originalVisibility.toString() }
            ?: error("FixNavigationBarHaze: mutable o2.setVisible(Z) not found.")

        require(mutableVisibility.implementation?.registerCount == 2)
        mutableVisibility.addInstructions(
            0,
            "invoke-static {p1}, ${EXTENSION_CLASS}->onBottomBarVisibilityChanged(Z)V"
        )

        val originalContent = contentMethod
            ?: error("FixNavigationBarHaze: m2.invoke() not found.")

        val mutableContent = mutableClassDefBy(originalContent.definingClass)
            .methods.firstOrNull { it.toString() == originalContent.toString() }
            ?: error("FixNavigationBarHaze: mutable m2.invoke() not found.")

        require(mutableContent.implementation?.registerCount == 22)
        mutableContent.addInstructions(
            0,
            "invoke-static {}, ${EXTENSION_CLASS}->onBottomBarContentComposed()V"
        )

        val originalSize = sizeMethod
            ?: error("FixNavigationBarHaze: o2.getSize() not found.")

        val mutableSize = mutableClassDefBy(originalSize.definingClass)
            .methods.firstOrNull { it.toString() == originalSize.toString() }
            ?: error("FixNavigationBarHaze: mutable o2.getSize() not found.")

        val sizeImplementation = mutableSize.implementation
            ?: error("FixNavigationBarHaze: getSize() implementation missing.")

        val sizeInstructions = sizeImplementation.instructions.toList()
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

        // PHASE B2 — GEOMETRY

        fun installGeometryHook(
            original: Method?,
            methodName: String,
            fieldName: String,
            geometryLabel: String
        ) {
            val target = original
                ?: error("FixNavigationBarHaze: $methodName not found.")

            val mutable = mutableClassDefBy(target.definingClass)
                .methods.firstOrNull { it.toString() == target.toString() }
                ?: error("FixNavigationBarHaze: mutable $methodName not found.")

            val implementation = mutable.implementation
                ?: error("FixNavigationBarHaze: $methodName implementation missing.")

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
                "FixNavigationBarHaze: unexpected field $reference"
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

        installGeometryHook(hazeSizeMethod, "X1", "U", "Size")
        installGeometryHook(hazeOffsetMethod, "W1", "V", "Offset")

        // PHASE B3 — DRAW DIAGNOSTICS

        val originalDraw = hazeDrawMethod
            ?: error("FixNavigationBarHaze: haze.x.W() not found.")

        val mutableDraw = mutableClassDefBy(originalDraw.definingClass)
            .methods.firstOrNull { it.toString() == originalDraw.toString() }
            ?: error("FixNavigationBarHaze: mutable haze.x.W() not found.")

        require(mutableDraw.implementation?.registerCount == 10)

        val drawInstructions = mutableDraw.implementation!!
            .instructions.toList()

        val g1Index = drawInstructions.indexOfFirst {
            it.opcode == Opcode.INVOKE_VIRTUAL &&
                (it as? ReferenceInstruction)
                    ?.reference?.toString()?.contains(
                        "$HAZE_NODE_CLASS->G1("
                    ) == true
        }

        require(g1Index >= 0) {
            "FixNavigationBarHaze: G1 call not found in W()."
        }

        // Insert the G1 log before the original call.
        mutableDraw.addInstructions(
            g1Index,
            drawLog("DRAW_G1")
        )

        // Insert entry log last, preserving the calculated index.
        mutableDraw.addInstructions(
            0,
            drawLog("DRAW_ENTER")
        )

        // PHASE B3 — BLUR ACTIVATION

        val originalRender = hazeRenderMethod
            ?: error("FixNavigationBarHaze: haze.x.G1() not found.")

        val mutableRender = mutableClassDefBy(originalRender.definingClass)
            .methods.firstOrNull { it.toString() == originalRender.toString() }
            ?: error("FixNavigationBarHaze: mutable haze.x.G1() not found.")

        require(mutableRender.implementation?.registerCount == 12)

        val renderInstructions = mutableRender.implementation!!
            .instructions.toList()

        val p1Index = renderInstructions.indexOfFirst {
            it.opcode == Opcode.INVOKE_VIRTUAL &&
                (it as? ReferenceInstruction)
                    ?.reference?.toString()?.contains(
                        "$HAZE_NODE_CLASS->P1("
                    ) == true
        }

        require(p1Index >= 0) {
            "FixNavigationBarHaze: P1 call not found in G1()."
        }

        mutableRender.addInstructions(
            p1Index,
            drawLog("G1_BLUR_ACTIVE")
        )

        debugNavHaze("Phase B3 diagnostics installed.")
    }
}
