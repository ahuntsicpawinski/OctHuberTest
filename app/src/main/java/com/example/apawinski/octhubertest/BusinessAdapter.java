package com.example.apawinski.octhubertest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

public class BusinessAdapter extends ArrayAdapter<HashMap<String, String>> {

    public BusinessAdapter(Context context, int resource) {
        super(context, resource);
    }

    public BusinessAdapter(Context context, int resource, List<HashMap<String, String>> businesss) {
        super(context, resource, businesss);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if(v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.custom_list_business, null);
        }

        HashMap<String, String> business = getItem(position);

        if (business != null) {
            TextView textName = (TextView) v.findViewById(R.id.name);
            TextView textHours = (TextView) v.findViewById(R.id.hours);

            if (textName != null) {
                textName.setText(business.get("name"));
            }
            if (textHours != null) {
                textHours.setText(business.get("hours"));
            }
        }

        return v;
    }
}
