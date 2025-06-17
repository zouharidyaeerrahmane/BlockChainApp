package fr.inventory;

import fr.inventory.utils.DatabaseUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    
    private static final String APP_TITLE = "Système de Gestion d'Inventaire avec Blockchain";
    private static final String APP_VERSION = "1.0.0";
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // Load main FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Scene scene = new Scene(loader.load());
            
            // Load CSS
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            // Configure stage
            primaryStage.setTitle(APP_TITLE + " v" + APP_VERSION);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1200);
            primaryStage.setMinHeight(800);
            primaryStage.setMaximized(true);
            
            // Set application icon (if available)
            try {
                primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/app-icon.png")));
            } catch (Exception e) {
                // Icon not found, continue without it
                System.out.println("Application icon not found, using default");
            }
            
            // Set close request handler
            primaryStage.setOnCloseRequest(event -> {
                // Cleanup database connection
                DatabaseUtils.closeConnection();
                Platform.exit();
                System.exit(0);
            });
            
            // Show the stage
            primaryStage.show();
            
            System.out.println("✅ Application démarrée avec succès");
            System.out.println("📱 Interface utilisateur chargée");
            System.out.println("🗄️ Base de données initialisée");
            System.out.println("⛓️ Prêt pour la connexion blockchain");
            
        } catch (IOException e) {
            System.err.println("❌ Erreur lors du chargement de l'interface: " + e.getMessage());
            e.printStackTrace();
            Platform.exit();
        }
    }
    
    @Override
    public void init() throws Exception {
        super.init();
        
        System.out.println("🚀 Initialisation de l'application...");
        System.out.println("📋 " + APP_TITLE);
        System.out.println("🔢 Version: " + APP_VERSION);
        System.out.println("☕ Java Version: " + System.getProperty("java.version"));
        System.out.println("🖥️ JavaFX Version: " + System.getProperty("javafx.version"));
        
        // Initialize database
        try {
            // Database is initialized in DatabaseUtils static block
            System.out.println("✅ Base de données H2 initialisée");
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'initialisation de la base de données: " + e.getMessage());
            throw e;
        }
    }
    
    @Override
    public void stop() throws Exception {
        super.stop();
        
        System.out.println("🛑 Arrêt de l'application...");
        
        // Cleanup database connection
        DatabaseUtils.closeConnection();
        System.out.println("✅ Connexion base de données fermée");
        
        System.out.println("👋 Application arrêtée proprement");
    }
    
    public static void main(String[] args) {
        // Set system properties for better performance
        System.setProperty("javafx.animation.pulse", "60");
        System.setProperty("quantum.multithreaded", "false");
        
        // Print startup information
        System.out.println("=" .repeat(60));
        System.out.println("🏭 SYSTÈME DE GESTION D'INVENTAIRE AVEC BLOCKCHAIN");
        System.out.println("=" .repeat(60));
        System.out.println("🎯 Projet étudiant - Architecture DAO/Service/Controller");
        System.out.println("⛓️ Intégration Blockchain avec Ganache et Web3j");
        System.out.println("🖥️ Interface JavaFX moderne et intuitive");
        System.out.println("=" .repeat(60));
        
        // Check for required system properties
        checkSystemRequirements();
        
        // Launch JavaFX application
        launch(args);
    }
    
    private static void checkSystemRequirements() {
        System.out.println("🔍 Vérification des prérequis système...");
        
        // Check Java version
        String javaVersion = System.getProperty("java.version");
        System.out.println("☕ Java: " + javaVersion);
        
        // Check available memory
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        System.out.println("💾 Mémoire disponible: " + maxMemory + " MB");
        
        // Check for JavaFX
        try {
            Class.forName("javafx.application.Application");
            System.out.println("🎨 JavaFX: Disponible");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ JavaFX non trouvé!");
            System.err.println("💡 Assurez-vous d'utiliser un JDK avec JavaFX ou d'ajouter les modules JavaFX");
        }
        
        System.out.println("✅ Vérifications terminées");
        System.out.println("");
        
        // Print instructions
        System.out.println("📋 INSTRUCTIONS D'UTILISATION:");
        System.out.println("1. Lancez Ganache sur http://127.0.0.1:7545");
        System.out.println("2. Allez dans l'onglet Blockchain pour vous connecter");
        System.out.println("3. Déployez le contrat smart");
        System.out.println("4. Commencez à gérer vos produits et transactions");
        System.out.println("");
    }
}