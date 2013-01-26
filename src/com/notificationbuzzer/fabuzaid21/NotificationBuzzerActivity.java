package com.notificationbuzzer.fabuzaid21;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class NotificationBuzzerActivity extends SherlockFragmentActivity {

	private static final String TAG = NotificationBuzzerActivity.class.getSimpleName();
	protected Dialog mSplashDialog;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		if (BuildConfig.DEBUG) {
			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites()
					.detectNetwork().penaltyLog().build());
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects().penaltyLog()
					.penaltyDeath().build());
		}
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		setContentView(R.layout.activity_notification_buzzer);
		showSplashScreen();

		final NotificationBuzzerFragment fragment = new NotificationBuzzerFragment();
		getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fragment).commit();
	}
	
	private void showSplashScreen() {

		mSplashDialog = new Dialog(this);
		mSplashDialog.setContentView(R.layout.splashscreen);
		mSplashDialog.setCancelable(false);
		mSplashDialog.show();

		// Set Runnable to remove splash screen just in case
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				removeSplashScreen();
			}
		}, 3000);
	}
	
	protected void removeSplashScreen() {
		   if (mSplashDialog != null) {
		        mSplashDialog.dismiss();
		        mSplashDialog = null;
		    }
			
		}

}
