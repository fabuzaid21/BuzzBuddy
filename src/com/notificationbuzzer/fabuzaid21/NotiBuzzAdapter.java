package com.notificationbuzzer.fabuzaid21;

import java.util.List;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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
		protected ImageView icon;
		protected TextView appName;
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {

		View view = null;

		if (convertView == null) {
			view = inflater.inflate(R.layout.app_row, null);
			final ViewHolder viewHolder = new ViewHolder();
			viewHolder.icon = (ImageView) view.findViewById(R.id.app_icon);
			viewHolder.appName = (TextView) view.findViewById(R.id.app_name);
			view.setTag(viewHolder);
		} else {
			view = convertView;
		}

		final ViewHolder holder = (ViewHolder) view.getTag();
		final ResolveInfo item = getItemFromLists(position);
		holder.icon.setImageDrawable(item.loadIcon(context.getPackageManager()));
		holder.appName.setText(item.loadLabel(context.getPackageManager()));

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
