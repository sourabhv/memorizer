package contagious.apps.memorizer;

import android.content.Context;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.widget.Button;

public class ColorView extends Button {

    public ColorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void blink() {
        TransitionDrawable transition = (TransitionDrawable) getBackground();
        transition.startTransition(300);
        transition.reverseTransition(500);
        try {
            Thread.sleep(800);
        } catch(InterruptedException ie) {
            // hope that this never happens
        }
    }

}
