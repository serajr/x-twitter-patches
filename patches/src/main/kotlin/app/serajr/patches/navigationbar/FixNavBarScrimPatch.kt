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
    description = "Corrige o bug de transparência total da Barra de Navegação.",
    default = true
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
        var foundMethod: com.android.tools.smali.dexlib2.iface.Method? = null
        var targetIndex: Int = -1

        // 1. FINGERPRINT: Localiza o onCreate da Activity Principal do X
        classDefForEach { classDef ->
            val className = classDef.type.toString()
            if (className.startsWith("Landroid") || className.startsWith("Lkotlin") || className.startsWith("Landroidx")) {
                return@classDefForEach
            }

            for (method in classDef.methods) {
                if (method.name != "onCreate" || method.returnType != "V" || method.parameterTypes.size != 1) {
                    continue
                }

                if (method.parameterTypes.toString() != "Landroid/os/Bundle;") {
                    continue
                }

                val instructions = method.implementation?.instructions?.toList() ?: continue

                val isMainActivityOnCreate = instructions.any { instruction ->
                    if (instruction.opcode != Opcode.INVOKE_SUPER) return@any false
                    val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: return@any false
                    reference.name == "onCreate" && reference.parameterTypes.size == 1 && reference.parameterTypes.toString() == "Landroid/os/Bundle;"
                }

                if (isMainActivityOnCreate) {
                    val callsWindow = instructions.any { instruction ->
                        if (instruction.opcode != Opcode.INVOKE_VIRTUAL) return@any false
                        val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: return@any false
                        ref.name == "getWindow"
                    }

                    if (callsWindow) {
                        foundMethod = method
                        targetIndex = 0 
                        return@classDefForEach
                    }
                }
            }
        }

        val method = foundMethod
            ?: throw IllegalStateException(
                "Morphe Patcher -> Não foi possível localizar a Activity raiz do X para injetar a correção."
            )

        println("Morphe Patcher -> Activity raiz localizada com sucesso para injeção direta: ${method.definingClass}")

        // 2. MODIFICAÇÃO DO BYTECODE VIA SMALI INLINE (Ignora o compileJava)
        val mutableClass = mutableClassDefBy(method.definingClass)
        val mutableMethod = mutableClass.methods.firstOrNull {
            it.name == method.name && it.returnType == method.returnType && it.parameterTypes == method.parameterTypes
        } ?: throw IllegalStateException("Morphe Patcher -> Falha ao obter o método mutável da Activity.")

        /*
         * Buscamos o objeto Window associado à Activity em execução (p0)
         * E forçamos a flag setNavigationBarContrastEnforced(true) direto no encadeamento do onCreate.
         * Como o método onCreate de uma Activity usa p0 como o ponteiro 'this', fazemos a extração limpa:
         * 
         * invoke-virtual {p0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;
         * move-result-object v0
         * const/4 v1, 0x1
         * invoke-virtual {v0, v1}, Landroid/view/Window;->setNavigationBarContrastEnforced(Z)V
         */
        mutableMethod.addInstructions(
            targetIndex,
            """
                invoke-virtual {p0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;
                move-result-object v0
                const/4 v1, 0x1
                invoke-virtual {v0, v1}, Landroid/view/Window;->setNavigationBarContrastEnforced(Z)V
            """
        )

        println("Morphe Patcher -> Injeção de contraste estático aplicada direto no ciclo de vida com sucesso!")
    }
}
