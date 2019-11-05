package org.telegram.ui.Cells2;

import android.content.Context;
import android.graphics.Canvas;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public class ManagementTabCell extends FrameLayout {

    private ImageView imageView;
    private TextView textView;
    private boolean needDivider;

    public ManagementTabCell(Context context) {
        super(context);

        imageView = new ImageView(context);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        addView(imageView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.START | Gravity.CENTER_VERTICAL, 9, 0, AndroidUtilities.dp(16), 0));

        textView = new TextView(context);
        textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        textView.setLines(1);
        textView.setMaxLines(1);
        textView.setSingleLine(true);
        textView.setGravity(Gravity.CENTER_VERTICAL);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.START | Gravity.CENTER_VERTICAL, 72, 0, AndroidUtilities.dp(16), 0));
        setClipChildren(false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(72), MeasureSpec.EXACTLY));
    }

    public void setIconAndText(int iconResId, int textResId, boolean divider) {
        imageView.setImageResource(iconResId);
        textView.setText(LocaleController.getInternalString(textResId));
        needDivider = divider;
        setWillNotDraw(!divider);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (needDivider) {
            canvas.drawLine(LocaleController.isRTL ? 0 : AndroidUtilities.dp(72), getMeasuredHeight() - 1, getMeasuredWidth() - (LocaleController.isRTL ? AndroidUtilities.dp(16) : 0), getMeasuredHeight() - 1, Theme.dividerPaint);
        }
    }
}
