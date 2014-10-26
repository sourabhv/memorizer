package contagious.apps.memorizer;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Typeface;
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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity {

    private static final int TIME_EASY = 600;
    private static final int TIME_HARD_INSANE = 300;
    private static final int SET_TO_START = 0;
    private static final int SET_TO_SCORE = 1;
    private static final int TOAST_TIME_LONG = 3500;
    private static final String HIGHSCORE_TAG = "highscore";
    private static final String SOUND_TAG = "sound";
    private static final String BLINK_TIME_TAG = "blink_time";
    private static final String INSANE_MODE_TAG = "insane_mode";
    private static final String NONE = "none";

    private static int BLINK_TIME;
    private static int FLASH_TIME = 300;
    private static boolean INSANE_MODE;

    private boolean inputMode = false;
    private boolean gameRunning = false;
    private int score = 0;
    private int highscore;
    private boolean soundStatus;
    private Random random = new Random();
    private int sounds[] = new int[]{-1, -1, -1, -1};

    private Button startButton;
    private Handler handler = new Handler();
    private List<Runnable> transitionRunnables;
    private ImageButton settingsButton;
    private ImageButton restartButton;
    private LinearLayout settingsView;
    private List<ColorView> colorViewList;
    private List<Integer> realPattern;
    private List<Integer> userPattern;
    private SharedPreferences sharedPreferences;
    private SoundPool soundPool;
    private TextView highscoreview;

    // game over toast vars
    TextView toastHighscore;
    TextView toastTitle;
    private Toast toast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT <= 10) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // set audio stream to music/media
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        startButton = (Button) findViewById(R.id.startButton);
        transitionRunnables = new ArrayList<Runnable>();
        settingsButton = (ImageButton) findViewById(R.id.settingsButton);
        restartButton = (ImageButton) findViewById(R.id.restartButton);
        settingsView = (LinearLayout) findViewById(R.id.settingsView);
        colorViewList = new ArrayList<ColorView>();
        colorViewList.add((ColorView) findViewById(R.id.red));
        colorViewList.add((ColorView) findViewById(R.id.green));
        colorViewList.add((ColorView) findViewById(R.id.yellow));
        colorViewList.add((ColorView) findViewById(R.id.blue));
        realPattern = new ArrayList<Integer>();
        userPattern = new ArrayList<Integer>();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        highscoreview = (TextView) findViewById(R.id.highscoreview);

        // set highscore
        highscore = getHighscore();
        displayHighscore();

        // set sound and blink time
        // default values will used without going into sharedPreferences
        // unless the user changes them at least once -- simple neat trick :)
        ToggleButton soundToggleButton = (ToggleButton) findViewById(R.id.soundToggleButton);
        soundStatus = sharedPreferences.getBoolean(SOUND_TAG, true);
        soundToggleButton.setChecked(soundStatus);
        BLINK_TIME = sharedPreferences.getInt(BLINK_TIME_TAG, TIME_EASY);
        INSANE_MODE = sharedPreferences.getBoolean(INSANE_MODE_TAG, false);

        // make the game over toast
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.gameover_toast,
                (ViewGroup) findViewById(R.id.gameover_toast_layout));
        toastHighscore = (TextView) layout.findViewById(R.id.gameover_toast_highscore);
        toastTitle = (TextView) layout.findViewById(R.id.gameover_toast_title);

        toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);

        // set font
        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/OpenSansLight.ttf");
        startButton.setTypeface(font);
        highscoreview.setTypeface(font);
        ((TextView) findViewById(R.id.settingsViewTitle)).setTypeface(font);
        ((TextView) findViewById(R.id.settingsDifficultyTitle)).setTypeface(font);
        ((TextView) findViewById(R.id.settingsButtonEasy)).setTypeface(font);
        ((TextView) findViewById(R.id.settingsButtonHard)).setTypeface(font);
        ((TextView) findViewById(R.id.settingsButtonInsane)).setTypeface(font);
        ((TextView) findViewById(R.id.settingsSoundTitle)).setTypeface(font);
        toastHighscore.setTypeface(font);
        toastTitle.setTypeface(font);
        ((TextView) layout.findViewById(R.id.gameoverToastMessage)).setTypeface(font);

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
        toast.cancel();
        soundPool.autoPause();
        if (isFinishing()) {
            soundPool.release();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        soundPool.autoResume();
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
            startButton.setText(Integer.toString(score));
        }
    }

    // reset function for gameReset and onRestartButtonClick
    private void metaGameReset(int resetTime) {
        // show settingsButton and hide restartButton
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                settingsButton.setVisibility(View.VISIBLE);
                restartButton.setVisibility(View.GONE);
            }
        }, resetTime);

        // cancel all pending transitions
        for (Runnable r : transitionRunnables)
                handler.removeCallbacks(r);

        // reset values
        realPattern.clear();
        userPattern.clear();
        gameRunning = false;
        inputMode = false;
        score = 0;
        startButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        updateButton(SET_TO_START);
    }

    private void gameReset() {
        // game over toast
        toastHighscore.setText(Integer.toString(score));
        if (score > highscore) {
            toastTitle.setText(getResources().getString(R.string.newhighscore));
            toastTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        } else {
            toastTitle.setText(getResources().getString(R.string.gameover));
            toastTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 35);
        }

        toast.show();

        // flash the next color in pattern
        final int colorId = realPattern.get(userPattern.size());
        for (int delay = 0; delay < TOAST_TIME_LONG - FLASH_TIME / 2; delay += FLASH_TIME / 2) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    colorViewList.get(colorId).blink(FLASH_TIME / 2);
                }
            }, delay);
        }

        // handle score
        if (score > highscore) {
            highscore = score;
            setHighscore(highscore);
            displayHighscore();
        }

        metaGameReset(TOAST_TIME_LONG);
    }

    private void swapColorViews() {
        // complete is unholy method
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

        // swap colors for insane mode
        if (score > 0 && score % 5 == 0)
            swapColorViews();

        // flash all colorViews one by one
        transitionRunnables.clear();
        int timeDelay = 1500; // initial delay before starting transitions
        for (final Integer x: realPattern) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    if (soundStatus)
                        soundPool.play(sounds[x], 1.0f, 1.0f, 0, 0, 1);
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
            startButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 35);
            gameRunning = true;
            showPattern();
            // hide the settingsButton
            settingsButton.setVisibility(View.GONE);
            // show the restart button
            restartButton.setVisibility(View.VISIBLE);
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
                    if (soundStatus)
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

    public void onRestartButtonClick(View view) {
        metaGameReset(0);
    }

    public void onSettingsButtonClick(View view) {
        settingsView.setVisibility(View.VISIBLE);
    }

    public void onSettingsPaneCrossClick(View view) {
        settingsView.setVisibility(View.GONE);
    }

    public void onDifficultyButtonClick(View view) {
        String tag = view.getTag().toString();
        if (tag.equals("1")) { // easy
            BLINK_TIME = TIME_EASY;
            INSANE_MODE = false;
            Toast.makeText(this, "Difficulty: Boring!", Toast.LENGTH_SHORT).show();
        } else if (tag.equals("2")) { // hard
            BLINK_TIME = TIME_HARD_INSANE;
            INSANE_MODE = false;
            Toast.makeText(this, "Difficulty: Bring It On!", Toast.LENGTH_SHORT).show();
        } else if (tag.equals("3")) {
            BLINK_TIME = TIME_HARD_INSANE;
            INSANE_MODE = true;
            Toast.makeText(this, "Difficulty: OH GOD! WHY?!", Toast.LENGTH_SHORT).show();
        }
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(BLINK_TIME_TAG, BLINK_TIME);
        editor.putBoolean(INSANE_MODE_TAG, INSANE_MODE);
        editor.apply();
    }

    @Override
    public void onBackPressed() {
        if (settingsView.getVisibility() == View.GONE)
            super.onBackPressed();
        else
            settingsView.setVisibility(View.GONE);
    }

    public void onSoundToggleButtonClicked(View view) {
        soundStatus = ((ToggleButton) view).isChecked();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(SOUND_TAG, soundStatus);
        editor.apply();
    }

}
