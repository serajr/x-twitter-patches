package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val forceNavigationBarScrimPatch = bytecodePatch(
    name = "Corrige o contraste da Barra de Navegação",
    description = "Corrige o bug de transparência total da Barra de Navegação.",
    default = true
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
        var foundMethod: com.android.tools.smali.dexlib2.iface.Method? = null
        var targetIndex: Int = -1

        // 1. FINGERPRINT ANTI-OFUSCAÇÃO: Localiza o onCreate da Activity Principal do X
        classDefForEach { classDef ->
            val className = classDef.type.toString()
            if (className.startsWith("Landroid") || className.startsWith("Lkotlin") || className.startsWith("Landroidx")) {
                return@classDefForEach
            }

            for (method in classDef.methods) {
                // Procuramos pelo método onCreate padrão de uma Activity do Android
                if (method.name != "onCreate" || method.returnType != "V" || method.parameterTypes.size != 1) {
                    continue
                }

                if (method.parameterTypes[0].toString() != "Landroid/os/Bundle;") {
                    continue
                }

                val instructions = method.implementation?.instructions?.toList() ?: continue

                // Identifica se é a Activity principal validando se ela invoca o super.onCreate(Bundle)
                val isMainActivityOnCreate = instructions.any { instruction ->
                    if (instruction.opcode != Opcode.INVOKE_SUPER) return@any false

                    val reference = (instruction as? ReferenceInstruction)?.reference 
                        as? MethodReference ?: return@any false

                    reference.name == "onCreate" && 
                    reference.parameterTypes.size == 1 && 
                    reference.parameterTypes[0].toString() == "Landroid/os/Bundle;"
                }

                // Também checa se essa Activity específica é quem infla o contêiner de janelas do sistema
                if (isMainActivityOnCreate) {
                    val callsWindow = instructions.any { instruction ->
                        if (instruction.opcode != Opcode.INVOKE_VIRTUAL) return@any false
                        val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: return@any false
                        ref.name == "getWindow"
                    }

                    if (callsWindow) {
                        foundMethod = method
                        // Vamos injetar logo no início do método, após a preparação inicial
                        targetIndex = 0 
                        return@classDefForEach
                    }
                }
            }
        }

        val method = foundMethod
            ?: throw IllegalStateException(
                "Morphe Patcher -> Não foi possível localizar a Activity raiz do X para injetar o Hook."
            )

        println("Morphe Patcher -> Activity raiz localizada com sucesso: ${method.definingClass}")

        // 2. INJEÇÃO DO HOOK JAVA NO BYTECODE
        val mutableClass = mutableClassDefBy(method.definingClass)
        val mutableMethod = mutableClass.methods.firstOrNull {
            it.name == method.name && it.returnType == method.returnType && it.parameterTypes == method.parameterTypes
        } ?: throw IllegalStateException("Morphe Patcher -> Falha ao obter o método mutável da Activity.")

        /*
         * Injeta a chamada estática para a nossa classe Java passando o 'this' (p0), 
         * que é a própria instância da Activity em execução.
         */
        mutableMethod.addInstructions(
            targetIndex,
            """
                invoke-static {p0}, Lapp/serajr/hooks/navigationbar/FixNavBarScrimHook;->forceSystemScrim(Landroid/app/Activity;)V
            """
        )

        println("Morphe Patcher -> Hook dinâmico injetado na inicialização da tela com sucesso!")
    }
}
