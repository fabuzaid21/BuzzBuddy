package com.notificationbuzzer.fabuzaid21;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class DrawableManager {

	private static final String TAG = DrawableManager.class.getSimpleName();

	private final Map<String, Drawable> drawableMap;
	private final PackageManager packageManager;

	public DrawableManager(final PackageManager pm) {
		drawableMap = new HashMap<String, Drawable>();
		packageManager = pm;
	}

	public synchronized Drawable fetchDrawable(final String packageName, final ResolveInfo rInfo) {
		synchronized (drawableMap) {
			Log.d(TAG, packageName);
			if (drawableMap.containsKey(packageName)) {
				final Drawable drawable = drawableMap.get(packageName);
				Log.d(TAG, "found drawable");
				if (drawable != null) {
					return drawable;
				}
			}
			Log.d(TAG, "drawable not found or was null");
			final Drawable icon = rInfo.loadIcon(packageManager);
			drawableMap.put(rInfo.activityInfo.applicationInfo.packageName, icon);
			return icon;
		}
	}

	private synchronized void add(final String packageName, final ResolveInfo rInfo, final PackageManager pm) {
		synchronized (drawableMap) {
			drawableMap.put(packageName, rInfo.loadIcon(pm));
		}
	}

	public synchronized void addAll(final List<ResolveInfo> apps) {
		for (final ResolveInfo entry : apps) {
			add(entry.activityInfo.applicationInfo.packageName, entry, packageManager);
		}

	}
}
