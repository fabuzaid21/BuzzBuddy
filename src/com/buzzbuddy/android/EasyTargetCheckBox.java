package com.buzzbuddy.android;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;

public class EasyTargetCheckBox extends CheckBox {

	private static final String TAG = EasyTargetCheckBox.class.getSimpleName();

	public EasyTargetCheckBox(final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}

	// Unfortunately, this doesn't help in ICS and above, but it helps for
	// Gingerbread!
	@Override
	public void getHitRect(final Rect outRect) {
		try {
			outRect.set(0, 0, getRight(), ((View) getParent()).getHeight());
		} catch (final Exception e) {
			Log.e(TAG, "parent not a view or null");
		}

	}
}
