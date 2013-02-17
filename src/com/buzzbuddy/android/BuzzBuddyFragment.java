package com.buzzbuddy.android;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.emilsjolander.components.stickylistheaders.StickyListHeadersListView;

public class BuzzBuddyFragment extends SherlockListFragment implements OnItemClickListener, OnDismissListener,
		OnCancelListener, Comparator<ResolveInfo>, OnClickListener, OnCheckedChangeListener {

	private static final String BUZZ_BUDDY_PACKAGE = BuzzBuddyFragment.class.getPackage().getName();
	private static final String ACTIVITY_NAME = BuzzBuddyFragment.class.getSimpleName();
	private static final String TAG = ACTIVITY_NAME;
	private static final String ACCESSIBILITY_SERVICE_NAME = BUZZ_BUDDY_PACKAGE + "/" + BUZZ_BUDDY_PACKAGE + "."
			+ NotificationDetectorService.class.getSimpleName();

	private BuzzDB base;
	private VibrationPatternDialog vibrationPatternDialog;
	private VibrationPattern vibrationPattern;
	private boolean isCanceled;
	private int listPosition;
	private List<ResolveInfo> unassignedApps, assignedApps, recommendedApps;
	private BuzzBuddyAdapter adapter;
	private StickyListHeadersListView stickyList;
	private CustomAlertDialog alertDialog;
	private boolean forceClear = false;

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_buzz_buddy, container, false);
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		setMenuVisibility(false);
	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		final BuzzBuddyApp app = (BuzzBuddyApp) getActivity().getApplication();
		base = app.getDatabase();

		stickyList = (StickyListHeadersListView) getListView();
		stickyList.setOnItemClickListener(this);
	}

	@Override
	public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
		inflater.inflate(R.menu.menu_buzz_buddy, menu);
		Log.d(TAG, "onCreateOptionsMenu");
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		// handle item selection
		switch (item.getItemId()) {
		case R.id.clear_selections:
			hideMenu();
			clearChecks();
			return true;
		case R.id.delete_selections:
			hideMenu();
			final Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
			vibrator.cancel();
			adapter.enablePlaybackButtons();
			deleteSelections();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void hideMenu() {
		setMenuVisibility(false);
		forceClear = adapter.getCheckedItemsSize() > 0;
	}

	private void clearChecks() {
		adapter.getCheckedItems().clear();
		// why call notifyDataSetChanged? the data hasn't changed, but we're
		// asking for the list to be re-rendered anyways. Which means that the
		// check boxes will all be unchecked (and we make sure they're unchecked
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
	public void onResume() {
		super.onResume();
		new GetListItemsTask().execute();
		// open accessibility menu
		checkAccessibility();
	}

	@Override
	public void onStop() {
		super.onStop();
		if (vibrationPattern != null && vibrationPatternDialog.isShowing()) {
			vibrationPatternDialog.dismiss();
		}
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
			alertDialog = new CustomAlertDialog(getActivity(), R.style.VibrationPatternDialogStyle);

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
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		} else {
			settingsIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
		}
		startActivity(settingsIntent);
	}

	private boolean isAccessibilityEnabled() {
		int accessibilityEnabled = 0;

		try {
			accessibilityEnabled = Settings.Secure.getInt(getActivity().getContentResolver(),
					android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
		} catch (final SettingNotFoundException e) {
			Log.d(TAG, "Error finding setting, default accessibility to not found: " + e.getMessage());
		}

		if (accessibilityEnabled == 0) {
			return false;
		}

		final String settingValue = Settings.Secure.getString(getActivity().getContentResolver(),
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
		final Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
		vibrator.cancel();
		hideMenu();
		clearChecks();
		adapter.enablePlaybackButtons();
		Log.d(TAG, "onItemClick, postion = " + position);
		listPosition = position;
		if (vibrationPatternDialog == null) {
			vibrationPatternDialog = new VibrationPatternDialog(getActivity(), R.style.VibrationPatternDialogStyle);
			vibrationPatternDialog.setOnDismissListener(this);
			vibrationPatternDialog.setOnCancelListener(this);
		}
		if (adapter.isItemAssigned(position)) {
			final String appName = getAppNameForPosition(position);
			final long[] pattern = getVibrationPattern(appName);
			vibrationPattern = new VibrationPattern(pattern);
		} else {
			vibrationPattern = new VibrationPattern();
		}
		vibrationPatternDialog.setVibrationPattern(vibrationPattern);
		vibrationPatternDialog.setCurrentApp((ResolveInfo) adapter.getItem(position));
		isCanceled = false;
		vibrationPatternDialog.show();
	}

	@Override
	public void onDismiss(final DialogInterface dialog) {
		if (!isCanceled) {
			Log.d(TAG, "onDismiss");
			final long[] finalPattern = vibrationPattern.getFinalizedPattern();
			if (finalPattern == null) {
				return;
			}
			final ContentValues values = new ContentValues();
			final String patternString = VibrationPatternUtils.serializePattern(finalPattern);
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
		Log.d(TAG, "deleting package: " + packageName);
		if (BuzzBuddyApp.recommendedPackages.contains(packageName)) {
			recommendedApps.add(removed);
			return removed;
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

	@Override
	public void onCancel(final DialogInterface dialog) {
		Log.d(TAG, "onCancel");
		isCanceled = true;
	}

	@Override
	public int compare(final ResolveInfo first, final ResolveInfo second) {
		final PackageManager pm = getActivity().getPackageManager();

		final String firstLabel = (String) first.loadLabel(pm);
		final String secondLabel = (String) second.loadLabel(pm);

		return firstLabel.compareToIgnoreCase(secondLabel);
	}

	@Override
	public void onClick(final View v) {
		if (v.isSelected()) {
			Log.d(TAG, "stop playback clicked, position = " + v.getTag());
			final Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
			vibrator.cancel();
			adapter.enablePlaybackButtons();
			return;
		}
		Log.d(TAG, "playback clicked, position = " + v.getTag());

		final Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);

		final int index = (Integer) v.getTag();

		final ResolveInfo item = assignedApps.get(index);
		final String pName = item.activityInfo.applicationInfo.packageName;
		final long[] vibrationPattern = getVibrationPattern(pName);
		Log.d(TAG, "playing vibration pattern!");
		final long delay = VibrationPatternUtils.totalPatternTime(vibrationPattern);
		adapter.disableOtherPlaybackButtonsForTime(index, delay);
		vibrator.vibrate(vibrationPattern, -1);
	}

	private long[] getVibrationPattern(final String pName) {
		String patternString = "0";
		final Cursor entry = base.queryByPackageName(pName);
		entry.moveToFirst();
		if (entry.getCount() > 0) {
			patternString = entry.getString(BuzzDB.APP_INDEX_VIBRATION);
		}
		entry.close();

		final long[] vibrationPattern = VibrationPatternUtils.deserializePattern(patternString);
		return vibrationPattern;
	}

	@Override
	public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
		final Set<Integer> checked = adapter.getCheckedItems();
		if (isChecked) {
			Log.d(TAG, "checkbox checked, position = " + buttonView.getTag());
			if (checked.size() == 0) {
				setMenuVisibility(true);
			}
			checked.add((Integer) buttonView.getTag());
		} else {
			Log.d(TAG, "checkbox unchecked, position = " + buttonView.getTag());
			if (checked.size() == 1) {
				if (!forceClear) {
					setMenuVisibility(false);
				}
			}
			forceClear = false;
			checked.remove(buttonView.getTag());
		}
	}

	private class GetListItemsTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(final Void... unused) {
			final BuzzBuddyApp app = (BuzzBuddyApp) BuzzBuddyFragment.this.getActivity().getApplication();
			unassignedApps = app.getUnassignedApps();
			assignedApps = app.getAssignedApps();
			recommendedApps = app.getRecommendedApps();

			return (null);
		}

		@Override
		protected void onPostExecute(final Void unused) {
			adapter = new BuzzBuddyAdapter(BuzzBuddyFragment.this.getActivity(), BuzzBuddyFragment.this, assignedApps,
					unassignedApps, recommendedApps);
			stickyList.setAdapter(adapter);
		}
	}
}
