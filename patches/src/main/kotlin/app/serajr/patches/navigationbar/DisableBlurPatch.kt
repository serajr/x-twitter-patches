package app.serajr.patches.navigationbar

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import app.serajr.patches.Constants

private const val HAZE_SCOPE = "Ldev/chrisbanes/haze/"
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

        // Varre as classes do pacote Haze para encontrar o gravador de efeito do Compose (12.30+)
        classDefForEach { classDef ->
            if (!classDef.type.toString().startsWith(HAZE_SCOPE)) return@classDefForEach
            
            val found = classDef.methods.firstOrNull { method -> 
                method.implementation != null && isHazeBlurEnabledRecorder(method) 
            }
            
            if (found != null) {
                targetMethod = found
                return@classDefForEach
            }
        }

        // Se encontrar o método, injeta o comando usando a extensão oficial InstructionExtensions
        targetMethod?.let { method ->
            val mutableClass = mutableClassDefBy(method.definingClass)
            val mutableMethod = mutableClass.methods.firstOrNull { it.toString() == method.toString() }
            
            if (mutableMethod != null) {
                // v1 corresponde ao registrador p1 em métodos virtuais (primeiro parâmetro booleano)
                val inputRegister = 1
                
                // A extensão addInstructions do Morphe estende diretamente o objeto do método mutável
                mutableMethod.addInstructions(
                    0,
                    """
                        const/4 v$inputRegister, 0x0
                    """
                )
            }
        }
    }
}

// Analisa a estrutura interna do método para garantir que é o configurador de blur do X 12.30+
private fun isHazeBlurEnabledRecorder(method: Method): Boolean {
    if (AccessFlags.STATIC.isSet(method.accessFlags) || !AccessFlags.PUBLIC.isSet(method.accessFlags)) return false
    if (method.returnType != "V" || method.parameterTypes.map { it.toString() } != listOf(BOOLEAN_DESCRIPTOR)) return false

    val instructions = method.implementation?.instructions?.toList() ?: return false
    val inputRegister = 1
    
    // Procura pela chamada que encapsula o Boolean primitivo
    val boxCalls = instructions.filter { inst ->
        inst.opcode == Opcode.INVOKE_STATIC && 
        (inst as? ReferenceInstruction)?.reference?.toString() == BOOLEAN_VALUE_OF_DESCRIPTOR
    }
    if (boxCalls.size != 1) return false
    if ((boxCalls.single() as? Instruction35c)?.registerC != inputRegister) return false

    // Garante que o valor encapsulado é adicionado na coleção de efeitos do Haze
    return instructions.any { inst ->
        inst.opcode == Opcode.INVOKE_INTERFACE && 
        ((inst as? ReferenceInstruction)?.reference as? MethodReference)?.let { ref ->
            ref.definingClass.toString() == COLLECTION_DESCRIPTOR && ref.name == "add"
        } == true
    }
}
