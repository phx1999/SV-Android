package com.sv.speakerverify.fragments;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.melnykov.fab.FloatingActionButton;
import com.sv.speakerverify.R;
import com.sv.speakerverify.RecordingService;
import java.io.File;

/**
 * A simple {@link Fragment} subclass. Activities that contain this fragment must implement the to
 * handle interaction events. Use the {@link RecordFragment#newInstance} factory method to create an
 * instance of this fragment.
 */
public class RecordFragment extends Fragment {

  // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
  private static final String ARG_POSITION = "position";
  private static final String LOG_TAG = RecordFragment.class.getSimpleName();
  long timeWhenPaused = 0; //stores time when user clicks pause button
  private int position;
  //Recording controls
  private FloatingActionButton mRecordButton = null;
  private Button mPauseButton = null;
  private TextView mRecordingPrompt;
  private int mRecordPromptCount = 0;
  private boolean mStartRecording = true;
  private boolean mPauseRecording = true;
  private Chronometer mChronometer = null;
  private RecordReceiver receiver = null;


  public RecordFragment() {
  }

  /**
   * Use this factory method to create a new instance of this fragment using the provided
   * parameters.
   *
   * @return A new instance of fragment Record_Fragment.
   */
  public static RecordFragment newInstance(int position) {
    RecordFragment f = new RecordFragment();
    Bundle b = new Bundle();
    b.putInt(ARG_POSITION, position);
    f.setArguments(b);

    return f;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    position = getArguments().getInt(ARG_POSITION);

    //监听 record service服务
    receiver = new RecordReceiver();
    IntentFilter filter = new IntentFilter();
    filter.addAction(getString(R.string.service_input_name));
    getActivity().registerReceiver(receiver, filter);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View recordView = inflater.inflate(R.layout.fragment_record, container, false);

    mChronometer = (Chronometer) recordView.findViewById(R.id.chronometer);
    //update recording prompt text
    mRecordingPrompt = (TextView) recordView.findViewById(R.id.recording_status_text);

    mRecordButton = (FloatingActionButton) recordView.findViewById(R.id.btnRecord);
    mRecordButton.setColorNormal(getResources().getColor(R.color.primary));
    mRecordButton.setColorPressed(getResources().getColor(R.color.primary_dark));
    mRecordButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onRecord(mStartRecording);
        mStartRecording = !mStartRecording;
      }
    });

    mPauseButton = (Button) recordView.findViewById(R.id.btnPause);
    mPauseButton.setVisibility(View.GONE); //hide pause button before recording starts
    mPauseButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onPauseRecord(mPauseRecording);
        mPauseRecording = !mPauseRecording;
      }
    });

    return recordView;
  }

  // Recording Start/Stop
  //TODO: recording pause
  private void onRecord(boolean start) {
    if (start) {
      Intent intent = new Intent(getActivity(), RecordingService.class);
      // start recording
      mRecordButton.setImageResource(R.drawable.ic_media_stop);
      //mPauseButton.setVisibility(View.VISIBLE);
      Toast.makeText(getActivity(), R.string.toast_recording_start, Toast.LENGTH_SHORT).show();
      File folder = new File(Environment.getExternalStorageDirectory() + "/SpeakerVerify");
      if (!folder.exists()) {
        //folder /speakerverify doesn't exist, create the folder
        folder.mkdir();
      }

      //start Chronometer
      mChronometer.setBase(SystemClock.elapsedRealtime());
      mChronometer.start();
      mChronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
        @Override
        public void onChronometerTick(Chronometer chronometer) {
          if (mRecordPromptCount == 0) {
            mRecordingPrompt.setText(getString(R.string.record_in_progress) + ".");
          } else if (mRecordPromptCount == 1) {
            mRecordingPrompt.setText(getString(R.string.record_in_progress) + "..");
          } else if (mRecordPromptCount == 2) {
            mRecordingPrompt.setText(getString(R.string.record_in_progress) + "...");
            mRecordPromptCount = -1;
          }

          mRecordPromptCount++;
        }
      });

      //start RecordingService
      getActivity().startService(intent);
      //keep screen on while recording
      getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

      mRecordingPrompt.setText(getString(R.string.record_in_progress) + ".");
      mRecordPromptCount++;

    } else {
      //stop recording
      mRecordButton.setImageResource(R.drawable.ic_mic_white_36dp);
      //mPauseButton.setVisibility(View.GONE);
      mChronometer.stop();
      mChronometer.setBase(SystemClock.elapsedRealtime());
      timeWhenPaused = 0;
      mRecordingPrompt.setText(getString(R.string.record_prompt));

//      getActivity().stopService(intent);
      Intent stop_intent = new Intent();
      stop_intent.setAction(getString(R.string.service_stop_record));
      getActivity().sendBroadcast(stop_intent);
      //allow the screen to turn off again once recording is finished
      getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
  }

  //TODO: implement pause recording
  private void onPauseRecord(boolean pause) {
    if (pause) {
      //pause recording
      mPauseButton.setCompoundDrawablesWithIntrinsicBounds
          (R.drawable.ic_media_play, 0, 0, 0);
      mRecordingPrompt.setText((String) getString(R.string.resume_recording_button).toUpperCase());
      timeWhenPaused = mChronometer.getBase() - SystemClock.elapsedRealtime();
      mChronometer.stop();
    } else {
      //resume recording
      mPauseButton.setCompoundDrawablesWithIntrinsicBounds
          (R.drawable.ic_media_pause, 0, 0, 0);
      mRecordingPrompt.setText((String) getString(R.string.pause_recording_button).toUpperCase());
      mChronometer.setBase(SystemClock.elapsedRealtime() + timeWhenPaused);
      mChronometer.start();
    }
  }

  class RecordReceiver extends BroadcastReceiver {

    String name = null;

    @Override
    public void onReceive(Context context, Intent intent) {
      final EditText inputServer = new EditText(context);
      if (name != null) {
        inputServer.setText(name);
      }
      AlertDialog.Builder builder = new AlertDialog.Builder(context);
      builder.setTitle(getString(R.string.input_name_dialog_title))
          .setIcon(android.R.drawable.ic_dialog_info).setView(inputServer)
          .setMessage(getString(R.string.input_name_dialog_message));

      builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
          Log.i(LOG_TAG, "Cancel to input name stop service");
          Intent intent = new Intent(getActivity(), RecordingService.class);
          getActivity().stopService(intent);
        }
      });

      builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
          name = inputServer.getText().toString();
          if (inputServer.getText().toString().equals("")) {
            Toast.makeText(getActivity(), getString(R.string.input_name_dialog_empty),
                Toast.LENGTH_LONG).show();
            Intent intent = new Intent();
            intent.setAction(getString(R.string.service_input_name));
            getActivity().sendBroadcast(intent);
            return;
          }
          Log.i(LOG_TAG, name);
          Intent intent = new Intent();
          intent.putExtra(getString(R.string.service_save_file_with_name), name);
          intent.setAction(getString(R.string.service_save_file));
          getActivity().sendBroadcast(intent);
        }
      });
      builder.show();
    }
  }

}