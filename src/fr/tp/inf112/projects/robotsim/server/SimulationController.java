package fr.tp.inf112.projects.robotsim.server;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.robotsim.app.SimulatorApplication;
import fr.tp.inf112.projects.robotsim.model.Factory;
import fr.tp.inf112.projects.robotsim.model.FactoryPersistenceManager;

@RestController
@RequestMapping("/simulation")
@CrossOrigin(origins = "*")
public class SimulationController {

    private static final Logger LOGGER = Logger.getLogger(SimulationController.class.getName());

    private final FactoryPersistenceManager persistenceManager;
    private final Map<String, Factory> simulatedFactories;
    private final Map<String, Thread> simulationThreads;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> simulationEventTemplate;

    @Autowired
    public SimulationController(ObjectMapper objectMapper, KafkaTemplate<String, String> simulationEventTemplate) {
        this.persistenceManager = new FactoryPersistenceManager(null);
        this.simulatedFactories = new ConcurrentHashMap<>();
        this.simulationThreads = new ConcurrentHashMap<>();
        this.objectMapper = objectMapper;
        this.simulationEventTemplate = simulationEventTemplate;
    }

    @GetMapping({"/start-simulation/{factoryId}", "/start/{factoryId}"})
    public ResponseEntity<Boolean> startSimulation(@PathVariable("factoryId") String factoryId) {
        LOGGER.info("Demande de demarrage de simulation pour le modele : " + factoryId);

        try {
            stopSimulationInternal(factoryId);

            Factory factory = null;
            try {
                Canvas canvas = persistenceManager.read(factoryId);
                if (canvas instanceof Factory) {
                    factory = (Factory) canvas;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Lecture du fichier " + factoryId + " impossible, tentative de generation automatique...", e);
            }

            // Fallback pour test.factory si le fichier n'est pas lisible
            if (factory == null && factoryId.contains("test.factory")) {
                LOGGER.info("Generation du modele test.factory pour la simulation...");
                factory = SimulatorApplication.createDefaultFactory();
            }

            if (factory == null) {
                LOGGER.severe("Impossible de trouver ou charger le modele : " + factoryId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(false);
            }

            simulatedFactories.put(factoryId, factory);

            final KafkaFactoryModelChangeNotifier notifier = new KafkaFactoryModelChangeNotifier(factory, simulationEventTemplate);
            factory.setNotifier(notifier);

            final Factory factoryToSimulate = factory;
            Thread simThread = new Thread(() -> {
                LOGGER.info("Thread de simulation demarre pour : " + factoryId);
                try {
                    factoryToSimulate.startSimulation();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Erreur pendant la simulation de " + factoryId, e);
                } finally {
                    LOGGER.info("Thread de simulation termine pour : " + factoryId);
                }
            }, "SimThread-" + factoryId);

            simulationThreads.put(factoryId, simThread);
            simThread.start();

            LOGGER.info("Simulation lancee avec succes pour : " + factoryId);
            return ResponseEntity.ok(true);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du demarrage de la simulation de " + factoryId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
        }
    }

    @GetMapping(value = {"/{factoryId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getSimulatedFactory(@PathVariable("factoryId") String factoryId) {
        Factory factory = simulatedFactories.get(factoryId);
        if (factory != null) {
            try {
                final String json = objectMapper.writeValueAsString(factory);
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur lors de la serialisation de la factory " + factoryId, e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        }
        LOGGER.fine("Modele non trouve dans les simulations actives : " + factoryId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }

    @GetMapping({"/stop-simulation/{factoryId}", "/stop/{factoryId}"})
    public ResponseEntity<Boolean> stopSimulation(@PathVariable("factoryId") String factoryId) {
        LOGGER.info("Demande d'arret de simulation pour : " + factoryId);
        boolean stopped = stopSimulationInternal(factoryId);
        return ResponseEntity.ok(stopped);
    }

    private boolean stopSimulationInternal(String factoryId) {
        Factory factory = simulatedFactories.remove(factoryId);
        if (factory != null) {
            factory.stopSimulation();
        }

        Thread thread = simulationThreads.remove(factoryId);
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }

        return factory != null;
    }
}
