package com.notificationbuzzer.fabuzaid21;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.emilsjolander.components.stickylistheaders.StickyListHeadersListView;

public class NotificationBuzzerActivity extends ListActivity implements OnItemClickListener, OnDismissListener,
		OnCancelListener {

	private static final String NOTIFICATION_BUZZER_PACKAGE = NotificationBuzzerActivity.class.getPackage().getName();
	private static final String ACTIVITY_NAME = NotificationBuzzerActivity.class.getSimpleName();
	private static final String TAG = ACTIVITY_NAME;

	private BuzzDB base;
	private VibrationPatternDialog vibrationPatternDialog;
	private VibrationPattern vibrationPattern;
	private boolean isCanceled;
	private int listPosition;
	private List<ResolveInfo> unassignedApps;
	private List<ResolveInfo> assignedApps;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_notification_buzzer);

		// Delete Database--for when I update the schema/triggers and need to
		// test boolean test =
		// this.getApplicationContext().deleteDatabase(LifeDB.DATABASE_NAME);

		// open the database to find apps that have a vibration associated with
		// them already.

		base = ((NotificationBuzzerApp) getApplication()).getDatabase();
		base.open();

		final StickyListHeadersListView stickyList = (StickyListHeadersListView) getListView();
		stickyList.setDivider(new ColorDrawable(0xffffffff));
		stickyList.setDividerHeight(1);
		// stickyList.setOnScrollListener(this);
		stickyList.setOnItemClickListener(this);
		// stickyList.setOnHeaderClickListener(this);

		final PackageManager pm = getPackageManager();

		final Intent intent = new Intent(Intent.ACTION_MAIN, null);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		final List<ResolveInfo> launcherApps = pm.queryIntentActivities(intent, PackageManager.PERMISSION_GRANTED);
		final List<ResolveInfo> candidateApps = filterSystemApps(launcherApps);

		unassignedApps = new ArrayList<ResolveInfo>();
		assignedApps = new ArrayList<ResolveInfo>();
		sortAppAssignment(candidateApps, unassignedApps, assignedApps, pm);

		final NotiBuzzAdapter adapter = new NotiBuzzAdapter(this, candidateApps, assignedApps.size() - 1);

		stickyList.setAdapter(adapter);
		stickyList.setOnItemClickListener(this);
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
		if (vibrationPattern != null && vibrationPatternDialog.isShowing()) {
			vibrationPatternDialog.dismiss();
		}
	}

	private void sortAppAssignment(final List<ResolveInfo> allApps, final List<ResolveInfo> unassignedApps,
			final List<ResolveInfo> assignedApps, final PackageManager pm) {

		final Set<String> appsInDatabase = new HashSet<String>();
		final Cursor baseApps = base.queryAll(BuzzDB.DATABASE_APP_TABLE);
		baseApps.moveToFirst();
		while (!baseApps.isAfterLast()) {
			final String appName = baseApps.getString(BuzzDB.APP_INDEX_NAME);
			Log.d(TAG, "first column = " + baseApps.getString(1) + ", second column = " + appName);
			appsInDatabase.add(appName);
			baseApps.moveToNext();
		}

		for (final ResolveInfo rInfo : allApps) {
			if (appsInDatabase.contains(rInfo.activityInfo.applicationInfo.packageName)) {
				assignedApps.add(rInfo);
			} else {
				unassignedApps.add(rInfo);
			}
		}
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
			if (info.getSettingsActivityName().endsWith(ACTIVITY_NAME)) {
				return;
			}
		}

		final AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Accessability Settings");
		alert.setMessage(getString(R.string.activate_accessability_settings));

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
	public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
		Log.d(TAG, "postion = " + position);
		listPosition = position;
		if (vibrationPatternDialog == null) {
			vibrationPatternDialog = new VibrationPatternDialog(this, R.style.VibrationPatternDialogStyle);
			vibrationPatternDialog.setOnDismissListener(this);
			vibrationPatternDialog.setOnCancelListener(this);
		}
		vibrationPattern = new VibrationPattern();
		vibrationPatternDialog.setVibrationPattern(vibrationPattern);
		isCanceled = false;
		vibrationPatternDialog.show();

	}

	@Override
	public void onDismiss(final DialogInterface dialog) {
		if (!isCanceled) {
			Log.d(TAG, "onDismiss");
			final Long[] finalPattern = vibrationPattern.getFinalizedPattern();
			final ContentValues values = new ContentValues();
			final String patternString = serializePattern(finalPattern);
			final String appName = getAppNameForPosition(listPosition);
			Log.d(TAG, "patternString = " + patternString);
			Log.d(TAG, "appName = " + appName);
			values.put(BuzzDB.APP_KEY_NAME, appName);
			values.put(BuzzDB.APP_KEY_VIBRATION, patternString);
			base.createRow(BuzzDB.DATABASE_APP_TABLE, values);
		}
	}

	private String getAppNameForPosition(final int position) {
		if (position < assignedApps.size()) {
			return assignedApps.get(position).activityInfo.applicationInfo.packageName;
		} else {
			return unassignedApps.get(position - assignedApps.size()).activityInfo.applicationInfo.packageName;
		}
	}

	private String serializePattern(final Long[] finalPattern) {
		String toReturn = "" + finalPattern[0];
		for (int i = 1; i < finalPattern.length; ++i) {
			toReturn += "-" + finalPattern[i];
		}
		return toReturn;
	}

	@Override
	public void onCancel(final DialogInterface dialog) {
		Log.d(TAG, "onCancel");
		isCanceled = true;

	}
}
