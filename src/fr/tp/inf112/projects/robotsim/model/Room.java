package fr.tp.inf112.projects.robotsim.model;

import java.util.ArrayList;
import java.util.List;

import fr.tp.inf112.projects.robotsim.model.shapes.PositionedShape;
import fr.tp.inf112.projects.robotsim.model.shapes.RectangularShape;

public class Room extends Component {
	
	private static final long serialVersionUID = 1449569724908316962L;

	public static enum WALL {LEFT, TOP, RIGHT, BOTTOM};
	
	private static final int WALL_THICKNESS = 5;
	
	private PositionedShape leftWall;
	
	private PositionedShape rightWall;
	
	private PositionedShape topWall;
	
	private PositionedShape bottomWall;
	
	private List<Area> areas;

	private List<Door> doors;

	public Room() {
		this(null, null, null);
	}

	public Room(final Factory factory,
				final RectangularShape shape,
				final String name) {
		super(factory, shape, name);
		
		if (shape != null) {
			initWalls();
		}
		
		areas = new ArrayList<>();
		doors = new ArrayList<>();
	}

	private void initWalls() {
		final int x = getxCoordinate();
		final int y = getyCoordinate();
		final int w = getWidth();
		final int h = getHeight();
		leftWall = new RectangularShape(x, y, WALL_THICKNESS, h + WALL_THICKNESS);
		rightWall = new RectangularShape(x + w, y, WALL_THICKNESS, h + WALL_THICKNESS);
		topWall = new RectangularShape(x, y, w, WALL_THICKNESS);
		bottomWall = new RectangularShape(x, y + h, w, WALL_THICKNESS);
	}

	private void checkWalls() {
		if (leftWall == null && getPositionedShape() != null) {
			initWalls();
		}
	}
	
	protected boolean addArea(final Area area) {
		return getAreas().add(area);
	}
	
	protected boolean addDoor(final Door door) {
		return getDoors().add(door);
	}
	
	public List<Area> getAreas() {
		if (areas == null) {
			areas = new ArrayList<>();
		}
		return areas;
	}

	public List<Door> getDoors() {
		if (doors == null) {
			doors = new ArrayList<>();
		}
		return doors;
	}

	@Override
	public boolean overlays(final PositionedShape shape) {
		checkWalls();
		if (leftWall == null) {
			return false;
		}
		return leftWall.overlays(shape) || rightWall.overlays(shape) || 
			   topWall.overlays(shape) || bottomWall.overlays(shape);
	}

	@Override
	public boolean canBeOverlayed(final PositionedShape shape) {
		final Door overlayedDoor = getOverlayedDoor(shape);
		
		if (overlayedDoor != null) {
			return overlayedDoor.canBeOverlayed(shape);
		}
		
		checkWalls();
		if (leftWall == null) {
			return true;
		}
		
		if (leftWall.overlays(shape) || rightWall.overlays(shape) || 
			topWall.overlays(shape) || bottomWall.overlays(shape)) {
			return false;
		}
		
		return true;
	}
	
	private Door getOverlayedDoor(final PositionedShape shape) {
		for (final Door door : getDoors()) {
			if (door.overlays(shape)) {
				return door;
			}
		}
		
		return null;
	}

	@Override
	public String toString() {
		return super.toString() + " areas=" + areas + "]";
	}
}
