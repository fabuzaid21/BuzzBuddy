package com.notificationbuzzer.fabuzaid21;

import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class NotificationBuzzerActivity extends SherlockFragmentActivity {

	private static final String TAG = NotificationBuzzerActivity.class.getSimpleName();

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

		final NotificationBuzzerFragment fragment = new NotificationBuzzerFragment();
		getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fragment).commit();
	}
}
