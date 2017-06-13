package com.swirlwave.android.peers;

import android.app.Fragment;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.swirlwave.android.R;
import com.swirlwave.android.sms.SmsSender;
import com.swirlwave.android.toast.Toaster;

import java.util.UUID;

public class PeersFragment extends Fragment {
    private PeersFragmentAdapter mPeersFragmentAdapter;
    private Context mContext;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.peer_list, container, false);

        mContext = getActivity();

        Cursor cursor = PeersDb.selectAll(mContext);
        mPeersFragmentAdapter = new PeersFragmentAdapter(mContext, cursor);

        ListView listView = (ListView)rootView.findViewById(R.id.peersListView);
        listView.setAdapter(mPeersFragmentAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = (Cursor) mPeersFragmentAdapter.getItem(position);
                int friendIdColumnIndex = cursor.getColumnIndex(PeersDb.PEER_ID_COLUMN);

                UUID friendId = UUID.fromString(cursor.getString(friendIdColumnIndex));
                Peer friend = PeersDb.selectByUuid(mContext, friendId);
                PeersDb.updateOnlineStatus(mContext, friend, true);
                Toaster.show(mContext, mContext.getString(R.string.sending_sms_to) + " " + friend.getName());
                new Thread(new SmsSender(mContext, friendId)).start();
            }
        });

        return rootView;
    }

    public void refresh() {
        Cursor cursor = PeersDb.selectAll(getActivity());
        Cursor oldCursor = mPeersFragmentAdapter.swapCursor(cursor);
        if (!oldCursor.isClosed()) oldCursor.close();
    }
}
