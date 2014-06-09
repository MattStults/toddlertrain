package com.mattstultsdragexperiments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends ActionBarActivity {
	private DrawPanel drawPanel;
	private Train train;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		TrainGameModel game = new TrainGameModel();
		drawPanel = new DrawPanel(this, game);
		setContentView(drawPanel);
		getWindow().setBackgroundDrawable(null);

		try {
			Display display = this.getWindowManager().getDefaultDisplay();
			train = new Train(TrainCarData.readTrainPieces(getAssets().open("trainCars.svg")));
			game.addModel(train);
			Track track = new Track(TrackData.readTracks(getAssets().open(
					"tracks.svg")) /* readTracks(display) */,
					display.getWidth(), display.getHeight());
			train.setTrack(track);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
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
		case R.id.action_settings:
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
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
