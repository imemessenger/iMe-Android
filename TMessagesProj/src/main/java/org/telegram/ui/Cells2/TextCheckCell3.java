package org.telegram.ui.Cells2;

import android.content.Context;
import android.graphics.Canvas;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.CheckBoxSquare;
import org.telegram.ui.Components.LayoutHelper;

public class TextCheckCell3 extends FrameLayout {

    private TextView textView;
    private ImageView imageView;
    private CheckBoxSquare checkBox;
    private boolean needDivider;

    public TextCheckCell3(Context context) {
        this(context, 21, false);
    }

    public TextCheckCell3(Context context, boolean needIcon) {
        this(context, 21, needIcon);
    }


    public TextCheckCell3(Context context, int padding, boolean needIcon) {
        this(context, padding, false, needIcon);
    }


    public TextCheckCell3(Context context, int padding, boolean dialog, boolean needIcon) {
        super(context);

        imageView = new ImageView(context);
        imageView.setVisibility(View.GONE);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        addView(imageView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.START | Gravity.CENTER_VERTICAL, padding, 0, padding, 0));

        textView = new TextView(context);
        textView.setTextColor(Theme.getColor(dialog ? Theme.key_dialogTextBlack : Theme.key_windowBackgroundWhiteBlackText));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        textView.setLines(1);
        textView.setMaxLines(1);
        textView.setSingleLine(true);
        textView.setGravity(Gravity.CENTER_VERTICAL);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.START | Gravity.CENTER_VERTICAL, needIcon ? 70 : padding, 0, padding, 0));

        checkBox = new CheckBoxSquare(context, false);
        addView(checkBox, LayoutHelper.createFrame(18, 18, (LocaleController.isRTL ? Gravity.START : Gravity.END) | Gravity.CENTER_VERTICAL, padding, 0, padding, 0));

        setClipChildren(false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(48), MeasureSpec.EXACTLY));
    }

    public void setTextAndCheck(String text, boolean checked, boolean divider) {
        textView.setText(text);
        checkBox.setChecked(checked, false);
        needDivider = divider;
        setWillNotDraw(!divider);
    }

    public void setChecked(boolean checked) {
        checkBox.setChecked(checked, true);
    }

    public boolean isChecked() {
        return checkBox.isChecked();
    }

    public void setIcon(int iconResId) {
        if (imageView != null) {
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageResource(iconResId);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (needDivider) {
            canvas.drawLine(LocaleController.isRTL ? 0 : AndroidUtilities.dp(20), getMeasuredHeight() - 1, getMeasuredWidth() - (LocaleController.isRTL ? AndroidUtilities.dp(20) : 0), getMeasuredHeight() - 1, Theme.dividerPaint);
        }
    }
}
