package com.buzzbuddy.android;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

public class CheckableRowItem extends LinearLayout {

	private static final String TAG = CheckableRowItem.class.getSimpleName();

	private EasyTargetCheckBox checkBox;
	private int checkBoxLeft;
	private int checkBoxWidth;

	public CheckableRowItem(final Context context, final AttributeSet attrs) {
		super(context, attrs);

	}

	@Override
	public boolean onTouchEvent(final MotionEvent e) {
		if (checkBox == null) {
			initializeCheckBox();
		}

		if (checkBox.getVisibility() == View.GONE) {
			Log.d(TAG, "checkBox not showing");
			return false;
		}
		if (e.getX() > (checkBoxLeft + checkBoxWidth)) {
			Log.d(TAG, "outside of range");
			return false;
		}
		if (e.getAction() == MotionEvent.ACTION_DOWN) {
			checkBox.setPressed(true);
			return true;
		} else if (e.getAction() == MotionEvent.ACTION_UP) {
			Log.d(TAG, "checkbox is showing");
			checkBox.setPressed(false);
			checkBox.performClick();
			return true;
		}
		return false;
	}

	private void initializeCheckBox() {
		checkBox = (EasyTargetCheckBox) getChildAt(0);
		checkBoxLeft = checkBox.getLeft();
		checkBoxWidth = checkBox.getWidth();
	}
}
