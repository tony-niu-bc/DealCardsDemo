package com.wzhnsc.dealcardsdemo;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

public class Cards extends CardsBase {
    public Cards(Context context) {
        this(context, null);
    }

    public Cards(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Cards(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context).inflate(R.layout.layout_cards, this);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.card);
        ((TextView)findViewById(R.id.tv_num_in_cards)).setText(String.valueOf(ta.getInt(R.styleable.card_number, 0)));
        ta.recycle();
    }
}
