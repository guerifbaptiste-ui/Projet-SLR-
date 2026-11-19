package fr.tp.inf112.projects.robotsim.model;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.canvas.model.CanvasChooser;
import fr.tp.inf112.projects.canvas.model.impl.AbstractCanvasPersistenceManager;
import fr.tp.inf112.projects.graph.impl.BasicVertex;
import fr.tp.inf112.projects.robotsim.model.shapes.PositionedShape;

public class RemotePersistenceManager extends AbstractCanvasPersistenceManager {

    private final String serverHost;
    private final int serverPort;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public RemotePersistenceManager(final CanvasChooser canvasChooser) {
        this(canvasChooser, "localhost", 8080);
    }

    public RemotePersistenceManager(final CanvasChooser canvasChooser, final String serverHost, final int serverPort) {
        super(canvasChooser);
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();

        final PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType(PositionedShape.class.getPackageName())
            .allowIfSubType(Component.class.getPackageName())
            .allowIfSubType(BasicVertex.class.getPackageName())
            .allowIfSubType("fr.tp.inf112.projects.canvas")
            .allowIfSubType(ArrayList.class.getName())
            .allowIfSubType(LinkedHashSet.class.getName())
            .build();
        this.objectMapper.activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Canvas read(final String canvasId) throws IOException {
        try {
            final URI uri = new URI("http", null, serverHost, serverPort, "/persistence/" + canvasId, null, null);
            final HttpRequest request = HttpRequest.newBuilder().uri(uri).GET().build();
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body() != null) {
                return objectMapper.readValue(response.body(), Factory.class);
            }
            throw new IOException("HTTP error: " + response.statusCode() + " - " + response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while reading canvas: " + canvasId, e);
        } catch (Exception ex) {
            throw new IOException("Erreur lors de la lecture distante de " + canvasId, ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void persist(Canvas canvasModel) throws IOException {
        try {
            final String json = objectMapper.writeValueAsString(canvasModel);
            final URI uri = new URI("http", null, serverHost, serverPort, "/persistence", null, null);
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 201) {
                throw new IOException("HTTP error while saving: " + response.statusCode() + " - " + response.body());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while persisting canvas", e);
        } catch (Exception ex) {
            throw new IOException("Erreur lors de la sauvegarde distante", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean delete(Canvas canvasModel) throws IOException {
        try {
            final URI uri = new URI("http", null, serverHost, serverPort, "/persistence/" + canvasModel.getId(), null, null);
            final HttpRequest request = HttpRequest.newBuilder().uri(uri).DELETE().build();
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception ex) {
            throw new IOException("Erreur lors de la suppression distante", ex);
        }
    }
}
