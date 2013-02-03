package com.buzzbuddy.android;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class TimerTextView extends TextView {

	public TimerTextView(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
		setTypeface();
	}

	public TimerTextView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		setTypeface();
	}

	public TimerTextView(final Context context) {
		super(context);
		setTypeface();
	}

	private void setTypeface() {
		if (!isInEditMode()) {
			final Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), "ds-digib.ttf");
			setTypeface(typeface);
		}
	}
}
