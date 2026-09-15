package br.com.dashboard;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;

/**
 * Abre a dashboard no navegador após a aplicação iniciar.
 */
@Component
public class BrowserLauncher {

    private static final String DASHBOARD_URL =
            "http://localhost:8080";

    @Value("${dashboard.open-browser:false}")
    private boolean openBrowser;

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowser() {

        if (!openBrowser) {
            return;
        }

        try {

            String operatingSystem =
                    System.getProperty("os.name")
                            .toLowerCase();

            /*
             * No Windows, usa o próprio sistema
             * para abrir o navegador padrão.
             */
            if (operatingSystem.contains("win")) {

                new ProcessBuilder(
                        "rundll32",
                        "url.dll,FileProtocolHandler",
                        DASHBOARD_URL
                ).start();

                return;
            }

            /*
             * Alternativa para outros sistemas.
             */
            if (Desktop.isDesktopSupported()) {

                Desktop.getDesktop().browse(
                        URI.create(DASHBOARD_URL)
                );
            }

        } catch (Exception exception) {

            System.err.println(
                    "Não foi possível abrir o navegador automaticamente: "
                            + exception.getMessage()
            );
        }
    }
}