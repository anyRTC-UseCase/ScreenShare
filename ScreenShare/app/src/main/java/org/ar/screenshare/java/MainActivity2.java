package org.ar.screenshare.java;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.ar.rtc.IRtcEngineEventHandler;
import org.ar.rtc.RtcEngine;
import org.ar.rtc.VideoEncoderConfiguration;
import org.ar.rtc.video.ARVideoFrame;
import org.ar.screenshare.Config;
import org.ar.screenshare.R;
import org.loka.screensharekit.ScreenShareKit;
import org.loka.screensharekit.callback.H264CallBack;

import java.nio.ByteBuffer;

public class MainActivity2 extends AppCompatActivity {

    private boolean isJoinChannel = false;
    private boolean isJoinSuccess = false;
    private RtcEngine engine;
    private TextView tvJoin;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvJoin = findViewById(R.id.tv_join);

        engine = RtcEngine.create(this, Config.Companion.getAppId(), new RtcEvent());
        engine.enableVideo();
        engine.setExternalVideoSource(true,true,true);
        engine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(VideoEncoderConfiguration.VD_1280x720));




    }



    public void onClick(View view) {
        switch (view.getId()){
            case R.id.tv_join:{
                if (isJoinChannel){
                    ScreenShareKit.INSTANCE.stop();
                    engine.leaveChannel();
                    isJoinSuccess =false;
                    tvJoin.setText("加入频道");
                    tvJoin.setSelected(false);
                }else{
                    requestCapture();//打开屏幕录制
                    tvJoin.setText("离开频道");
                    tvJoin.setSelected(true);
                    engine.joinChannel("","12345","","");
                }
                isJoinChannel = !isJoinChannel;
            }
        }


    }
    private class RtcEvent extends IRtcEngineEventHandler {
        @Override
        public void onJoinChannelSuccess(String channel, String uid, int elapsed) {
            super.onJoinChannelSuccess(channel, uid, elapsed);
            isJoinSuccess = true;
        }
    }

    private void requestCapture() {
        ScreenShareKit.INSTANCE.init(this).onH264(new H264CallBack() {
            @Override
            public void onH264(@NonNull ByteBuffer byteBuffer, boolean isKeyframe, long timeStamp) {
                if (isJoinSuccess) {
                    ARVideoFrame frame = new ARVideoFrame();
                    frame.bufType = ARVideoFrame.BUFFER_TYPE_H264;
                    frame.format = isKeyframe ? ARVideoFrame.FORMAT_VIDEO_KEY_FRAME:ARVideoFrame.FORMAT_VIDEO_NOR_FRAME;
                    frame.timeStamp = timeStamp;
                    frame.height = Resources.getSystem().getDisplayMetrics().heightPixels;
                    frame.stride = Resources.getSystem().getDisplayMetrics().widthPixels;
                    byte[] array = new byte[byteBuffer.remaining()];
                    byteBuffer.get(array);
                    frame.buf =array;
                    engine.pushExternalVideoFrame(frame);
                }
            }
        }).start();
    }

}