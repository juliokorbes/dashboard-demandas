package br.com.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.awt.Desktop;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Inicializa a aplicação e prepara os recursos locais.
 */
@SpringBootApplication
public class DashboardDemandasApplication {

	private static final String DASHBOARD_URL =
			"http://localhost:8080";

	public static void main(String[] args) {

		configureDatabasePath();

		/*
		 * Se o Dashboard já estiver rodando,
		 * apenas abre o navegador e encerra
		 * esta segunda execução.
		 */
		if (isDashboardAlreadyRunning()) {

			openDashboard();

			return;
		}

		SpringApplication.run(
				DashboardDemandasApplication.class,
				args
		);
	}

	/**
	 * Verifica se uma instância do Dashboard
	 * já está respondendo na porta 8080.
	 */
	private static boolean isDashboardAlreadyRunning() {

		HttpURLConnection connection = null;

		try {

			URI uri = URI.create(
					DASHBOARD_URL
							+ "/dashboard/summary"
			);

			connection =
					(HttpURLConnection)
							uri.toURL()
									.openConnection();

			connection.setRequestMethod("GET");
			connection.setConnectTimeout(800);
			connection.setReadTimeout(800);

			int responseCode =
					connection.getResponseCode();

			return responseCode >= 200
					&& responseCode < 300;

		} catch (Exception exception) {

			return false;

		} finally {

			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	/**
	 * Abre o Dashboard no navegador padrão.
	 */
	private static void openDashboard() {

		try {

			String operatingSystem =
					System.getProperty("os.name")
							.toLowerCase();

			if (operatingSystem.contains("win")) {

				new ProcessBuilder(
						"rundll32",
						"url.dll,FileProtocolHandler",
						DASHBOARD_URL
				).start();

				return;
			}

			if (Desktop.isDesktopSupported()) {

				Desktop.getDesktop().browse(
						URI.create(DASHBOARD_URL)
				);
			}

		} catch (Exception exception) {

			System.err.println(
					"Não foi possível abrir o navegador automaticamente."
			);
		}
	}

	/**
	 * Cria uma pasta gravável para o SQLite.
	 *
	 * Exemplo:
	 * C:/Users/usuario/AppData/Local/DashboardDemandas/data
	 */
	private static void configureDatabasePath() {

		try {

			String localAppData =
					System.getenv("LOCALAPPDATA");

			Path applicationDirectory;

			if (
					localAppData != null
							&& !localAppData.isBlank()
			) {

				applicationDirectory =
						Paths.get(
								localAppData,
								"DashboardDemandas",
								"data"
						);

			} else {

				applicationDirectory =
						Paths.get(
								System.getProperty("user.home"),
								"DashboardDemandas",
								"data"
						);
			}

			Files.createDirectories(
					applicationDirectory
			);

			Path databasePath =
					applicationDirectory
							.resolve("dashboard.db")
							.toAbsolutePath();

			String normalizedPath =
					databasePath
							.toString()
							.replace("\\", "/");

			System.setProperty(
					"dashboard.database.path",
					normalizedPath
			);

		} catch (IOException exception) {

			throw new IllegalStateException(
					"Não foi possível preparar a pasta do banco de dados.",
					exception
			);
		}
	}
}