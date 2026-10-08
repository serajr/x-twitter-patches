package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val forceXNavigationBarScrimPatch = bytecodePatch(
    name = "Corrige o contraste da Barra de Navegação",
    description = "Corrige o bug de transparência total forçando o contraste nativo.",
    default = true
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
        var foundMethod: com.android.tools.smali.dexlib2.iface.Method? = null

        // 1. VARREDURA GLOBAL ANTI-OFUSCAÇÃO: Localiza a classe e o método utilitário de Window
        classDefForEach { classDef ->
            val className = classDef.type.toString()
            if (className.startsWith("Landroid") || className.startsWith("Lkotlin") || className.startsWith("Landroidx")) {
                return@classDefForEach
            }

            for (method in classDef.methods) {
                // Filtra pelo método estático: recebe exatamente 1 parâmetro (Window) e retorna Void (V)
                if (method.returnType != "V" || method.parameterTypes.size != 1) {
                    continue
                }

                // Garante que o parâmetro é a Window do Android
                if (method.parameterTypes[0].toString() != "Landroid/view/Window;") {
                    continue
                }

                val instructions = method.implementation?.instructions?.toList() ?: continue

                // Procura a assinatura da chamada interna setNavigationBarContrastEnforced
                val hasTargetInstruction = instructions.any { instruction ->
                    if (instruction.opcode != Opcode.INVOKE_VIRTUAL) return@any false

                    val reference = (instruction as? ReferenceInstruction)?.reference 
                        as? MethodReference ?: return@any false

                    reference.definingClass.toString() == "Landroid/view/Window;" &&
                    reference.name == "setNavigationBarContrastEnforced" &&
                    reference.parameterTypes.size == 1 &&
                    reference.parameterTypes[0].toString() == "Z" &&
                    reference.returnType == "V"
                }

                if (hasTargetInstruction) {
                    // Garante que estamos pegando a função que passa 'false' (0)
                    val containsFalseConst = instructions.any { it.opcode == Opcode.CONST_4 && it.toString().contains("0") }
                    if (containsFalseConst) {
                        foundMethod = method
                        return@classDefForEach
                    }
                }
            }
        }

        val method = foundMethod
            ?: throw IllegalStateException(
                "Morphe Patcher -> Não foi possível localizar o método utilitário oculto da Navigation Bar."
            )

        println("Morphe Patcher -> Método utilitário localizado: ${method.definingClass} -> ${method.name}")

        // 2. MODIFICAÇÃO DO BYTECODE: Reescrevemos o método inteiro de forma estática
        val mutableClass = mutableClassDefBy(method.definingClass)
        val mutableMethod = mutableClass.methods.firstOrNull {
            it.name == method.name && it.returnType == method.returnType && it.parameterTypes == method.parameterTypes
        } ?: throw IllegalStateException("Morphe Patcher -> Falha ao obter o método mutável.")

        // Limpa todas as instruções originais do método do X para não depender de registradores bagunçados pelo ProGuard
        mutableMethod.implementation?.instructions?.clear()

        // Injeta uma implementação nova, limpa e estática baseada na API clássica do Smali do Patcher
        // p0 é a Window recebida por parâmetro. v0 é a constante 1 (true).
        mutableMethod.addInstructions(
            0,
            "const/4 v0, 0x1\n" +
            "invoke-virtual {p0, v0}, Landroid/view/Window;->setNavigationBarContrastEnforced(Z)V\n" +
            "return-void"
        )

        println("Morphe Patcher -> Método utilitário reescrito com sucesso para forçar TRUE!")
    }
}
