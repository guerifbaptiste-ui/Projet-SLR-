package fr.tp.inf112.projects.robotsim.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import fr.tp.inf112.projects.canvas.model.Style;
import fr.tp.inf112.projects.canvas.model.impl.RGBColor;
import fr.tp.inf112.projects.robotsim.model.motion.Motion;
import fr.tp.inf112.projects.robotsim.model.path.FactoryPathFinder;
import fr.tp.inf112.projects.robotsim.model.shapes.CircularShape;
import fr.tp.inf112.projects.robotsim.model.shapes.PositionedShape;
import fr.tp.inf112.projects.robotsim.model.shapes.RectangularShape;

public class Robot extends Component {

    private static final long serialVersionUID = -1218857231970296747L;

    private static final Style STYLE = new ComponentStyle(RGBColor.GREEN, RGBColor.BLACK, 3.0f, null);

    private static final Style BLOCKED_STYLE = new ComponentStyle(RGBColor.RED, RGBColor.BLACK, 3.0f, new float[]{4.0f});

    private Battery battery;

    private int speed;

    private List<Component> targetComponents;

    private transient Iterator<Component> targetComponentsIterator;

    private Component currTargetComponent;

    private transient Iterator<Position> currentPathPositionsIter;

    private transient boolean blocked;

    private Position blockedTargetPosition;

    private FactoryPathFinder pathFinder;

    public Robot() {
        this(null, null, null, null, null);
    }

    public Robot(final Factory factory,
            final FactoryPathFinder pathFinder,
            final CircularShape shape,
            final Battery battery,
            final String name) {
        super(factory, shape, name);

        this.pathFinder = pathFinder;

        this.battery = battery;

        targetComponents = new ArrayList<>();
        currTargetComponent = null;
        currentPathPositionsIter = null;
        speed = 5;
        blocked = false;
        blockedTargetPosition = null;
    }

    public Battery getBattery() {
        return battery;
    }

    public void setBattery(final Battery battery) {
        this.battery = battery;
    }

