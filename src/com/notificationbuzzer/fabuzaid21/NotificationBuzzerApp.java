package com.notificationbuzzer.fabuzaid21;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.util.Log;

@ReportsCrashes(formKey = "dDhUXzBZT1VmWEYtbDMwazlGa2loRlE6MQ", logcatArguments = { "-t", "150", "-v", "long",
		"dalvikvm:S" })
public class NotificationBuzzerApp extends Application implements Comparator<ResolveInfo> {

	private static final String NOTIFICATION_BUZZER_PACKAGE = NotificationBuzzerApp.class.getPackage().getName();
	private static final String INSTALL_SHORTCUT_INTENT = "com.android.launcher.action.INSTALL_SHORTCUT";
	private static final String HOME_SCREEN_ACTIVITY = NotificationBuzzerActivity.class.getSimpleName();
	private static final String TAG = NotificationBuzzerApp.class.getSimpleName();
	private static final String APP_PACKAGE = NotificationBuzzerApp.class.getPackage().getName();
	private static final String DATABASE_FILENAME = BuzzDB.DATABASE_NAME;

	private BuzzDB base;
	private ArrayList<ResolveInfo> unassignedApps;
	private ArrayList<ResolveInfo> assignedApps;
	private ArrayList<ResolveInfo> recommendedApps;

	static final Set<String> recommendedPackages = new HashSet<String>() {
		private static final long serialVersionUID = 1L;

		{
			add("com.jb.gosms");
			add("com.whatsapp");
			add("com.yahoo.mobile.client.android.mail");
			add("com.sgiggle.production");
			add("com.instagram.android");
			add("com.twitter.android");
			add("com.skype.raider");
			add("com.android.vending");
			add("com.google.android.apps.maps");
			add("com.google.android.apps.plus");
			add("com.omgpop.dstfree");
			add("com.zynga.scramble");
			add("com.facebook.orca");
			add("com.google.android.gm");
			add("com.google.android.talk");
			add("com.facebook.katana");
			add("com.google.android.apps.googlevoice");
			add("com.android.mms");
			add("com.android.calendar");
		}
	};

	private final Runnable appListRunnable = new Runnable() {

		@Override
		public void run() {
			unassignedApps = assignedApps = recommendedApps = null;
			getAppsFromPhone();
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate");
		ACRA.init(this);
		base = new BuzzDB(this);
		base.open();
		refreshAppList();
		if (isFirstRun()) {
			Log.d(TAG, "first Run");
			addShortcutToHomeScreen();
		}
	}

	public void refreshAppList() {
		new Thread(appListRunnable).start();
	}

	public List<ResolveInfo> getUnassignedApps() {
		if (unassignedApps == null) {
			Log.d(TAG, "unassignedApps is null");
			getAppsFromPhone();
		}
		return unassignedApps;
	}

	public List<ResolveInfo> getAssignedApps() {
		if (assignedApps == null) {
			getAppsFromPhone();
		}
		return assignedApps;
	}

	public List<ResolveInfo> getRecommendedApps() {
		if (recommendedApps == null) {
			getAppsFromPhone();
		}
		return recommendedApps;
	}

	private synchronized void getAppsFromPhone() {
		if (unassignedApps != null || recommendedApps != null || assignedApps != null) {
			return;
		}
		final PackageManager pm = getPackageManager();

		final Intent intent = new Intent(Intent.ACTION_MAIN, null);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		final List<ResolveInfo> launcherApps = pm.queryIntentActivities(intent, PackageManager.PERMISSION_GRANTED);
		final Map<String, ResolveInfo> candidateApps = filterSystemApps(launcherApps);

		unassignedApps = new ArrayList<ResolveInfo>();
		assignedApps = new ArrayList<ResolveInfo>();
		recommendedApps = new ArrayList<ResolveInfo>();
		sortAppAssignment(candidateApps, unassignedApps, recommendedApps, assignedApps, pm);
	}

	private synchronized void sortAppAssignment(final Map<String, ResolveInfo> allApps,
			final List<ResolveInfo> unassignedApps, final List<ResolveInfo> recommendedApps,
			final List<ResolveInfo> assignedApps, final PackageManager pm) {

		final Cursor baseApps = base.queryAll(BuzzDB.DATABASE_APP_TABLE);
		baseApps.moveToFirst();
		while (!baseApps.isAfterLast()) {
			final String packageName = baseApps.getString(BuzzDB.APP_INDEX_NAME);
			Log.d(TAG,
					"first column = " + packageName + ", second column = "
							+ baseApps.getString(BuzzDB.APP_INDEX_VIBRATION));
			assignedApps.add(allApps.remove(packageName));
			baseApps.moveToNext();
		}
		unassignedApps.addAll(allApps.values());

		for (final String elem : recommendedPackages) {
			if (allApps.containsKey(elem)) {
				recommendedApps.add(allApps.get(elem));
				unassignedApps.remove(allApps.get(elem));
			}
		}

		Collections.sort(unassignedApps, this);
		Collections.sort(recommendedApps, this);
		baseApps.close();
	}

	private static Map<String, ResolveInfo> filterSystemApps(final List<ResolveInfo> allApps) {
		final Map<String, ResolveInfo> notificationApps = new HashMap<String, ResolveInfo>();
		for (final ResolveInfo rInfo : allApps) {

			final String packageName = rInfo.activityInfo.applicationInfo.packageName;
			Log.d(TAG, "" + packageName);
			if (rInfo.activityInfo.applicationInfo.sourceDir.startsWith("/data/app")
					|| packageName.matches("(com.android.(mms|contacts|calendar|email)|com.google.android.*)")) {

				if (packageName.equals(NOTIFICATION_BUZZER_PACKAGE)) {
					continue;
				}
				notificationApps.put(packageName, rInfo);
			}
		}
		return notificationApps;
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

	@Override
	public int compare(final ResolveInfo first, final ResolveInfo second) {
		final PackageManager pm = getPackageManager();

		final String firstLabel = (String) first.loadLabel(pm);
		final String secondLabel = (String) second.loadLabel(pm);

		return firstLabel.compareToIgnoreCase(secondLabel);
	}
}
