# Robotic Factory - Simulateur Distribué & Événementiel (SLR)

Projet de simulation d'usine robotisée distribuée développé dans le cadre des TPs SLR (Télécom Paris).

Le projet met en œuvre une architecture **microservices** et **orientée événements (Event-Driven)** combinant **Spring Boot**, **Apache Kafka** et une interface graphique **Java Swing**.

---

## 🏗️ Architecture et Fonctionnement

Le système repose sur un découplage complet entre le moteur de calcul de la simulation et l'interface utilisateur :

```
                  ┌─────────────────────────────────────────┐
                  │      Serveur (Spring Boot / Port 8080)   │
                  │  - Gère le cycle de vie (start / stop)  │
                  │  - Calcule la simulation physique       │
                  │  - Rôle : Kafka Producer                │
                  └────────────────────┬────────────────────┘
                                       │ Publie chaque mouvement
                                       ▼ (JSON complet)
                  ┌─────────────────────────────────────────┐
                  │       Apache Kafka (Port 9092)          │
                  │   Topic : simulation-<factoryId>        │
                  └────────────────────┬────────────────────┘
                                       │ Flux streaming temps réel
                                       ▼ (consommation asynchrone)
                  ┌─────────────────────────────────────────┐
                  │       Client Swing (SimulatorApp)       │
                  │  - Rôle : Kafka Consumer                │
                  │  - Rafraîchit la vue à chaque événement │
                  └─────────────────────────────────────────┘
```

1. **Le Serveur (Moteur de simulation & Producer) :**
   * Expose des endpoints REST pour démarrer (`/simulation/start-simulation/{id}`) et arrêter la simulation.
   * Fait évoluer le monde physique (déplacements des robots avec évitement d'obstacles et recherche de chemin Dijkstra).
   * À chaque déplacement, publie l'état sérialisé en JSON de l'usine sur un topic Kafka dédié.

2. **Le Broker Kafka :**
   * Canal de streaming temps réel qui découple l'interface du serveur et supprime tout besoin de polling HTTP.

3. **Le Client Graphique (Consumer) :**
   * S'abonne au topic Kafka de l'usine via un thread d'écoute en arrière-plan (`FactorySimulationEventConsumer`).
   * Réceptionne les messages JSON et notifie la vue Swing (`CanvasViewer`) en temps réel.

---

## 📋 Prérequis

* **Java 17** ou supérieur
* **Apache Kafka** (ex: version `3.8.1` téléchargée localement)
* **Maven** (le wrapper `./mvnw` est déjà inclus dans le projet)

---

## 🚀 Comment lancer le projet (Guide pas à pas)

Le démarrage s'effectue en **4 étapes simples** :

### Étape 1 : Démarrer Zookeeper et Kafka

Ouvrez **deux terminaux** distincts et lancez les services Kafka dans l'ordre :

* **Terminal 1 — Lancer Zookeeper :**
  ```bash
  kafka_2.13-3.8.1/bin/zookeeper-server-start.sh kafka_2.13-3.8.1/config/zookeeper.properties
  ```
  *(Laissez ce terminal tourner)*

* **Terminal 2 — Lancer le broker Kafka :**
  ```bash
  kafka_2.13-3.8.1/bin/kafka-server-start.sh kafka_2.13-3.8.1/config/server.properties
  ```
  *(Laissez ce terminal tourner également)*

---

### Étape 2 : Démarrer le Serveur Spring Boot

* **Option A (Via VS Code - Recommandé) :**
  1. Ouvrez le panneau *Run and Debug* (`Cmd + Shift + D` ou `Ctrl + Shift + D`).
  2. Sélectionnez la configuration **`Serveur`** (`fr.tp.inf112.projects.robotsim.server.Serveur`).
  3. Cliquez sur **Play ▶** (F5).

* **Option B (En ligne de commande) :**
  ```bash
  ./mvnw spring-boot:run -Dspring-boot.run.main-class=fr.tp.inf112.projects.robotsim.server.Serveur
  ```
  *(Attendez le message confirmant que Spring Boot a démarré sur le port 8080)*

---

### Étape 3 : Lancer l'Application Client (IHM Swing)

* **Option A (Via VS Code) :**
  1. Dans le panneau *Run and Debug*, sélectionnez **`Run SimulatorApplication`**.
  2. Cliquez sur **Play ▶** (F5).

* **Option B (En ligne de commande) :**
  ```bash
  ./mvnw compile exec:java -Dexec.mainClass="fr.tp.inf112.projects.robotsim.app.SimulatorApplication"
  ```

Une fenêtre graphique Swing intitulée **Canvas Viewer** s'ouvre, affichant l'usine avec ses salles, convoyeurs, stations de recharge et robots.

---

### Étape 4 : Lancer la Simulation

Dans le menu de la fenêtre graphique :
1. Cliquez sur **Simulation** > **Start**.
2. Les robots commencent à se déplacer en temps réel en naviguant entre les machines grâce aux événements Kafka !
3. Pour stopper : cliquez sur **Simulation** > **Stop**.

---

## 🧪 Tests Unitaires

Pour vérifier la conformité de la sérialisation / désérialisation polymorphique Jackson :

```bash
./mvnw test
```

Ou lancez la configuration **`Run TestRobotSimSerializationJSON`** depuis VS Code.

---

## 📁 Structure Principale des Fichiers

* **`src/fr/tp/inf112/projects/robotsim/model/`** :
  * Modèle de domaine : `Factory`, `Robot`, `Room`, `Machine`, `Conveyor`, `PositionedShape`...
  * `FactoryModelChangedNotifier` & `LocalFactoryModelChangedNotifier` : Abstraction SRP du patron Observer.
* **`src/fr/tp/inf112/projects/robotsim/server/`** :
  * `Serveur.java` : Point d'entrée Spring Boot.
  * `SimulationController.java` : API REST de contrôle de la simulation.
  * `SimulationServiceConfig.java` : Configuration du Producer Kafka avec Jackson.
  * `KafkaFactoryModelChangeNotifier.java` : Publication des événements vers Kafka.
  * `SimulationServiceUtils.java` : Propriétés et constantes Kafka.
* **`src/fr/tp/inf112/projects/robotsim/app/`** :
  * `SimulatorApplication.java` : Point d'entrée de l'IHM Swing.
  * `RemoteSimulatorController.java` : Contrôleur distant connecté à Kafka.
  * `FactorySimulationEventConsumer.java` : Consumer Kafka en arrière-plan.
