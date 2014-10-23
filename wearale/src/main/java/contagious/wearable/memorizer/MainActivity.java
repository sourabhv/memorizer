package contagious.wearable.memorizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity {

    private static final int SET_TO_START = 0;
    private static final int SET_TO_SCORE = 1;

    private static int BLINK_TIME = 600;

    private boolean inputMode = false;
    private boolean gameRunning = false;
    private int score = 0;
    private Random random = new Random();

    private Button startButton;
    private Handler handler = new Handler();
    private List<Runnable> transitionRunnables;
    private List<ColorView> colorViewList;
    private List<Integer> realPattern;
    private List<Integer> userPattern;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = (Button) findViewById(R.id.startButton);
        transitionRunnables = new ArrayList<Runnable>();
        colorViewList = new ArrayList<ColorView>();
        colorViewList.add((ColorView) findViewById(R.id.red));
        colorViewList.add((ColorView) findViewById(R.id.green));
        colorViewList.add((ColorView) findViewById(R.id.yellow));
        colorViewList.add((ColorView) findViewById(R.id.blue));
        realPattern = new ArrayList<Integer>();
        userPattern = new ArrayList<Integer>();

        // set startButton size
        int len = (int) (getResources().getDisplayMetrics().widthPixels * 0.3);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) startButton.getLayoutParams();
        params.width = len;
        params.height = len;
        startButton.setLayoutParams(params);

        // set OnLongClickListener on startButton
        startButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                gameReset(false);
                return true;
            }
        });
    }

    private void updateButton (int mode) {
        if (mode == SET_TO_START) {
            startButton.setText(getResources().getString(R.string.go));
        } else if (mode == SET_TO_SCORE) {
            startButton.setText(Integer.toString(score));
        }
    }

    // reset function for gameReset and onRestartButtonClick
    private void metaGameReset(int resetTime) {
    }

    private void gameReset(boolean showAlertDialog) {
        // alert dialog
        if (showAlertDialog)
            new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.gameover))
                .setMessage(getResources().getString(R.string.gameovermessage) + Integer.toString(score))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // do nothing
                    }
                })
                .show();

        // cancel all pending transitions
        for (Runnable r : transitionRunnables)
            handler.removeCallbacks(r);

        // reset values
        realPattern.clear();
        userPattern.clear();
        gameRunning = false;
        inputMode = false;
        score = 0;
        updateButton(SET_TO_START);
    }

    private void addNewPatternStep() {
        int next = random.nextInt(colorViewList.size());
        realPattern.add(next);
    }

    private void showPattern() {
        inputMode = false;
        userPattern.clear();
        // add next step in pattern that user memorizes
        addNewPatternStep();
        // update score on center button
        updateButton(SET_TO_SCORE);

        // flash all colorViews one by one
        transitionRunnables.clear();
        int timeDelay = 1500; // initial delay before starting transitions
        for (final Integer x: realPattern) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    colorViewList.get(x).blink(BLINK_TIME);
                }
            };
            handler.postDelayed(r, timeDelay);
            transitionRunnables.add(r);
            timeDelay += BLINK_TIME; // time for next transition
        }
        // activate input after all blink methods complete
        Runnable r = new Runnable() {
            @Override
            public void run() {
                inputMode = true;
            }
        };
        handler.postDelayed(r, timeDelay - BLINK_TIME / 3); // (- BLINK_TIME / 3) for those impatient people
        transitionRunnables.add(r);
    }

    public void onStartButtonClick(View view) {
        if (!gameRunning) {
            gameRunning = true;
            showPattern();
        }
    }

    public void onColorViewClick(View view) {
        if (inputMode && gameRunning) {
            try {
                ColorView cv = (ColorView) view;
                int tag = Integer.parseInt(cv.getTag().toString());

                if (realPattern.get(userPattern.size()) == tag) {
                    // correct colorView selected
                    userPattern.add(tag);
                    colorViewList.get(tag).blink(BLINK_TIME);
                    if (realPattern.size() == userPattern.size()) {
                        // one more point!
                        score++;
                        showPattern();
                    }
                } else {
                    // wrong colorView selected, end of game and the world as we know it
                    gameReset(true);
                }
            } catch (Exception e) {
                // should never happen
            }
        }
    }

}
