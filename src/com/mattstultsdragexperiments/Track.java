package com.mattstultsdragexperiments;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mattstultsdragexperiments.PathIntersectionFinder.Segment;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Bitmap.Config;
import android.graphics.Path.Direction;
import android.graphics.PathDashPathEffect;
import android.graphics.PathMeasure;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.SumPathEffect;

class Track {
	private static List<Paint> trackPaint;
	private static Paint trackBars;
	private static Paint arrowPaint;
	static Paint debugPaint;
	static {
		Paint red = new Paint();
		red.setARGB(150, 125, 0, 0);
		red.setStyle(Paint.Style.STROKE);
		red.setStrokeWidth(40);
		Paint yellow = new Paint();
		yellow.setARGB(150, 150, 125, 0);
		yellow.setStyle(Paint.Style.STROKE);
		yellow.setStrokeWidth(40);
		Paint green = new Paint();
		green.setARGB(150, 0, 125, 0);
		green.setStyle(Paint.Style.STROKE);
		green.setStrokeWidth(40);
		Paint black = new Paint();
		black.setARGB(150, 0, 0, 0);
		black.setStyle(Paint.Style.STROKE);
		black.setStrokeWidth(40);
		trackPaint = new ArrayList<Paint>();
		trackPaint.add(black);
		trackPaint.add(green);
		trackPaint.add(yellow);
		trackPaint.add(red);
		trackBars = new Paint();
		trackBars.setARGB(255, 0, 0, 0);
		
		arrowPaint = new Paint();
		arrowPaint.setStyle(Paint.Style.STROKE);
		arrowPaint.setStrokeWidth(40);
		
		debugPaint = new Paint();
		debugPaint.setColor(Color.BLACK);
	}
	private static final Bitmap arrow = Bitmap.createBitmap(60, 100, Bitmap.Config.ALPHA_8);
	static {
		Canvas c = new Canvas(arrow);
		c.drawPath(makeTriangle(), new Paint(Color.BLACK));
	}
	private static Paint buildTrackBarPaint(float distance) {
		trackBars.setPathEffect(new SumPathEffect(
				new PathDashPathEffect(makeTrackBars(),0.5f,distance-1, PathDashPathEffect.Style.ROTATE),
				new PathDashPathEffect(makeTrackLines(), 20, distance-1, PathDashPathEffect.Style.ROTATE)));
		return trackBars;
	}
	private static Path makeTrackBars() {
		Path ret = new Path();
		ret.addRect(0, -20, 1, -15, Direction.CCW);
		ret.addRect(0, 15, 1, 20, Direction.CCW);
		return ret;
	}
	private static Path makeTrackLines() {
		Path ret = new Path();
		ret.addRect(0, -25, 10, 25, Direction.CCW);
		return ret;
	}
	private static Path makeTriangle() {
		Path ret = new Path();
		ret.moveTo(0, 0);
		ret.lineTo(0, 100);
		ret.lineTo(60, 50);
		ret.lineTo(0, 0);
		return ret;
	}
	private static final int nLapsPerTrack = trackPaint.size() -1;
	private final List<TrackModel> trackPaths;
	private static final int pathGranularity = 50;
	private int lastIdx = -1;
	private int lastCycle = -1;
	private TrackModel lastTrack;
	
	public Track(List<TrackData> paths, float width, float height) {
		trackPaths = new ArrayList<TrackModel>(paths.size());
		for (TrackData trackData : paths) {
			Path path = new Path();
			trackData.getPathAtSize(path, width, height, 300);
			trackPaths.add(new TrackModel(path, trackData.getCenter(), trackData.getCycles()));
		}
		lastTrack = null;
	}
	
	public interface TrackRider {
		float getDistanceFront();
		float getDistanceBack();
		float getDistanceTravelled();
		void Draw(Canvas canvas);
		void setTrackPosition(float distanceTravelled);
	}
	
	private static Path scratchPath = new Path();
	static class TrackPiece {
		private final Bitmap track;
		private final List<Bitmap> colors;
		private final List<Bitmap> highlights;
		private final float left;
		private final float top;
		private final Path path;
		private final float start;
		final float stop;
		private final PathMeasure pathMeasure;
		final int depth;
		public TrackPiece(PathMeasure path, float buffer, float start, float stop, int depth) {
			this.start = start;
			this.stop = stop;
			this.path = new Path();
			this.colors = new ArrayList<Bitmap>();
			this.highlights = new ArrayList<Bitmap>();
			this.depth = depth;
		    path.getSegment(start, this.stop, this.path, true);
		    this.path.rLineTo(0, 0);
		    pathMeasure = new PathMeasure(this.path, false);
		    RectF dimensions = getDimensions(path, (int)(this.stop-start), start, this.stop);
		    left = dimensions.left - buffer;
		    top = dimensions.top - buffer;
		    track = Bitmap.createBitmap(
		        (int)(2*buffer + dimensions.right - dimensions.left),
		        (int)(2*buffer + dimensions.bottom - dimensions.top),
		        Config.ALPHA_8);
		    Canvas c = new Canvas(track);
		    c.translate(-left, -top);
		    c.drawPath(this.path, buildTrackBarPaint(start));
		    for (Paint paint : trackPaint) {
		    	colors.add(buildColorTrack(paint, buffer, dimensions));
		    	highlights.add(buildHighlightTrack(paint, buffer, dimensions));
		    }
		}
		
