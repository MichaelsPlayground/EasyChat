package de.androidcrypto.easychat.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback;
import androidx.recyclerview.widget.RecyclerView;

import static android.graphics.PorterDuff.Mode.CLEAR;
import static androidx.recyclerview.widget.ItemTouchHelper.LEFT;
import static androidx.recyclerview.widget.ItemTouchHelper.RIGHT;

/**
 A simple and lightweight class that extends SimpleCallBack and can be used to attach a swipe listener to RecyclerView items.
 Currently, it only supports left or right swipes with a LinearLayoutManager.

 Example usage is given below:
 RecyclerSwipeHelper mRecyclerSwipeHelper = new RecyclerSwipeHelper(swipeRightColor, swipeLeftColor, swipeRightIconResource, swipeLeftIconResource, context) {
@Override
public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
mAdapter.notifyItemChanged(viewHolder.getBindingAdapterPosition());
//Handle left swipe
if (direction == ItemTouchHelper.LEFT)
//handle right swipe
else
}
};
 ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(mRecyclerSwipeHelper);
 mItemTouchHelper.attachToRecyclerView(mRecyclerView);

 You must call RecyclerView.Adapter.notifyItemChanged() in the onSwiped method to reset the swiped item back to its original state.

 Further, if you wish to prevent an item from being swiped on, then in your adapter's onBindViewHolder() method, use ViewHolder.ItemView.setTag() method and set the tag to @link @TAG_NO_SWIPE;

 Author: RecyclerSwipeHelper was created by Mohammad Yasir

 Source: https://gist.github.com/hafizmdyasir/cfcb971eb6177b442a80d8bc753e7ce5

 Tutorial: https://turbofuture.com/computers/swipe-android-recyclerview
 */

public abstract class RecyclerSwipeHelper extends SimpleCallback {


    public static final String TAG_NO_SWIPE = "don't swipe this item";

    private final int intrinsicWidth;
    private final int intrinsicHeight;
    private final int swipeLeftColor;
    private final int swipeRightColor;

    private final Paint clearPaint;
    private final Drawable swipeRightIcon;
    private final Drawable swipeLeftIcon;
    private final ColorDrawable background = new ColorDrawable();


    public RecyclerSwipeHelper(@ColorInt int swipeRightColor, @ColorInt int swipeLeftColor,
                               @DrawableRes int swipeRightIconResource,
                               @DrawableRes int swipeLeftIconResource, Context context) {
        super(0, LEFT|RIGHT);

        clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(CLEAR));

        this.swipeLeftColor = swipeLeftColor;
        this.swipeRightColor = swipeRightColor;

        this.swipeRightIcon = ContextCompat.getDrawable(context, swipeRightIconResource);
        this.swipeLeftIcon = ContextCompat.getDrawable(context, swipeLeftIconResource);

        if (swipeRightIcon == null || swipeLeftIcon == null)
            throw new Resources.NotFoundException("There was an error trying to load the drawables");

        intrinsicHeight = swipeRightIcon.getIntrinsicHeight();
        intrinsicWidth = swipeRightIcon.getIntrinsicWidth();
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onChildDrawOver(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                int actionState, boolean isCurrentlyActive) {

        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getBottom() - itemView.getTop();
        boolean isCanceled = (dX == 0f) && !isCurrentlyActive;

        if (isCanceled) {
            clearCanvas(c, itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, false);
            return;
        }

        if(dX < 0) {
            background.setColor(swipeLeftColor);
            background.setBounds((int) (itemView.getRight() + dX), itemView.getTop(), itemView.getRight(), itemView.getBottom());
            background.draw(c);

            int itemTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
            int itemMargin = (itemHeight - intrinsicHeight) / 2;
            int itemLeft = itemView.getRight() - itemMargin - intrinsicWidth;
            int itemRight = itemView.getRight() - itemMargin;
            int itemBottom = itemTop + intrinsicHeight;

            int alpha = ((int)((-itemView.getTranslationX()/itemView.getWidth())*510));
            if(alpha > 255) alpha = 255;

            swipeLeftIcon.setAlpha(alpha);
            swipeLeftIcon.setBounds(itemLeft, itemTop, itemRight, itemBottom);
            swipeLeftIcon.draw(c);

        }
        else {
            background.setColor(swipeRightColor);
            background.setBounds((int) (itemView.getLeft() + dX), itemView.getTop(), itemView.getLeft(), itemView.getBottom());
            background.draw(c);

            int itemTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
            int itemMargin = (itemHeight - intrinsicHeight) / 2;
            int itemLeft = itemView.getLeft() + itemMargin;
            int itemRight = itemView.getLeft() + itemMargin + intrinsicWidth;
            int itemBottom = itemTop + intrinsicHeight;

            int alpha = ((int)((itemView.getTranslationX()/itemView.getWidth())*510));
            if(alpha > 255) alpha = 255;

            swipeRightIcon.setAlpha(alpha);
            swipeRightIcon.setBounds(itemLeft, itemTop, itemRight, itemBottom);
            swipeRightIcon.draw(c);
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    @Override
    public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        if (TAG_NO_SWIPE.equals(viewHolder.itemView.getTag())) return 0;
        return super.getSwipeDirs(recyclerView, viewHolder);
    }

    private void clearCanvas(Canvas c, float left, float top, float right, float bottom) {
        if(c != null) c.drawRect(left, top, right, bottom, clearPaint);
    }
}