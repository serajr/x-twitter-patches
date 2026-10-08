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
    description = "Corrige o bug de transparência total forçando o contraste nativo!",
    default = true
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
        var foundMethod: com.android.tools.smali.dexlib2.iface.Method? = null
        var targetIndex: Int = -1

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
                if (method.parameterTypes.toString() != "Landroid/view/Window;") {
                    continue
                }

                val instructions = method.implementation?.instructions?.toList() ?: continue

                // Procura a assinatura da chamada interna setNavigationBarContrastEnforced
                instructions.forEachIndexed { index, instruction ->
                    if (instruction.opcode != Opcode.INVOKE_VIRTUAL) return@forEachIndexed

                    val reference = (instruction as? ReferenceInstruction)?.reference 
                        as? MethodReference ?: return@forEachIndexed

                    if (
                        reference.definingClass.toString() == "Landroid/view/Window;" &&
                        reference.name == "setNavigationBarContrastEnforced" &&
                        reference.parameterTypes.size == 1 &&
                        reference.parameterTypes.toString() == "Z" &&
                        reference.returnType == "V"
                    ) {
                        // Garante que estamos pegando a função que passa 'false' (0)
                        val hasFalseConst = instructions.subList(0, index).any { 
                            it.opcode == Opcode.CONST_4 && it.toString().contains("0") 
                        }
                        
                        if (hasFalseConst) {
                            foundMethod = method
                            targetIndex = index
                            return@classDefForEach
                        }
                    }
                }
            }
        }

        val method = foundMethod
            ?: throw IllegalStateException(
                "Morphe Patcher -> Não foi possível localizar o método utilitário oculto da Navigation Bar."
            )

        println("Morphe Patcher -> Método utilitário localizado: method.definingClass -> {method.name}")

        // 2. MODIFICAÇÃO DO BYTECODE POR INTERCEPTAÇÃO SEGURA DE REGISTRADORES
        val mutableClass = mutableClassDefBy(method.definingClass)
        val mutableMethod = mutableClass.methods.firstOrNull {
            it.name == method.name && it.returnType == method.returnType && it.parameterTypes == method.parameterTypes
        } ?: throw IllegalStateException("Morphe Patcher -> Falha ao obter o método mutável.")

        /*
         * Estratégia de Força Bruta de Registradores:
         * Como a lista de instruções do Dexlib2 barrou o uso do `.clear()`, nós preservamos a estrutura original.
         * Para contornar de vez as otimizações do ProGuard, injetamos o valor verdadeiro (1) em todos os possíveis
         * registradores locais e de parâmetros que o compilador do X poderia usar como o argumento booleano da chamada.
         * Isso força o 'true' de maneira imutável sem quebrar restrições de array do Java.
         */
        mutableMethod.addInstructions(
            targetIndex,
            """
                const/4 v0, 0x1
                const/4 v1, 0x1
                const/4 p1, 0x1
            """
        )

        println("Morphe Patcher -> Interceptores de contraste injetados com sucesso!")
    }
}
