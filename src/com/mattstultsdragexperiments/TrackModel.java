package com.mattstultsdragexperiments;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.os.AsyncTask;

import com.mattstultsdragexperiments.PathIntersectionFinder.Segment;
import com.mattstultsdragexperiments.Track.TrackPiece;

class TrackModel {
	private static final float PIECE_SIZE = 500f;

	private final float center;
	private final int cycles;
	private final PathMeasure pathMeasure;
	private final Matrix scratchMatrix = new Matrix();
	private final List<List<TrackPiece>> tracks;
	private final List<List<TrackRider>> riders;
	private final int depths;
	private final float totalLength;
	private final List<Segment> segments;
	private final List<TrackPiece> tracksInOrder;
	private AsyncLoaderTask loaderTask;

	public TrackModel(Path path, float centerPercent, int cycles) {
		this.pathMeasure = new PathMeasure();
		this.pathMeasure.setPath(path, true);
		;
		totalLength = pathMeasure.getLength();
		this.center = centerPercent * totalLength;
		this.cycles = cycles;
		segments = PathIntersectionFinder.getIntersections(totalLength, center,
				PIECE_SIZE);
		this.depths = segments.size();
		tracks = new ArrayList<List<TrackPiece>>();
		riders = new ArrayList<List<TrackRider>>();
		for (int i = 0; i < depths; i++) {
			tracks.add(new ArrayList<TrackPiece>());
			riders.add(new ArrayList<TrackRider>());
		}
		tracksInOrder = new ArrayList<TrackPiece>(tracks.size());
	}

	public void initialize() {
		if (loaderTask == null) {
			preinitialize();
		}
		try {
			tracksInOrder.addAll(loaderTask.get());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // handle the case where we're not loaded yet!
		assignTrackDepths(tracksInOrder, tracks);
		loaderTask = null;
	}

	public void preinitialize() {
		if (loaderTask == null) {
			loaderTask = new AsyncLoaderTask(pathMeasure);
			loaderTask.execute(segments.toArray(new Segment[segments.size()]));
		}
	}

	public void unload() {
		if (loaderTask != null) {
			loaderTask.cancel(true);
			loaderTask = null;
		}
		tracksInOrder.clear();
		for (int i = 0; i < depths; i++) {
			tracks.get(i).clear();
			riders.get(i).clear();
		}
	}

	// TODO: Temporary until I can internalize the use of PathMeasure
	public PathMeasure getPathMeasure() {
		return pathMeasure;
	}

	public float getLength() {
		return totalLength;
	}

	public float getTrackEnd() {
		return totalLength * cycles;
	}

	public void assignTrackDepths(List<TrackPiece> trackInput,
			List<List<TrackPiece>> tracksByDepths) {
		// For now just do linearly increasing for now.
		for (int i = 0; i < trackInput.size(); i++) {
			int depth = trackInput.get(i).getDepth();
			tracksByDepths.get(depth).add(trackInput.get(i));
		}
	}

	public void draw(Canvas canvas, float transitionPoint,
			float highlightLength, int color1, int color2,
			Iterable<TrackRider> trainPieces) {
		for (List<TrackRider> list : riders) {
			list.clear();
		}

		for (TrackRider trackRider : trainPieces) {
			riders.get(
					getDepthAtPosition(
							getPathPosition(trackRider.getDistanceBack()),
							getPathPosition(trackRider.getDistanceFront())))
					.add(trackRider);
		}

		for (int i = 0; i < depths; i++) {
			for (TrackPiece trackPiece : tracks.get(i)) {
				trackPiece.draw(canvas, transitionPoint, highlightLength,
						totalLength, color1, color2);
			}
			for (TrackRider trackRider : riders.get(i)) {
				drawPiece(canvas, trackRider);
			}
		}

		canvas.drawText("distance: " + transitionPoint / totalLength, 40, 140,
				Track.debugPaint);
	}

	private int getIndex(float position) {
		int i = 0;
		for (TrackPiece trackPiece : tracksInOrder) {
			if (trackPiece.stop >= position) {
				return i;
			}
			i++;
		}
		return tracksInOrder.size() - 1;
	}

	int getDepthAtPosition(float back, float front) {
		int start = getIndex(back);
		int end = getIndex(front);
		int max = 0;
		if (start <= end) {
			for (int i = start; i <= end; i++) {
				max = Math.max(max, tracksInOrder.get(i).depth);
			}
		} else {
			for (int i = 0; i <= end; i++) {
				max = Math.max(max, tracksInOrder.get(i).depth);
			}
			for (int i = start; i < tracksInOrder.size(); i++) {
				max = Math.max(max, tracksInOrder.get(i).depth);
			}
		}
		return max;
	}

	private float getPathPosition(float raw) {
		float position = raw % getLength();
		if (position < 0) {
			position += getLength();
		}
		return position;
	}

	private void drawPiece(Canvas canvas, TrackRider trackRider) {
		canvas.save();
		getMatrix(trackRider.getDistanceTravelled(), scratchMatrix);
		canvas.concat(scratchMatrix);
		trackRider.Draw(canvas);
		canvas.restore();
	}

	public void getMatrix(float distanceTravelled, Matrix matrix) {
		float position = getPathPosition(distanceTravelled);
		pathMeasure.getMatrix(position, matrix,
				PathMeasure.POSITION_MATRIX_FLAG
						| PathMeasure.TANGENT_MATRIX_FLAG);
	}

	private static class AsyncLoaderTask extends
			AsyncTask<Segment, Void, List<TrackPiece>> {
		private final AtomicReference<PathMeasure> pathMeasure;

		public AsyncLoaderTask(PathMeasure pathMeasure) {
			this.pathMeasure = new AtomicReference<PathMeasure>(pathMeasure);
		}

		@Override
		protected List<TrackPiece> doInBackground(Segment... segments) {
			List<TrackPiece> ret = new ArrayList<TrackPiece>(segments.length);
			PathMeasure pathMeasure = this.pathMeasure.get();
			for (Segment segment : segments) {
				ret.add(new TrackPiece(pathMeasure, 25, segment.start,
						segment.stop, segment.depth));
			}
			return ret;
		}
	}
}