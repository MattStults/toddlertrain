package com.mattstultsdragexperiments;

import android.graphics.Canvas;

public interface TrackRider {
	float getDistanceFront();

	float getDistanceBack();

	float getDistanceTravelled();

	void Draw(Canvas canvas);

	void setTrackPosition(float distanceTravelled);
}