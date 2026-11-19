package fr.tp.inf112.projects.robotsim.model.shapes;

import fr.tp.inf112.projects.canvas.model.OvalShape;

public class CircularShape extends PositionedShape implements OvalShape {
	
	private static final long serialVersionUID = -1912941556210518344L;

	private int radius;
	
	public CircularShape() {
		this(0, 0, 0);
	}

	public CircularShape( 	final int xCoordinate,
							final int yCoordinate,
							final int radius ) {
		super( xCoordinate, yCoordinate );
		
		this.radius = radius;
	}

	public int getRadius() {
		return radius;
	}

	public void setRadius(int radius) {
		this.radius = radius;
	}

	@Override
	public int getWidth() {
		return 2 * radius;
	}

	public void setWidth(int width) {
		this.radius = width / 2;
	}

	@Override
	public int getHeight() {
		return getWidth();
	}

	public void setHeight(int height) {
		this.radius = height / 2;
	}

	@Override
	public String toString() {
		return super.toString() + " [radius=" + radius + "]";
	}
}
