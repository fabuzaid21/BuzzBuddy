package com.notificationbuzzer.fabuzaid21;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.os.Vibrator;

import com.actionbarsherlock.app.SherlockListActivity;
import com.emilsjolander.components.stickylistheaders.StickyListHeadersListView;

public class NotificationBuzzerActivity extends SherlockListActivity implements OnItemClickListener, OnDismissListener,
		OnCancelListener, Comparator<ResolveInfo>, OnClickListener {

	private static final String NOTIFICATION_BUZZER_PACKAGE = NotificationBuzzerActivity.class.getPackage().getName();
	private static final String ACTIVITY_NAME = NotificationBuzzerActivity.class.getSimpleName();
	private static final String TAG = ACTIVITY_NAME;
	private static final String ACCESSIBILITY_SERVICE_NAME = "com.notificationbuzzer.fabuzaid21/com.notificationbuzzer.fabuzaid21.NotificationDetectorService";

	private BuzzDB base;
	private VibrationPatternDialog vibrationPatternDialog;
	private VibrationPattern vibrationPattern;
	private boolean isCanceled;
	private int listPosition;
	private List<ResolveInfo> unassignedApps;
	private List<ResolveInfo> assignedApps;
	private NotiBuzzAdapter adapter;
	private StickyListHeadersListView stickyList;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_notification_buzzer);

		// Delete Database--for when I update the schema/triggers and need to
		// test
		// boolean test
		// =this.getApplicationContext().deleteDatabase(BuzzDB.DATABASE_NAME);

		// open the database to find apps that have a vibration associated with
		// them already.

		base = ((NotificationBuzzerApp) getApplication()).getDatabase();
		base.open();

		stickyList = (StickyListHeadersListView) getListView();
		stickyList.setOnItemClickListener(this);

		final PackageManager pm = getPackageManager();

		final Intent intent = new Intent(Intent.ACTION_MAIN, null);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		final List<ResolveInfo> launcherApps = pm.queryIntentActivities(intent, PackageManager.PERMISSION_GRANTED);
		final List<ResolveInfo> candidateApps = filterSystemApps(launcherApps);

		unassignedApps = new ArrayList<ResolveInfo>();
		assignedApps = new ArrayList<ResolveInfo>();
		sortAppAssignment(candidateApps, unassignedApps, assignedApps, pm);

		adapter = new NotiBuzzAdapter(this, assignedApps, unassignedApps);

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
			Log.d(TAG, "first column = " + appName + ", second column = " + baseApps.getString(2));
			appsInDatabase.add(appName);
			baseApps.moveToNext();
		}

		for (final ResolveInfo rInfo : allApps) {
			if (appsInDatabase.contains(rInfo.activityInfo.applicationInfo.packageName)) {
				assignedApps.add(rInfo);

			} else {
				unassignedApps.add(rInfo);
				Collections.sort(unassignedApps, this);
			}
		}
	}

	private static List<ResolveInfo> filterSystemApps(final List<ResolveInfo> allApps) {
		final List<ResolveInfo> notificationApps = new ArrayList<ResolveInfo>();
		for (final ResolveInfo info : allApps) {

			final String packageName = info.activityInfo.applicationInfo.packageName;
			Log.d(TAG, "" + packageName);
			if (info.activityInfo.applicationInfo.sourceDir.startsWith("/data/app")
					|| packageName.matches("(com.android.(mms|contacts|calendar|email)|com.google.android.*)")) {

				if (packageName.equals(NOTIFICATION_BUZZER_PACKAGE)) {
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

	public String getApplicationName(final PackageInfo info) {
		return info.applicationInfo.processName;
	}

	private void checkAccessibility() {
		if (isAccessibilityEnabled()) {
			return;
		}

		final AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Accessability Settings");
		alert.setMessage(getString(R.string.activate_accessability_settings));

		alert.setPositiveButton("Activate", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int whichButton) {
				enableAccessabilitySettings();
			}
		});
		alert.show();
	}

	protected void enableAccessabilitySettings() {
		final Intent settingsIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
		startActivity(settingsIntent);
	}

	private boolean isAccessibilityEnabled() {
		int accessibilityEnabled = 0;

		try {
			accessibilityEnabled = Settings.Secure.getInt(this.getContentResolver(),
					android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
		} catch (final SettingNotFoundException e) {
			Log.d(TAG, "Error finding setting, default accessibility to not found: " + e.getMessage());
		}

		if (accessibilityEnabled == 0) {
			return false;
		}

		final String settingValue = Settings.Secure.getString(getContentResolver(),
				Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
		if (settingValue != null) {
			final TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
			splitter.setString(settingValue);
			while (splitter.hasNext()) {
				final String accessabilityService = splitter.next();
				if (accessabilityService.equalsIgnoreCase(ACCESSIBILITY_SERVICE_NAME)) {
					return true;
				}
			}
		}
		return false;
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
			if (finalPattern == null) {
				return;
			}
			final ContentValues values = new ContentValues();
			final String patternString = serializePattern(finalPattern);
			final String appName = getAppNameForPosition(listPosition);

			Log.d(TAG, "patternString = " + patternString);
			Log.d(TAG, "appName = " + appName);
			values.put(BuzzDB.APP_KEY_NAME, appName);
			values.put(BuzzDB.APP_KEY_VIBRATION, patternString);
			values.put(BuzzDB.APP_KEY_DATE, Calendar.getInstance().getTimeInMillis());

			final Cursor nameCheck = base.queryByPackageName(appName);
			nameCheck.moveToFirst();
			if (nameCheck.getCount() > 0) {
				final long rowId = nameCheck.getLong(BuzzDB.INDEX_ROW_ID);
				base.updateRow(BuzzDB.DATABASE_APP_TABLE, rowId, values);
				updateLists(listPosition, true);
			} else {
				base.createRow(BuzzDB.DATABASE_APP_TABLE, values);
				updateLists(listPosition, false);
			}
		}
	}

	private void updateLists(final int position, final boolean update) {

		if (!update) {
			assignedApps.add(0, unassignedApps.get(position - assignedApps.size()));
			unassignedApps.remove(position - (assignedApps.size() - 1));
		} else {
			assignedApps.add(0, assignedApps.remove(position));
		}
		adapter.notifyDataSetChanged();
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

	@Override
	public int compare(final ResolveInfo first, final ResolveInfo second) {
		final PackageManager pm = getPackageManager();

		final String firstLabel = (String) first.loadLabel(pm);
		final String secondLabel = (String) second.loadLabel(pm);

		return firstLabel.compareTo(secondLabel);
	}

	@Override
	public void onClick(final View v) {
		Log.d(TAG, "playback clicked, position = " + v.getTag());
		
		final Vibrator vibrator;
		
		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		
		int index=(Integer)v.getTag();
		String patternString="0";
		
		ResolveInfo item=assignedApps.get(index);
		String pName=item.activityInfo.applicationInfo.packageName;
		Cursor entry=base.query(BuzzDB.DATABASE_APP_TABLE, BuzzDB.APP_KEYS_ALL, BuzzDB.APP_KEY_NAME+"=\""+pName+"\"");
		entry.moveToFirst();
		if(entry.getCount()>0)
		{
			patternString=entry.getString(BuzzDB.APP_INDEX_VIBRATION);
		}
		
		
		final long[] vibrationPattern = NotificationDetectorService.deserializePattern(patternString);
		Log.d(TAG, "playing vibration pattern!");
		vibrator.vibrate(vibrationPattern, -1);
	
		
	}
}
