package com.mattstultsdragexperiments;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.view.MotionEvent;

public class ToddlerLockButton implements Model{
	private SvgData.SvgDrawable lockIcon;
	private final OnUnlockCallback onUnlockCallback;
	private static float radius = 50f;
	private long lastUpdate;
	private final Paint lockPaint;
	private final Paint lockTextPaint;
	private final int totalTapsRequired = 5;
	private int nTapsRecorded;
	private Context context;
	private Status status;
	private String lastMessage;
	private float x,y;
	public enum Status {
		SILENT(Long.MAX_VALUE, false) {
			@Override
			void updatePaint(Paint lockPaint, Paint textPaint, long statusStart, long currentTime) {
				lockPaint.setAlpha((int)(lockPaint.getAlpha() * .75f));
				textPaint.setAlpha((int)(textPaint.getAlpha() * .75f));
			}
		},
		CREATED(3000L, false) {
			@Override
			void updatePaint(Paint lockPaint, Paint textPaint, long statusStart, long currentTime) {
				lockPaint.setAlpha(255);
				textPaint.setAlpha(255);
			}
		},
		UNLOCKING(2000L, true) {
			@Override
			void updatePaint(Paint lockPaint, Paint textPaint, long statusStart, long currentTime) {
				long msIn = currentTime - statusStart;
				double cycle = (((float)msIn) / 4000L) * Math.PI * 2 ;
				int alpha = (int)(255*(1f + (float)Math.cos(cycle)) / 2f);
				lockPaint.setAlpha(alpha);
				textPaint.setAlpha(255);
			}
		},
		ATTEMPTED_APP_LEAVE(2500L, true) {
			@Override
			void updatePaint(Paint lockPaint, Paint textPaint, long statusStart, long currentTime) {
				long msIn = currentTime - statusStart;
				double cycle = (((float)msIn) / cycleLenthMs) * Math.PI * 2 ;
				float percent = (1f + (float)Math.cos(cycle)) / 2f;
				int alpha = (int)(100 + 155*(percent));
				lockPaint.setAlpha(alpha);
				textPaint.setAlpha(255);
			}
		};
		
		private static final long cycleLenthMs = 1000L;
		private final long effectDurationMs;
		private final boolean receivesInput;
		private Status(long effectDurationMs, boolean receivesInput) {
			this.effectDurationMs = effectDurationMs;
			this.receivesInput = receivesInput;
		}
		
		abstract void updatePaint(Paint lockPaint, Paint textPaint, long statusStart, long currentTime);
		
		public boolean receivesInput() {
			return receivesInput;
		}
	}
	public ToddlerLockButton(OnUnlockCallback onUnlockCallback) {
		this.onUnlockCallback = onUnlockCallback;
		lockPaint = new Paint();
		lockTextPaint = new Paint();
		lockTextPaint.setTextAlign(Align.CENTER);
	}
	
	@Override
	synchronized public void draw(Canvas canvas) {
		status = getStatus();
		status.updatePaint(lockPaint, lockTextPaint, lastUpdate, System.currentTimeMillis());
		if (lockPaint.getAlpha() > 0 || lockTextPaint.getAlpha() > 0) {
			canvas.save();
			canvas.translate(x, y);
			if (lockPaint.getAlpha() > 0) {
				lockIcon.draw(canvas, lockPaint);
			}
			if (lockTextPaint.getAlpha() > 0) {
				canvas.drawText(getCurrentMessage(), 0, 2 * radius + lockTextPaint.descent() + lockTextPaint.ascent(), lockTextPaint);
			}
			canvas.translate(x, y);
			canvas.restore();
		}
	}
	
	private String getCurrentMessage() {
		switch (status) {
		case ATTEMPTED_APP_LEAVE:
		case UNLOCKING:
			int tapsRemaining = this.totalTapsRequired - this.nTapsRecorded;
			if (tapsRemaining > 0) {
				lastMessage = context.getString(R.string.toddler_unlock_instructions, tapsRemaining);
			} else {
				lastMessage = context.getString(R.string.toddler_unlocked);
			}
			break;
		case CREATED:
			lastMessage = context.getString(R.string.toddler_locked_notification);
			break;
		default:
			break;
		}
		return lastMessage;
	}

	@Override
	synchronized public boolean handleInput(float[] offset, MotionEvent event) {
		if (!status.receivesInput) {
			return false;
		}
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			for (int i = 0; i < event.getPointerCount(); i++) {
				float distX = event.getX(i) - offset[0] - x;
				float distY = event.getY(i) - offset[1] - y;
				float distSquared = distX * distX + distY * distY;
				if (distSquared <= radius * radius) {
					setStatus(Status.UNLOCKING);
					if (++nTapsRecorded >= totalTapsRequired) {
					    onUnlockCallback.onUnlock();
					}
					return true;
				}
			}
			// tapped somewhere else...
			nTapsRecorded = 0;
		}
		return false;
	}
	
	public interface OnUnlockCallback {
		void onUnlock();
	}

	synchronized public void initializeForScreen(SvgData lockIcon, Context context, int screenWidth, int height) {
		radius = screenWidth / 8;
		lockTextPaint.setTextSize(radius/2);
		this.lockIcon = lockIcon.getIconAtHeight(radius * 2);
		x = screenWidth / 2;
		y = height - radius*2;
		this.context = context;
		
		setStatus(Status.CREATED);
	}
	
	synchronized private Status getStatus() {
		long statusDuration = System.currentTimeMillis() - lastUpdate;
		if (statusDuration >= status.effectDurationMs) {
			setStatus(Status.SILENT);
		}
		return status;
	}
	
	synchronized public void setStatus(Status status) {
		this.status = status;
		lastUpdate = System.currentTimeMillis();
		if (status != Status.UNLOCKING) {
			nTapsRecorded = 0;
		}
	}

}
