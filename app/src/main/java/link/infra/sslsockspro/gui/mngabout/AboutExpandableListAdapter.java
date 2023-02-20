package link.infra.sslsockspro.gui.mngabout;

import android.content.Context;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import link.infra.sslsockspro.R;

public class AboutExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private final List<AboutItem> mItems;

    public AboutExpandableListAdapter(Context context, List<AboutItem> mItems) {
        this.context = context;
        this.mItems = mItems;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        String title = ((AboutItem) getGroup(groupPosition)).title;
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context.
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.about_group,null);
        }
        TextView titleTextView = convertView.findViewById(R.id.about_title);
        titleTextView.setText(title);
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        Spanned contents = ((AboutItem) getGroup(groupPosition)).contents;
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context.
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.about_item,null);
        }
        TextView contentsTextView = convertView.findViewById(R.id.about_item);
        contentsTextView.setText(contents);
        return convertView;
    }

    @Override
    // Gets the data associated with the given group.
    public Object getGroup(int groupPosition) {
        return mItems.get(groupPosition);
    }

    @Override
    // Gets the data associated with the given child within the given group.
    public Object getChild(int groupPosition, int childPosition) {
        return mItems.get(groupPosition).contents;
    }

    @Override
    // Gets the number of groups.
    public int getGroupCount() {
        return mItems.size();
    }

    @Override
    // Gets the number of children in a specified group.
    public int getChildrenCount(int groupPosition) {
        return 1;
    }

    @Override
    // Gets the ID for the group at the given position. This group ID must be unique across groups.
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    // Gets the ID for the given child within the given group.
    // This ID must be unique across all children within the group. Hence we can pick the child uniquely
    public long getChildId(int groupPosition, int childPosition) {
        return 0;
    }

    @Override
    // Indicates whether the child and group IDs are stable across changes to the underlying data.
    public boolean hasStableIds() {
        return true;
    }

    @Override
    // Whether the child at the specified position is selectable.
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }
}