    @Override
    public String toString() {
        return super.toString() + " battery=" + battery + "]";
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(final int speed) {
        this.speed = speed;
    }

    @JsonIgnore
    public FactoryPathFinder getPathFinder() {
        return pathFinder;
    }

    public void setPathFinder(final FactoryPathFinder pathFinder) {
        this.pathFinder = pathFinder;
    }

    @JsonIgnore
    public Component getCurrTargetComponent() {
        return currTargetComponent;
    }

    public void setCurrTargetComponent(final Component currTargetComponent) {
        this.currTargetComponent = currTargetComponent;
    }

    @JsonIgnore
    public Position getBlockedTargetPosition() {
        return blockedTargetPosition;
    }

    public void setBlockedTargetPosition(final Position blockedTargetPosition) {
        this.blockedTargetPosition = blockedTargetPosition;
    }

    public Position findFreeNeighbouringPosition() {
        final Position position = getPosition();
        if (position == null || getFactory() == null) {
            return null;
        }
        final int x = position.getxCoordinate();
        final int y = position.getyCoordinate();
        final int res = getFactory().getPathResolution();
        final int[] dx = {0, 0, res, -res};
        final int[] dy = {res, -res, 0, 0};

        for (int i = 0; i < 4; i++) {
            final int nextX = x + dx[i];
            final int nextY = y + dy[i];

            if (nextX >= 0 && nextY >= 0 && nextX < getFactory().getWidth() && nextY < getFactory().getHeight()) {
                final Position neighbouringPosition = new Position(nextX, nextY);
                final PositionedShape shape = new RectangularShape(nextX, nextY, 2, 2);

                if (!getFactory().hasObstacleAt(shape) && !getFactory().hasMobileComponentAt(shape, this)) {
                    return neighbouringPosition;
                }
            }
        }
        return null;
    }

    private int moveToNextPathPosition() {
        final Motion motion = computeMotion();
        int displacement = (motion == null) ? 0 : motion.moveToTarget();

        if (displacement != 0) {
            notifyObservers();
        } else if (isLivelyLocked()) {
            final Position freeNeighbouringPosition = findFreeNeighbouringPosition();
            if (freeNeighbouringPosition != null && getPosition() != null) {
                final Motion avoidanceMotion = new Motion(getPosition(), freeNeighbouringPosition);
                displacement = avoidanceMotion.moveToTarget();
                this.blockedTargetPosition = null;
                computePathToCurrentTargetComponent();
                notifyObservers();
            }
        }
        return displacement;
    }

    @JsonIgnore
    public List<Component> getTargetComponents() {
        if (targetComponents == null) {
            targetComponents = new ArrayList<>();
        }

        return targetComponents;
    }

    public void setTargetComponents(final List<Component> targetComponents) {
        this.targetComponents = targetComponents;
    }

    public boolean addTargetComponent(final Component targetComponent) {
        return getTargetComponents().add(targetComponent);
    }

    public boolean removeTargetComponent(final Component targetComponent) {
        return getTargetComponents().remove(targetComponent);
    }

    @Override
    public boolean isMobile() {
        return true;
    }

    @Override
    public boolean behave() {
        if (getTargetComponents().isEmpty()) {
            return false;
        }

        if (currTargetComponent == null || hasReachedCurrentTarget()) {
            currTargetComponent = nextTargetComponentToVisit();

            computePathToCurrentTargetComponent();
        }

        return moveToNextPathPosition() != 0;
    }

    private Component nextTargetComponentToVisit() {
        if (targetComponentsIterator == null || !targetComponentsIterator.hasNext()) {
            targetComponentsIterator = getTargetComponents().iterator();
        }

        return targetComponentsIterator.hasNext() ? targetComponentsIterator.next() : null;
    }

    private void computePathToCurrentTargetComponent() {
        if (pathFinder == null || currTargetComponent == null) {
            return;
        }
        final List<Position> currentPathPositions = pathFinder.findPath(this, currTargetComponent);
        currentPathPositionsIter = currentPathPositions == null ? null : currentPathPositions.iterator();
    }

    private Motion computeMotion() {
        if (currentPathPositionsIter == null || !currentPathPositionsIter.hasNext()) {

            // There is no free path to the target
            blocked = true;

            return null;
        }

        final Position targetPosition = getTargetPosition();
        if (targetPosition == null || getFactory() == null) {
            return null;
        }
        final PositionedShape shape = new RectangularShape(targetPosition.getxCoordinate(),
                targetPosition.getyCoordinate(),
                2,
                2);

        // If there is another robot, memorize the blocked target position for the next run
        if (getFactory().hasMobileComponentAt(shape, this)) {
            this.blockedTargetPosition = targetPosition;

            return null;
        }

        // Reset the memorized position
        this.blockedTargetPosition = null;

        return new Motion(getPosition(), targetPosition);
    }

    private Position getTargetPosition() {
        // If a target position was memorized, it means that the robot was blocked during the last iteration 
        // so it waited for another robot to pass. So try to move to this memorized position otherwise move to  
        // the next position from the path
        if (currentPathPositionsIter == null) {
            return this.blockedTargetPosition;
        }
        return this.blockedTargetPosition == null ? (currentPathPositionsIter.hasNext() ? currentPathPositionsIter.next() : null) : this.blockedTargetPosition;
    }

    @JsonIgnore
    public boolean isLivelyLocked() {
        if (blockedTargetPosition == null || getFactory() == null) {
            return false;
        }

        final Component otherComponent = getFactory().getMobileComponentAt(blockedTargetPosition,
                this);

        if (otherComponent instanceof Robot) {
            return getPosition().equals(((Robot) otherComponent).getBlockedTargetPosition());
        }

        return false;
    }

    private boolean hasReachedCurrentTarget() {
        if (getPositionedShape() == null || currTargetComponent == null || currTargetComponent.getPositionedShape() == null) {
            return false;
        }
        return getPositionedShape().overlays(currTargetComponent.getPositionedShape());
    }

    @Override
    public boolean canBeOverlayed(final PositionedShape shape) {
        return true;
    }

    @Override
    public Style getStyle() {
        return blocked ? BLOCKED_STYLE : STYLE;
    }
}
