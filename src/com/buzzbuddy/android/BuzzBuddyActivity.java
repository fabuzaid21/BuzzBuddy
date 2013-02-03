package com.buzzbuddy.android;

import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class BuzzBuddyActivity extends SherlockFragmentActivity {

	private static final String TAG = BuzzBuddyActivity.class.getSimpleName();

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		if (BuildConfig.DEBUG) {
			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites()
					.detectNetwork().penaltyLog().build());
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects().penaltyLog()
					.penaltyDeath().build());
		}
		super.onCreate(savedInstanceState);
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "onCreate");
		}
		setContentView(R.layout.activity_buzz_buddy);

		final BuzzBuddyFragment fragment = new BuzzBuddyFragment();
		getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fragment).commit();
	}
}
