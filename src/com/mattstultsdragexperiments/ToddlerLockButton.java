package com.mattstultsdragexperiments;

import java.util.concurrent.ConcurrentLinkedQueue;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;

public class ToddlerLockButton implements Model{
	private final OnUnlockCallback onUnlockCallback;
	private static final float radius = 50f;
	private static final Paint circlePaint = new Paint();
	static {
		circlePaint.setColor(Color.BLACK);
	}
	float x,y;
	public ToddlerLockButton(OnUnlockCallback onUnlockCallback) {
		this.onUnlockCallback = onUnlockCallback;
	}
	
	@Override
	synchronized public void draw(Canvas canvas) {
		canvas.drawCircle(x, y, radius, circlePaint);
	}

	@Override
	synchronized public boolean handleInput(float[] offset, MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			for (int i = 0; i < event.getPointerCount(); i++) {
				float distX = event.getX(i) - offset[0] - x;
				float distY = event.getY(i) - offset[1] - y;
				float distSquared = distX * distX + distY * distY;
				if (distSquared <= radius * radius) {
					onUnlockCallback.onUnlock();
					return true;
				}
			}
		}
		return false;
	}
	
	public interface OnUnlockCallback {
		void onUnlock();
	}

	synchronized public void setBackgroundDimensions(int screenWidth, int height) {
		x = screenWidth-radius * 2;
		y = radius*2;
	}

}
