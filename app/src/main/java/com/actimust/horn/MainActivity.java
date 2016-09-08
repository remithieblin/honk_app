package com.actimust.horn;

import android.content.ClipData;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringSystem;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

public class MainActivity extends AppCompatActivity {

    private ImageButton mNextLevelButton;
    private InterstitialAd mInterstitialAd;

    private ImageView wheelView;
    private Spring spring;
    private ActimustSpringListener springListener = new ActimustSpringListener();

    private SoundPool soundPool;
//    private int soundID;
    boolean plays = false, loaded = false;
    float actVolume, maxVolume, volume;

    private static final String LEVEL = "level";

    private static final int ITEMS_COUNT = 5;
    private int mLevel;
    private final int[] nextIcons = {R.drawable.next_1,R.drawable.next_1,R.drawable.next_1
            ,R.drawable.next_1,R.drawable.next_1};
    private final int[] drawables = {R.drawable.horn_level_0, R.drawable.horn_level_1,
            R.drawable.horn_level_2,R.drawable.horn_level_3, R.drawable.horn_level_4};
    private final int[] soundResources = {R.raw.horn, R.raw.horn_2, R.raw.horn_light, R.raw.horn, R.raw.horn_epic};
    private int[] soundIds = new int[ITEMS_COUNT];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        if(savedInstanceState != null ){
            Object savedLevel = savedInstanceState.get(LEVEL);
            mLevel = savedLevel != null ? (int)savedLevel : 0;
        }

        try {
            getSupportActionBar().hide();
        } catch (Exception e) {
            Log.i("horn", "Error while hiding action bar");
        }

        // AudioManager audio settings for adjusting the volume
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        actVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        volume = actVolume / maxVolume;

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Load the soundResources
        setupSoundPool();

        wheelView = (ImageView) findViewById(R.id.steering_wheel);

        // Create a system to run the physics loop for a set of springs.
        SpringSystem springSystem = SpringSystem.create();
        spring = springSystem.createSpring();
        spring.addListener(springListener);
        spring.setEndValue(0);

        wheelView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // When pressed start solving the spring to 1.
                        spring.setEndValue(1);

                        if (loaded && !plays) {
                            // the sound will play for ever if we put the loop parameter -1
                            soundIds[mLevel] = soundPool.play(soundIds[mLevel], volume, volume, 1, 0, 1f);
                            plays = true;
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        // When released start solving the spring to 0.
                        spring.setEndValue(0);

                        if (plays) {
                            soundPool.pause(soundIds[mLevel]);
                            soundIds[mLevel] = soundPool.load(MainActivity.this, soundResources[mLevel], 1);
                            plays = false;
                        }
                        break;
                }
                return true;
            }
        });

//         Create the next level button, which tries to show an interstitial when clicked.
        mNextLevelButton = ((ImageButton) findViewById(R.id.next_level_button));
        mNextLevelButton.setEnabled(false);
        mNextLevelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInterstitial();
            }
        });

        wheelView.setImageResource(drawables[mLevel]);
        mNextLevelButton.setImageResource(nextIcons[mLevel]);

        // Create the InterstitialAd and set the adUnitId (defined in values/strings.xml).
        mInterstitialAd = newInterstitialAd();
        loadInterstitial();

//        AdView adView = (AdView) findViewById(R.id.ad);
//        AdRequest adRequest = new AdRequest.Builder().addTestDevice(
//                "yourDeviceId").build();
//        adView.loadAd(adRequest);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private InterstitialAd newInterstitialAd() {
        InterstitialAd interstitialAd = new InterstitialAd(this);
        interstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));
        interstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                mNextLevelButton.setEnabled(true);
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                mNextLevelButton.setEnabled(true);
            }

            @Override
            public void onAdClosed() {
                // Proceed to the next level.
                goToNextLevel();
            }
        });
        return interstitialAd;
    }

    private void showInterstitial() {
        // Show the ad if it's ready. Otherwise toast and reload the ad.
//        if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
//            mInterstitialAd.show();
//        } else {
//            Toast.makeText(this, "Ad did not load", Toast.LENGTH_SHORT).show();
            goToNextLevel();
//        }
    }

    private void loadInterstitial() {
        // Disable the next level button and load the ad.
        mNextLevelButton.setEnabled(false);
        AdRequest adRequest = new AdRequest.Builder().addTestDevice("12EAF24E53A39FBF2B5B9DB71B0D1BD8")
                .setRequestAgent("android_studio:ad_template").build();
        mInterstitialAd.loadAd(adRequest);
    }

    private void goToNextLevel() {
        // Show the next level and reload the ad to prepare for the level after.
        mLevel += 1;
        if(mLevel >= ITEMS_COUNT) mLevel = 0;


//        int id = getResources().getIdentifier("yourpackagename:drawable/" + StringGenerated, null, null);
        wheelView.setImageResource(drawables[mLevel]);
        mNextLevelButton.setImageResource(nextIcons[mLevel]);
        mInterstitialAd = newInterstitialAd();
        loadInterstitial();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Add a listener to the spring when the Activity resumes.
        spring.addListener(springListener);

        setupSoundPool();
    }

    @Override
    public void onPause() {
        super.onPause();


        // Remove the listener to the spring when the Activity pauses.
        spring.removeListener(springListener);
        soundPool.release();
    }

    private void setupSoundPool() {
        soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                loaded = true;
            }
        });
        for(int i = 0; i < ITEMS_COUNT; i++)
            soundIds[i] = soundPool.load(this, soundResources[i], 1);
    }

    private class ActimustSpringListener extends SimpleSpringListener {
        @Override
        public void onSpringUpdate(Spring spring) {
            super.onSpringUpdate(spring);
            // You can observe the updates in the spring
            // state by asking its current value in onSpringUpdate.
            float value = (float) spring.getCurrentValue();
            float scale = 1f - (value * 0.5f);
            wheelView.setScaleX(scale);
            wheelView.setScaleY(scale);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(LEVEL, mLevel);
        super.onSaveInstanceState(savedInstanceState);
    }

}
