package contagious.apps.memorizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class MainActivity extends Activity {

    public static final int SET_TO_START = 0;
    public static final int SET_TO_SCORE = 1;
    public static final String HIGHSCORE_TAG = "highscore";
    public static final String NONE = "none";

    private boolean inputMode = false;
    private boolean gameRunning = false;
    private int score = 0;
    private int highscore;
    private Random random = new Random();;

    private List<ColorView> colorViewList;
    private List<Integer> realPattern;
    private List<Integer> userPattern;
    private Button startButton;
    private TextView highscoreview;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT <= 10) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        colorViewList = new ArrayList<ColorView>();
        colorViewList.add((ColorView) findViewById(R.id.red));
        colorViewList.add((ColorView) findViewById(R.id.green));
        colorViewList.add((ColorView) findViewById(R.id.yellow));
        colorViewList.add((ColorView) findViewById(R.id.blue));
        realPattern = new ArrayList<Integer>();
        userPattern = new ArrayList<Integer>();
        startButton = (Button) findViewById(R.id.startButton);
        highscoreview = (TextView) findViewById(R.id.highscoreview);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        highscore = getHighscore();
        displayHighscore();
    }

    private int getHighscore() {
        String hs = sharedPreferences.getString(HIGHSCORE_TAG, NONE);
        if (hs.equals(NONE)) {
            setHighscore(0);
            return 0;
        }
        return Integer.parseInt(hs);
    }

    private void setHighscore(int hs) {
        SharedPreferences.Editor editor= sharedPreferences.edit();
        editor.putString(HIGHSCORE_TAG, Integer.toString(hs));
        editor.commit();
    }

    private void displayHighscore() {
        String hs = Integer.toString(highscore);
        highscoreview.setText(getResources().getString(R.string.highscore) + hs);
    }

    private void updateButton (int mode) {
        if (mode == SET_TO_START) {
            startButton.setText(getResources().getString(R.string.start));
        } else if (mode == SET_TO_SCORE) {
            startButton.setText(getResources().getString(R.string.score) + Integer.toString(score));
        }
    }

    private void gameReset() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.gameover))
                .setMessage(getString(R.string.gameovermessage) + Integer.toString(score))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // do nothing
                    }
                }).show();
        realPattern.clear();
        userPattern.clear();
        gameRunning = false;
        inputMode = false;
        // handle score
        if (score > highscore) {
            highscore = score;
            setHighscore(highscore);
            displayHighscore();
        }
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

        for (Integer x: realPattern)
            colorViewList.get(x).blink();

        inputMode = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT > 10) {
            // set UI visibility flags
            getWindow().getDecorView()
                    .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.INVISIBLE);
        }
    }

    public void startButtonClick(View view) {
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
                    colorViewList.get(tag).blink();
                    if (realPattern.size() == userPattern.size()) {
                        // one more point!
                        score++;
                        showPattern();
                    }
                } else {
                    // wrong colorView selected, end of game and the world as we know it
                    gameReset();
                }
            } catch (Exception e) {
                // should never happen
            }
        }
    }

}
