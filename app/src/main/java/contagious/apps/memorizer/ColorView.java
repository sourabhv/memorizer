package contagious.apps.memorizer;

import android.content.Context;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.widget.Button;

public class ColorView extends Button {

    public ColorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void blink(int time) {
        TransitionDrawable transition = (TransitionDrawable) getBackground();
        transition.startTransition(time / 2);
        transition.reverseTransition(time / 2);
    }

}
