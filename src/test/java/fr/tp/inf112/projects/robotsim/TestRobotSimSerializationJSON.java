package fr.tp.inf112.projects.robotsim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

import fr.tp.inf112.projects.canvas.model.impl.BasicPoint;
import fr.tp.inf112.projects.graph.impl.BasicVertex;
import fr.tp.inf112.projects.robotsim.model.Area;
import fr.tp.inf112.projects.robotsim.model.Battery;
import fr.tp.inf112.projects.robotsim.model.ChargingStation;
import fr.tp.inf112.projects.robotsim.model.Component;
import fr.tp.inf112.projects.robotsim.model.Conveyor;
import fr.tp.inf112.projects.robotsim.model.Door;
import fr.tp.inf112.projects.robotsim.model.Factory;
import fr.tp.inf112.projects.robotsim.model.Machine;
import fr.tp.inf112.projects.robotsim.model.Robot;
import fr.tp.inf112.projects.robotsim.model.Room;
import fr.tp.inf112.projects.robotsim.model.path.CustomDijkstraFactoryPathFinder;
import fr.tp.inf112.projects.robotsim.model.path.FactoryPathFinder;
import fr.tp.inf112.projects.robotsim.model.path.JGraphTDijkstraFactoryPathFinder;
import fr.tp.inf112.projects.robotsim.model.shapes.BasicPolygonShape;
import fr.tp.inf112.projects.robotsim.model.shapes.CircularShape;
import fr.tp.inf112.projects.robotsim.model.shapes.PositionedShape;
import fr.tp.inf112.projects.robotsim.model.shapes.RectangularShape;

public class TestRobotSimSerializationJSON {

    private static final Logger LOGGER = Logger.getLogger(TestRobotSimSerializationJSON.class.getName());

    private final ObjectMapper objectMapper;
    private final Factory myFactory;

    public TestRobotSimSerializationJSON() {
        objectMapper = new ObjectMapper();
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType(PositionedShape.class.getPackageName())
            .allowIfSubType(Component.class.getPackageName())
            .allowIfSubType(BasicVertex.class.getPackageName())
            .allowIfSubType("fr.tp.inf112.projects.canvas")
            .allowIfSubType(ArrayList.class.getName())
            .allowIfSubType(LinkedHashSet.class.getName())
            .build();
        objectMapper.activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL);
        myFactory = createTestFactory();
    }

    public static Factory createTestFactory() {
        final Factory factory = new Factory(200, 200, "Simple Test Puck Factory", 5);
        factory.setId("test.factory");
        final Room room1 = new Room(factory, new RectangularShape(20, 20, 75, 75), "Production Room 1");
        new Door(room1, Room.WALL.BOTTOM, 10, 20, true, "Entrance");
        final Area area1 = new Area(room1, new RectangularShape(35, 35, 50, 50), "Production Area 1");
        final Machine machine1 = new Machine(area1, new RectangularShape(50, 50, 15, 15), "Machine 1");

        final Room room2 = new Room(factory, new RectangularShape(120, 22, 75, 75), "Production Room 2");
        new Door(room2, Room.WALL.LEFT, 10, 20, true, "Entrance");
        final Area area2 = new Area(room2, new RectangularShape(135, 35, 50, 50), "Production Area 1");
        final Machine machine2 = new Machine(area2, new RectangularShape(150, 50, 15, 15), "Machine 1");

        final int baselineSize = 3;
        final int xCoordinate = 10;
        final int yCoordinate = 165;
        final int width = 10;
        final int height = 30;
        final BasicPolygonShape conveyorShape = new BasicPolygonShape();
        conveyorShape.addPoint(new BasicPoint(xCoordinate, yCoordinate));
        conveyorShape.addPoint(new BasicPoint(xCoordinate + width, yCoordinate));
        conveyorShape.addPoint(new BasicPoint(xCoordinate + width, yCoordinate + height - baselineSize));
        conveyorShape.addPoint(new BasicPoint(xCoordinate + width + baselineSize, yCoordinate + height - baselineSize));
        conveyorShape.addPoint(new BasicPoint(xCoordinate + width + baselineSize, yCoordinate + height));
        conveyorShape.addPoint(new BasicPoint(xCoordinate - baselineSize, yCoordinate + height));
        conveyorShape.addPoint(new BasicPoint(xCoordinate - baselineSize, yCoordinate + height - baselineSize));
        conveyorShape.addPoint(new BasicPoint(xCoordinate, yCoordinate + height - baselineSize));
        new Conveyor(factory, conveyorShape, "Conveyor 1");

        final Room chargingRoom = new Room(factory, new RectangularShape(125, 125, 50, 50), "Charging Room");
        new Door(chargingRoom, Room.WALL.RIGHT, 10, 20, false, "Entrance");
        new ChargingStation(factory, new RectangularShape(150, 145, 15, 15), "Charging Station");

        final int factoryPathResolution = factory.getPathResolution();
        final FactoryPathFinder jgraphPathFinder = new JGraphTDijkstraFactoryPathFinder(factory, factoryPathResolution);
        final Robot robot1 = new Robot(factory, jgraphPathFinder, new CircularShape(5, 5, 2), new Battery(10), "Robot 1");
        robot1.addTargetComponent(machine1);
        robot1.addTargetComponent(machine2);

        final FactoryPathFinder customPathFinder = new CustomDijkstraFactoryPathFinder(factory, factoryPathResolution);
        final Robot robot2 = new Robot(factory, customPathFinder, new CircularShape(45, 5, 2), new Battery(10), "Robot 2");
        robot2.addTargetComponent(machine1);
        robot2.addTargetComponent(machine2);

        return factory;
    }

    public Factory getRoundTripFactory() throws JsonProcessingException {
        final String json = objectMapper.writeValueAsString(myFactory);
        return objectMapper.readValue(json, Factory.class);
    }

    public static void main(String[] args) {
        try {
            TestRobotSimSerializationJSON test = new TestRobotSimSerializationJSON();
            test.testSerialization();
            System.out.println("TEST SUCCESS: Factory serialized and deserialized successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @org.junit.Test
    @org.junit.jupiter.api.Test
    public void testSerialization() throws JsonProcessingException {
        final String factoryAsJsonString = objectMapper.writeValueAsString(myFactory);
        LOGGER.info(factoryAsJsonString);

        final Factory roundTrip = objectMapper.readValue(factoryAsJsonString, Factory.class);
        LOGGER.info(roundTrip.toString());

        assertNotNull(roundTrip);
        assertEquals(myFactory.getId(), roundTrip.getId());
        assertEquals(myFactory.getWidth(), roundTrip.getWidth());
        assertEquals(myFactory.getHeight(), roundTrip.getHeight());
        assertEquals(myFactory.getComponents().size(), roundTrip.getComponents().size());
    }
}
