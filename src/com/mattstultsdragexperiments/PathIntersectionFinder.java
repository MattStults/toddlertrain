package com.mattstultsdragexperiments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.util.SparseArray;

public class PathIntersectionFinder {
	
	public static int getNumberSegments(float pathLength, float center, float atomicLength) {
		return (int)Math.ceil(pathLength / atomicLength) + 1;
	}
	//This idea failed.  short circuit and do ever increasing.
	public static List<Segment> getIntersections(float pathLength, float center, float atomicLength) {
		float start = Track.wrapFloat(center, atomicLength);
		int reset = (int)(center / atomicLength);
		
		int iterations = getNumberSegments(pathLength, center, atomicLength);
		
		List<Segment> ret = new ArrayList<Segment>(iterations);
		int maxDepth = iterations - 1;
		ret.add(new Segment(0, start, maxDepth - reset));
		for (int i = 1; i < iterations; i++) {
			int depth;
			if (i <= reset) {
				depth = maxDepth - reset + i;
			} else {
				depth = i - reset;
			}
			float end = Math.min(start + atomicLength, pathLength);
			ret.add(new Segment(start, end, depth));
			start = end;
		}
		return ret;
	}

	public static class Segment {
		public final float start;
		public final float stop;
		public final int depth;
		private Segment(float start, float stop, int depth) {
			this.start = (int)start;
			this.stop = (int)stop+2;
			this.depth = depth;
		}
	}
}
