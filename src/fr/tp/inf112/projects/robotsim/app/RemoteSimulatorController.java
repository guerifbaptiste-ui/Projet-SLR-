package fr.tp.inf112.projects.robotsim.app;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

import fr.tp.inf112.projects.canvas.controller.Observer;
import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.canvas.model.CanvasPersistenceManager;
import fr.tp.inf112.projects.robotsim.model.Component;
import fr.tp.inf112.projects.robotsim.model.Factory;
import fr.tp.inf112.projects.robotsim.model.FactoryModelChangedNotifier;
import fr.tp.inf112.projects.robotsim.model.LocalFactoryModelChangedNotifier;
import fr.tp.inf112.projects.robotsim.model.shapes.PositionedShape;

public class RemoteSimulatorController extends SimulatorController {

    private static final Logger LOGGER = Logger.getLogger(RemoteSimulatorController.class.getName());

    private static final String SERVICE_HOST = "localhost";
    private static final int SERVICE_PORT = 8080;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final FactoryModelChangedNotifier notifier;
    private Thread updateThread;
    private volatile boolean animationRunning = false;

    public RemoteSimulatorController(final CanvasPersistenceManager persistenceManager) {
        this(null, persistenceManager);
    }

    public RemoteSimulatorController(final Factory factoryModel,
            final CanvasPersistenceManager persistenceManager) {
        super(factoryModel, persistenceManager);
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.notifier = new LocalFactoryModelChangedNotifier();

        final PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType(PositionedShape.class.getPackageName())
            .allowIfSubType(Component.class.getPackageName())
            .allowIfSubType("fr.tp.inf112.projects.graph")
            .allowIfSubType("fr.tp.inf112.projects.canvas")
            .allowIfSubType(ArrayList.class.getName())
            .allowIfSubType(LinkedHashSet.class.getName())
            .build();
        this.objectMapper.activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL);
    }

    @Override
    public boolean addObserver(final Observer observer) {
        return notifier.addObserver(observer);
    }

    @Override
    public boolean removeObserver(final Observer observer) {
        return notifier.removeObserver(observer);
    }

    private Factory getFactoryModel() {
        return (Factory) getCanvas();
    }

    @Override
    public boolean isAnimationRunning() {
        return animationRunning;
    }

    @Override
    public void startAnimation() {
        if (getFactoryModel() == null) {
            return;
        }

        try {
            final String factoryId = getFactoryModel().getId();
            final URI uri = new URI("http", null, SERVICE_HOST, SERVICE_PORT,
                "/simulation/start-simulation/" + factoryId, null, null);
            final HttpRequest request = HttpRequest.newBuilder().uri(uri).GET().build();
            final HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

            LOGGER.info("Démarrage simulation distante : status=" + response.statusCode()
                + " body=" + response.body());

            if (response.statusCode() == 200 && Boolean.parseBoolean(response.body())) {
                animationRunning = true;

                final Factory factory = getFactoryModel();
                final FactorySimulationEventConsumer consumer =
                    new FactorySimulationEventConsumer(this, factory);
                updateThread = new Thread(() -> {
                    try {
                        consumer.consumeMessages();
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Erreur dans le consumer Kafka", e);
                    } finally {
                        animationRunning = false;
                    }
                }, "KafkaConsumer-" + factoryId);
                updateThread.setDaemon(true);
                updateThread.start();
            } else {
                LOGGER.warning("Le microservice n'a pas pu démarrer (réponse: " + response.body() + ")");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du démarrage distant", e);
        }
    }

    @Override
    public void stopAnimation() {
        animationRunning = false;
        if (getFactoryModel() == null) {
            return;
        }

        try {
            final String factoryId = getFactoryModel().getId();
            final URI uri = new URI("http", null, SERVICE_HOST, SERVICE_PORT,
                "/simulation/stop-simulation/" + factoryId, null, null);
            final HttpRequest request = HttpRequest.newBuilder().uri(uri).GET().build();
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (updateThread != null && updateThread.isAlive()) {
                updateThread.interrupt();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'arrêt distant", e);
        }
    }

    public void setCanvas(final String jsonString) {
        try {
            final Factory remoteFactory = objectMapper.readValue(jsonString, Factory.class);
            setCanvas((Canvas) remoteFactory);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Impossible de désérialiser la factory depuis Kafka", e);
        }
    }

    @Override
    public void setCanvas(final Canvas canvasModel) {
        super.setCanvas(canvasModel);
        notifier.notifyObservers();
    }
}
