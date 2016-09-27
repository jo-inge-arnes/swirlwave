package com.swirlwave.android.peers;

import android.app.Fragment;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.swirlwave.android.R;

public class PeersFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.peer_list, container, false);

        Context context = getActivity();

        Cursor cursor = PeersDb.selectAll(context);
        PeersFragmentAdapter adapter = new PeersFragmentAdapter(context, cursor);

        ListView listView = (ListView)rootView.findViewById(R.id.peersListView);
        listView.setAdapter(adapter);

        return rootView;
    }
}
