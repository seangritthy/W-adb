package com.adb.scrcpy.connect.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.adb.scrcpy.connect.sync.AdbSyncClient;
import java.util.ArrayList;
import java.util.List;

public class FileExplorerAdapter extends BaseAdapter {
    private final Context context;
    private final List<AdbSyncClient.FileItem> items = new ArrayList<>();

    public FileExplorerAdapter(Context context) {
        this.context = context;
    }

    public void setItems(List<AdbSyncClient.FileItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public AdbSyncClient.FileItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            TextView tv = new TextView(context);
            tv.setPadding(32, 24, 32, 24);
            tv.setTextSize(16);
            convertView = tv;
        }

        AdbSyncClient.FileItem item = getItem(position);
        TextView tv = (TextView) convertView;
        if (item.isDirectory) {
            tv.setText("📁 " + item.name);
            tv.setTextColor(0xFF00E676);
        } else {
            String sizeStr = formatSize(item.size);
            tv.setText("📄 " + item.name + " (" + sizeStr + ")");
            tv.setTextColor(0xFFFFFFFF);
        }
        return convertView;
    }

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024.0));
    }
}