		private Bitmap buildColorTrack(Paint paint, float buffer, RectF dimensions) {
			Bitmap coloredTrack = Bitmap.createBitmap(
			        (int)(2*buffer + dimensions.right - dimensions.left),
			        (int)(2*buffer + dimensions.bottom - dimensions.top),
			        Config.ARGB_8888);
	    	Canvas c = new Canvas(coloredTrack);
	    	c.translate(-left, -top);
	    	c.drawPath(this.path, paint);
	    	c.drawPath(this.path, buildTrackBarPaint(start));
	    	return coloredTrack;
		}
		
		private Bitmap buildHighlightTrack(Paint paint, float buffer, RectF dimensions) {
			Bitmap highlightTrack = Bitmap.createBitmap(
			        (int)(2*buffer + dimensions.right - dimensions.left),
			        (int)(2*buffer + dimensions.bottom - dimensions.top),
			        Config.ARGB_8888);
	    	Canvas c = new Canvas(highlightTrack);
	    	c.translate(-left, -top);
	    	arrowPaint.setColor(paint.getColor());
	    	arrowPaint.setAlpha(255);
	    	c.drawPath(this.path, buildTrackBarPaint(start));
	    	c.drawPath(this.path, arrowPaint);
	    	return highlightTrack;
		}
		
		public int getDepth() {
			return depth;
		}
		
		public void draw(Canvas canvas, float changePoint, float highlightLength, float totalLength, int colorIdx1, int colorIdx2) {
			float wrapped = wrapFloat(highlightLength + changePoint, totalLength);
			Paint color1 = trackPaint.get(colorIdx1);
			Paint color2 = trackPaint.get(colorIdx2);
			if (highlightLength + changePoint < start) {  // backwards!?
				draw(canvas, colorIdx1);
			} else if (changePoint > stop) {
				if (wrapped < start) {
				  draw(canvas, colorIdx2);
				} else if (wrapped > stop) {
					if (wrapped == (highlightLength + changePoint) /* UGH! */ ) {
						 draw(canvas, colorIdx2);
					} else {
						drawHighlight(canvas, color2);
					}
				 
				} else {
					drawPartial(canvas, 0, wrapped, color2, color2);
				}
			} else if (changePoint < start && changePoint + highlightLength > stop){
				drawHighlight(canvas, color2);
			} else {
				
				drawPartial(canvas, changePoint, changePoint + highlightLength, color1, color2);
			}
		}
		
		private void drawHighlight(Canvas canvas, Paint paint, float start, float end) {
			canvas.drawBitmap(track, left, top, null);
			arrowPaint.setColor(paint.getColor());
			arrowPaint.setAlpha(255);
			drawColor(canvas, arrowPaint, start, end);
		}
		
		private void drawHighlight(Canvas canvas, Paint paint) {
			drawHighlight(canvas, paint, start, stop);
		}
		
		private void drawPartial(Canvas canvas, float begin, float end, Paint color1, Paint color2) {
			drawColor(canvas, color1, end, stop);
			drawColor(canvas, color2,  start, begin);
			drawHighlight(canvas, color2, begin, end);
		}
		
		public void draw(Canvas canvas, int colorIdx) {
			canvas.drawBitmap(colors.get(colorIdx), left, top, null);
		}
		public void drawColor(Canvas canvas, Paint color, float start, float end) {
			if (start >= end) {
				return;
			}
			start = Math.max(this.start, start);
			end = Math.min(this.stop, end);
			scratchPath.rewind();
			pathMeasure.getSegment(start - this.start, end - this.start, scratchPath, true);
			scratchPath.rLineTo(0, 0);
			canvas.drawPath(scratchPath, color);
		}
		private static RectF getDimensions(PathMeasure path, int precision, float start, float stop) {
			RectF dimensions = new RectF(Float.MAX_VALUE, Float.MAX_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
			float position[] = new float[2];
			float tan[] = new float[2];
			for (int i = 0; i <= precision; i++) {
				path.getPosTan(start + i * (stop - start) / precision, position, tan);
				dimensions.left = Math.min(dimensions.left, position[0]);
				dimensions.top = Math.min(dimensions.top, position[1]);
				dimensions.right = Math.max(dimensions.right, position[0]);
				dimensions.bottom = Math.max(dimensions.bottom, position[1]);
			}
			return dimensions;
		}
	}
	
