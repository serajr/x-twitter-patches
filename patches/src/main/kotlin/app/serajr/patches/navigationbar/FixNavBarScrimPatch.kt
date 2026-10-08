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
            // Ignora classes nativas do AndroidX e bibliotecas conhecidas para acelerar o build
            val className = classDef.type.toString()
            if (className.startsWith("Landroid") || className.startsWith("Lkotlin")) {
                return@classDefForEach
            }

            for (method in classDef.methods) {
                // O método original do X recebe 1 parâmetro (Window) e retorna Void (V)
                if (method.returnType != "V" || method.parameterTypes.size != 1 || 
                    method.parameterTypes[0].toString() != "Landroid/view/Window;") {
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
                        // Encontramos o método oculto da porta de UI do X, independente do nome ofuscado!
                        foundMethod = method
                        targetIndex = index
                        return@classDefForEach
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
         * No Smali ofuscado de métodos estáticos/utilitários com 1 parâmetro (Window),
         * o parâmetro Window fica no registrador p0, e o valor booleano falso calculado fica em v0 ou v1.
         * 
         * No entanto, como o X chama 'window.setNavigationBarContrastEnforced(false)', o registrador
         * de argumentos da chamada invoke-virtual sempre passará o booleano como o último parâmetro do par.
         * Forçamos p1 (ou v1, dependendo do alinhamento) para 1 (true) para interceptar o valor injetado.
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
