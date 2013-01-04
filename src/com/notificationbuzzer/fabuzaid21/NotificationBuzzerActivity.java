package com.notificationbuzzer.fabuzaid21;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ListView;

public class NotificationBuzzerActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_notification_buzzer);
		ListView list = (ListView) findViewById(R.id.app_list);
		AppAdapter adapter = new AppAdapter(this, getPackageManager().getInstalledPackages(0));
		list.setAdapter(adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_notification_buzzer, menu);
		return true;
	}

}
