package com.harsha.callrecorder.activity;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.ads.consent.ConsentInformation;
import com.google.ads.consent.ConsentStatus;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.harsha.callrecorder.R;
import com.harsha.callrecorder.envr.AppEnvr;
import com.harsha.callrecorder.object.IncomingCallObject;
import com.harsha.callrecorder.object.OutgoingCallObject;
import com.harsha.callrecorder.util.LogUtil;
import com.harsha.callrecorder.util.ResourceUtil;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import io.realm.Sort;

public class CallActivity extends AppCompatActivity {
    private static final String TAG = CallActivity.class.getSimpleName();

    private boolean mIsIncoming = false;
    private boolean mIsOutgoing = false;

    private Realm mRealm = null;

    private IncomingCallObject mIncomingCallObject = null;
    private OutgoingCallObject mOutgoingCallObject = null;

    private MediaPlayer mMediaPlayer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.d(TAG, "Activity create");

        setContentView(R.layout.activity_call);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();

        long beginTimestamp = 0L, endTimestamp = 0L;

        if (intent != null) {
            if (intent.hasExtra(AppEnvr.INTENT_ACTION_INCOMING_CALL) && intent.getBooleanExtra(AppEnvr.INTENT_ACTION_INCOMING_CALL, false)) {
                mIsIncoming = true;
            }

            if (intent.hasExtra(AppEnvr.INTENT_ACTION_OUTGOING_CALL) && intent.getBooleanExtra(AppEnvr.INTENT_ACTION_OUTGOING_CALL, false)) {
                mIsOutgoing = true;
            }

            // ----

            if (mIsIncoming || mIsOutgoing) {
                if (intent.hasExtra("mBeginTimestamp")) {
                    beginTimestamp = intent.getLongExtra("mBeginTimestamp", 0L);
                }
                if (intent.hasExtra("mEndTimestamp")) {
                    endTimestamp = intent.getLongExtra("mEndTimestamp", 0L);
                }
            }
        }

        if (beginTimestamp == 0L || endTimestamp == 0L) {
            getMissingDataDialog().show();

            return;
        }

        // ----

        // Realm
        try {
            mRealm = Realm.getDefaultInstance();
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }

        if (mRealm != null && !mRealm.isClosed()) {
            if (mIsIncoming) {
                mIncomingCallObject = mRealm.where(IncomingCallObject.class)
                        .equalTo("mBeginTimestamp", beginTimestamp)
                        .equalTo("mEndTimestamp", endTimestamp)
                        .sort("mBeginTimestamp", Sort.DESCENDING)
                        .findFirst();
            } else if (mIsOutgoing) {
                mOutgoingCallObject = mRealm.where(OutgoingCallObject.class)
                        .equalTo("mBeginTimestamp", beginTimestamp)
                        .equalTo("mEndTimestamp", endTimestamp)
                        .sort("mBeginTimestamp", Sort.DESCENDING)
                        .findFirst();
            }
        }
        // - Realm

        mIsIncoming = mIsIncoming && mIncomingCallObject != null;
        mIsOutgoing = mIsOutgoing && mOutgoingCallObject != null;

        if (!mIsIncoming && !mIsOutgoing) {
            getMissingDataDialog().show();

            return;
        }

        // ----

        if (intent.hasExtra("mCorrespondentName")) {
            String correspondentName = intent.getStringExtra("mCorrespondentName");

            if (actionBar != null) {
                actionBar.setTitle(correspondentName);
            }

            if (mIsIncoming) {
                mIncomingCallObject.setCorrespondentName(correspondentName);
            }

            if (mIsOutgoing ) {
                mOutgoingCallObject.setCorrespondentName(correspondentName);
            }
        }

