package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val fixNavBarScrimPatch = bytecodePatch(
    name = "Preservar contraste da Barra de Navegação",
    description = "Corrige o bug de transparência total da Barra de Navegação.",
    default = true
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
        var foundMethod: com.android.tools.smali.dexlib2.iface.Method? = null

        // 1. VARREDURA GLOBAL ANTI-OFUSCAÇÃO: Localiza a classe e o método "a"
        classDefForEach { classDef ->
            val className = classDef.type.toString()
            if (className.startsWith("Landroid") || className.startsWith("Lkotlin")) {
                return@classDefForEach
            }

            for (method in classDef.methods) {
                // Filtra pelo método estático: recebe 1 parâmetro (Window) e retorna Void (V)
                if (method.returnType != "V" || method.parameterTypes.size != 1 || 
                    method.parameterTypes.toString() != "[Landroid/view/Window;]") {
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
                    reference.parameterTypes.toString() == "[Z]" &&
                    reference.returnType == "V"
                }

                // O método utilitário "a" chama com "false". Mas para garantir que não pegamos o método "b" 
                // (que ativa com true), validamos se existe um const/4 com valor 0 antes do invoke.
                if (hasTargetInstruction) {
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
                "Morphe Patcher -> Não foi possível localizar o método utilitário ofuscado TransparentNavigationBarEffect."
            )

        println("Morphe Patcher -> Método alvo localizado: ${method.definingClass} -> ${method.name}")

        // 2. REESCRITA INTEGRAL DO MÉTODO (Imune a registradores locais ofuscados)
        val mutableClass = mutableClassDefBy(method.definingClass)
        val mutableMethod = mutableClass.methods.firstOrNull {
            it.name == method.name && it.returnType == method.returnType && it.parameterTypes == method.parameterTypes
        } ?: throw IllegalStateException("Morphe Patcher -> Falha ao obter o método mutável.")

        // Limpa o corpo do método "a" e injeta um código estático limpo e seguro
        // p0 = Window (parâmetro)
        // v0 = registrador local que definimos como 1 (true)
        mutableMethod.addInstructions(
            0,
            """
                .registers 2
                const/4 v0, 0x1
                invoke-virtual {p0, v0}, Landroid/view/Window;->setNavigationBarContrastEnforced(Z)V
                return-void
            """,
            replace = true // Substitui completamente o array de instruções original do método "a"
        )

        println("Morphe Patcher -> Método 'a' reescrito com sucesso. Agora ele força TRUE de forma nativa!")
    }
}
