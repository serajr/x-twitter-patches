package app.serajr.hooks.navigationbar;

import android.app.Activity;
import android.os.Build;
import android.view.Window;
import android.view.View;

public class FixNavBarScrimHook {
  
    public static void forceSystemScrim(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            final Window window = activity.getWindow();
            
            // 1. Força a flag de contraste nativa logo na inicialização da tela
            window.setNavigationBarContrastEnforced(true);
            
            // 2. Intercepta as atualizações de layout em tempo real (Animação de Rolagem / Mudança de Aba)
            View decorView = window.getDecorView();
            decorView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
              
                @Override
                public android.view.WindowInsets onApplyWindowInsets(View v, android.view.WindowInsets insets) {
                    // Sempre que o Compose mexer nos paddings ou tentar ocultar as abas,
                    // nós forçamos o Android a reestabelecer o contraste da barra
                    window.setNavigationBarContrastEnforced(true);
                    return v.onApplyWindowInsets(insets);
                }
            });
        }
    }
}