        if (mIsIncoming) {
            String phoneNumber = mIncomingCallObject.getPhoneNumber();

            if (mIncomingCallObject.getCorrespondentName() == null) {
                if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                    if (actionBar != null) {
                        actionBar.setTitle(phoneNumber);
                    }
                } else {
                    if (actionBar != null) {
                        actionBar.setTitle(getString(R.string.unknown_number));
                    }
                }
            }
        } else if (mIsOutgoing) {
            String phoneNumber = mOutgoingCallObject.getPhoneNumber();

            if (mOutgoingCallObject.getCorrespondentName() == null) {
                if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                    if (actionBar != null) {
                        actionBar.setTitle(phoneNumber);
                    }
                } else {
                    if (actionBar != null) {
                        actionBar.setTitle(getString(R.string.unknown_number));
                    }
                }
            }
        }

        // ----

        TextView typeTextView = findViewById(R.id.content_call_type_text_view);

        ImageView typeImageView = findViewById(R.id.content_call_type_image_view);

        String beginTimeDate = null, endTimeDate = null;

        if (mIsIncoming) {
            String phoneNumber = mIncomingCallObject.getPhoneNumber();

            Bitmap imageBitmap = null;

            if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                ((TextView) findViewById(R.id.content_call_number_text_view)).setText(getString(R.string.number_param, phoneNumber));

                // ----

                try {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));

                        Cursor cursor = getContentResolver().query(uri, new String[]{ContactsContract.PhoneLookup._ID}, null, null, null);

                        if (cursor != null) {
                            if (cursor.moveToFirst()) {
                                String id = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup._ID));

                                if (id != null && !id.trim().isEmpty()) {
                                    InputStream inputStream = null;
                                    try {
                                        inputStream = ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(), ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.valueOf(id)));
                                    } catch (Exception e) {
                                        LogUtil.e(TAG, e.getMessage());
                                        LogUtil.e(TAG, e.toString());

                                        e.printStackTrace();
                                    }

                                    if (inputStream != null) {
                                        Bitmap bitmap = null;
                                        try {
                                            bitmap = BitmapFactory.decodeStream(inputStream);
                                        } catch (Exception e) {
                                            LogUtil.e(TAG, e.getMessage());
                                            LogUtil.e(TAG, e.toString());

                                            e.printStackTrace();
                                        }

                                        if (bitmap != null) {
                                            imageBitmap = ResourceUtil.getBitmapClippedCircle(bitmap);
                                        }
                                    }
                                }
                            }

                            cursor.close();
                        }
                    }
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }
            } else {
                ((TextView) findViewById(R.id.content_call_number_text_view)).setText(getString(R.string.unknown_number));
            }

            // ----

            typeTextView.setText(getString(R.string.incoming_call_record));

            if (imageBitmap != null) {
                typeImageView.setImageBitmap(imageBitmap);
            } else {
                typeImageView.setImageDrawable(ResourceUtil.getDrawable(this, R.drawable.shape_incoming));
            }

            // ----

            if (!DateFormat.is24HourFormat(this)) {
                try {
                    beginTimeDate = new SimpleDateFormat("hh:mm:ss a - E dd/MM/yy", Locale.getDefault()).format(new Date(mIncomingCallObject.getBeginTimestamp()));
                    endTimeDate = new SimpleDateFormat("hh:mm:ss a - E dd/MM/yy", Locale.getDefault()).format(new Date(mIncomingCallObject.getEndTimestamp()));
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }
            } else {
                try {
                    beginTimeDate = new SimpleDateFormat("HH:mm:ss - E dd/MM/yy", Locale.getDefault()).format(new Date(mIncomingCallObject.getBeginTimestamp()));
                    endTimeDate = new SimpleDateFormat("HH:mm:ss - E dd/MM/yy", Locale.getDefault()).format(new Date(mIncomingCallObject.getEndTimestamp()));
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }
            }

            // ----

            String durationString = null;

            Date beginDate = new Date(mIncomingCallObject.getBeginTimestamp());
            Date endDate = new Date(mIncomingCallObject.getEndTimestamp());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    Duration duration = Duration.between(beginDate.toInstant(), endDate.toInstant());

                    long minutes = TimeUnit.SECONDS.toMinutes(duration.getSeconds());

                    durationString = String.format(Locale.getDefault(), "%d min, %d sec",
                            minutes,
                            duration.getSeconds() - TimeUnit.MINUTES.toSeconds(minutes));
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }
            } else {
                long durationMs = endDate.getTime() - beginDate.getTime();

                try {
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs);

                    durationString = String.format(Locale.getDefault(), "%d min, %d sec",
                            minutes,
                            TimeUnit.MILLISECONDS.toSeconds(durationMs) - TimeUnit.MINUTES.toSeconds(minutes));
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }
            }

            durationString = getString(R.string.duration_param, durationString != null && !durationString.isEmpty() ? durationString : "N/A");

            ((TextView) findViewById(R.id.content_call_duration_text_view)).setText(durationString);
        } else if (mIsOutgoing) {
            String phoneNumber = mOutgoingCallObject.getPhoneNumber();

            Bitmap imageBitmap = null;

            if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                ((TextView) findViewById(R.id.content_call_number_text_view)).setText(getString(R.string.number_param, phoneNumber));

                // ----

                try {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));

                        Cursor cursor = getContentResolver().query(uri, new String[]{ContactsContract.PhoneLookup._ID}, null, null, null);

                        if (cursor != null) {
                            if (cursor.moveToFirst()) {
                                String id = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup._ID));

                                if (id != null && !id.trim().isEmpty()) {
                                    InputStream inputStream = null;
                                    try {
                                        inputStream = ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(), ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.valueOf(id)));
                                    } catch (Exception e) {
                                        LogUtil.e(TAG, e.getMessage());
                                        LogUtil.e(TAG, e.toString());

                                        e.printStackTrace();
                                    }

                                    if (inputStream != null) {
                                        Bitmap bitmap = null;
                                        try {
                                            bitmap = BitmapFactory.decodeStream(inputStream);
                                        } catch (Exception e) {
                                            LogUtil.e(TAG, e.getMessage());
                                            LogUtil.e(TAG, e.toString());

                                            e.printStackTrace();
                                        }

                                        if (bitmap != null) {
                                            imageBitmap = ResourceUtil.getBitmapClippedCircle(bitmap);
                                        }
                                    }
                                }
                            }

                            cursor.close();
                        }
                    }
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }
            } else {
                ((TextView) findViewById(R.id.content_call_number_text_view)).setText(getString(R.string.unknown_number));
            }

            // ----

            typeTextView.setText(getString(R.string.outgoing_call_record));

            if (imageBitmap != null) {
                typeImageView.setImageBitmap(imageBitmap);
            } else {
                typeImageView.setImageDrawable(ResourceUtil.getDrawable(this, R.drawable.shape_outgoing));
            }

            // ----

            if (!DateFormat.is24HourFormat(this)) {
                try {
                    beginTimeDate = new SimpleDateFormat("hh:mm:ss a - E dd/MM/yy", Locale.getDefault()).format(new Date(mOutgoingCallObject.getBeginTimestamp()));
                    endTimeDate = new SimpleDateFormat("hh:mm:ss a - E dd/MM/yy", Locale.getDefault()).format(new Date(mOutgoingCallObject.getEndTimestamp()));
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }
            } else {
                try {
                    beginTimeDate = new SimpleDateFormat("HH:mm:ss - E dd/MM/yy", Locale.getDefault()).format(new Date(mOutgoingCallObject.getBeginTimestamp()));
                    endTimeDate = new SimpleDateFormat("HH:mm:ss - E dd/MM/yy", Locale.getDefault()).format(new Date(mOutgoingCallObject.getEndTimestamp()));
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }
            }

            // ----

            String durationString = null;

            Date beginDate = new Date(mOutgoingCallObject.getBeginTimestamp());
            Date endDate = new Date(mOutgoingCallObject.getEndTimestamp());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    Duration duration = Duration.between(beginDate.toInstant(), endDate.toInstant());

                    long minutes = TimeUnit.SECONDS.toMinutes(duration.getSeconds());

                    durationString = String.format(Locale.getDefault(), "%d min, %d sec",
                            minutes,
                            duration.getSeconds() - TimeUnit.MINUTES.toSeconds(minutes));
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }
            } else {
                long durationMs = endDate.getTime() - beginDate.getTime();

                try {
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs);

                    durationString = String.format(Locale.getDefault(), "%d min, %d sec",
                            minutes,
                            TimeUnit.MILLISECONDS.toSeconds(durationMs) - TimeUnit.MINUTES.toSeconds(minutes));
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }
            }

            durationString = getString(R.string.duration_param, durationString != null && !durationString.isEmpty() ? durationString : "N/A");

            ((TextView) findViewById(R.id.content_call_duration_text_view)).setText(durationString);
        }

        TextView beginTimeDateTextView = findViewById(R.id.content_call_begin_time_date_text_view);
        beginTimeDateTextView.setText(getString(R.string.begin_param, beginTimeDate != null && !beginTimeDate.trim().isEmpty() ? beginTimeDate : "N/A"));

        TextView endTimeDateTextView = findViewById(R.id.content_call_end_time_date_text_view);
        endTimeDateTextView.setText(getString(R.string.end_param, endTimeDate != null && !endTimeDate.trim().isEmpty() ? endTimeDate : "N/A"));

        // ----

        // Consent information
        ConsentInformation consentInformation = ConsentInformation.getInstance(this);
        // - Consent information

        // Google AdMob
        AdRequest.Builder adRequestBuilder = new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
        adRequestBuilder.addTestDevice("8632890FFB195CCA67B145C4A69A06CF"); // TODO: Add your device ID (Android ID) from logcat output

        if (consentInformation != null && consentInformation.isRequestLocationInEeaOrUnknown()) {
            if (consentInformation.getConsentStatus() == ConsentStatus.NON_PERSONALIZED) {
                Bundle extras = new Bundle();
                extras.putString("npa", "1");

                adRequestBuilder.addNetworkExtrasBundle(AdMobAdapter.class, extras);
            }
        }
        // - Google AdMob

        // ----

        float mainMargin = getResources().getDimension(R.dimen.activity_margin);

        // ----

        File file = null;
        try {
            if (mIsIncoming) {
                file = new File(mIncomingCallObject.getOutputFile());
            } else if (mIsOutgoing) {
                file = new File(mOutgoingCallObject.getOutputFile());
            }
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }

        String path = file != null ? file.getPath() : null;

        boolean exists = false, isFile = false;

        if (file != null) {
            exists = file.exists();
            isFile = file.isFile();
        }

        // ----

        // Call player
        if (path != null && !path.trim().isEmpty()) {
            if (exists && isFile) {
                SeekBar playSeekBar = findViewById(R.id.content_call_play_seek_bar);
                playSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        if (b) {
                            if (mMediaPlayer != null) {
                                mMediaPlayer.seekTo(i);
                            }

                            playSeekBar.setProgress(i);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });

                TextView playTimeElapsedTextView = findViewById(R.id.content_call_play_time_elapsed);
                TextView playTimeRemainingTextView = findViewById(R.id.content_call_play_time_remaining);

                Drawable playDrawable = ResourceUtil.getDrawable(this, R.drawable.ic_outline_play_circle_outline_24px);
                Drawable pauseDrawable = ResourceUtil.getDrawable(this, R.drawable.ic_outline_pause_circle_outline_24px);

                ImageButton playImageButton = findViewById(R.id.content_call_play_image_button);
                playImageButton.setOnClickListener(view -> {
                    if (mMediaPlayer != null) {
                        if (mMediaPlayer.isPlaying()) {
                            mMediaPlayer.pause(); // Pause

                            playImageButton.setImageDrawable(playDrawable); // Play
                        } else {
                            mMediaPlayer.start(); // Start

                            playImageButton.setImageDrawable(pauseDrawable); // Pause
                        }
                    } else {
                        playImageButton.setImageDrawable(playDrawable);
                    }
                });

                SeekBar volumeSeekBar = findViewById(R.id.content_call_play_volume_seek_bar);
                volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        if (b) {
                            if (mMediaPlayer != null) {
                                mMediaPlayer.setVolume(i / 100f, i / 100f);
                            }

                            volumeSeekBar.setProgress(i);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });

                try {
                    mMediaPlayer = MediaPlayer.create(this, Uri.parse(path));
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }

                if (mMediaPlayer != null) {
                    mMediaPlayer.setOnCompletionListener(mediaPlayer -> {
                        if (mediaPlayer != null) {
                            if (mediaPlayer.isPlaying()) {
                                mediaPlayer.pause(); // Pause

                                playImageButton.setImageDrawable(playDrawable); // Play
                            } else {
                                mediaPlayer.start(); // Start

                                playImageButton.setImageDrawable(pauseDrawable); // Pause
                            }
                        } else {
                            playImageButton.setImageDrawable(playDrawable);
                        }
                    });

                    mMediaPlayer.setOnInfoListener((mp, what, extra) -> false); // On info
                    mMediaPlayer.setOnErrorListener((mp, what, extra) -> false); // On error

                    mMediaPlayer.seekTo(0);
                    mMediaPlayer.setVolume(0.5f, 0.5f);

                    // ----

                    playSeekBar.setMax(mMediaPlayer.getDuration());

                    // ----

                    Handler handler = new Handler();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mMediaPlayer != null){
                                int currentPosition = mMediaPlayer.getCurrentPosition();

                                playSeekBar.setProgress(currentPosition);

                                // Elapsed time
                                String elapsedTime;
                                int minElapsed = currentPosition / 1000 / 60;
                                int secElapsed = currentPosition / 1000 % 60;
                                elapsedTime = minElapsed + ":";
                                if (secElapsed < 10) {
                                    elapsedTime += "0";
                                }
                                elapsedTime += secElapsed;

                                playTimeElapsedTextView.setText(elapsedTime);
                                // - Elapsed time

                                // Remaining time
                                String remainingTime;
                                int minRemaining = (playSeekBar.getMax() - currentPosition) / 1000 / 60;
                                int secRemaining = (playSeekBar.getMax() - currentPosition) / 1000 % 60;
                                remainingTime = minRemaining + ":";
                                if (secRemaining < 10) {
                                    remainingTime += "0";
                                }
                                remainingTime += secRemaining;

                                playTimeRemainingTextView.setText(remainingTime);
                                // - Remaining time
                            }

                            handler.postDelayed(this, 1000);
                        }
                    });
                }
            }
        }
        // - Call player

        // ----

        // Google AdMob
        ProgressBar advertisementBox1ProgressBar = findViewById(R.id.content_call_advertisement_box_1_progress_bar);
        TextView advertisementBox1NotAvailableTextView = findViewById(R.id.content_call_advertisement_1_box_not_available_text_view);
        AdView advertisementBox1AdView = findViewById(R.id.content_call_advertisement_box_1_ad_view);

        new Runnable() {
            @Override
            public void run() {
                AdRequest banner1AdRequest = null;
                try {
                    banner1AdRequest = adRequestBuilder.build();
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }

                if (banner1AdRequest != null) {
                    advertisementBox1AdView.setAdListener(new AdListener() {
                        @Override
                        public void onAdFailedToLoad(int errorCode) {
                            switch (errorCode) {
                                case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                                    break;
                                case AdRequest.ERROR_CODE_INVALID_REQUEST:
                                    break;
                                case AdRequest.ERROR_CODE_NETWORK_ERROR:
                                    break;
                                case AdRequest.ERROR_CODE_NO_FILL:
                                    break;
                            }

                            // ----

                            if (advertisementBox1ProgressBar.getVisibility() != View.GONE) {
                                advertisementBox1ProgressBar.setVisibility(View.GONE);
                            }

                            if (advertisementBox1AdView.getVisibility() != View.GONE) {
                                advertisementBox1AdView.setVisibility(View.GONE);
                            }

                            if (advertisementBox1NotAvailableTextView.getVisibility() != View.VISIBLE) {
                                advertisementBox1NotAvailableTextView.setVisibility(View.VISIBLE);
                            }

                            // ----

                            super.onAdFailedToLoad(errorCode);
                        }

                        @Override
                        public void onAdLoaded() {
                            if (advertisementBox1ProgressBar.getVisibility() != View.GONE) {
                                advertisementBox1ProgressBar.setVisibility(View.GONE);
                            }

                            if (advertisementBox1NotAvailableTextView.getVisibility() != View.GONE) {
                                advertisementBox1NotAvailableTextView.setVisibility(View.GONE);
                            }

                            if (advertisementBox1AdView.getVisibility() != View.VISIBLE) {
                                advertisementBox1AdView.setVisibility(View.VISIBLE);
                            }

                            // ----

                            super.onAdLoaded();
                        }
                    });

                    advertisementBox1AdView.loadAd(banner1AdRequest);
                } else {
                    if (advertisementBox1ProgressBar.getVisibility() != View.GONE) {
                        advertisementBox1ProgressBar.setVisibility(View.GONE);
                    }

                    if (advertisementBox1NotAvailableTextView.getVisibility() != View.VISIBLE) {
                        advertisementBox1NotAvailableTextView.setVisibility(View.VISIBLE);
                    }
                }
            }
        }.run();
        // - Google AdMob

        // ----

        // File info
        if (path != null && !path.trim().isEmpty()) {
            ((TextView) findViewById(R.id.content_call_path)).setText(getString(R.string.path_param, path));
        } else {
            ((TextView) findViewById(R.id.content_call_path)).setText(getString(R.string.path_param, getString(R.string.unknown_path)));
        }

        // ----

        int audioSource = -1, outputFormat = -1, audioEncoder = -1;

        if (mIsIncoming) {
            audioSource = mIncomingCallObject.getAudioSource();
            outputFormat = mIncomingCallObject.getOutputFormat();
            audioEncoder = mIncomingCallObject.getAudioEncoder();
        } else if (mIsOutgoing) {
            audioSource = mOutgoingCallObject.getAudioSource();
            outputFormat = mOutgoingCallObject.getOutputFormat();
            audioEncoder = mOutgoingCallObject.getAudioEncoder();
        }

        String audioSourceString; // Audio source
        switch (audioSource) {
            case MediaRecorder.AudioSource.VOICE_CALL:
                audioSourceString = "VOICE_CALL";
                break;
            case MediaRecorder.AudioSource.VOICE_COMMUNICATION:
                audioSourceString = "VOICE_COMMUNICATION";
                break;
            case MediaRecorder.AudioSource.VOICE_RECOGNITION:
                audioSourceString = "VOICE_RECOGNITION";
                break;
        /*case MediaRecorder.AudioSource.VOICE_UPLINK:
            audioSource = "VOIDE_UPLINK";
            break;*/
        /*case MediaRecorder.AudioSource.VOICE_DOWNLINK:
            audioSource = "VOICE_DOWNLINK";
            break;*/
            case MediaRecorder.AudioSource.MIC:
                audioSourceString = "MIC";
                break;
            case MediaRecorder.AudioSource.DEFAULT:
                audioSourceString = "DEFAULT";
                break;

            default:
                audioSourceString = "N/A";
                break;
        }

        String outputFormatString; // Output format
        switch (outputFormat) {
            case MediaRecorder.OutputFormat.MPEG_4:
                outputFormatString = "MPEG_4";
                break;
            case MediaRecorder.OutputFormat.THREE_GPP:
                outputFormatString = "THREE_GPP";
                break;
            case MediaRecorder.OutputFormat.AAC_ADTS:
                outputFormatString = "AAC_ADTS";
                break;
            case MediaRecorder.OutputFormat.AMR_NB:
                outputFormatString = "AMR_NB";
                break;
            case MediaRecorder.OutputFormat.AMR_WB:
                outputFormatString = "AMR_WB";
                break;
            case MediaRecorder.OutputFormat.WEBM:
                outputFormatString = "WEBM";
                break;
            case MediaRecorder.OutputFormat.OGG:
                outputFormatString = "OGG";
                break;
            case MediaRecorder.OutputFormat.DEFAULT:
                outputFormatString = "DEFAULT";
                break;

            default:
                outputFormatString = "N/A";
                break;
        }

        String audioEncoderString; // Audio encoder
        switch (audioEncoder) {
            case MediaRecorder.AudioEncoder.AAC:
                audioEncoderString = "AAC";
                break;
            case MediaRecorder.AudioEncoder.HE_AAC:
                audioEncoderString = "HE_AAC";
                break;
            case MediaRecorder.AudioEncoder.AAC_ELD:
                audioEncoderString = "AAC_ELD";
                break;
            case MediaRecorder.AudioEncoder.AMR_NB:
                audioEncoderString = "AMR_NB";
                break;
            case MediaRecorder.AudioEncoder.AMR_WB:
                audioEncoderString = "AMR_WB";
                break;
            case MediaRecorder.AudioEncoder.VORBIS:
                audioEncoderString = "VORBIS";
                break;
            case MediaRecorder.AudioEncoder.OPUS:
                audioEncoderString = "OPUS";
                break;
            case MediaRecorder.AudioEncoder.DEFAULT:
                audioEncoderString = "DEFAULT";
                break;

            default:
                audioEncoderString = "N/A";
                break;
        }

        TableLayout fileInfoTableLayout = findViewById(R.id.content_call_file_info_table_layout);
        fileInfoTableLayout.setStretchAllColumns(true);

        LinkedHashMap<String, String> fileInfoLinkedHashMap = new LinkedHashMap<>(); // Using "LinkedHashMap" in order to keep the insertion order
        fileInfoLinkedHashMap.put(getString(R.string.file_exists), exists ? getString(R.string.yes) : getString(R.string.no));
        fileInfoLinkedHashMap.put(getString(R.string.file_file), isFile ? getString(R.string.yes) : getString(R.string.no));
        fileInfoLinkedHashMap.put(getString(R.string.audio_source), audioSourceString);
        fileInfoLinkedHashMap.put(getString(R.string.output_format), outputFormatString);
        fileInfoLinkedHashMap.put(getString(R.string.audio_encoder), audioEncoderString);

        Set fileInfoSet = fileInfoLinkedHashMap.entrySet();

        for (Object aSet : fileInfoSet) {
            Map.Entry entry = (Map.Entry) aSet;

            TextView keyTextView = new TextView(fileInfoTableLayout.getContext()); // Instance
            keyTextView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT)); // Layout params
            keyTextView.setPadding((int) mainMargin, (int) mainMargin, (int) mainMargin, (int) mainMargin); // Padding
            keyTextView.setBackgroundColor(Color.parseColor("#EAEAEA")); // Background color
            keyTextView.setText(entry.getKey().toString()); // Text

            TextView valueTextView = new TextView(fileInfoTableLayout.getContext()); // Instance
            valueTextView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT)); // Layout params
            valueTextView.setPadding((int) mainMargin, (int) mainMargin, (int) mainMargin, (int) mainMargin); // Padding
            valueTextView.setBackgroundColor(Color.parseColor("#F7F7F7")); // Background color
            valueTextView.setText(entry.getValue().toString()); // Text
            valueTextView.setTextColor(ResourceUtil.getColor(this, R.color.colorAccent)); // Text color

            TableRow tableRow = new TableRow(fileInfoTableLayout.getContext());
            tableRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
            tableRow.addView(keyTextView);
            tableRow.addView(valueTextView);

            fileInfoTableLayout.addView(tableRow); // Content row

            View separatorView = new View(fileInfoTableLayout.getContext());
            separatorView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 2)); // Using "LinearLayout" params because "TableRow" is a "LinearLayout"
            separatorView.setBackgroundColor(Color.parseColor("#E1E3E5"));

            fileInfoTableLayout.addView(separatorView); // Separator row
        }
        // - File info

        // ----

        // Google AdMob
        ProgressBar advertisementBox2ProgressBar = findViewById(R.id.content_call_advertisement_box_2_progress_bar);
        TextView advertisementBox2NotAvailableTextView = findViewById(R.id.content_call_advertisement_2_box_not_available_text_view);
        AdView advertisementBox2AdView = findViewById(R.id.content_call_advertisement_box_2_ad_view);

        new Runnable() {
            @Override
            public void run() {
                AdRequest banner2AdRequest = null;
                try {
                    banner2AdRequest = adRequestBuilder.build();
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }

                if (banner2AdRequest != null) {
                    advertisementBox2AdView.setAdListener(new AdListener() {
                        @Override
                        public void onAdFailedToLoad(int errorCode) {
                            switch (errorCode) {
                                case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                                    break;
                                case AdRequest.ERROR_CODE_INVALID_REQUEST:
                                    break;
                                case AdRequest.ERROR_CODE_NETWORK_ERROR:
                                    break;
                                case AdRequest.ERROR_CODE_NO_FILL:
                                    break;
                            }

                            // ----

                            if (advertisementBox2ProgressBar.getVisibility() != View.GONE) {
                                advertisementBox2ProgressBar.setVisibility(View.GONE);
                            }

                            if (advertisementBox2AdView.getVisibility() != View.GONE) {
                                advertisementBox2AdView.setVisibility(View.GONE);
                            }

                            if (advertisementBox2NotAvailableTextView.getVisibility() != View.VISIBLE) {
                                advertisementBox2NotAvailableTextView.setVisibility(View.VISIBLE);
                            }

                            // ----

                            super.onAdFailedToLoad(errorCode);
                        }

                        @Override
                        public void onAdLoaded() {
                            if (advertisementBox2ProgressBar.getVisibility() != View.GONE) {
                                advertisementBox2ProgressBar.setVisibility(View.GONE);
                            }

                            if (advertisementBox2NotAvailableTextView.getVisibility() != View.GONE) {
                                advertisementBox2NotAvailableTextView.setVisibility(View.GONE);
                            }

                            if (advertisementBox2AdView.getVisibility() != View.VISIBLE) {
                                advertisementBox2AdView.setVisibility(View.VISIBLE);
                            }

                            // ----

                            super.onAdLoaded();
                        }
                    });

                    advertisementBox2AdView.loadAd(banner2AdRequest);
                } else {
                    if (advertisementBox2ProgressBar.getVisibility() != View.GONE) {
                        advertisementBox2ProgressBar.setVisibility(View.GONE);
                    }

                    if (advertisementBox2NotAvailableTextView.getVisibility() != View.VISIBLE) {
                        advertisementBox2NotAvailableTextView.setVisibility(View.VISIBLE);
                    }
                }
            }
        }.run();
        // - Google AdMob

        // ----

        // SIM card info
        String simSerialNumber;

        if (mIsIncoming) {
            simSerialNumber = mIncomingCallObject.getSimSerialNumber();
        } else {
            simSerialNumber = mOutgoingCallObject.getSimSerialNumber();
        }

        if (simSerialNumber != null && !simSerialNumber.trim().isEmpty()) {
            ((TextView) findViewById(R.id.content_call_serial_number)).setText(getString(R.string.serial_number_param, simSerialNumber));
        } else {
            ((TextView) findViewById(R.id.content_call_serial_number)).setText(getString(R.string.serial_number_param, getString(R.string.unknown_serial_number)));
        }

        String simOperator = null, simOperatorName = null, simCountryIso = null;

        if (mIsIncoming) {
            simOperator = mIncomingCallObject.getSimOperator();
            simOperatorName = mIncomingCallObject.getSimOperatorName();
            simCountryIso = mIncomingCallObject.getSimCountryIso();
        } else if (mIsOutgoing) {
            simOperator = mOutgoingCallObject.getSimOperator();
            simOperatorName = mOutgoingCallObject.getSimOperatorName();
            simCountryIso = mOutgoingCallObject.getSimCountryIso();
        }

        if (simOperator == null || simOperator.trim().isEmpty()) {
            simOperator = "N/A";
        }
        if (simOperatorName == null || simOperatorName.trim().isEmpty()) {
            simOperatorName = "N/A";
        }
        if (simCountryIso == null || simCountryIso.trim().isEmpty()) {
            simCountryIso = "N/A";
        }

        TableLayout simCardInfoTableLayout = findViewById(R.id.content_call_sim_card_info_table_layout);
        simCardInfoTableLayout.setStretchAllColumns(true);

        LinkedHashMap<String, String> simCardInfoLinkedHashMap = new LinkedHashMap<>(); // Using "LinkedHashMap" in order to keep the insertion order
        simCardInfoLinkedHashMap.put(getString(R.string.operator), simOperator);
        simCardInfoLinkedHashMap.put(getString(R.string.operator_name), simOperatorName);
        simCardInfoLinkedHashMap.put(getString(R.string.country_iso), simCountryIso.toUpperCase());

        Set simCardInfoSet = simCardInfoLinkedHashMap.entrySet();

        for (Object aSet : simCardInfoSet) {
            Map.Entry entry = (Map.Entry) aSet;

            TextView keyTextView = new TextView(simCardInfoTableLayout.getContext()); // Instance
            keyTextView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT)); // Layout params
            keyTextView.setPadding((int) mainMargin, (int) mainMargin, (int) mainMargin, (int) mainMargin); // Padding
            keyTextView.setBackgroundColor(Color.parseColor("#EAEAEA")); // Background color
            keyTextView.setText(entry.getKey().toString()); // Text

            TextView valueTextView = new TextView(simCardInfoTableLayout.getContext()); // Instance
            valueTextView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT)); // Layout params
            valueTextView.setPadding((int) mainMargin, (int) mainMargin, (int) mainMargin, (int) mainMargin); // Padding
            valueTextView.setBackgroundColor(Color.parseColor("#F7F7F7")); // Background color
            valueTextView.setText(entry.getValue().toString()); // Text
            valueTextView.setTextColor(ResourceUtil.getColor(this, R.color.colorAccent)); // Text color

            TableRow tableRow = new TableRow(simCardInfoTableLayout.getContext());
            tableRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
            tableRow.addView(keyTextView);
            tableRow.addView(valueTextView);

            simCardInfoTableLayout.addView(tableRow); // Content row

            View separatorView = new View(simCardInfoTableLayout.getContext());
            separatorView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 2)); // Using "LinearLayout" params because "TableRow" is a "LinearLayout"
            separatorView.setBackgroundColor(Color.parseColor("#E1E3E5"));

            simCardInfoTableLayout.addView(separatorView); // Separator row
        }
        // - SIM card info

        // ----

        // Network info
        String networkOperator = null, networkOperatorName = null, networkCountryIso = null;

        if (mIsIncoming) {
            networkOperator = mIncomingCallObject.getNetworkOperator();
            networkOperatorName = mIncomingCallObject.getNetworkOperatorName();
            networkCountryIso = mIncomingCallObject.getNetworkCountryIso();
        } else if (mIsOutgoing) {
            networkOperator = mOutgoingCallObject.getNetworkOperator();
            networkOperatorName = mOutgoingCallObject.getNetworkOperatorName();
            networkCountryIso = mOutgoingCallObject.getNetworkCountryIso();
        }

        if (networkOperator == null || networkOperator.trim().isEmpty()) {
            networkOperator = "N/A";
        }
        if (networkOperatorName == null || networkOperatorName.trim().isEmpty()) {
            networkOperatorName = "N/A";
        }
        if (networkCountryIso == null || networkCountryIso.trim().isEmpty()) {
            networkCountryIso = "N/A";
        }

        TableLayout networkInfoTableLayout = findViewById(R.id.content_call_network_info_table_layout);
        networkInfoTableLayout.setStretchAllColumns(true);

        LinkedHashMap<String, String> networkInfoLinkedHashMap = new LinkedHashMap<>(); // Using "LinkedHashMap" in order to keep the insertion order
        networkInfoLinkedHashMap.put(getString(R.string.operator), networkOperator);
        networkInfoLinkedHashMap.put(getString(R.string.operator_name), networkOperatorName);
        networkInfoLinkedHashMap.put(getString(R.string.country_iso), networkCountryIso.toUpperCase());

        Set networkInfoSet = networkInfoLinkedHashMap.entrySet();

        for (Object aSet : networkInfoSet) {
            Map.Entry entry = (Map.Entry) aSet;

            TextView keyTextView = new TextView(networkInfoTableLayout.getContext()); // Instance
            keyTextView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT)); // Layout params
            keyTextView.setPadding((int) mainMargin, (int) mainMargin, (int) mainMargin, (int) mainMargin); // Padding
            keyTextView.setBackgroundColor(Color.parseColor("#EAEAEA")); // Background color
            keyTextView.setText(entry.getKey().toString()); // Text

            TextView valueTextView = new TextView(networkInfoTableLayout.getContext()); // Instance
            valueTextView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT)); // Layout params
            valueTextView.setPadding((int) mainMargin, (int) mainMargin, (int) mainMargin, (int) mainMargin); // Padding
            valueTextView.setBackgroundColor(Color.parseColor("#F7F7F7")); // Background color
            valueTextView.setText(entry.getValue().toString()); // Text
            valueTextView.setTextColor(ResourceUtil.getColor(this, R.color.colorAccent)); // Text color

            TableRow tableRow = new TableRow(networkInfoTableLayout.getContext());
            tableRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
            tableRow.addView(keyTextView);
            tableRow.addView(valueTextView);

            networkInfoTableLayout.addView(tableRow); // Content row

            View separatorView = new View(networkInfoTableLayout.getContext());
            separatorView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 2)); // Using "LinearLayout" params because "TableRow" is a "LinearLayout"
            separatorView.setBackgroundColor(Color.parseColor("#E1E3E5"));

            networkInfoTableLayout.addView(separatorView); // Separator row
        }
        // - Network info
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtil.d(TAG, "Activity destroy");

        if (mMediaPlayer != null) {
            try {
                mMediaPlayer.stop(); // Stop
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }

            try {
                mMediaPlayer.reset(); // Reset
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }

            try {
                mMediaPlayer.release(); // Release
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }

            mMediaPlayer = null;
        }

        // Realm
        if (mIncomingCallObject != null) {
            mIncomingCallObject = null;
        }
        if (mOutgoingCallObject != null) {
            mOutgoingCallObject = null;
        }

        if (mRealm != null) {
            if (!mRealm.isClosed()) {
                try {
                    mRealm.close();
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }
            }

            mRealm = null;
        }
        // - Realm

        if (mIsIncoming) {
            mIsIncoming = false;
        }
        if (mIsOutgoing) {
            mIsOutgoing = false;
        }
    }

    // ----

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (menu == null) {
            return false;
        }

        getMenuInflater().inflate(R.menu.menu_call, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        /*if (menuItem == null) {
            return false;
        }*/

        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();

                return true;

            case R.id.action_make_phone_call:
                makePhoneCall();

                return true;

            case R.id.action_delete:
                delete();

                return true;
        }

        return super.onOptionsItemSelected(menuItem);
    }

    // ----

    private Dialog getMissingDataDialog() {
        return new AlertDialog.Builder(this)
                .setTitle("Cannot get call recording data")
                .setMessage("Getting call recording data is not possible. Some data is missing at all.")
                .setNeutralButton(android.R.string.ok, (dialogInterface, i) -> {
                    dialogInterface.dismiss();

                    // ----

                    finish();
                })
                .setCancelable(false)
                .create();
    }

    private void makePhoneCall() {
        LogUtil.i(TAG, "Make phone call");

        String phoneNumber = null;

        if (mIsIncoming && mIncomingCallObject != null) {
            phoneNumber = mIncomingCallObject.getPhoneNumber();
        } else if (mIsOutgoing && mOutgoingCallObject != null) {
            phoneNumber = mOutgoingCallObject.getPhoneNumber();
        }

        if (phoneNumber != null && !phoneNumber.trim().isEmpty()
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            try {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phoneNumber, null)));
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Cannot make phone call")
                    .setMessage("Making phone call to this correspondent is not possible.")
                    .setNeutralButton(android.R.string.ok, (dialogInterface, i) -> {
                        dialogInterface.dismiss();
                    })
                    .create().show();
        }
    }

    private void delete() {
        LogUtil.i(TAG, "Delete");

        new AlertDialog.Builder(this)
                .setTitle("Delete call recording")
                .setMessage("Are you sure you want to delete this call recording (and its audio file)? Data cannot be recovered.")
                .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                    dialogInterface.dismiss();

                    // ----

                    Realm realm = null;
                    try {
                        realm = Realm.getDefaultInstance();
                    } catch (Exception e) {
                        LogUtil.e(TAG, e.getMessage());
                        LogUtil.e(TAG, e.toString());

                        e.printStackTrace();
                    }

                    if (realm != null && !realm.isClosed()) {
                        try {
                            realm.beginTransaction();

                            if (mIsIncoming && mIncomingCallObject != null) {
                                IncomingCallObject incomingCallObject1 = realm.where(IncomingCallObject.class)
                                        .equalTo("mBeginTimestamp", mIncomingCallObject.getBeginTimestamp())
                                        .equalTo("mEndTimestamp", mIncomingCallObject.getEndTimestamp())
                                        .findFirst();

                                if (incomingCallObject1 != null) {
                                    File outputFile = null;
                                    try {
                                        outputFile = new File(incomingCallObject1.getOutputFile());
                                    } catch (Exception e) {
                                        LogUtil.e(TAG, e.getMessage());
                                        LogUtil.e(TAG, e.toString());

                                        e.printStackTrace();
                                    }

                                    if (outputFile != null) {
                                        if (outputFile.exists() && outputFile.isFile()) {
                                            try {
                                                outputFile.delete();
                                            } catch (Exception e) {
                                                LogUtil.e(TAG, e.getMessage());
                                                LogUtil.e(TAG, e.toString());

                                                e.printStackTrace();
                                            }
                                        }
                                    }

                                    incomingCallObject1.deleteFromRealm();

                                    realm.commitTransaction();

                                    Toast.makeText(this, "Call recording is deleted", Toast.LENGTH_SHORT).show();

                                    finish();
                                } else {
                                    realm.cancelTransaction();

                                    Toast.makeText(this, "Call recording is not deleted", Toast.LENGTH_SHORT).show();
                                }
                            } else if (mIsOutgoing && mOutgoingCallObject != null) {
                                OutgoingCallObject outgoingCallObject1 = realm.where(OutgoingCallObject.class)
                                        .equalTo("mBeginTimestamp", mOutgoingCallObject.getBeginTimestamp())
                                        .equalTo("mEndTimestamp", mOutgoingCallObject.getEndTimestamp())
                                        .findFirst();

                                if (outgoingCallObject1 != null) {
                                    File outputFile = null;
                                    try {
                                        outputFile = new File(outgoingCallObject1.getOutputFile());
                                    } catch (Exception e) {
                                        LogUtil.e(TAG, e.getMessage());
                                        LogUtil.e(TAG, e.toString());

                                        e.printStackTrace();
                                    }

                                    if (outputFile != null) {
                                        if (outputFile.exists() && outputFile.isFile()) {
                                            try {
                                                outputFile.delete();
                                            } catch (Exception e) {
                                                LogUtil.e(TAG, e.getMessage());
                                                LogUtil.e(TAG, e.toString());

                                                e.printStackTrace();
                                            }
                                        }
                                    }

                                    outgoingCallObject1.deleteFromRealm();

                                    realm.commitTransaction();

                                    Toast.makeText(this, "Call recording is deleted", Toast.LENGTH_SHORT).show();

                                    finish();
                                } else {
                                    realm.cancelTransaction();

                                    Toast.makeText(this, "Call recording is not deleted", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                realm.cancelTransaction();

                                Toast.makeText(this, "Call recording is not deleted", Toast.LENGTH_SHORT).show();
                            }

                            realm.close();
                        } catch (Exception e) {
                            LogUtil.e(TAG, e.getMessage());
                            LogUtil.e(TAG, e.toString());

                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(this, "Call recording is not deleted", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.no, (dialogInterface, i) -> dialogInterface.dismiss())
                .create().show();
    }
}
