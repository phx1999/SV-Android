package com.sv.speakerverify;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import com.sv.speakerverify.utils.JsonUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Daniel on 12/28/2014. Modify bt phx on 10/20/2020
 */
public class RecordingService extends Service {

  private static final String LOG_TAG = "RecordingService";
  private static final SimpleDateFormat mTimerFormat = new SimpleDateFormat("mm:ss",
      Locale.getDefault());
  private final OnTimerChangedListener onTimerChangedListener = null;
  private final int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
  private final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
  byte[] buffer = null;
  private ExecutorService mExecutorService;
  private DBHelper mDatabase;
  private long mStartingTimeMillis = 0;
  private TimerTask mIncrementTimerTask = null;
  private AudioRecord audioRecord = null;
  private int sampleRate = 16000;
  private boolean isRecording = false;
  private SaveFileReceiver saveFileReceiver = null;
  private StopRecordReceiver stopRecordReceiver = null;

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    mDatabase = new DBHelper(getApplicationContext());
    mExecutorService = Executors.newSingleThreadExecutor();
    ParseRegisterJsonFile();
    saveFileReceiver = new SaveFileReceiver(this);
    IntentFilter filter = new IntentFilter();
    filter.addAction(getString(R.string.service_save_file));
    registerReceiver(saveFileReceiver, filter);
    stopRecordReceiver = new StopRecordReceiver(this);
    filter = new IntentFilter();
    filter.addAction(getString(R.string.service_stop_record));
    registerReceiver(stopRecordReceiver, filter);
  }

  private void ParseRegisterJsonFile() {
    Log.i(LOG_TAG, JsonUtils.getJson(getApplicationContext(), "RegisterID.json"));
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    isRecording = true;
    mExecutorService.submit(new Runnable() {
      @Override
      public void run() {
        startRecording();
      }
    });
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    if (audioRecord != null) {
      isRecording = false;
      stopRecording();
    }
    unregisterReceiver(saveFileReceiver);
    unregisterReceiver(stopRecordReceiver);
    super.onDestroy();
  }

  public void startRecording() {
    if (MySharedPreferences.getPrefHighQuality(this)) {
      sampleRate = 44100;
    }

    int bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRate,
        channelConfiguration, audioEncoding); // need to be larger than size of a frame

    Log.i(LOG_TAG, "bufferSizeInBytes=" + bufferSizeInBytes);

    audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfiguration,
        audioEncoding, bufferSizeInBytes); //初始化麦克风

    audioRecord.startRecording();
    mStartingTimeMillis = System.currentTimeMillis();

    Log.i(LOG_TAG, "start recording");
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    buffer = new byte[bufferSizeInBytes];

    int bufferReadResult = 0;
    while (isRecording) {

      bufferReadResult = audioRecord.read(buffer, 0, bufferSizeInBytes);

      if (bufferReadResult > 0) {
        baos.write(buffer, 0, bufferReadResult);
      }
    }
    buffer = baos.toByteArray();
    try {
      baos.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    setFileNameAndPath();
  }

  public void setFileNameAndPath() {
    Intent intent = new Intent();
    intent.setAction(getString(R.string.service_input_name));
    sendBroadcast(intent);
  }

  public void stopRecording() {
    audioRecord.stop();
    audioRecord.release();
    //remove notification
    if (mIncrementTimerTask != null) {
      mIncrementTimerTask.cancel();
      mIncrementTimerTask = null;
    }

    audioRecord = null;
  }

  private void saveWavFile(String name) {
    OutputStream out = null;

    try {
      int count = 0;
      File recordingFile = null;
      String filePath = null;
      String filename = null;
      do {
        count++;
        filename = getString(R.string.default_file_name)
            + "_" + (mDatabase.getCount() + count) + ".wav";
        filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        filePath += "/SpeakerVerify/" + filename;

        recordingFile = new File(filePath);
      } while (recordingFile.exists() && !recordingFile.isDirectory());
      Log.i(LOG_TAG, "stop recording,file=" + recordingFile.getAbsolutePath());

      Log.i(LOG_TAG, "audio byte len=" + buffer.length);

      out = new FileOutputStream(recordingFile);
      out.write(getWavHeader(buffer.length));
      out.write(buffer);

      Toast.makeText(this, getString(R.string.toast_recording_finish) + " " + filePath,
          Toast.LENGTH_LONG).show();
      Log.i(LOG_TAG, "File write finished!");

      try {
        long mElapsedMillis = (System.currentTimeMillis() - mStartingTimeMillis);
        mDatabase.addRecording(filename, filePath, mElapsedMillis);

      } catch (Exception e) {
        Log.e(LOG_TAG, "exception", e);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      Intent intent = new Intent(this, RecordingService.class);
      stopService(intent);
    }
  }

  public byte[] getWavHeader(long totalAudioLen) {
    int mChannels = 1;
    long totalDataLen = totalAudioLen + 36;
    long longSampleRate = sampleRate;
    long byteRate = sampleRate * 2 * mChannels;

    byte[] header = new byte[44];
    header[0] = 'R';  // RIFF/WAVE header
    header[1] = 'I';
    header[2] = 'F';
    header[3] = 'F';
    header[4] = (byte) (totalDataLen & 0xff);
    header[5] = (byte) ((totalDataLen >> 8) & 0xff);
    header[6] = (byte) ((totalDataLen >> 16) & 0xff);
    header[7] = (byte) ((totalDataLen >> 24) & 0xff);
    header[8] = 'W';
    header[9] = 'A';
    header[10] = 'V';
    header[11] = 'E';
    header[12] = 'f';  // 'fmt ' chunk
    header[13] = 'm';
    header[14] = 't';
    header[15] = ' ';
    header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
    header[17] = 0;
    header[18] = 0;
    header[19] = 0;
    header[20] = 1;  // format = 1
    header[21] = 0;
    header[22] = (byte) mChannels;
    header[23] = 0;
    header[24] = (byte) (longSampleRate & 0xff);
    header[25] = (byte) ((longSampleRate >> 8) & 0xff);
    header[26] = (byte) ((longSampleRate >> 16) & 0xff);
    header[27] = (byte) ((longSampleRate >> 24) & 0xff);
    header[28] = (byte) (byteRate & 0xff);
    header[29] = (byte) ((byteRate >> 8) & 0xff);
    header[30] = (byte) ((byteRate >> 16) & 0xff);
    header[31] = (byte) ((byteRate >> 24) & 0xff);
    header[32] = (byte) (2 * mChannels);  // block align
    header[33] = 0;
    header[34] = 16;  // bits per sample
    header[35] = 0;
    header[36] = 'd';
    header[37] = 'a';
    header[38] = 't';
    header[39] = 'a';
    header[40] = (byte) (totalAudioLen & 0xff);
    header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
    header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
    header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

    return header;
  }

  public interface OnTimerChangedListener {

    void onTimerChanged(int seconds);
  }

  class SaveFileReceiver extends BroadcastReceiver {

    RecordingService service;

    public SaveFileReceiver(RecordingService recordingService) {
      super();
      this.service = recordingService;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
      Bundle bundle = intent.getExtras();
      String name = bundle.getString(getString(R.string.service_save_file_with_name));
      Log.i(LOG_TAG, name);
      service.saveWavFile(name);
    }
  }

  class StopRecordReceiver extends BroadcastReceiver {

    RecordingService service;

    public StopRecordReceiver(RecordingService recordingService) {
      super();
      this.service = recordingService;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
      if (audioRecord != null) {
        isRecording = false;
        stopRecording();
      }
    }
  }
}
