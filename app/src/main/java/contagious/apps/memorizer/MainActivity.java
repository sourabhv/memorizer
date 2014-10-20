package contagious.apps.memorizer;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity {

    private static final int TIME_EASY = 600;
    private static final int TIME_HARD = 400;
    private static final int TIME_INSANE = 200;
    private static final int FLASH_TIME = 300;
    private static final int SET_TO_START = 0;
    private static final int SET_TO_SCORE = 1;
    private static final int LONG_TOAST_TIME = 3500;
    private static final String HIGHSCORE_TAG = "highscore";
    private static final String NONE = "none";

    public static int BLINK_TIME = TIME_EASY;

    private boolean inputMode = false;
    private boolean gameRunning = false;
    private int score = 0;
    private int highscore;
    private Random random = new Random();
    private int sounds[] = new int[]{-1, -1, -1, -1};

    private List<ColorView> colorViewList;
    private List<Integer> realPattern;
    private List<Integer> userPattern;
    private Button startButton;
    private TextView highscoreview;
    private SharedPreferences sharedPreferences;
    private SoundPool soundPool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT <= 10) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
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

        // load sounds in sound pool

        try {
            // get assetFileDescriptors
            AssetFileDescriptor beep1 = getAssets().openFd("beep1.ogg");
            AssetFileDescriptor beep2 = getAssets().openFd("beep2.ogg");
            AssetFileDescriptor beep3 = getAssets().openFd("beep3.ogg");
            AssetFileDescriptor beep4 = getAssets().openFd("beep4.ogg");
            // load into soundPool
            sounds[0] = soundPool.load(beep1, 1);
            sounds[1] = soundPool.load(beep2, 1);
            sounds[2] = soundPool.load(beep3, 1);
            sounds[3] = soundPool.load(beep4, 1);
        } catch (IOException e) {
            Log.d("SoundPool", "File opening error");
            e.printStackTrace();
        }

        // set startButton size
        int len = (int) (getResources().getDisplayMetrics().widthPixels * 0.4);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) startButton.getLayoutParams();
        params.width = len;
        params.height = len;
        startButton.setLayoutParams(params);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing())
            soundPool.release();
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
        editor.apply();
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
        // game over toast
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.gameover_toast,
                (ViewGroup) findViewById(R.id.gameover_toast_layout));
        TextView toastHighscore = (TextView) layout.findViewById(R.id.gameover_toast_highscore);
        toastHighscore.setText(Integer.toString(score));
        if (score > highscore) {
            TextView toastTitle = (TextView) layout.findViewById(R.id.gameover_toast_title);
            toastTitle.setText(getResources().getString(R.string.newhighscore));
            toastTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        }

        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();

        // flash the next color in pattern
        final int colorId = realPattern.get(userPattern.size());
        for (int delay = 0; delay < LONG_TOAST_TIME - FLASH_TIME / 2; delay += FLASH_TIME / 2) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    colorViewList.get(colorId).blink(FLASH_TIME / 2);
                }
            }, delay);
        }

        // reset values
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

        // flash all colorViews one by one
        int timeDelay = 1500; // initial delay before starting transitions
        for (final Integer x: realPattern) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    soundPool.play(sounds[x], 1.0f, 1.0f, 0, 0, 1);
                    colorViewList.get(x).blink(BLINK_TIME);
                }
            }, timeDelay);
            timeDelay += BLINK_TIME; // time for next transition
        }
        // activate input after all blink methods complete
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                inputMode = true;
            }
        }, timeDelay - BLINK_TIME / 3); // -200 for those impatient people
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
                    soundPool.play(sounds[tag], 1.0f, 1.0f, 0, 0, 1);
                    colorViewList.get(tag).blink(BLINK_TIME);
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
