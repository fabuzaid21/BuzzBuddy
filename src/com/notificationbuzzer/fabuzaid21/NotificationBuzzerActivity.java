package com.notificationbuzzer.fabuzaid21;

import java.util.ArrayList;
import java.util.List;


import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class NotificationBuzzerActivity extends ListActivity {

	private BuzzDB base;
	ArrayList<ResolveInfo> vibratedApps;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_notification_buzzer);

		//Delete Database--for when I update the schema/triggers and need to test
		//boolean test=this.getApplicationContext().deleteDatabase(LifeDB.DATABASE_NAME);

		//open the database to find apps that have a vibration associated with them already.
		base = new BuzzDB(this);
		base.open();

		
				
		final ListView list = getListView();
		final PackageManager pm = getPackageManager();
						
		
		final Intent intent = new Intent(Intent.ACTION_MAIN, null);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		final List<ResolveInfo> launcherApps = pm.queryIntentActivities(intent, PackageManager.PERMISSION_GRANTED);
		for (ResolveInfo rInfo : launcherApps) {
			Log.w("Installed Applications", rInfo.loadLabel(pm).toString());
		}
		
		vibratedApps=getVibratedApps(launcherApps, pm);
		
		SectionAdapter adapter=new SectionAdapter(this.getApplicationContext());
		
		final AppAdapter unusedApps = new AppAdapter(this, filterSystemApps(launcherApps));
		final AppAdapter usedApps= new AppAdapter(this, vibratedApps);
		
		adapter.addSection("Add a pattern", unusedApps);
		adapter.addSection("Review a pattern", usedApps);
		
		list.setAdapter(adapter);

		//open accessibility menu
		checkAccessibility();
	}

	private ArrayList<ResolveInfo> getVibratedApps(List<ResolveInfo> launcherApps, PackageManager pm) {
		
		//We have the list of all apps, we want two lists. One with used apps, one with unused apps.
		
		ArrayList<ResolveInfo> usedApps=new ArrayList<ResolveInfo>();
		
		ArrayList<String> appNames=new ArrayList<String>();		
		for(int x=0;x<launcherApps.size();x++)
		appNames.add(launcherApps.get(x).activityInfo.applicationInfo.packageName);
				
		Cursor baseApps=base.queryAll(BuzzDB.DATABASE_APP_TABLE);
		baseApps.moveToFirst();
		while(!baseApps.isAfterLast())
		{
			String name=baseApps.getString(BuzzDB.APP_INDEX_NAME);
			if(appNames.contains(name))
			{
				int index=appNames.indexOf(name);
				ResolveInfo item=launcherApps.get(index);
				usedApps.add(item);
				appNames.set(index, "");
				launcherApps.set(index, null);
			}
			
			baseApps.moveToNext();
		}
		
		for(int x=0;x<launcherApps.size();x++)
		{
			if(launcherApps.get(x)==null)
			{
				launcherApps.remove(x);
				x--;
			}
		}
				
		return usedApps;
		
	}

	private static List<ResolveInfo> filterSystemApps(final List<ResolveInfo> allApps) {
		final List<ResolveInfo> notificationApps = new ArrayList<ResolveInfo>();
		for (final ResolveInfo info : allApps) {
			if ((info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0 ||
					info.activityInfo.applicationInfo.packageName.matches("com.android.(mms|contacts|calendar|email)")) {
				notificationApps.add(info);
			}
		}
		return notificationApps;
	}

	public void onDestroy()
	{
		base.close();
		super.onDestroy();
	}

	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_notification_buzzer, menu);
		return true;
	}

	public String getApplicationName(PackageInfo info)
	{
		return info.applicationInfo.packageName;
	}

	public void checkAccessibility()
	{
		AccessibilityManager accMan =
			(AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
		
		List<AccessibilityServiceInfo> validList=accMan.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_HAPTIC);
		
		for(int x=0;x<validList.size();x++)
		{
			String[]packageNames=validList.get(x).packageNames;
			
			for(int y=0;y<packageNames.length;y++)
			{
				if(packageNames[y].equals("com.notificationbuzzer.fabuzaid21"))
					return;
			}
		}
		
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

	protected void enableAccessabilitySettings() {
		Intent settingsIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
		startActivity(settingsIntent);

	}

}
