package fr.tp.inf112.projects.robotsim.app;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import fr.tp.inf112.projects.robotsim.model.Factory;
import fr.tp.inf112.projects.robotsim.server.SimulationServiceUtils;

public class FactorySimulationEventConsumer {

    private static final Logger LOGGER = Logger.getLogger(FactorySimulationEventConsumer.class.getName());

    private final RemoteSimulatorController controller;
    private final String topicName;

    public FactorySimulationEventConsumer(final RemoteSimulatorController controller,
            final Factory factoryModel) {
        this.controller = controller;
        this.topicName = SimulationServiceUtils.getTopicName(factoryModel);
    }

    public void consumeMessages() {
        final Properties props = SimulationServiceUtils.getDefaultConsumerProperties();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(topicName));
            LOGGER.info("Consumer Kafka abonné au topic : " + topicName);

            while (controller.isAnimationRunning()) {
                try {
                    final ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                    records.forEach(record -> {
                        try {
                            LOGGER.fine("Message Kafka reçu depuis " + topicName);
                            controller.setCanvas(record.value());
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Erreur lors du traitement du message Kafka", e);
                        }
                    });
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Erreur dans la boucle Kafka consumer", e);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur fatale dans FactorySimulationEventConsumer", e);
        } finally {
            LOGGER.info("Consumer Kafka arrêté pour le topic : " + topicName);
        }
    }
}
