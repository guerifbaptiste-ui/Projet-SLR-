package fr.tp.inf112.projects.robotsim.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import fr.tp.inf112.projects.canvas.controller.Observable;
import fr.tp.inf112.projects.canvas.controller.Observer;
import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.canvas.model.Figure;
import fr.tp.inf112.projects.canvas.model.Style;
import fr.tp.inf112.projects.robotsim.model.shapes.PositionedShape;
import fr.tp.inf112.projects.robotsim.model.shapes.RectangularShape;

public class Factory extends Component implements Canvas, Observable {

    private static final long serialVersionUID = 5156526483612458192L;

    private static final ComponentStyle DEFAULT = new ComponentStyle(5.0f);

    @JsonManagedReference
    private final List<Component> components;

    @JsonIgnore
    private transient List<Observer> observers;

    @JsonIgnore
    private transient FactoryModelChangedNotifier notifier;

    private transient boolean simulationStarted;

    private int pathResolution;

    public Factory() {
        this(0, 0, null, 0);
    }

    public Factory(final int width,
            final int height,
            final String name,
            final int pathResolution) {
        super(null, new RectangularShape(0, 0, width, height), name);

        this.pathResolution = pathResolution;
        components = new ArrayList<>();
        observers = null;
        simulationStarted = false;
    }

    @JsonIgnore
    public List<Observer> getObservers() {
        if (observers == null) {
            observers = new ArrayList<>();
        }

        return observers;
    }

    public void setNotifier(FactoryModelChangedNotifier notifier) {
        this.notifier = notifier;
    }

    @Override
    public boolean addObserver(Observer observer) {
        if (notifier != null) {
            notifier.addObserver(observer);
        }
        return getObservers().add(observer);
    }

    @Override
    public boolean removeObserver(Observer observer) {
        if (notifier != null) {
            notifier.removeObserver(observer);
        }
        return getObservers().remove(observer);
    }

    @Override
    public void notifyObservers() {
        if (notifier != null) {
            notifier.notifyObservers();
        } else {
            for (final Observer observer : getObservers()) {
                observer.modelChanged();
            }
        }
    }

    public boolean addComponent(final Component component) {
        if (components.add(component)) {
            notifyObservers();

            return true;
        }

        return false;
    }

    public boolean removeComponent(final Component component) {
        if (components.remove(component)) {
            notifyObservers();

            return true;
        }

        return false;
    }

    public List<Component> getComponents() {
        return components;
    }

    @JsonIgnore
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Collection<Figure> getFigures() {
        return (Collection) components;
    }

    public int getPathResolution() {
        return pathResolution;
    }

    public void setPathResolution(int pathResolution) {
        this.pathResolution = pathResolution;
    }

    @Override
    public String toString() {
        return super.toString() + " components=" + components + "]";
    }

    public boolean isSimulationStarted() {
        return simulationStarted;
    }

    public void setSimulationStarted(final boolean simulationStarted) {
        this.simulationStarted = simulationStarted;
    }

    public void startSimulation() {
        if (!isSimulationStarted()) {
            this.simulationStarted = true;
            notifyObservers();

            while (isSimulationStarted()) {
                behave();

                try {
                    Thread.sleep(100);
                } catch (final InterruptedException ex) {
                    System.err.println("Simulation was abruptly interrupted");
                }
            }
        }
    }

    public void stopSimulation() {
        if (isSimulationStarted()) {
            this.simulationStarted = false;

            notifyObservers();
        }
    }

    @Override
    public boolean behave() {
        boolean behaved = true;

        for (final Component component : getComponents()) {
            behaved = component.behave() || behaved;
        }

        return behaved;
    }

    @Override
    public Style getStyle() {
        return DEFAULT;
    }

    public boolean hasObstacleAt(final PositionedShape shape) {
        for (final Component component : getComponents()) {
            if (component.overlays(shape) && !component.canBeOverlayed(shape)) {
                return true;
            }
        }

        return false;
    }

    public boolean hasMobileComponentAt(final PositionedShape shape,
            final Component movingComponent) {
        for (final Component component : getComponents()) {
            if (component != movingComponent && component.isMobile() && component.overlays(shape)) {
                return true;
            }
        }

        return false;
    }

    public Component getComponentAt(Position position, Component componentToIgnore) {
        for (final Component component : getComponents()) {
            if (component.getPosition().equals(position) && component != componentToIgnore) {
                return component;
            }
        }
        return null;
    }

    public Component getMobileComponentAt(final Position position,
            final Component ignoredComponent) {
        if (position == null) {
            return null;
        }

        return getMobileComponentAt(new RectangularShape(position.getxCoordinate(), position.getyCoordinate(), 2, 2), ignoredComponent);
    }

    public Component getMobileComponentAt(final PositionedShape shape,
            final Component ignoredComponent) {
        if (shape == null) {
            return null;
        }

        for (final Component component : getComponents()) {
            if (component != ignoredComponent && component.isMobile() && component.overlays(shape)) {
                return component;
            }
        }

        return null;
    }
}
