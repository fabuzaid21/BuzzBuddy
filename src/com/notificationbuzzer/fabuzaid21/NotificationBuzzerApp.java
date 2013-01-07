package com.notificationbuzzer.fabuzaid21;

import android.app.Application;

public class NotificationBuzzerApp extends Application {

	private BuzzDB base;

	@Override
	public void onCreate() {
		super.onCreate();
		base = new BuzzDB(this);

	}

	public BuzzDB getDatabase() {
		return base;
	}

}
