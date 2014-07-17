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
	
	public static class CubicComponent {
		private final Context context;
		private final Point control1, control2, destination;
		public CubicComponent(Context context, Point control1, Point control2, Point destination) {
			this.context = context;
			this.control1 = control1;
			this.control2 = control2;
			this.destination = destination;
		}
		
		public static CubicComponent passingThrough(Context context, Point t0, Point t1, Point t2, Point t3) {
			Point p1 = Point.of(
					controlPoint1(t0.getX(), t1.getX(), t2.getX(), t3.getX()),
					controlPoint1(t0.getY(), t1.getY(), t2.getY(), t3.getY()));
			Point p2 = Point.of(
					controlPoint2(t0.getX(), p1.getX(), t2.getX(), t3.getX()),
					controlPoint2(t0.getY(), p1.getY(), t2.getY(), t3.getY()));
			return new CubicComponent(context, p1, p2, t3);
		}
		
		private static float controlPoint1(float p0, float t1, float t2, float p3) {
			return 3 * (t1 - t2/2) - (8*p0+p3)/9 + (p0 + 8*p3)/18;
		}
		
		private static float controlPoint2(float p0, float p1, float t2, float p3) {
			return (9f/4f) * (t2 - (p0 + 8 * p3)/27 - (2 * p1/9)); 
		}
		
		protected Context getContext() {
			return context;
		}
		
		public Point getControl1() {
			return control1;
		}
		
		public Point getControl2() {
			return control2;
		}
		
		public Point getDestination() {
			return destination;
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
		
		public Vector getTangentAtPercentAlong(float percent, Point start) {
			Point c1 = getAbsoluteCoordinate(start, control1);
			Point c2 = getAbsoluteCoordinate(start, control2);
			Point dest = getAbsoluteCoordinate(start, destination);
			return new Vector(
				solveDerivative(percent, start.getX(), c1.getX(), c2.getX(), dest.getX()),
				solveDerivative(percent, start.getY(), c1.getY(), c2.getY(), dest.getY()));
		}
		
		public Point getOffsetPoint(float offset, float percentAlong, Point start) {
			return getPointAtPercentAlong(percentAlong, start).plus(getTangentAtPercentAlong(percentAlong, start).perpendicular().resizeToLength(offset));
		}
		
		private static float solveCubic(float t, float p0, float p1, float p2, float p3) {
			float iT = 1f - t;
			return iT * iT * iT * p0
					+ 3 * iT * iT * t * p1
					+ 3 * iT * t * t * p2
					+ t * t * t * p3;
		}
		
		private static float solveDerivative(float t, float p0, float p1, float p2, float p3) {
			float iT = 1f - t;
			return iT * iT * iT * 3 * (p1 - p0)
					+ iT * t * 6 * (p2 - p1)
					+ t * t * 3 * (p3 - p2);
		}
	}
}
