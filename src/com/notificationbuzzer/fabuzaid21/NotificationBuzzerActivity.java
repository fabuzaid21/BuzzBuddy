package com.notificationbuzzer.fabuzaid21;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.Vibrator;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.emilsjolander.components.stickylistheaders.StickyListHeadersListView;

public class NotificationBuzzerActivity extends SherlockListActivity implements OnItemClickListener, OnDismissListener,
		OnCancelListener, Comparator<ResolveInfo>, OnClickListener, OnCheckedChangeListener {

	private static final String NOTIFICATION_BUZZER_PACKAGE = NotificationBuzzerActivity.class.getPackage().getName();
	private static final String ACTIVITY_NAME = NotificationBuzzerActivity.class.getSimpleName();
	private static final String TAG = ACTIVITY_NAME;
	private static final String ACCESSIBILITY_SERVICE_NAME = NOTIFICATION_BUZZER_PACKAGE + "/"
			+ NOTIFICATION_BUZZER_PACKAGE + "." + NotificationDetectorService.class.getSimpleName();

	private static final String[] recommendedPackages = { "com.jb.gosms", "com.whatsapp",
			"com.yahoo.mobile.client.android.mail", "com.sgiggle.production", "com.instagram.android",
			"com.twitter.android", "com.skype.raider", "com.android.vending", "com.google.android.apps.maps",
			"com.google.android.apps.plus", "com.omgpop.dstfree", "com.zynga.scramble", "com.facebook.orca",
			"com.google.android.gm", "com.google.android.talk", "com.facebook.katana",
			"com.google.android.apps.googlevoice" };

	private BuzzDB base;
	private VibrationPatternDialog vibrationPatternDialog;
	private VibrationPattern vibrationPattern;
	private boolean isCanceled;
	private int listPosition;
	private List<ResolveInfo> unassignedApps, assignedApps, recommendedApps;
	private NotiBuzzAdapter adapter;
	private StickyListHeadersListView stickyList;
	private ActionMode checkedActionMode;
	private CustomAlertDialog alertDialog;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		if (BuildConfig.DEBUG) {
			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites()
					.detectNetwork().penaltyLog().build());
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects().penaltyLog()
					.penaltyDeath().build());
		}
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_notification_buzzer);

		// Delete Database--for when I update the schema/triggers and need to
		// test
		// boolean test
		// =this.getApplicationContext().deleteDatabase(BuzzDB.DATABASE_NAME);

		// open the database to find apps that have a vibration associated with
		// them already.
		base = ((NotificationBuzzerApp) getApplication()).getDatabase();

		stickyList = (StickyListHeadersListView) getListView();
		stickyList.setOnItemClickListener(this);

		final PackageManager pm = getPackageManager();

		final Intent intent = new Intent(Intent.ACTION_MAIN, null);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		final List<ResolveInfo> launcherApps = pm.queryIntentActivities(intent, PackageManager.PERMISSION_GRANTED);
		final Map<String, ResolveInfo> candidateApps = filterSystemApps(launcherApps);

		unassignedApps = new ArrayList<ResolveInfo>();
		assignedApps = new ArrayList<ResolveInfo>();
		recommendedApps = new ArrayList<ResolveInfo>();
		sortAppAssignment(candidateApps, unassignedApps, recommendedApps, assignedApps, pm);

		adapter = new NotiBuzzAdapter(this, assignedApps, unassignedApps, recommendedApps);

		stickyList.setAdapter(adapter);
		stickyList.setOnItemClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		Log.d(TAG, "onCreateOptionsMenu");
		return true;
	}

	private void clearChecks() {
		adapter.getCheckedItems().clear();
		// why call notifyDataSetChanged? the data hasn't changed, but we're
		// asking for the list to be re-rendered anyways. Which means that the
		// check boxes will all be unchecked (and we make sue they're unchecked
		// when getView is called)
		adapter.notifyDataSetChanged();
	}

	private void deleteSelections() {
		final List<ResolveInfo> toDelete = new LinkedList<ResolveInfo>();
		final Set<Integer> checkedItems = adapter.getCheckedItems();
		for (final Integer index : checkedItems) {
			toDelete.add(deleteFromRecordedApps(index));
		}
		assignedApps.removeAll(toDelete);
		Collections.sort(unassignedApps, this);
		Collections.sort(recommendedApps, this);
		checkedItems.clear();
		adapter.notifyDataSetChanged();
		deleteAppsFromDatabase(toDelete);
	}

	private void deleteAppsFromDatabase(final List<ResolveInfo> toDelete) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				for (final ResolveInfo elem : toDelete) {
					base.deleteByPackageName(elem.activityInfo.applicationInfo.packageName);
				}

			};

		}).start();
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

	private void sortAppAssignment(final Map<String, ResolveInfo> allApps, final List<ResolveInfo> unassignedApps,
			final List<ResolveInfo> recommendedApps, final List<ResolveInfo> assignedApps, final PackageManager pm) {
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

		for (int x = 0; x < recommendedPackages.length; x++) {
			if (allApps.containsKey(recommendedPackages[x])) {
				recommendedApps.add(allApps.get(recommendedPackages[x]));
				unassignedApps.remove(allApps.get(recommendedPackages[x]));
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

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		super.onDestroy();
	}

	private void checkAccessibility() {
		if (isAccessibilityEnabled()) {
			return;
		}
		if (alertDialog != null && alertDialog.isShowing()) {
			return;
		}
		if (alertDialog == null) {
			alertDialog = new CustomAlertDialog(this, R.style.VibrationPatternDialogStyle);

			alertDialog.setOnClickListener(new DialogInterface.OnClickListener() {
				@Override
				public void onClick(final DialogInterface dialog, final int whichButton) {
					enableAccessabilitySettings();
					alertDialog.dismiss();
				}
			});
		}
		alertDialog.show();
	}

	private void enableAccessabilitySettings() {
		final Intent settingsIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
		settingsIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
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
		final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		vibrator.cancel();
		clearChecks();
		adapter.enabledPlaybackButtons();
		Log.d(TAG, "postion = " + position);
		listPosition = position;
		if (vibrationPatternDialog == null) {
			vibrationPatternDialog = new VibrationPatternDialog(this, R.style.VibrationPatternDialogStyle);
			vibrationPatternDialog.setOnDismissListener(this);
			vibrationPatternDialog.setOnCancelListener(this);
		}
		vibrationPattern = new VibrationPattern();
		vibrationPatternDialog.setVibrationPattern(vibrationPattern);
		vibrationPatternDialog.setCurrentApp((ResolveInfo) adapter.getItem(position));
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
				updateOrAddToRecordedApps(listPosition, true);
				new Thread(new Runnable() {

					@Override
					public void run() {
						base.updateRow(BuzzDB.DATABASE_APP_TABLE, rowId, values);

					}
				}).start();
			} else {
				updateOrAddToRecordedApps(listPosition, false);
				new Thread(new Runnable() {

					@Override
					public void run() {
						base.createRow(BuzzDB.DATABASE_APP_TABLE, values);

					}
				}).start();
			}
			nameCheck.close();
		}
	}

	private void updateOrAddToRecordedApps(final int position, final boolean update) {

		// if adding new
		if (!update) {
			// if you are adding a recommended app
			if (position < assignedApps.size() + recommendedApps.size()) {
				assignedApps.add(0, recommendedApps.remove(position - assignedApps.size()));
			} else {
				assignedApps.add(0, unassignedApps.remove(position - assignedApps.size() - recommendedApps.size()));
			}
		} else {
			assignedApps.add(0, assignedApps.remove(position));
		}
		adapter.notifyDataSetChanged();
	}

	private ResolveInfo deleteFromRecordedApps(final int position) {
		final ResolveInfo removed = assignedApps.get(position);
		final String packageName = removed.activityInfo.applicationInfo.packageName;
		for (int x = 0; x < recommendedPackages.length; x++) {
			if (recommendedPackages[x].equals(packageName)) {
				recommendedApps.add(removed);
				return removed;
			}
		}
		unassignedApps.add(removed);
		return removed;
	}

	private String getAppNameForPosition(final int position) {
		if (position < assignedApps.size()) {
			return assignedApps.get(position).activityInfo.applicationInfo.packageName;
		} else if (position < assignedApps.size() + recommendedApps.size()) {
			return recommendedApps.get(position - assignedApps.size()).activityInfo.applicationInfo.packageName;
		} else {
			return unassignedApps.get(position - assignedApps.size() - recommendedApps.size()).activityInfo.packageName;
		}
	}

	private static String serializePattern(final Long[] finalPattern) {
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

		return firstLabel.compareToIgnoreCase(secondLabel);
	}

	@Override
	public void onClick(final View v) {
		if (v.isSelected()) {
			Log.d(TAG, "stop playback clicked, position = " + v.getTag());
			final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			vibrator.cancel();
			adapter.enabledPlaybackButtons();
			return;
		}
		Log.d(TAG, "playback clicked, position = " + v.getTag());

		final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		final int index = (Integer) v.getTag();

		String patternString = "0";

		final ResolveInfo item = assignedApps.get(index);
		final String pName = item.activityInfo.applicationInfo.packageName;
		final Cursor entry = base.queryByPackageName(pName);
		entry.moveToFirst();
		if (entry.getCount() > 0) {
			patternString = entry.getString(BuzzDB.APP_INDEX_VIBRATION);
		}

		final long[] vibrationPattern = NotificationDetectorService.deserializePattern(patternString);
		Log.d(TAG, "playing vibration pattern!");
		final long delay = totalPatternTime(vibrationPattern);
		adapter.disableOtherPlaybackButtonsForTime(index, delay);
		vibrator.vibrate(vibrationPattern, -1);
		entry.close();
	}

	private static long totalPatternTime(final long[] pattern) {
		long toReturn = 0;
		for (final long elem : pattern) {
			toReturn += elem;
		}
		return toReturn;
	}

	@Override
	public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
		final Set<Integer> checked = adapter.getCheckedItems();
		if (isChecked) {
			Log.d(TAG, "checkbox checked, position = " + buttonView.getTag());
			checked.add((Integer) buttonView.getTag());
			if (checked.size() == 1) {
				checkedActionMode = startActionMode(checkedActionModeCallback);
			}
		} else {
			Log.d(TAG, "checkbox unchecked, position = " + buttonView.getTag());
			checked.remove(buttonView.getTag());
			if (checked.size() == 0) {
				if (checkedActionMode != null) {
					checkedActionMode.finish();
				}
			}
		}
	}

	private final ActionMode.Callback checkedActionModeCallback = new ActionMode.Callback() {

		private boolean isDeleting;

		// Called when the action mode is created; startActionMode() was called
		@Override
		public boolean onCreateActionMode(final ActionMode mode, final Menu menu) {
			// Inflate a menu resource providing context menu items
			Log.d(TAG, "inflating checked action menu");
			final MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.notification_buzzer_menu, menu);
			return true;
		}

		// Called each time the action mode is shown. Always called after
		// onCreateActionMode, but may be called multiple times if the mode
		// is invalidated.
		@Override
		public boolean onPrepareActionMode(final ActionMode mode, final Menu menu) {
			isDeleting = false;
			return true;
		}

		// Called when the user selects a contextual menu item
		@Override
		public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
			switch (item.getItemId()) {
			case R.id.delete_selections:
				deleteSelections();
				isDeleting = true;
				mode.finish();
				return true;
			default:
				return false;
			}

		}

		// Called when the user exits the action mode
		@Override
		public void onDestroyActionMode(final ActionMode mode) {
			if (!isDeleting) {
				clearChecks();
			}
			checkedActionMode = null;
		}
	};
}
