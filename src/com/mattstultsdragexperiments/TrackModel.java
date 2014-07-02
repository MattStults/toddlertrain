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

import com.mattstultsdragexperiments.Track.TrackPiece;

class TrackModel {
	private static final float PIECE_SIZE = 500f;

	private final float center;
	private final int cycles;
	private final Path path;
	private final Matrix scratchMatrix = new Matrix();
	private final float[] scratchPosition = new float[2];
	private final float[] scratchTangent = new float[2];
	private final List<List<TrackPiece>> tracks;
	private final List<List<TrackRider>> riders;
	private final int depths;
	private final float totalLength;
	private final List<TrackPiece> tracksInOrder;
	private AsyncLoaderTask loaderTask;

	public TrackModel(Path path, float centerPercent, int cycles, int depths) {
		this.path = path;
		totalLength = computeLength(path);
		this.center = centerPercent * totalLength;
		this.cycles = cycles;
		this.depths = depths;
		tracks = new ArrayList<List<TrackPiece>>();
		riders = new ArrayList<List<TrackRider>>();
		for (int i = 0; i < depths; i++) {
			tracks.add(new ArrayList<TrackPiece>());
			riders.add(new ArrayList<TrackRider>());
		}
		tracksInOrder = new ArrayList<TrackPiece>(tracks.size());
	}

	private static float computeLength(Path contours) {
		PathMeasure measure = new PathMeasure(contours, false);
		float length = 0;
		for( length = measure.getLength(); measure.nextContour(); length += measure.getLength());
		return length;
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
			loaderTask = new AsyncLoaderTask();
			loaderTask.execute(path);
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
	
	private TrackPiece getAtPosition(float distance) {
		for (TrackPiece piece : tracksInOrder) {
			if (piece.start <= distance && piece.stop >= distance) {
				return piece;
			}
		}
		throw new IllegalArgumentException("distance beyond length: " + distance);
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
		getPosTan(getPathPosition(trackRider.getDistanceTravelled()), scratchPosition, scratchTangent);
		trackRider.draw(canvas, scratchTangent);
		canvas.restore();
	}

	public void getMatrix(float distanceTravelled, Matrix matrix) {
		float realDistance = getPathPosition(distanceTravelled);
		TrackPiece piece = getAtPosition(realDistance);
		piece.pathMeasure.getMatrix(realDistance - piece.start, matrix,
				PathMeasure.POSITION_MATRIX_FLAG
						| PathMeasure.TANGENT_MATRIX_FLAG);
	}

	private static class AsyncLoaderTask extends
			AsyncTask<Path, Void, List<TrackPiece>> {

		@Override
		protected List<TrackPiece> doInBackground(Path... paths) {
			List<TrackPiece> ret = new ArrayList<TrackPiece>();
			float start = 0;
			int depth = 0;
			for (Path path : paths) {
				PathMeasure pathMeasure = new PathMeasure(path, false);
				do {
					float length = pathMeasure.getLength();
					ret.add(new TrackPiece(pathMeasure, 35, start,
							start + length, depth++));
					start += length;
				} while (pathMeasure.nextContour());
			}
			return ret;
		}
	}

	public void getPosTan(float distance, float[] position, float[] tangent) {
		float realDistance = getPathPosition(distance);
		TrackPiece piece = getAtPosition(realDistance);
		piece.pathMeasure.getPosTan(distance - piece.start, position, tangent);
	}
}