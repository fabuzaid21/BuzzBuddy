package com.notificationbuzzer.fabuzaid21;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PackageReceiver extends BroadcastReceiver {

	private static final String TAG = PackageReceiver.class.getSimpleName();

	@Override
	public void onReceive(final Context context, final Intent intent) {
		Log.d(TAG, intent.getData().getEncodedSchemeSpecificPart());
		Log.d(TAG, "Action: " + intent.getAction());
		final NotificationBuzzerApp app = (NotificationBuzzerApp) context.getApplicationContext();
		// intent is either pacakge added or package removed
		if (!intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)) {
			// check to make sure this is not happening because of an update
			if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
				Log.d(TAG, "app is being replaced (updated), not added");
				return;
			}
		}
		app.refreshAppList();
	}
}
