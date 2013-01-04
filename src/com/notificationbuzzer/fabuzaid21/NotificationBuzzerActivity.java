package com.notificationbuzzer.fabuzaid21;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ListView;

public class NotificationBuzzerActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_notification_buzzer);
		ListView list = (ListView) findViewById(R.id.app_list);
		List<PackageInfo> installedPackages = getPackageManager().getInstalledPackages(PackageManager.GET_META_DATA);
		AppAdapter adapter = new AppAdapter(this, filterSystemApps(installedPackages));
		list.setAdapter(adapter);
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

}
