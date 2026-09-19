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
import java.nio.file.StandardCopyOption;

/**
 * Inicializa a aplicação e prepara os recursos locais.
 */
@SpringBootApplication
public class DashboardDemandasApplication {

	private static final String DASHBOARD_URL =
			"http://localhost:8080";

	private static final String APPLICATION_FOLDER =
			"Dashboard de Prazos e Demandas";

	private static final String LEGACY_APPLICATION_FOLDER =
			"DashboardDemandas";

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
	 * Define uma pasta gravável para o banco SQLite.
	 *
	 * No Windows:
	 *
	 * C:/Users/usuario/AppData/Local/
	 * Dashboard de Prazos e Demandas/data/dashboard.db
	 *
	 * Também migra automaticamente o banco usado
	 * pela versão antiga, caso ele ainda exista.
	 */
	private static void configureDatabasePath() {

		try {

			String localAppData =
					System.getenv("LOCALAPPDATA");

			Path applicationDirectory;
			Path legacyDirectory;

			if (
					localAppData != null
							&& !localAppData.isBlank()
			) {

				applicationDirectory =
						Paths.get(
								localAppData,
								APPLICATION_FOLDER,
								"data"
						);

				legacyDirectory =
						Paths.get(
								localAppData,
								LEGACY_APPLICATION_FOLDER,
								"data"
						);

			} else {

				String userHome =
						System.getProperty("user.home");

				applicationDirectory =
						Paths.get(
								userHome,
								APPLICATION_FOLDER,
								"data"
						);

				legacyDirectory =
						Paths.get(
								userHome,
								LEGACY_APPLICATION_FOLDER,
								"data"
						);
			}

			Files.createDirectories(
					applicationDirectory
			);

			migrateLegacyDatabase(
					legacyDirectory,
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

			System.out.println(
					"Banco de dados: "
							+ normalizedPath
			);

		} catch (IOException exception) {

			throw new IllegalStateException(
					"Não foi possível preparar a pasta do banco de dados.",
					exception
			);
		}
	}

	/**
	 * Preserva dados de versões anteriores.
	 *
	 * A migração só acontece quando o banco novo
	 * ainda não existe.
	 */
	private static void migrateLegacyDatabase(
			Path legacyDirectory,
			Path newDirectory
	) throws IOException {

		Path newDatabase =
				newDirectory.resolve(
						"dashboard.db"
				);

		if (Files.exists(newDatabase)) {
			return;
		}

		Path legacyDatabase =
				legacyDirectory.resolve(
						"dashboard.db"
				);

		if (!Files.exists(legacyDatabase)) {
			return;
		}

		Files.copy(
				legacyDatabase,
				newDatabase,
				StandardCopyOption.REPLACE_EXISTING
		);

		copyIfExists(
				legacyDirectory.resolve(
						"dashboard.db-wal"
				),
				newDirectory.resolve(
						"dashboard.db-wal"
				)
		);

		copyIfExists(
				legacyDirectory.resolve(
						"dashboard.db-shm"
				),
				newDirectory.resolve(
						"dashboard.db-shm"
				)
		);

		System.out.println(
				"Banco de dados antigo migrado para a nova pasta da aplicação."
		);
	}

	private static void copyIfExists(
			Path source,
			Path destination
	) throws IOException {

		if (!Files.exists(source)) {
			return;
		}

		Files.copy(
				source,
				destination,
				StandardCopyOption.REPLACE_EXISTING
		);
	}
}