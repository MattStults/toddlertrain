package com.mattstultsdragexperiments;

import android.graphics.Canvas;

public interface TrackRider {
	float getDistanceFront();

	float getDistanceBack();

	float getDistanceTravelled();

	void draw(Canvas canvas, float[] tangent);

	void setFrontPosition(float distanceTravelled);
}