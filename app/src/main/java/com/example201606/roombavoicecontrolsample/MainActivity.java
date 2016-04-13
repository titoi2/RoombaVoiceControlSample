package com.example201606.roombavoicecontrolsample;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.physicaloid.lib.Physicaloid;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    // ルンバとの通信速度(bps)
    private static final int SERIAL_BPS = 115200;

    // 音声入力のリクエストコード
    private static final int REQUEST_CODE = 217;

    private TextView mTvLog;
    private Button mButtonStart;

    private Intent mSpeechIntent;
    private Physicaloid mPhysicaloid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // スリープ抑止
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mTvLog = (TextView) findViewById(R.id.tvLog);
        mTvLog.setMovementMethod(ScrollingMovementMethod.getInstance());

        mButtonStart = (Button) findViewById(R.id.buttonStart);
        mButtonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 開始ボタン押下処理
                roombaStop();
                startListen();
            }
        });

        // Physicaloidのインスタンスを生成
        mPhysicaloid = new Physicaloid(this);

        // 音声認識の　Intent インスタンスを生成
        mSpeechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10);
        mSpeechIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "音声を入力");

    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        roombaStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * 音声入力開始
     */
    private void startListen() {
        // インテント発行
        startActivityForResult(mSpeechIntent, REQUEST_CODE);
    }


    /**
     * 音声入力からのリザルト
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v(TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // 認識結果を取得
                ArrayList<String> candidates = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                // コマンド解析
                String command = commandAnalyze(candidates);
                mTvLog.setText(command);
            } else {
                roombaStop();
            }
        }
    }

    /**
     * 音声入力テキスト解析処理
     * キーワード「止前後左右」が含まれていたら、ルンバにコマンドを送信する
     *
     * @return 実行したコマンドを返す
     */
    private String commandAnalyze(ArrayList<String> candidates) {
        for (String candidate : candidates) {
            if (candidate.contains("止")) {

                roombaStop();
                return candidate;
            }
            if (candidate.contains("後")) {

                roombaBackward();
                return candidate;
            }
            if (candidate.contains("前")) {

                roombaForward();
                return candidate;
            }
            if (candidate.contains("左")) {

                roombaTurnLeft();
                return candidate;
            }
            if (candidate.contains("右")) {

                roombaTurnRight();
                return candidate;
            }
        }
        return "なんだって？";
    }


    /**
     * ルンバ停止
     */
    void roombaStop() {
        sendDriveDirect(0, 0);
    }

    /**
     * ルンバ前進
     */
    void roombaForward() {
        sendDriveDirect(300, 300);
    }

    /**
     * ルンバ後退
     */
    void roombaBackward() {
        sendDriveDirect(-300, -300);
    }

    /**
     * ルンバ左回転
     */
    void roombaTurnLeft() {
        sendDriveDirect(-300, 300);
    }

    /**
     * ルンバ右回転
     */
    void roombaTurnRight() {
        sendDriveDirect(300, -300);
    }

    /**
     * 車輪制御コマンドを送信する
     *
     * @param l 左車輪パラメータ、-500〜500 (mm/s)
     * @param r 右車輪パラメータ、-500〜500 (mm/s)
     */
    private void sendDriveDirect(int l, int r) {
        byte[] commands = new byte[7];
        commands[0] = (byte) 128;        // Start

        commands[1] = (byte) 132;        // Full

        commands[2] = (byte) 145;        // Drive Direct
        commands[3] = (byte) (r >> 8);   // Right velocity high byte
        commands[4] = (byte) r;          // Right velocity low byte
        commands[5] = (byte) (l >> 8);   // Left velocity high byte
        commands[6] = (byte) l;          // Left velocity low byte

        // バイト列をシリアルに送信
        sendCommand(commands);
    }

    /**
     * シリアル通信でルンバにコマンドを送信する
     *
     * @param commands
     */
    private void sendCommand(byte[] commands) {
        if (mPhysicaloid.open()) {
            mPhysicaloid.setBaudrate(SERIAL_BPS);
            mPhysicaloid.write(commands, commands.length);
            mPhysicaloid.close();
        }
    }
}
