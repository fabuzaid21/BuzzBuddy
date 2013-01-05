package com.notificationbuzzer.fabuzaid21;

import java.util.ArrayList;
import java.util.List;


import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.accessibility.AccessibilityManager;
import android.widget.ListView;

public class NotificationBuzzerActivity extends ListActivity {

	BuzzDB base;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_notification_buzzer);
		ListView list = getListView();
		
		//Delete Database--for when I update the schema/triggers and need to test
		//boolean test=this.getApplicationContext().deleteDatabase(LifeDB.DATABASE_NAME);

		//open the database to find apps that have a vibratino associated with them already.		
		base=new BuzzDB(this);
		base.open();
		
		Cursor currentVibes=base.queryAll(BuzzDB.DATABASE_APP_TABLE);
		currentVibes.moveToFirst();
		
		
		List<PackageInfo> installedPackages = getPackageManager().getInstalledPackages(PackageManager.GET_META_DATA);
		AppAdapter adapter = new AppAdapter(this, filterSystemApps(installedPackages));
		list.setAdapter(adapter);

		//open accessibility menu
		checkAccessibility();


	}

	private static List<PackageInfo> filterSystemApps(final List<PackageInfo> allApps) {
		final List<PackageInfo> notificationApps = new ArrayList<PackageInfo>();
		for (final PackageInfo info : allApps) {
			if ((info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
				notificationApps.add(info);
			}
		}
		return notificationApps;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_notification_buzzer, menu);
		return true;
	}
	
	public String getApplicationName(PackageInfo info)
	{
		return info.applicationInfo.processName;
	}

	public void checkAccessibility()
	{
		AccessibilityManager accMan =
			(AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
		final boolean accessEnabled = accMan.isEnabled();

		if (!accessEnabled) {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle("Accessability Settings");
			alert.setMessage("You need to activate accessability settings to use Notification Buzzer. Continue?");

			alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					enableAccessabilitySettings();
				}
			});

			alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Canceled.
				}
			});

			alert.show();

		}


	}

	protected void enableAccessabilitySettings() {
		Intent settingsIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
		startActivity(settingsIntent);

	}

}


