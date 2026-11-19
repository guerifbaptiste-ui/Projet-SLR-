package fr.tp.inf112.projects.robotsim.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.robotsim.model.Component;
import fr.tp.inf112.projects.robotsim.model.Factory;
import fr.tp.inf112.projects.robotsim.model.FactoryPersistenceManager;
import fr.tp.inf112.projects.robotsim.model.shapes.PositionedShape;

@SpringBootApplication
@RestController
@CrossOrigin(origins = "*")
public class Serveur {

    private final FactoryPersistenceManager localPersistenceManager;

    public Serveur() {
        this.localPersistenceManager = new FactoryPersistenceManager(null);
    }

    public static void main(String[] args) {
        System.out.println("Démarrage du microservice Spring Boot (Persistance & Simulation)...");
        SpringApplication.run(Serveur.class, args);
    }

    @GetMapping({"/persistence", "/factories"})
    public ResponseEntity<?> handleGetRoot(@RequestParam(value = "factoryId", required = false) String factoryId) {
        if (factoryId != null && !factoryId.isBlank()) {
            return getFactory(factoryId);
        }
        return ResponseEntity.ok(listFactories());
    }

    @GetMapping({"/persistence/list", "/factories/list"})
    public List<String> listFactories() {
        File dir = new File(".");
        String[] files = dir.list((d, name) -> name.endsWith(".ser") || name.endsWith(".factory"));
        return files != null ? Arrays.asList(files) : Collections.emptyList();
    }

    @GetMapping({"/persistence/{factoryId}", "/factories/{factoryId}"})
    public ResponseEntity<?> getFactory(@PathVariable("factoryId") String factoryId) {
        try {
            Canvas canvas = localPersistenceManager.read(factoryId);
            if (canvas instanceof Factory) {
                return ResponseEntity.ok((Factory) canvas);
            } else if (canvas != null) {
                return ResponseEntity.ok(canvas);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Modèle introuvable : " + factoryId);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Erreur lors de la lecture du fichier : " + e.getMessage());
        }
    }

    @PostMapping({"/persistence", "/factories"})
    public ResponseEntity<String> persistFactory(@RequestBody Factory factory) {
        try {
            localPersistenceManager.persist(factory);
            return ResponseEntity.status(HttpStatus.CREATED).body("Factory sauvegardée avec succès.");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la sauvegarde : " + e.getMessage());
        }
    }

    @DeleteMapping({"/persistence/{factoryId}", "/factories/{factoryId}"})
    public ResponseEntity<String> deleteFactory(@PathVariable("factoryId") String factoryId) {
        try {
            Canvas canvas = localPersistenceManager.read(factoryId);
            if (canvas != null && localPersistenceManager.delete(canvas)) {
                return ResponseEntity.ok("Modèle supprimé avec succès : " + factoryId);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Fichier introuvable pour suppression : " + factoryId);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la suppression : " + e.getMessage());
        }
    }
}
