package com.notificationbuzzer.fabuzaid21;

import java.util.List;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.emilsjolander.components.stickylistheaders.StickyListHeadersAdapter;

public class NotiBuzzAdapter extends BaseAdapter implements StickyListHeadersAdapter {

	private final List<ResolveInfo> assignedApps;
	private final List<ResolveInfo> unassignedApps;
	private final LayoutInflater inflater;
	Context context;

	public NotiBuzzAdapter(final Context context, final List<ResolveInfo> assignedApps,
			final List<ResolveInfo> unassignedApps) {
		inflater = LayoutInflater.from(context);
		this.assignedApps = assignedApps;
		this.unassignedApps = unassignedApps;
		this.context = context;
	}

	@Override
	public int getCount() {
		return assignedApps.size() + unassignedApps.size();
	}

	@Override
	public Object getItem(final int position) {
		return getItemFromLists(position);
	}

	private ResolveInfo getItemFromLists(final int position) {
		if (position >= assignedApps.size()) {
			return unassignedApps.get(position - assignedApps.size());
		}
		return assignedApps.get(position);
	}

	@Override
	public long getItemId(final int position) {
		return position;
	}

	private static class ViewHolder {
		CheckBox checkBox;
		ImageView icon;
		TextView appName;
		ImageView playback;
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {

		View view = null;

		if (convertView == null) {
			view = inflater.inflate(R.layout.app_row, null);
			final ViewHolder viewHolder = new ViewHolder();
			viewHolder.checkBox = (CheckBox) view.findViewById(R.id.checkbox);
			viewHolder.icon = (ImageView) view.findViewById(R.id.app_icon);
			viewHolder.appName = (TextView) view.findViewById(R.id.app_name);
			viewHolder.playback = (ImageView) view.findViewById(R.id.playback);
			view.setTag(viewHolder);
		} else {
			view = convertView;
		}

		final ViewHolder holder = (ViewHolder) view.getTag();
		final ResolveInfo item = getItemFromLists(position);
		holder.icon.setImageDrawable(item.loadIcon(context.getPackageManager()));
		holder.appName.setText(item.loadLabel(context.getPackageManager()));

		final CheckBox checkBox = holder.checkBox;
		final ImageView playback = holder.playback;
		if (position >= assignedApps.size()) {
			playback.setVisibility(View.INVISIBLE);
			checkBox.setVisibility(View.GONE);
		} else {
			playback.setVisibility(View.VISIBLE);
			checkBox.setVisibility(View.VISIBLE);
			playback.setTag(position);
			playback.setOnClickListener((NotificationBuzzerActivity) context);
			parent.post(new Runnable() {
				// Post in the parent's message queue to make sure the parent
				// lays out its children before we call getHitRect()
				@Override
				public void run() {
					final Rect r = new Rect();
					playback.getHitRect(r);
					r.top = 0;
					r.bottom = parent.getHeight();
					r.right += parent.getWidth() - playback.getRight();
					parent.setTouchDelegate(new TouchDelegate(r, playback));
				}
			});

			checkBox.setTag(position);
			checkBox.setChecked(false);
			checkBox.setOnCheckedChangeListener((NotificationBuzzerActivity) context);
		}

		return view;
	}

	@Override
	public View getHeaderView(final int position, View convertView, final ViewGroup parent) {
		HeaderViewHolder holder;
		if (convertView == null) {
			holder = new HeaderViewHolder();
			convertView = inflater.inflate(R.layout.header, parent, false);
			holder.text = (TextView) convertView.findViewById(R.id.text);
			convertView.setTag(holder);
		} else {
			holder = (HeaderViewHolder) convertView.getTag();
		}

		// set header text as first char in name
		if (position >= assignedApps.size()) {
			holder.text.setText("Unrecorded Apps");
		} else {
			holder.text.setText("Recorded Apps");
		}

		return convertView;
	}

	// remember that these have to be static, postion=1 should walys return the
	// same Id that is.
	@Override
	public long getHeaderId(final int position) {
		// return the first character of the country as ID because this is what
		// headers are based upon
		if (position >= assignedApps.size()) {
			return 1;
		} else {
			return 0;
		}
	}

	static class HeaderViewHolder {
		TextView text;
	}

}
