package app.serajr.patches.navigationbar

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import app.serajr.patches.Constants

private const val BOOLEAN_DESCRIPTOR = "Z"
private const val BOOLEAN_VALUE_OF_DESCRIPTOR = "Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;"
private const val COLLECTION_DESCRIPTOR = "Ljava/util/Collection;"

@Suppress("unused")
val disableBlurPatch = bytecodePatch(
    name = "Desativar Blur do X",
    description = "Remove permanentemente os efeitos de desfoque (blur) do Jetpack Compose no aplicativo do X.",
    default = true
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
        var targetMethod: Method? = null

        // Varre todas as classes procurando pela assinatura do gravador de efeitos do Haze
        classDefForEach { classDef ->
            val found = classDef.methods.firstOrNull { method -> 
                method.implementation != null && isHazeBlurEnabledRecorder(method) 
            }
            
            if (found != null) {
                targetMethod = found
                return@classDefForEach
            }
        }

        targetMethod?.let { method ->
            // --- LINHAS DE LOG INTEGRADAS ---
            println("Morphe Patcher -> Alvo encontrado com sucesso!")
            println("Classe Ofuscada: ${method.definingClass}")
            println("Metodo Interceptado: ${method.name}")
            println("Assinatura Completa: $method")
            // ---------------------------------

            val mutableClass = mutableClassDefBy(method.definingClass)
            val mutableMethod = mutableClass.methods.firstOrNull { it.toString() == method.toString() }
            
            if (mutableMethod != null) {
                // Se for método estático, o primeiro argumento costuma ser v0. Se for virtual, é v1.
                // Forçamos a zerar ambos os registradores iniciais por segurança para cobrir qualquer cenário do R8
                mutableMethod.addInstructions(
                    0,
                    """
                        const/4 v0, 0x0
                        const/4 v1, 0x0
                    """
                )
                println("Morphe Patcher -> Codigo injetado com sucesso no indice 0!")
            }
        } ?: run {
            println("Morphe Patcher -> Erro: O metodo alvo nao foi localizado no pacote Haze do X.")
        }
    }
}

private fun isHazeBlurEnabledRecorder(method: Method): Boolean {
    // Removemos o travamento de escopo rígido e a restrição a métodos estáticos para driblar o ProGuard
    if (method.returnType != "V" || method.parameterTypes.map { it.toString() } != listOf(BOOLEAN_DESCRIPTOR)) return false

    val instructions = method.implementation?.instructions?.toList() ?: return false
    
    // Verifica se o método faz o boxing do parâmetro Boolean (impressão digital clássica da biblioteca)
    val hasBoxCall = instructions.any { inst ->
        inst.opcode == Opcode.INVOKE_STATIC && 
        (inst as? ReferenceInstruction)?.reference?.toString() == BOOLEAN_VALUE_OF_DESCRIPTOR
    }
    if (!hasBoxCall) return false

    // Garante que a lista de efeitos adiciona (add) o elemento encapsulado na coleção
    return instructions.any { inst ->
        inst.opcode == Opcode.INVOKE_INTERFACE && 
        ((inst as? ReferenceInstruction)?.reference as? MethodReference)?.let { ref ->
            ref.definingClass.toString() == COLLECTION_DESCRIPTOR && ref.name == "add"
        } == true
    }
}
