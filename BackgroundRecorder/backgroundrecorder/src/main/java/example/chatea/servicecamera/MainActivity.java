package example.chatea.servicecamera;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.DecimalFormat;

import example.chatea.servicecamera.messages.MeasurementStepMessage;
import example.chatea.servicecamera.messages.MessageHUB;
import example.chatea.servicecamera.messages.MessageListener;


public class MainActivity extends Activity implements MessageListener {

    private Button bt_recordingButton;
    TextView _currentDistanceView,_currentState;
    private final static DecimalFormat _decimalFormater = new DecimalFormat(
            "0.0");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bt_recordingButton = (Button) findViewById(R.id.recording_button);
        bt_recordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecording();
            }
        });
        MessageHUB.get().registerListener(MainActivity.this);
        _currentDistanceView = (TextView) findViewById(R.id.currentDistance);
        _currentDistanceView.setText("distance111");

        _currentState = (TextView) findViewById(R.id.currentState);
        _currentState.setText("State222");

    }

    private void startRecording() {
        Intent intent = new Intent(this, CameraService.class);
        startService(intent);

    }

    /**
     * Update the UI values.
     *
     * @param //eyeDist
     * @param //frameTime
     */
    public void updateUI(final MeasurementStepMessage message) {
//        Log.d("TAG", "=======updateUI");


        _currentDistanceView.setText(_decimalFormater.format(message
                .getDistToFace()) + " cm");

        float fontRatio = message.getDistToFace() / 29.7f;

        _currentDistanceView.setTextSize(fontRatio * 20);

    }

    int cnt=0;
    @Override
    public void onMessage(int messageID, Object message) {
        Log.d("TAG", "=======ListenerCatch");

        switch (messageID) {

            case MessageHUB.MEASUREMENT_STEP:
                updateUI((MeasurementStepMessage) message);
                break;

            case MessageHUB.DONE_CALIBRATION:
//                _calibrateButton.setBackgroundResource(R.drawable.green_button);
                break;

            case MessageHUB.LOG_ONSERVICE:
                Log.d("TAG", "======= service in on message");
                _currentState.setText("ONSERVICE");
                break;

            case MessageHUB.LOG_PREVIEWFRAME:
                cnt++;
                _currentState.setText(""+cnt);
                break;

            case MessageHUB.LOG_CAMERA_FAILED:
                _currentState.setText("CAMERA_FAILED");
                break;

            case MessageHUB.LOG_PARA:
                _currentState.setText("LOG_PARA");
                break;

            case MessageHUB.LOG_PREVIEW:
                _currentState.setText("STARTPREVIEW");
                break;

            case MessageHUB.setPreviewDisplay_error:
                _currentState.setText("setPreviewDisplay_error");
                break;
            default:
                break;
        }
    }
}
