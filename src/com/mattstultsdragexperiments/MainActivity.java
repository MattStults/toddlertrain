package com.mattstultsdragexperiments;

import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.WindowManager;

public class MainActivity extends ActionBarActivity {
	public static final String COMPONENT_NAME = "com.mattstultsdragexperiments.MainActivity";
	private DrawPanel drawPanel;
	private Train train;
	private TrainGameModel game;
	private Intent toddlerLockIntent;
	private Map<String,SvgData> icons;
	private boolean isToddlerLocked;
	private final ToddlerLockButton toddlerLockButton = new ToddlerLockButton(
			new ToddlerLockButton.OnUnlockCallback() {
				@Override
				public void onUnlock() {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							doStopService();
							game.removeModel(toddlerLockButton);
							getSupportActionBar().show();
							isToddlerLocked = false;
						}
					});
				}
			});
	
	private void doBindService() {
		startService((toddlerLockIntent = new Intent(this, ToddlerLockService.class)));
	}
	
	private void doStopService() {
		stopService(toddlerLockIntent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		game = new TrainGameModel();
		drawPanel = new DrawPanel(this, game);
		setContentView(drawPanel);
		getWindow().setBackgroundDrawable(null);

		try {
			icons = SvgData.readSvgObjectLibrary(getAssets().open("icons.svg"));
			Display display = this.getWindowManager().getDefaultDisplay();
			train = new Train(SvgData.readSvgObjects(getAssets().open("trainCars.svg")));
			game.addModelFront(train);
			Track track = new Track(TrackData.readTracks(getAssets().open(
					"tracks.svg")) /* readTracks(display) */,
					display.getWidth(), display.getHeight());
			train.setTrack(track);
			toddlerLockButton.initializeForScreen(icons.get("lock"), this, display.getWidth(), display.getHeight());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// Ignore back when in toddler lock.
		if (isToddlerLocked && (keyCode == KeyEvent.KEYCODE_BACK)) {
			this.toddlerLockButton.setStatus(ToddlerLockButton.Status.ATTEMPTED_APP_LEAVE);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onPause() {
		super.onPause();
		// Debug.stopMethodTracing();
		drawPanel.pause();
	}

	@Override
	public void onResume() {
		super.onResume();
		// Debug.startMethodTracing("trainPerf");
		updatePreferences(PreferenceManager.getDefaultSharedPreferences(this));
		drawPanel.resume();
		if (isToddlerLocked) {
			toddlerLockButton.setStatus(ToddlerLockButton.Status.ATTEMPTED_APP_LEAVE);
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		switch (id) {
		case R.id.toddler_lock:
			isToddlerLocked = true;
			if (isToddlerLocked) {
				doBindService();
				game.addModel(toddlerLockButton);
				toddlerLockButton.setStatus(ToddlerLockButton.Status.CREATED);
				getSupportActionBar().hide();
			}
			return true;
		case R.id.quit_button:
			finish();
			doStopService();
			System.exit(0);
		}
		return super.onOptionsItemSelected(item);
	}

	private void updatePreferences(SharedPreferences sharedPreferences) {
		Resources res = getResources();
		train.setNumberOfCars(Integer.parseInt(sharedPreferences.getString(
				res.getString(R.string.pref_carCount_key),
				res.getString(R.string.pref_carCount_default))));
		train.setTrainScale(Float.parseFloat(sharedPreferences.getString(
				res.getString(R.string.pref_trainScale_key),
				res.getString(R.string.pref_trainScale_default))));
	}

}
