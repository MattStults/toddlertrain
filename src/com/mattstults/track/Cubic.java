package com.mattstults.track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mattstults.track.Svgable.PathPart;

public class Cubic implements PathPart {
	private List<CubicComponent> components;
	
	public Cubic(List<CubicComponent> components) {
		this.components = Collections.unmodifiableList(components);
	}
	
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		
		for (CubicComponent component : components) {
			ret.append(component.toString());
		}
		return ret.toString();
	}
	
	public static class Builder {
		private final List<CubicComponent> parts = new ArrayList<CubicComponent>();
		
		public Builder(Context context, Point c1, Point c2, Point dest) {
			parts.add(new CubicComponent(context, c1, c2, dest));
		}
		
		public Builder append(Context context, Point c1, Point c2, Point dest) {
			parts.add(new CubicComponent(context, c1, c2, dest));
			return this;
		}
		
		public Cubic build() {
			return new Cubic(parts);
		}
	}
	
	private static class CubicComponent {
		private final Context context;
		private final Point control1, control2, destination;
		public CubicComponent(Context context, Point control1, Point control2, Point destination) {
			this.context = context;
			this.control1 = control1;
			this.control2 = control2;
			this.destination = destination;
		}
		@Override
		public String toString() {
			StringBuilder ret = new StringBuilder(context == Context.ABSOLUTE
					? "C"
					: "c");
			return ret.append(" ").append(control1.toString())
			    .append(" ").append(control2.toString())
			    .append(" ").append(destination.toString()).toString();
		}
		
		private Point getAbsoluteCoordinate(Point absoluteOrigin, Point localPoint) {
			if (context == Context.ABSOLUTE) {
				return localPoint;
			} else {
				return absoluteOrigin.plus(localPoint);
			}
		}
		
		public Point getPointAtPercentAlong(float percent, Point start) {
			Point c1 = getAbsoluteCoordinate(start, control1);
			Point c2 = getAbsoluteCoordinate(start, control2);
			Point dest = getAbsoluteCoordinate(start, destination);
			return new Point(
				solveCubic(percent, start.getX(), c1.getX(), c2.getX(), dest.getX()),
				solveCubic(percent, start.getY(), c1.getY(), c2.getY(), dest.getY()));
		}
		
		private static float solveCubic(float t, float p0, float p1, float p2, float p3) {
			float iT = 1f - t;
			return (float) (iT * iT * iT * p0
					+ 3 * iT * iT * t * p1
					+ 3 * iT * t * t * p2
					+ t * t * t * p3);
		}
	}
}
