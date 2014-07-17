package com.mattstults.track;

public class Vector extends Point{

	public Vector(float x, float y) {
		super(x, y);
	}
	
	public Vector(Point point) {
		super(point.getX(), point.getY());
	}
	
	public Vector perpendicular() {
		return new Vector(getY(), -getX());
	}
	
	public Vector resizeToLength(float newLength) {
		return new Vector(times(newLength/getLength()));
	}

	public float getLengthSquared() {
		return getX() * getX() + getY() * getY(); 
	}
	
	public float getLength() {
		return (float) Math.sqrt(getLengthSquared());
	}
}
