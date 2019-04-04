package com.example.hp.virtualeye;

import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class ExampleAdapter extends RecyclerView.Adapter<ExampleAdapter.ExampleViewHolder> {

    private ArrayList<BTLE_Device> devices;
    private onItemClickListener listener;

    public interface onItemClickListener{
        void onItemClick(int position);
    }


    public void setOnItemClickListener(onItemClickListener listener)
    {
        this.listener=listener;
    }
    public static class ExampleViewHolder extends RecyclerView.ViewHolder {
        public TextView tv_name;
        public TextView tv_macaddr;
       // public TextView tv_rssi;



        public ExampleViewHolder(@NonNull View itemView, final onItemClickListener listener) {
            super(itemView);
            tv_name=itemView.findViewById(R.id.tv_name);
            tv_macaddr=itemView.findViewById(R.id.tv_macaddr);
         //   tv_rssi=itemView.findViewById(R.id.tv_rssi);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener!=null)
                    {
                        int position=getAdapterPosition();
                        if (position!=RecyclerView.NO_POSITION)
                        {
                            listener.onItemClick(position);
                        }
                    }
                }
            });
        }
    }


    public ExampleAdapter(ArrayList<BTLE_Device> mDevice){
        devices = mDevice;
    }

    @NonNull
    @Override
    public ExampleViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v=LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item,viewGroup,false);
        ExampleViewHolder evh=new ExampleViewHolder(v,listener);
        return evh;
    }


    @Override
    public void onBindViewHolder(@NonNull ExampleViewHolder exampleViewHolder, int i) {
       BTLE_Device current_example=devices.get(i);


        exampleViewHolder.tv_name.setText(current_example.getName());
        exampleViewHolder.tv_macaddr.setText(current_example.getAddress());
        //exampleViewHolder.tv_rssi.setText(current_example.getRSSI());


    }


    @Override
    public int getItemCount() {
        return devices.size();
    }
}

