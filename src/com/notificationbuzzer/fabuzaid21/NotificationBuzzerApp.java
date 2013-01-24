package com.notificationbuzzer.fabuzaid21;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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
import android.net.Uri;
import android.util.Log;

@ReportsCrashes(formKey = "dDhUXzBZT1VmWEYtbDMwazlGa2loRlE6MQ", logcatArguments = { "-t", "150", "-v", "long",
		"dalvikvm:S" })
public class NotificationBuzzerApp extends Application implements Comparator<ResolveInfo> {

	private static final String NOTIFICATION_BUZZER_PACKAGE = NotificationBuzzerApp.class.getPackage().getName();
	private static final String INSTALL_SHORTCUT_INTENT = "com.android.launcher.action.INSTALL_SHORTCUT";
	private static final String HOME_SCREEN_ACTIVITY = NotificationBuzzerFragment.class.getSimpleName();
	private static final String TAG = NotificationBuzzerApp.class.getSimpleName();
	private static final String DATABASE_FILENAME = BuzzDB.DATABASE_NAME;

	private BuzzDB base;
	private DrawableManager drawableManager;

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

	private final class AppListThread extends Thread {

		private final String packageToDelete;

		public AppListThread(final String packageName) {
			super();
			packageToDelete = packageName;
		}

		@Override
		public void run() {
			if (packageToDelete != null) {
				Log.d(TAG, "deleting package " + packageToDelete + " from database, since app was just deleted");
				base.deleteByPackageName(packageToDelete);
			}
			unassignedApps = assignedApps = recommendedApps = null;
			getAppsFromPhone();
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate");
		ACRA.init(this);
		if (isFirstRun()) {
			Log.d(TAG, "first Run");
			addShortcutToHomeScreen();
		}
		drawableManager = new DrawableManager(getPackageManager());
		addSystemAppsToRecommendedPackages();
		base = new BuzzDB(this);
		base.open();
		refreshAppList(null);
	}

	private void addSystemAppsToRecommendedPackages() {
		final PackageManager pm = getPackageManager();
		final Intent stockIntent = new Intent();
		stockIntent.addCategory(Intent.CATEGORY_DEFAULT);

		stockIntent.setAction(Intent.ACTION_SENDTO);
		stockIntent.setType("vnd.android-dir/mms-sms");
		stockIntent.setData(Uri.parse("sms:2125551234"));
		addAllPackageStrings(recommendedPackages,
				pm.queryIntentActivities(stockIntent, PackageManager.MATCH_DEFAULT_ONLY));

		stockIntent.setAction(Intent.ACTION_CALL);
		stockIntent.setType(null);
		stockIntent.setData(Uri.parse("tel:1234567890"));
		addAllPackageStrings(recommendedPackages,
				pm.queryIntentActivities(stockIntent, PackageManager.MATCH_DEFAULT_ONLY));

		stockIntent.setAction(Intent.ACTION_SENDTO);
		stockIntent.setType(null);
		stockIntent.setData(Uri.parse("mailto:foo@bar.com"));
		addAllPackageStrings(recommendedPackages,
				pm.queryIntentActivities(stockIntent, PackageManager.MATCH_DEFAULT_ONLY));

		final Intent calendarIntent = new Intent(Intent.ACTION_EDIT);
		calendarIntent.setType("vnd.android.cursor.item/event");
		calendarIntent.putExtra("allDay", true);
		calendarIntent.putExtra("rrule", "FREQ=YEARLY");
		addAllPackageStrings(recommendedPackages,
				pm.queryIntentActivities(calendarIntent, PackageManager.MATCH_DEFAULT_ONLY));
	}

	private void addAllPackageStrings(final Set<String> set, final List<ResolveInfo> apps) {
		for (final ResolveInfo rInfo : apps) {
			set.add(rInfo.activityInfo.applicationInfo.packageName);
		}
	}

	public void refreshAppList(final String packageToDelete) {
		new AppListThread(packageToDelete).start();
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
			Log.d(TAG, "assignedApps is null");
			getAppsFromPhone();
		}
		return assignedApps;
	}

	public List<ResolveInfo> getRecommendedApps() {
		if (recommendedApps == null) {
			Log.d(TAG, "recommendeApps is null");
			getAppsFromPhone();
		}
		return recommendedApps;
	}

	private synchronized void getAppsFromPhone() {
		Log.d(TAG, "entering getAppsFromPhone, thread id = " + Thread.currentThread().getId());
		if (unassignedApps != null || recommendedApps != null || assignedApps != null) {
			Log.d(TAG, "we already have the data, let's exit");
			return;
		}
		Log.d(TAG, "do not have data, not exiting getAppsFromPhone");
		final PackageManager pm = getPackageManager();

		final Intent intent = new Intent(Intent.ACTION_MAIN, null);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		final List<ResolveInfo> launcherApps = pm.queryIntentActivities(intent, 0);
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
				final ResolveInfo current = allApps.get(elem);
				if (!recommendedApps.contains(current)) {
					recommendedApps.add(current);
				}
				unassignedApps.remove(current);
			}
		}

		Collections.sort(unassignedApps, this);
		Collections.sort(recommendedApps, this);
		baseApps.close();
	}

	private Map<String, ResolveInfo> filterSystemApps(final List<ResolveInfo> allApps) {
		final Map<String, ResolveInfo> notificationApps = new HashMap<String, ResolveInfo>();
		final List<ResolveInfo> drawableList = new LinkedList<ResolveInfo>();
		for (final ResolveInfo rInfo : allApps) {

			final String packageName = rInfo.activityInfo.applicationInfo.packageName;
			if (rInfo.activityInfo.applicationInfo.sourceDir.startsWith("/data/app")
					|| packageName.matches("(com.android.(mms|contacts|calendar|email|phone)|com.google.android.*)")
					|| recommendedPackages.contains(packageName)) {

				if (packageName.equals(NOTIFICATION_BUZZER_PACKAGE)) {
					continue;
				}
				notificationApps.put(packageName, rInfo);
				drawableList.add(rInfo);
			}
		}
		new Thread(new Runnable() {

			@Override
			public void run() {
				drawableManager.addAll(drawableList);
			}
		}).start();
		return notificationApps;
	}

	private boolean isFirstRun() {
		final File file = new File("/data/data/" + NOTIFICATION_BUZZER_PACKAGE + "/databases/" + DATABASE_FILENAME);
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
		drawableManager = null;
		base.close();
		base = null;
	}

	@Override
	public int compare(final ResolveInfo first, final ResolveInfo second) {
		final PackageManager pm = getPackageManager();

		final String firstLabel = (String) first.activityInfo.applicationInfo.loadLabel(pm);
		final String secondLabel = (String) second.activityInfo.applicationInfo.loadLabel(pm);

		return firstLabel.compareToIgnoreCase(secondLabel);
	}

	public DrawableManager getDrawableManager() {
		return drawableManager;
	}
}
