package com.mattstults.track;

public class Point {
	private final float x, y;
	
	public Point(float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	public static Point of(float x, float y) {
		return new Point(x,y);
	}
	
	public float getX() {
		return x;
	}
	
	public float getY() {
		return y;
	}
	
	public Point plus(Point right) {
		return new Point(x + right.x, y + right.y);
	}
	
	public Vector minus(Point right) {
		return new Vector(x - right.x, y - right.y);
	}
	
	public Point times(float scalar) {
		return new Point(x * scalar, y * scalar);
	}
	
	public float distanceSquared(Point other) {
		Point vector = minus(other);
		return vector.x * vector.x + vector.y * vector.y;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(x).append(",").append(y).toString();
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Point)) {
			return false;
		}
		Point other = (Point) o;
		float distanceSquared = distanceSquared(other);
		return distanceSquared < 0.01f;
	}
}
