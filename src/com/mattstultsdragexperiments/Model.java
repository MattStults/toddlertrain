package com.mattstultsdragexperiments;

import android.graphics.Canvas;
import android.view.MotionEvent;

public interface Model {
	void draw(Canvas canvas);
	void handleInput(float[] offset, MotionEvent event);
}