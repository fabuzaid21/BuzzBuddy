package com.notificationbuzzer.fabuzaid21;

import java.io.File;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;

public class NotificationBuzzerApp extends Application {

	private static final String INSTALL_SHORTCUT_INTENT = "com.android.launcher.action.INSTALL_SHORTCUT";
	private static final String HOME_SCREEN_ACTIVITY = NotificationBuzzerActivity.class.getSimpleName();
	private static final String TAG = NotificationBuzzerApp.class.getSimpleName();
	private static final String APP_PACKAGE = NotificationBuzzerApp.class.getPackage().getName();
	private static final String DATABASE_FILENAME = BuzzDB.DATABASE_NAME;

	private BuzzDB base;

	@Override
	public void onCreate() {
		super.onCreate();
		base = new BuzzDB(this);
		base.open();
		if (isFirstRun()) {
			addShortcutToHomeScreen();
		}
	}

	private boolean isFirstRun() {
		final File file = new File("/data/data/" + APP_PACKAGE + "/databases/" + DATABASE_FILENAME);
		return !file.exists();
	}

	private void addShortcutToHomeScreen() {
		Log.d(TAG, "creating Shortcut!");
		final Intent shortcutIntent = new Intent();
		shortcutIntent.setComponent(new ComponentName(getPackageName(), "." + HOME_SCREEN_ACTIVITY));

		shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		final Intent addIntent = new Intent();
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.app_name));
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
				Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_launcher));
		addIntent.setAction(INSTALL_SHORTCUT_INTENT);
		sendBroadcast(addIntent);
	}

	public BuzzDB getDatabase() {
		return base;
	}

	@Override
	public void onTerminate() {
		base.close();
		base = null;
	}
}
