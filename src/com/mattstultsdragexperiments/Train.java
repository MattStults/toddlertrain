package com.mattstultsdragexperiments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;

class Train implements Model {
	private static Paint circlePaint = new Paint();
	private static Paint red = new Paint();
	private static Paint yellow = new Paint();
	private static Paint blue = new Paint();
	private static Paint fucia = new Paint();
	private static Paint green = new Paint();
	private static List<Paint> paints = new ArrayList<Paint>();
	static {
		paints.add(blue);
		paints.add(red);
		paints.add(yellow);
		paints.add(fucia);
		paints.add(green);
		circlePaint.setARGB(255, 100, 255, 50);
		red.setARGB(255, 255, 0, 0);
		red.setStyle(Paint.Style.FILL_AND_STROKE);
		green.setARGB(255, 0, 128, 0);
		fucia.setARGB(255, 255, 0, 255);
		fucia.setStyle(Paint.Style.FILL_AND_STROKE);
		yellow.setARGB(255, 255, 255, 0);
		yellow.setStyle(Paint.Style.FILL_AND_STROKE);
		blue.setARGB(255, 0, 0, 255);
		blue.setStyle(Paint.Style.FILL_AND_STROKE);
	}
	private float radius = 100;
	private Integer trackingPointer = null;
	private AtomicReference<Track> track = new AtomicReference<Track>();
	private float distanceTravelled = 0;
	private static final float distanceCanMovePerFrame = 50f;
	private final AtomicInteger nCars = new AtomicInteger(5);
	private static final float carSpacing = 125f;
	private final AtomicReference<Float> scale = new AtomicReference<Float>(
			1.5f);
	private final Path scrathPath = new Path();
	private final List<TrackRider> trackRiders;
	private final float[] position = new float[2];
	private final float[] tangent = new float[2];
	
	public Train(List<SvgData> cars) {
		trackRiders = buildTrackRiders(100, cars);
	}

	public void setNumberOfCars(int nCars) {
		this.nCars.set(nCars);
	}

	public int getNumberOfCars() {
		return nCars.get();
	}

	public void setTrainScale(float scale) {
		this.scale.set(scale);
	}

	public float getTrainScale() {
		return scale.get();
	}

	@Override
	public void draw(Canvas canvas) {
		distanceTravelled = track.get().registerDistance(distanceTravelled);
		float curPos = distanceTravelled;
		for (int i = trackRiders.size()-1; i >=0; i--) {
			TrackRider trainCar = trackRiders.get(i);
			trainCar.setFrontPosition(curPos);
			curPos = trainCar.getDistanceBack();
		}
		if (track.get() != null) {
			track.get().draw(canvas, distanceTravelled, trackRiders);
		}

	}

	private List<TrackRider> buildTrackRiders(float height, List<SvgData> data) {
		List<TrackRider> ret = new ArrayList<TrackRider>();
		for (int i = data.size() -1; i >=0; i--) {
			ret.add(data.get(i).getCarAtHeight(height));
		}
		return Collections.unmodifiableList(ret);
	}

	public void setTrack(Track track) {
		this.track.set(track);
		trackingPointer = null;
	}

	@Override
	public boolean handleInput(float[] offset, MotionEvent event) {
		switch (event.getActionMasked()) {
		case MotionEvent.ACTION_DOWN:
			break;
		case MotionEvent.ACTION_MOVE:
			track.get().getPosition(distanceTravelled, position, tangent);
			if (trackingPointer == null
					|| event.findPointerIndex(trackingPointer) < 0) {
				trackingPointer = TrainGameModel.findClosestPointer(event,
						position[0], position[1], scale.get() * radius, offset);
			}
			if (trackingPointer != null) {
				int idx = event.findPointerIndex(trackingPointer);
				float in[] = new float[] { event.getX(idx) - offset[0],
						event.getY(idx) - offset[1] };
				distanceTravelled = track.get().getClosestPathPosition(
						distanceTravelled, distanceCanMovePerFrame, 300, in);
				return true;
			}
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			trackingPointer = null;
			break;
		}
		return false;
	}
}