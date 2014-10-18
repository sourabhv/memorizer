package contagious.apps.memorizer;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.widget.Button;

public class ColorView extends Button {

    public ColorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void blink() {
        String tag = getTag().toString();
        String str = "ColorView: " + tag;
        MainActivity.tv.setText(str);

        AnimationDrawable animation = (AnimationDrawable) this.getBackground();
        animation.start();
    }

}
