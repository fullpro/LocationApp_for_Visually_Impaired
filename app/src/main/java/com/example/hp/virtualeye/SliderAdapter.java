package com.example.hp.virtualeye;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SliderAdapter extends PagerAdapter {

    Context context;
    LayoutInflater layoutInflater;

    public SliderAdapter(Context context){
        this.context = context;
    }


    //Array
    public int[] slide_images = {
            //R.drawable.ic_launcher_background,
            //R.drawable....
            //R.drawable....

    };

    public String[] slide_headings = {
            "String1",
            "String2",
            "String3"
    };


    public String[] slide_descs = {
            "String1",
            "String2",
            "String3"
    };

    @Override
    public int getCount() {
        return slide_headings.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
        return view == (RelativeLayout) o;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        layoutInflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.slide_layout,container,false);

        ImageView slideImageView = (ImageView) view.findViewById(R.id.imageView);
        TextView slideTextHeading = (TextView) view.findViewById(R.id.HeadingText);
        TextView slideTextDesc = (TextView) view.findViewById(R.id.Discription);

       // slideImageView.setImageResource(slide_images[position]);
        slideTextHeading.setText(slide_headings[position]);
        slideTextDesc.setText(slide_descs[position]);

        container.addView(view);

        return view;
    }


    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((RelativeLayout) object);
    }
}
