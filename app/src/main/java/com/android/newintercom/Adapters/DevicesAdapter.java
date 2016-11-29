package com.android.newintercom.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.newintercom.Models.Devices;
import com.android.newintercom.R;

import java.util.ArrayList;
import java.util.List;


public class DevicesAdapter extends BaseAdapter {

    private List<Devices> devicesList  = new ArrayList<>();;
    Context mContext;

    public DevicesAdapter(List<Devices> contactsList , Context context) {
        this.devicesList = contactsList;
        this.mContext = context;
    }


    @Override
    public int getCount() {
        return devicesList.size();
    }

    @Override
    public Object getItem(int position) {
        return devicesList.get(position);

    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.device_row_item, parent, false);

        MyViewHolder holder = new MyViewHolder(itemView);
        Devices devices = devicesList.get(position);

        holder.name.setText(devices.getName());
        if(devices.isDnDon()){
            holder.ivDnD.setBackgroundResource(R.drawable.dnd_off);
        }else{
            holder.ivDnD.setBackgroundResource(R.drawable.dnd_on);
        }

        return itemView;
    }


    public class MyViewHolder {
        private TextView name;
        private ImageView ivDnD;

        public MyViewHolder(View view) {
            name = (TextView) view.findViewById(R.id.tvName);
            ivDnD = (ImageView) view.findViewById(R.id.ivDnD);
        }


    }
}
