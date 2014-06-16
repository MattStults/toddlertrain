package com.mattstultsdragexperiments;

import java.util.Timer;
import java.util.TimerTask;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.view.WindowManager;

public class ToddlerLockService extends Service{
	private final int UPDATE_INTERVAL_MS = 100;
	private Timer timer = null;
	private final IBinder binder = new LocalBinder();
	
	public class LocalBinder extends Binder {
		ToddlerLockService getService() {
			return ToddlerLockService.this;
		}
	}
	@Override
	public IBinder onBind(Intent arg0) {
		return binder;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		startToddlerLock();
		return START_STICKY;
	}
	
	public void startToddlerLock() {
		if (timer != null) {
			timer.cancel();
		}
		final ActivityManager manager = (ActivityManager)(getSystemService(ACTIVITY_SERVICE));
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if(!getPackageName().equalsIgnoreCase(manager.getRunningTasks(1).get(0).topActivity.getPackageName())) {
					Intent trainIntent = new Intent(getBaseContext(), MainActivity.class);
					trainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					getApplication().startActivity(trainIntent);
				}
			}
		}, UPDATE_INTERVAL_MS, UPDATE_INTERVAL_MS);
	}
	
	@Override
	public void onDestroy() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

}
