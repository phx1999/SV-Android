package com.sv.speakerverify.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.EditText;
import com.melnykov.fab.FloatingActionButton;

/**
 * @author phx
 * @create 2020-10-20-17:44
 */
public class RecordButtonAdapter extends FloatingActionButton {
    public RecordButtonAdapter(Context context) {
      super(context);
    }

    public RecordButtonAdapter(Context context, AttributeSet attrs) {
      super(context, attrs);
    }

    public RecordButtonAdapter(Context context, AttributeSet attrs, final int defStyleAttr) {
      super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
      switch (ev.getAction()) {
        case MotionEvent.ACTION_DOWN:
          Log.d("test","verify begin");
          break;
        case MotionEvent.ACTION_UP:
          Log.d("test","verify end");
          break;
        default:
      }
      return super.onTouchEvent(ev);
    }
}
