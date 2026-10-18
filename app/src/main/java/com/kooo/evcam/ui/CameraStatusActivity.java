package com.kooo.evcam.ui;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.kooo.evcam.R;
/** Ordinary troubleshooting, with no camera initialization, probes, retries or recording interruption. */
public final class CameraStatusActivity extends AppCompatActivity {
 private final Handler handler=new Handler(Looper.getMainLooper());private TextView status;
 private final Runnable refresh=new Runnable(){public void run(){if(status!=null){String text=CameraStatusText.report(CameraStatusActivity.this);
  if(!text.contentEquals(status.getText()))status.setText(text);handler.postDelayed(this,1000);}}};
 @Override protected void onCreate(Bundle state){super.onCreate(state);setContentView(R.layout.activity_camera_status);
  status=findViewById(R.id.camera_status_report);findViewById(R.id.camera_status_close).setOnClickListener(v->finish());
  findViewById(R.id.camera_status_diagnostics).setOnClickListener(v->startActivity(new Intent(this,com.kooo.evcam.zeekr.DiagnosticsActivity.class)));}
 @Override protected void onStart(){super.onStart();handler.post(refresh);}
 @Override protected void onStop(){handler.removeCallbacks(refresh);super.onStop();}
}
