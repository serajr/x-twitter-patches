package app.serajr.patches.navigationbar

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import app.serajr.patches.Constants

private const val HAZE_TARGET_CLASS = "Ldev/chrisbanes/haze/t;"
private const val INVOKE_METHOD_NAME = "invoke"
private const val OBJECT_DESCRIPTOR = "Ljava/lang/Object;"

@Suppress("unused")
val disableBlurPatch = bytecodePatch(
    name = "Desativar Blur do X",
    description = "Remove permanentemente os efeitos de desfoque (blur) do Jetpack Compose no aplicativo do X.",
    default = true
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
        var targetMethod: Method? = null

        // Localiza diretamente a classe t do pacote Haze e o método invoke()
        classDefForEach { classDef ->
            if (classDef.type.toString() != HAZE_TARGET_CLASS) return@classDefForEach
            
            val found = classDef.methods.firstOrNull { method -> 
                method.name == INVOKE_METHOD_NAME && method.returnType == OBJECT_DESCRIPTOR
            }
            
            if (found != null) {
                targetMethod = found
                return@classDefForEach
            }
        }

        targetMethod?.let { method ->
            println("Morphe Patcher -> Alvo Haze 2.0 encontrado com sucesso!")
            println("Classe Ofuscada: ${method.definingClass}")
            println("Metodo Interceptado: ${method.name}")
            println("Assinatura Completa: $method")

            val mutableClass = mutableClassDefBy(method.definingClass)
            val mutableMethod = mutableClass.methods.firstOrNull { it.toString() == method.toString() }
            
            if (mutableMethod != null) {
                // Injeta um retorno nulo imediato no índice 0 da execução do método
                // Fazendo a função dar um 'return null' logo de cara, abortando o desfoque
                mutableMethod.addInstructions(
                    0,
                    """
                        const/4 v0, 0x0
                        return-object v0
                    """
                )
                println("Morphe Patcher -> Injeção de retorno nulo aplicada com sucesso!")
            }
        } ?: run {
            println("Morphe Patcher -> Erro: O metodo invoke da classe dev.chrisbanes.haze.t nao foi localizado.")
        }
    }
}
