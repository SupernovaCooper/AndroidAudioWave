package me.relex.waveformdemo;

import android.content.res.AssetFileDescriptor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

import me.relex.widget.waveform.WaveFormInfo;
import me.relex.widget.waveform.WaveFormSelectionView;
import me.relex.widget.waveform.WaveFormSelectionViewListener;
import me.relex.widget.waveform.WaveFormThumbSelectionView;
import me.relex.widget.waveform.WaveFormViewListener;

public class MainActivity extends AppCompatActivity {

    private WaveFormSelectionView mWaveFormView;
    private WaveFormThumbSelectionView mWaveFormThumbView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mWaveFormView = (WaveFormSelectionView) findViewById(R.id.wave_form_view);
        mWaveFormView.setWaveFormListener(new WaveFormViewListener() {
            @Override
            public void onScrollChanged(long startTime, long endTime) {
                mWaveFormThumbView.updateThumbTime(startTime, endTime);
            }
        });
        mWaveFormView.setWaveFormSelectionViewListener(new WaveFormSelectionViewListener() {
            @Override
            public void onHandlerSelectionChanged(long startTime, long endTime) {
                mWaveFormThumbView.updateSelectionTime(startTime, endTime);

                ((TextView) findViewById(R.id.time_start_tv)).setText(longToTime(startTime));
                ((TextView) findViewById(R.id.time_end_tv)).setText(longToTime(endTime));
                ((TextView) findViewById(R.id.time_duration_tv)).setText(longToTime(endTime - startTime));
            }

            @Override
            public void onPlayTimeSelectionChanged(long currentTime) {
                ((TextView) findViewById(R.id.time_play_tv)).setText(longToTime(currentTime));
            }
        });

        mWaveFormThumbView = (WaveFormThumbSelectionView) findViewById(R.id.wave_form_thumb_view);
        mWaveFormThumbView.setOnDragThumbListener(new WaveFormThumbSelectionView.OnDragThumbListener() {
            @Override
            public void onDrag(long startTime) {
                mWaveFormView.setStartTime(startTime);
            }
        });


//        new ReaderTask() {
//            @Override
//            protected void onPostExecute(WaveFormInfo waveFormInfo) {
////                mWaveFormThumbView.setWave(waveFormInfo); // Must be first.
//                mWaveFormView.setWave(waveFormInfo);
//            }
//        }.execute();


        try {
            AssetFileDescriptor afd = getAssets().openFd("1.mp3");
            new WaveFormInfo.Factory(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength()).build(new WaveFormInfo.Factory.Callback() {
                @Override
                public void onExtraFailed(@NonNull Exception e) {
                    e.printStackTrace();
                }

                @Override
                public void onExtraStart(@NonNull WaveFormInfo data) {
                    findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                }

                @Override
                public void onExtraProgress(@NonNull WaveFormInfo data, float progress, @NonNull byte[] newData) {
                    ((ProgressBar) findViewById(R.id.progressBar)).setProgress(((int) progress));
                }

                @Override
                public void onExtraComplete(@NonNull WaveFormInfo data) {
                    findViewById(R.id.progressBar).setVisibility(View.GONE);
                    mWaveFormThumbView.setWave(data); // Must be first.
                    mWaveFormView.setWave(data);

                    //播放指针
                    mWaveFormView.updatePlayCursorTime(2200);
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }

        //TODO 这个demo只是演示控件怎么用，具体其他逻辑，例如按钮、播放进度回调等这里就不放了。
    }

    public class ReaderTask extends AsyncTask<Void, Void, WaveFormInfo> {

        @Override
        protected WaveFormInfo doInBackground(Void... params) {
            InputStream inputStream = null;
            try {
                inputStream = getResources().openRawResource(R.raw.waveform);
                byte[] data = new byte[inputStream.available()];
                inputStream.read(data);
                return JSON.parseObject(data, WaveFormInfo.class);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    public static String longToTime(long time) {
        return longToString(time, "mm:ss:SSS");
    }

    public static String longToString(long longtime, String formatType) {
        return new SimpleDateFormat(formatType, Locale.getDefault()).format(longtime);
    }
}
