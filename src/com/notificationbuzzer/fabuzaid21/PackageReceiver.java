package com.notificationbuzzer.fabuzaid21;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PackageReceiver extends BroadcastReceiver {

	private static final String TAG = PackageReceiver.class.getSimpleName();
	private static final String PACKAGE_NAME = PackageReceiver.class.getPackage().getName();

	@Override
	public void onReceive(final Context context, final Intent intent) {
		final String packageName = intent.getData().getEncodedSchemeSpecificPart();
		Log.d(TAG, packageName);
		Log.d(TAG, "Action: " + intent.getAction());

		if (packageName.equals(PACKAGE_NAME)) {
			// if it's our own package, ignore it
			return;
		}
		final NotificationBuzzerApp app = (NotificationBuzzerApp) context.getApplicationContext();

		// check to make sure this is not happening because of an update
		if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
			Log.d(TAG, "app is being replaced (updated), not added");
			return;
		}
		Log.d(TAG, "refreshing app list from PackageReceiver");
		if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {
			// pass in package name so that it can be deleted from the
			// database
                        Log.d(TAG, "passing in package name to be removed from database");
			app.refreshAppList(intent.getData().getEncodedSchemeSpecificPart());
		} else {
                        Log.d(TAG, "passing in null, not deleting from database");
			app.refreshAppList(null);
		}
	}
}
