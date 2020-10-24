package com.sv.speakerverify.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.sv.speakerverify.R;
import com.sv.speakerverify.adapters.RecordButtonAdapter;

/**
 * @author phx
 * @create 2020-10-20-16:46
 */
public class VerifyFragment extends Fragment {

  private static final String ARG_POSITION = "position";
  private static final String LOG_TAG = VerifyFragment.class.getSimpleName();

  private int position;
  private RecordButtonAdapter mRecordButtonAdapter = null;
  private TextView mVerifyIntroductionText;
  private TextView mVerifyResultText;

  public static VerifyFragment newInstance(int position) {
    VerifyFragment f = new VerifyFragment();
    Bundle b = new Bundle();
    b.putInt(ARG_POSITION, position);
    f.setArguments(b);

    return f;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    position = getArguments().getInt(ARG_POSITION);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View verifyView = inflater.inflate(R.layout.fragment_verify, container, false);

    //update verify introduction text
    mVerifyIntroductionText = (TextView) verifyView.findViewById(R.id.verify_introduction_text);
    mVerifyResultText = (TextView) verifyView.findViewById(R.id.verify_result);

    mRecordButtonAdapter = (RecordButtonAdapter) verifyView.findViewById(R.id.btnVerify);
    mRecordButtonAdapter.setColorNormal(getResources().getColor(R.color.primary));
    mRecordButtonAdapter.setColorPressed(getResources().getColor(R.color.primary_dark));

    return verifyView;
  }
}
