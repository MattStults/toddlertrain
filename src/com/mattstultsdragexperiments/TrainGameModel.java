package com.mattstultsdragexperiments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.graphics.Canvas;
import android.view.MotionEvent;

public class TrainGameModel implements DrawPanel.GameModel {
	private final float[] offset = new float[2];
	private final List<Model> models = new ArrayList<Model>();

	public void addModel(Model model) {
		models.add(model);
	}
	
	public void addModelFront(Model model) {
		removeModel(model);
		models.add(0, model);
	}
	
	public void removeModel(Model model) {
		models.remove(model);
	}

	public void setOffset(int[] offset) {
		this.offset[0] = offset[0];
		this.offset[1] = offset[1];
	}

	@Override
	public void draw(Canvas canvas, long time) {
		for (Model model : models) {
			model.draw(canvas);
		}
	}

	@Override
	public void handleInput(MotionEvent event) {
		for (Model model : models) {
			if (model.handleInput(offset, event)) {
				return; // We've handled the input--we're done.
			}
		}
	}

	static Integer findClosestPointer(MotionEvent event, float x, float y,
			float minDist, float[] offset) {
		Integer pointer = null;
		float closestDistance = minDist * minDist;
		for (int i = 0; i < event.getPointerCount(); i++) {
			float distance = distSquare(x, y, event.getX(i) - offset[0],
					event.getY(i) - offset[1]);
			if (distance < closestDistance) {
				closestDistance = distance;
				pointer = event.getPointerId(i);
			}
		}
		return pointer;
	}

	static float distSquare(float x1, float y1, float x2, float y2) {
		float x = x1 - x2;
		float y = y1 - y2;
		return x * x + y * y;
	}

	public static float distSquare(float[] a, float[] b) {
		return distSquare(a[0], a[1], b[0], b[1]);
	}
}
