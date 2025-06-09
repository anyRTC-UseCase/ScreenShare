package org.ar.screenshare

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.MemoryFile
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.TextureView
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import org.ar.rtc.Constants
import org.ar.rtc.IRtcEngineEventHandler
import org.ar.rtc.RtcEngine
import org.ar.rtc.VideoEncoderConfiguration
import org.ar.rtc.VideoEncoderConfiguration.ORIENTATION_MODE
import org.ar.rtc.VideoEncoderConfiguration.VideoDimensions
import org.ar.rtc.video.ARVideoFrame
import org.ar.rtc.video.VideoCanvas
import org.ar.screenshare.databinding.ActivityMainBinding
import org.loka.screensharekit.EncodeBuilder
import org.loka.screensharekit.ScreenShareKit
import org.loka.screensharekit.callback.RGBACallBack
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {


    private var isJoinChannel = false
    private var isJoinSuccess = false
    private val rtcEngine by lazy { RtcEngine.create(this,Config.appId,RtcEvent()) }
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        rtcEngine.enableVideo()
        rtcEngine.setExternalVideoSource(true,true,true)

        binding.run {
            tvJoin.setOnClickListener({
                if (isJoinChannel){
                    ScreenShareKit.stop()
                    rtcEngine.leaveChannel()
                    isJoinSuccess =false
                    tvJoin.text = "加入频道"
                    tvJoin.isSelected = false
                }else{
                    requestCapture()//打开屏幕录制
                    tvJoin.text = "离开频道"
                    tvJoin.isSelected = true
                    rtcEngine.joinChannel("","123456","","")
                }
                isJoinChannel = !isJoinChannel
            })
        }
    }



    private fun requestCapture() {
        ScreenShareKit.init(this).config(if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 1280 else 720 ,
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 720 else 1280,5,1000000, EncodeBuilder.SCREEN_DATA_TYPE.RGBA,
        ).onRGBA(object :RGBACallBack{
            override fun onRGBA(
                rgba: ByteArray,
                width: Int,
                height: Int,
                stride: Int,
                rotation: Int,
                rotationChanged: Boolean
            ) {

                if (isJoinSuccess){
                    if (rotationChanged) {
                        val configuration = VideoEncoderConfiguration()
                        configuration.dimensions = VideoDimensions(width, height)
                        configuration.bitrate = 1000
                        configuration.minBitrate = 1000
                        configuration.minFrameRate = 1
                        configuration.frameRate = 5
                        configuration.orientationMode =
                            if (rotation == 90 || rotation == 270) ORIENTATION_MODE.ORIENTATION_MODE_FIXED_LANDSCAPE else ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
                        rtcEngine?.setVideoEncoderConfiguration(configuration)
                    }

                    val videoFrame = ARVideoFrame()
                    videoFrame.bufType = ARVideoFrame.BUFFER_TYPE_ARRAY
                    videoFrame.buf = rgba
                    videoFrame.stride = stride
                    videoFrame.width=width
                    videoFrame.height = height
                    videoFrame.rotation = 0
                    videoFrame.format = ARVideoFrame.FORMAT_RGBA
                    rtcEngine?.pushExternalVideoFrame(videoFrame)
                }
            }

        }).start()
    }


    inner class RtcEvent:IRtcEngineEventHandler(){
        override fun onJoinChannelSuccess(channel: String?, uid: String?, elapsed: Int) {
            super.onJoinChannelSuccess(channel, uid, elapsed)
            runOnUiThread {
                isJoinSuccess = true
            }
        }


        override fun onUserJoined(uid: String?, elapsed: Int) {
            super.onUserJoined(uid, elapsed)
        }

        override fun onUserOffline(uid: String?, reason: Int) {
            super.onUserOffline(uid, reason)
        }

        override fun onFirstRemoteVideoDecoded(
            uid: String?,
            width: Int,
            height: Int,
            elapsed: Int
        ) {
            super.onFirstRemoteVideoDecoded(uid, width, height, elapsed)
        }


    }




    override fun onDestroy() {
        super.onDestroy()
        ScreenShareKit.stop()
        RtcEngine.destroy()
    }
}