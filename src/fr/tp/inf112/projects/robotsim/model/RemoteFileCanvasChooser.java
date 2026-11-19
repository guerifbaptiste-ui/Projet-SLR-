package fr.tp.inf112.projects.robotsim.model;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import javax.swing.JOptionPane;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.tp.inf112.projects.canvas.view.FileCanvasChooser;

public class RemoteFileCanvasChooser extends FileCanvasChooser {
    private final String serverHost;
    private final int serverPort;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public RemoteFileCanvasChooser() {
        this("localhost", 8080);
    }

    public RemoteFileCanvasChooser(String serverHost, int serverPort) {
        super("factory", "Robot Factory (*.factory)");
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }

    @Override
    public String browseCanvases(boolean open) {
        if (open) {
            // 1. Récupérer les fichiers distants via HTTP REST
            String[] files = getRemoteFiles();

            if (files == null || files.length == 0) {
                JOptionPane.showMessageDialog(null, 
                    "Aucun modèle trouvé sur le serveur.", 
                    "Information", 
                    JOptionPane.INFORMATION_MESSAGE);
                return null;
            }

            // 2. Boîte avec menu déroulant
            Object selectedFile = JOptionPane.showInputDialog(
                null,                                // Fenêtre parente
                "Sélectionnez le modèle à charger :", // Message
                "Ouvrir un modèle distant",         // Titre
                JOptionPane.QUESTION_MESSAGE,        // Type de message
                null,                                // Icône personnalisée (null = défaut)
                files,                               // Le tableau -> génère le menu déroulant !
                files[0]                             // Valeur sélectionnée par défaut
            );

            // Retourne le nom du fichier choisi (ou null si annulé)
            return (String) selectedFile;

        } else {
            // Sauvegarde : simple boîte avec champ de saisie texte
            String fileName = JOptionPane.showInputDialog(
                null,
                "Entrez le nom du modèle à sauvegarder :",
                "Sauvegarder le modèle",
                JOptionPane.PLAIN_MESSAGE
            );

            return fileName;
        }
    }

    private String[] getRemoteFiles() {
        try {
            final URI uri = new URI("http", null, serverHost, serverPort, "/persistence/list", null, null);
            final HttpRequest request = HttpRequest.newBuilder().uri(uri).GET().build();
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body() != null) {
                List<String> list = objectMapper.readValue(response.body(), new TypeReference<List<String>>() {});
                return list.toArray(new String[0]);
            }
            return new String[0];
        } catch (Exception e) {
            e.printStackTrace();
            return new String[0];
        }
    }
}
