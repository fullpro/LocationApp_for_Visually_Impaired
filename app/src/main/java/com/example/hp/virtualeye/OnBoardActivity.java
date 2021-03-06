package com.example.hp.virtualeye;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OnBoardActivity extends AppCompatActivity {

    private ViewPager mViewPager;
    private LinearLayout mLinearLayout;

    private TextView[] mDots;

    SliderAdapter sliderAdapter;

    private int mCurrentPage;

    private Button mPrevBtn;
    private Button mNextBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_on_board);

        mViewPager = findViewById(R.id.SlideViewPager);
        mLinearLayout =  findViewById(R.id.DotsLayout);
        sliderAdapter = new SliderAdapter(this);

        mNextBtn = findViewById(R.id.NextButton);
        mPrevBtn = findViewById(R.id.PrevButton);

        mViewPager.setAdapter(sliderAdapter);



        addDotsIndicator(0);
        mViewPager.addOnPageChangeListener(viewListener);

        mNextBtn.setOnClickListener(v ->    {
            if(mViewPager.getCurrentItem() == 2){
                finishOnBoarding();
            } else{
                mViewPager.setCurrentItem(mCurrentPage +1);
            }
        });

        mPrevBtn.setOnClickListener(v -> mViewPager.setCurrentItem(mCurrentPage -1));



    }


    public void finishOnBoarding() {
        SharedPreferences preferences = getSharedPreferences("my_preferences", MODE_PRIVATE);
        preferences.edit().putBoolean("onBoarding_complete", true).apply();

        Intent main = new Intent(this, MainActivity.class);
        startActivity(main);
        finish();

    }

    public void addDotsIndicator(int position){
        mDots = new TextView[3];
        mLinearLayout.removeAllViews();

        for(int i = 0; i < mDots.length;i++){
            mDots[i] = new  TextView(this);
            mDots[i].setText(Html.fromHtml("&#8226;"));
            mDots[i].setTextSize(35);
            mDots[i].setTextColor(getResources().getColor(R.color.colorTransparent));

            mLinearLayout.addView(mDots[i]);
        }

        if(mDots.length > 0){
            mDots[position].setTextColor(getResources().getColor(R.color.colorWhite));
        }
    }

    ViewPager.OnPageChangeListener viewListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int i, float v, int i1) {

        }

        @Override
        public void onPageSelected(int i) {

            addDotsIndicator(i);
            mCurrentPage = i;

            if(i == 0){
                mNextBtn.setEnabled(true);
                mPrevBtn.setEnabled(false);
                mPrevBtn.setVisibility(View.INVISIBLE);

                mNextBtn.setText("Next");
                mPrevBtn.setText("");
            }else if(i == mDots.length - 1){
                mNextBtn.setEnabled(true);
                mPrevBtn.setEnabled(true);
                mPrevBtn.setVisibility(View.VISIBLE);

                mNextBtn.setText("Finish");
                mPrevBtn.setText("Back");


            }else {
                mNextBtn.setEnabled(true);
                mPrevBtn.setEnabled(true);
                mPrevBtn.setVisibility(View.VISIBLE);

                mNextBtn.setText("Next");
                mPrevBtn.setText("Back");
            }
        }

        @Override
        public void onPageScrollStateChanged(int i) {

        }
    };
}
