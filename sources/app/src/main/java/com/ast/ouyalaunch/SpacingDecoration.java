
package com.ast.ouyalaunch;

import android.graphics.Rect;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;

public class SpacingDecoration extends RecyclerView.ItemDecoration {
    private int space;
    public SpacingDecoration(int spacePx) { this.space = spacePx; }
    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.left = space/2;
        outRect.right = space/2;
        outRect.top = space/2;
        outRect.bottom = space/2;
    }
}
