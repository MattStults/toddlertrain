package com.mattstultsdragexperiments;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.provider.CalendarContract.Colors;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class DrawPanel extends SurfaceView implements SurfaceHolder.Callback{

	private AtomicReference<DrawThread> thread = new AtomicReference<DrawThread>();
	private final FpsMonitor fpsMonitor;
	private final GameModel model;
	public DrawPanel(Context context, GameModel model) {
		super(context);
		this.model = model;
		getHolder().addCallback(this);
		fpsMonitor = new FpsMonitor();
		setFocusable(true);
	}
	
	public synchronized void pause() {
		DrawThread thread = this.thread.get();
		if (thread != null) {
		  thread.setRunning(false);
		  this.thread.set(null);
		}
	}
	
	public synchronized void resume() {
		DrawThread thread = this.thread.get();
		if (thread == null) {
			thread = new DrawThread(getHolder(), this, model);
			thread.setRunning(true);
			thread.start();
			fpsMonitor.initialize(System.currentTimeMillis());
			this.thread.set(thread);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		int offset[] = new int[2];
		this.getLocationOnScreen(offset);
		model.setOffset(offset);
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		this.setWillNotDraw(false);
		resume();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		DrawThread thread = this.thread.get();
		pause();
		while (thread != null) {
			try {
				thread.join(); // attempt to shut down.
				return; //we were successful!
			} catch (InterruptedException e) {
				// We still really want to shut down.
				// try again in the while loop.
			}
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		DrawThread thread = this.thread.get();
		if (thread != null) {
			thread.postInput(event);
		}
		return true;
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		//canvas.drawColor(Color.WHITE);
		long timeDelta = fpsMonitor.tick(System.currentTimeMillis());
		model.draw(canvas, timeDelta);
		
		displayFps(canvas, fpsMonitor.fps());
	}

	private void displayFps(Canvas canvas, int fps) {
		Paint fpsPaint = new Paint();
		fpsPaint.setARGB(255, 255, 0, 0);
		fpsPaint.setTextSize(30);
		canvas.drawText("fps: " + fps, 40, 70, fpsPaint);
	}
	private static class FpsMonitor {
		private static final int nSamples = 100;
		private long frames = 0;
		private long lastUpdate;
		private long totalTime = 0;
		
		private final ArrayList<Long> frameLengths;
		
		public FpsMonitor() {
			frameLengths = new ArrayList<Long>(nSamples);
			for (int i = 0; i < nSamples; i++) {
				frameLengths.add(0l);
			}
		}
		
		public void initialize(long initTime) {
			lastUpdate = initTime;
		}
		public int fps() {
			if (frames >= nSamples) {
				return (int)(1.0 / (totalTime / (1000.0 * nSamples)));
			}
			return 0;
		}
		
		public long tick(long currentTime) {
			long frameLength = currentTime - lastUpdate;
			lastUpdate = currentTime;
			int idx = (int) (frames++ % nSamples);
			totalTime -= frameLengths.get(idx);
			totalTime += frameLength;
			frameLengths.set(idx, frameLength);
			return frameLength;
		}
	}
	public interface GameModel {
		public void draw(Canvas canvas, long time);
		public void setOffset(int[] offset);
		public void handleInput(MotionEvent event);
	}
	
	private static class DrawThread extends Thread {
		private static final int MAX_FPS = 60;
		private static final int FRAME_MS = 1000 / MAX_FPS;
		private AtomicBoolean isRunning;
		private final SurfaceHolder surfaceHolder;
		private final DrawPanel drawPanel;
		private final ConcurrentLinkedQueue<MotionEvent> inputQueue;
		private final GameModel gameModel;
		
		public DrawThread(SurfaceHolder surfaceHolder, DrawPanel drawPanel, GameModel gameModel) {
			this.surfaceHolder = surfaceHolder;
			this.surfaceHolder.setFormat(PixelFormat.OPAQUE);
			this.drawPanel = drawPanel;
			this.inputQueue = new ConcurrentLinkedQueue<MotionEvent>();
			this.isRunning = new AtomicBoolean();
			this.gameModel = gameModel;
		}
		
		public void setRunning(boolean isRunning) {
			this.isRunning.set(isRunning);
		}
		
		public void postInput(MotionEvent input) {
			inputQueue.add(input);
		}
		
		@Override
		public void run() {
			while (isRunning.get()) {
				long frameStartTime = System.currentTimeMillis();
				while (!inputQueue.isEmpty()) {
					gameModel.handleInput(inputQueue.remove());
				}
				if (surfaceHolder.getSurface().isValid()) {
				Canvas canvas = surfaceHolder.lockCanvas();
					if (canvas != null) {
						synchronized(surfaceHolder) { // why??
							canvas.drawColor(Color.WHITE);
							//drawPanel.draw(canvas);
							drawPanel.postInvalidate();
						}
						surfaceHolder.unlockCanvasAndPost(canvas);
					}
				}
				long timeLeftover = FRAME_MS - (System.currentTimeMillis() - frameStartTime);
				if (timeLeftover > 0) {
					try {
						Thread.sleep(timeLeftover);
					} catch (InterruptedException e) {
						//Bad...
					}
				}
			}
		}
	}
}