	private TrackRider arrowHead = new TrackRider() {
		private float distance;
		private final float width = 50f;
		@Override
		public float getDistanceFront() {
			return distance + width;
		}
		
		@Override
		public float getDistanceBack() {
			return distance;
		}
		
		@Override
		public float getDistanceTravelled() {
			return distance;
		}

		@Override
		public void Draw(Canvas canvas) {
			canvas.translate(0, -50);
			canvas.drawBitmap(arrow, 0, 0, arrowPaint);
		}

		@Override
		public void setTrackPosition(float distanceTravelled) {
			this.distance = distanceTravelled;
		}
		
	};

	Path partialPath = new Path();
	private List<TrackRider> trackRiders = new ArrayList<TrackRider>();
	public void draw(Canvas canvas, float distanceTravelled, List<TrackRider> trackRiders) {
		PathMeasure fullPath = lastTrack.getPathMeasure();
		float length = fullPath.getLength();
		float distance = wrapFloat(distanceTravelled, length);
		getCycle(distanceTravelled, length);
		float arrowLength = 400;
		this.trackRiders.clear();
		this.trackRiders.addAll(trackRiders);
		this.trackRiders.add(arrowHead);
		arrowPaint.setColor(trackPaint.get(lastCycle+1).getColor());
		arrowPaint.setAlpha(255);
		arrowHead.setTrackPosition(distanceTravelled + arrowLength - 1);
		lastTrack.draw(canvas, distance, arrowLength + 1, lastCycle, lastCycle+1, this.trackRiders);
	}
	
	private void getCycle(float distanceTraveled, float length) {
		int cycle = (int) (distanceTraveled / length);
		cycle = cycle % (nLapsPerTrack);
		if (cycle < 0) {
			cycle += nLapsPerTrack;
		}
		if (cycle != lastCycle) {
			lastCycle = cycle;
		}
	}
	
	private final float scratch[] = new float[2];
	public void getPosition(float distanceAlongPath, float[] position) {
		PathMeasure pathMeasure = lastTrack.getPathMeasure();
		float distance = wrapFloat(distanceAlongPath, pathMeasure.getLength());
		lastTrack.getPathMeasure().getPosTan(distance, position, scratch);
	}
	
	public float getClosestPathPosition(
			float distanceAlongPath,
			float distanceToMovePerFrame,
			float minAcceptableDistance,
			float[] proposedPoint) {
		if (lastTrack == null) {
			return distanceAlongPath;
		}
		PathMeasure pathMeasure = lastTrack.getPathMeasure();
		float distance = pathMeasure.getLength();
		float minDistanceSquared = minAcceptableDistance * minAcceptableDistance;
		float start = wrapFloat(wrapFloat(distanceAlongPath, pathMeasure.getLength()) - distanceToMovePerFrame, distance);
		float pos[] = new float[2];
		float tangent[] = scratch;
		float position = distanceAlongPath;
		for (int i = 0; i < pathGranularity; i++) {
			float offset = i * 2 * distanceToMovePerFrame/pathGranularity;
			float location = wrapFloat(start + offset, distance);
			pathMeasure.getPosTan(location, pos, tangent);
			float localMinSquared = TrainGameModel.distSquare(proposedPoint, pos);
			if (localMinSquared <= minDistanceSquared) {
				minDistanceSquared = localMinSquared;
				position = distanceAlongPath + offset - distanceToMovePerFrame;
			}
		}
		return position;
	}
	
	public float registerDistance(float distanceTravelled) {
		if (lastTrack == null || distanceTravelled >= lastTrack.getTrackEnd()) {
			lastIdx = (lastIdx + 1) % trackPaths.size();
			if (lastTrack != null) {
				lastTrack.unload();
			}
			lastTrack = trackPaths.get(lastIdx);
			lastTrack.initialize();
			
			distanceTravelled = 0;
			trackPaths.get((lastIdx + 1) % trackPaths.size()).preinitialize();
		} else {
			
		}
		return distanceTravelled;
	}
	
	
	
	static float wrapFloat(float cur, float max) {
		while (cur < 0) {
			cur += max;
		}
		while (cur > max) {
			cur -= max;
		}
		return cur;
	}
	
	
}