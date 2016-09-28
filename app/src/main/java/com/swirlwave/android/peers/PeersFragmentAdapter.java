package com.swirlwave.android.peers;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.swirlwave.android.R;

// Used this article: https://github.com/codepath/android_guides/wiki/Populating-a-ListView-with-a-CursorAdapter
// See the last about changing cursor?

public class PeersFragmentAdapter extends CursorAdapter {
    public PeersFragmentAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.peer_list_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView nameTextView = (TextView) view.findViewById(R.id.name);
        TextView phoneTextView = (TextView) view.findViewById(R.id.phone);

        String name = cursor.getString(cursor.getColumnIndexOrThrow(PeersDb.NAME_COLUMN));
        String phone = cursor.getString(cursor.getColumnIndexOrThrow(PeersDb.PHONE_NUMBER_COLUMN));

        nameTextView.setText(name);
        phoneTextView.setText(phone);
    }
}
