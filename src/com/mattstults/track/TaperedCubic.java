package com.mattstults.track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mattstults.track.Svgable.PathPart;

public class TaperedCubic implements PathPart {
	private final Point topStart;
	private final Point bottomStart;
	private final Cubic.CubicComponent top;
	private final Cubic.CubicComponent bottom;
	
	private TaperedCubic(
			Point topStart,
			Cubic.CubicComponent top,
			Point bottomStart,
			Cubic.CubicComponent bottom) {
		this.topStart = topStart;
		this.top = top;
		this.bottomStart = bottomStart;
		this.bottom = bottom;
	}
	
	@Override
	public String toString() {
		return String.format(
				"M %s %s L %s %s L %s M %s ",
				topStart.toString(),
				top.toString(),
				bottomStart.toString(),
				bottom.toString(),
				topStart.toString(),
				bottomStart.toString());
	}
	
	public static TaperedCubic taperedCubic(
			Point start,
			Point control1,
			Point control2,
			Point end,
			float startWidth,
			float endWidth) {
		return taperedCubic(start, new Cubic.CubicComponent(Context.ABSOLUTE, control1, control2, end), startWidth, endWidth);
	}
	public static TaperedCubic taperedCubic(
			Point start,
			Cubic.CubicComponent pathToTrace,
			float startWidth,
			float endWidth) {
		Vector tangentStartA = pathToTrace.getTangentAtPercentAlong(0, start).perpendicular();
		Vector tangentOneThirdA = pathToTrace.getTangentAtPercentAlong(1/3f, start).perpendicular();
		Vector tangentTwoThirdsA = pathToTrace.getTangentAtPercentAlong(2/3f, start).perpendicular();
		Vector tangentEndA = pathToTrace.getTangentAtPercentAlong(1, start).perpendicular();
		Point startingPointA = start.plus(tangentStartA.resizeToLength(startWidth/2f));
		Point t1A = pathToTrace.getPointAtPercentAlong(1/3f, startingPointA).plus(tangentOneThirdA.resizeToLength(startWidth/2f + (endWidth-startWidth)/6f));
		Point t2A = pathToTrace.getPointAtPercentAlong(2/3f, startingPointA).plus(tangentTwoThirdsA.resizeToLength(startWidth/2f + (endWidth-startWidth)*2/6f));
		Point endingPointA = pathToTrace.getDestination().plus(tangentEndA.resizeToLength(endWidth/2f));
		Point startingPointB = pathToTrace.getDestination().plus(tangentEndA.resizeToLength(-endWidth/2f));
		Point t1B = pathToTrace.getPointAtPercentAlong(2/3f, startingPointA).plus(tangentTwoThirdsA.resizeToLength(-1*(startWidth/2f + (endWidth-startWidth)*2/6f)));
		Point t2B = pathToTrace.getPointAtPercentAlong(1/3f, startingPointA).plus(tangentOneThirdA.resizeToLength(-1*(startWidth/2f + (endWidth-startWidth)/6f)));
		Point endingPointB = start.plus(tangentStartA.resizeToLength(-startWidth/2f));
		return new TaperedCubic(
				startingPointA,
				Cubic.CubicComponent.passingThrough(
						Context.ABSOLUTE, startingPointA, t1A, t2A, endingPointA),
				startingPointB,
				Cubic.CubicComponent.passingThrough(
						Context.ABSOLUTE, startingPointB, t1B, t2B, endingPointB));
	}
}
