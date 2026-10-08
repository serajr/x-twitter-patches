package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val forceXNavigationBarScrimPatch = bytecodePatch(
    name = "Preservar o contraste da Barra de Navegação",
    description = "Corrige o bug de transparência total da Barra de Navegação.",
    default = true
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
        var foundMethod: com.android.tools.smali.dexlib2.iface.Method? = null
        var targetIndex: Int = -1

        // 1. VARREDURA GLOBAL ANTI-OFUSCAÇÃO: Procura a classe e o método pela sua estrutura
        classDefForEach { classDef ->
            val className = classDef.type.toString()
            // Ignora classes nativas do AndroidX e bibliotecas conhecidas para acelerar o build
            if (className.startsWith("Landroid") || className.startsWith("Lkotlin") || className.startsWith("Landroidx")) {
                return@classDefForEach
            }

            for (method in classDef.methods) {
                // O método original do X recebe exatamente 1 parâmetro (Window) e retorna Void (V)
                if (method.returnType != "V" || method.parameterTypes.size != 1) {
                    continue
                }

                // Valida se o primeiro parâmetro é a Window do Android
                val firstParam = method.parameterTypes[0].toString()
                if (firstParam != "Landroid/view/Window;") {
                    continue
                }

                val instructions = method.implementation?.instructions?.toList() ?: continue

                // Escaneia o corpo do método procurando a assinatura da Window do sistema
                instructions.forEachIndexed { index, instruction ->
                    if (instruction.opcode != Opcode.INVOKE_VIRTUAL) return@forEachIndexed

                    val reference = (instruction as? ReferenceInstruction)?.reference 
                        as? MethodReference ?: return@forEachIndexed

                    if (
                        reference.definingClass.toString() == "Landroid/view/Window;" &&
                        reference.name == "setNavigationBarContrastEnforced" &&
                        reference.parameterTypes.size == 1 &&
                        reference.parameterTypes[0].toString() == "Z" &&
                        reference.returnType == "V"
                    ) {
                        // Certifica-se de que encontramos a função "a" (que passa false/0), evitando a função "b"
                        // Procuramos se há uma instrução const/4 com valor 0 nas proximidades anteriores
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
                "Morphe Patcher -> Não foi possível localizar o método utilitário ofuscado da Navigation Bar."
            )

        println("Morphe Patcher -> Fingerprint estrutural localizou o método oculto com sucesso: ${method.definingClass} -> ${method.name}")

        // 2. MODIFICAÇÃO DO BYTECODE
        val mutableClass = mutableClassDefBy(method.definingClass)
        val mutableMethod = mutableClass.methods.firstOrNull {
            it.name == method.name && it.returnType == method.returnType && it.parameterTypes == method.parameterTypes
        } ?: throw IllegalStateException("Morphe Patcher -> Falha ao obter o método mutável.")

        /*
         * No Smali ofuscado da classe utilitária do X "w.a(Window)", a Window entra como p0.
         * O parâmetro boolean para o método setNavigationBarContrastEnforced é injetado via p1.
         * Forçamos p1 para 1 (true) logo antes do invoke-virtual.
         */
        mutableMethod.addInstructions(
            targetIndex,
            """
                const/4 p1, 0x1
            """
        )

        println("Morphe Patcher -> Bug do contraste corrigido com sucesso de forma dinâmica!")
    }
}
