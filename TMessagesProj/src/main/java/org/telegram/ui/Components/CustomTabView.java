package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.PorterDuff;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;

public class CustomTabView extends FrameLayout {

    private int mainColor = Theme.getColor(Theme.key_actionBarDefault);

    private ImageView icon;
    private ImageView indicator;

    public CustomTabView(@NonNull Context context) {
        super(context);

        icon = new ImageView(context);
        addView(icon, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 8, 4, 8, 4));
        indicator = new ImageView(context);
        indicator.setImageResource(R.drawable.ic_new_indicator);
        indicator.setVisibility(View.INVISIBLE);
        indicator.setColorFilter(mainColor, PorterDuff.Mode.SRC_IN);
        addView(indicator, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER | Gravity.TOP, 12, 0, 0, 0));
    }

    public void setIcon(int iconResId) {
        icon.setImageResource(iconResId);
    }

    public void highlightIcon(boolean highlight) {
        if (highlight) {
            icon.setColorFilter(mainColor, PorterDuff.Mode.SRC_IN);
        } else {
            icon.clearColorFilter();
        }
    }

    public void showIndicator(boolean show) {
        int visibility = show ? View.VISIBLE : View.INVISIBLE;
        indicator.setVisibility(visibility);
    }
}
