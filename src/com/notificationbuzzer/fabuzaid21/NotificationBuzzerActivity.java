package com.notificationbuzzer.fabuzaid21;

import java.util.ArrayList;
import java.util.List;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class NotificationBuzzerActivity extends ListActivity implements OnItemClickListener {

	private static final String NOTIFICATION_BUZZER_PACKAGE = NotificationBuzzerActivity.class.getPackage().getName();
	private static final String ACTIVITY_NAME = NotificationBuzzerActivity.class.getSimpleName();

	private BuzzDB base;
	private List<ResolveInfo> vibratedApps;
	private Dialog vibrationPatternDialog;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_notification_buzzer);

		// Delete Database--for when I update the schema/triggers and need to
		// test boolean test =
		// this.getApplicationContext().deleteDatabase(LifeDB.DATABASE_NAME);

		// open the database to find apps that have a vibration associated with
		// them already.
		base = new BuzzDB(this);
		base.open();

		final ListView list = getListView();
		final PackageManager pm = getPackageManager();

		final Intent intent = new Intent(Intent.ACTION_MAIN, null);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		final List<ResolveInfo> launcherApps = pm.queryIntentActivities(intent, PackageManager.PERMISSION_GRANTED);

		vibratedApps = getVibratedApps(launcherApps, pm);

		final SectionAdapter adapter = new SectionAdapter(this.getApplicationContext());

		final AppAdapter unusedApps = new AppAdapter(this, filterSystemApps(launcherApps));
		final AppAdapter usedApps = new AppAdapter(this, vibratedApps);

		adapter.addSection("Add a pattern", unusedApps);
		adapter.addSection("Review a pattern", usedApps);

		list.setAdapter(adapter);

		list.setOnItemClickListener(this);

	}

	@Override
	protected void onResume() {
		super.onResume();
		// open accessibility menu
		checkAccessibility();
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (vibrationPatternDialog.isShowing()) {
			vibrationPatternDialog.dismiss();
		}
	}

	private List<ResolveInfo> getVibratedApps(final List<ResolveInfo> launcherApps, final PackageManager pm) {

		// We have the list of all apps, we want two lists. One with used apps,
		// one with unused apps.

		final List<ResolveInfo> usedApps = new ArrayList<ResolveInfo>();

		final List<String> appNames = new ArrayList<String>();
		for (int x = 0; x < launcherApps.size(); x++) {
			appNames.add(launcherApps.get(x).activityInfo.applicationInfo.packageName);
		}

		final Cursor baseApps = base.queryAll(BuzzDB.DATABASE_APP_TABLE);
		baseApps.moveToFirst();
		while (!baseApps.isAfterLast()) {
			final String name = baseApps.getString(BuzzDB.APP_INDEX_NAME);
			if (appNames.contains(name)) {
				final int index = appNames.indexOf(name);
				final ResolveInfo item = launcherApps.get(index);
				usedApps.add(item);
				appNames.set(index, "");
				launcherApps.set(index, null);
			}

			baseApps.moveToNext();
		}

		for (int x = 0; x < launcherApps.size(); x++) {
			if (launcherApps.get(x) == null) {
				launcherApps.remove(x);
				x--;
			}
		}

		return usedApps;

	}

	private static List<ResolveInfo> filterSystemApps(final List<ResolveInfo> allApps) {
		final List<ResolveInfo> notificationApps = new ArrayList<ResolveInfo>();
		for (final ResolveInfo info : allApps) {
			if ((info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0
					|| info.activityInfo.applicationInfo.packageName
							.matches("com.android.(mms|contacts|calendar|email)")) {

				if (info.activityInfo.applicationInfo.packageName.equals(NOTIFICATION_BUZZER_PACKAGE)) {
					continue;
				}
				notificationApps.add(info);
			}
		}
		return notificationApps;
	}

	@Override
	public void onDestroy() {
		base.close();
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_notification_buzzer, menu);
		return true;
	}

	public String getApplicationName(final PackageInfo info) {
		return info.applicationInfo.processName;
	}

	private void checkAccessibility() {
		final AccessibilityManager accMan = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);

		final List<AccessibilityServiceInfo> validList = accMan
				.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_HAPTIC);

		for (final AccessibilityServiceInfo info : validList) {
			// Log.w(ACTIVITY_NAME, info.getSettingsActivityName());
			// }
			// for (int x = 0; x < validList.size(); x++) {
			// final String[] packageNames = validList.get(x).packageNames;
			//
			// for (int y = 0; y < packageNames.length; y++) {
			// if (packageNames[y].equals(NOTIFICATION_BUZZER_PACKAGE)) {
			if (info.getSettingsActivityName().endsWith(ACTIVITY_NAME)) {
				return;
			}
		}

		final AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Accessability Settings");
		alert.setMessage("You need to activate accessability settings to use Notification Buzzer. Continue?");

		alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int whichButton) {
				enableAccessabilitySettings();
			}
		});

		alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int whichButton) {
				// Canceled.
			}
		});

		alert.show();
	}

	protected void enableAccessabilitySettings() {
		final Intent settingsIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
		startActivity(settingsIntent);
	}

	@Override
	public void onItemClick(final AdapterView<?> arg0, final View arg1, final int arg2, final long arg3) {
		if (vibrationPatternDialog == null) {
			vibrationPatternDialog = new Dialog(this, R.style.VibrationPatternDialogStyle);
			vibrationPatternDialog.setContentView(R.layout.vibration_pattern);
		}
		vibrationPatternDialog.show();

	}

}
