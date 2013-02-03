package com.buzzbuddy.android;

import java.util.List;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.support.v4.util.LruCache;
import android.util.Log;

public class DrawableManager {

	private static final String TAG = DrawableManager.class.getSimpleName();

	private final LruCache<String, Drawable> drawableCache;
	private final PackageManager packageManager;
	private static final int CACHE_SIZE = 100;

	public DrawableManager(final PackageManager pm) {
		packageManager = pm;
		drawableCache = new LruCache<String, Drawable>(CACHE_SIZE);
	}

	public synchronized Drawable fetchDrawable(final String packageName, final ResolveInfo rInfo) {
		synchronized (drawableCache) {
			final Drawable drawable = drawableCache.get(packageName);
			if (drawable != null) {
				if (BuildConfig.DEBUG) {
					Log.d(TAG, "FOUND " + packageName);
				}
				return drawable;
			}
			if (BuildConfig.DEBUG) {
				Log.d(TAG, packageName + " NOT found");
			}
			final ApplicationInfo applicationInfo = rInfo.activityInfo.applicationInfo;
			final Drawable icon = applicationInfo.loadIcon(packageManager);
			drawableCache.put(applicationInfo.packageName, icon);
			return icon;
		}
	}

	private synchronized void add(final String packageName, final ResolveInfo rInfo, final PackageManager pm) {
		synchronized (drawableCache) {
			drawableCache.put(packageName, rInfo.activityInfo.applicationInfo.loadIcon(pm));
		}
	}

	public synchronized void addAll(final List<ResolveInfo> apps) {
		for (final ResolveInfo entry : apps) {
			add(entry.activityInfo.applicationInfo.packageName, entry, packageManager);
		}

	}
}
