package org.ar.screenshare

import android.app.Application
import android.media.projection.MediaProjection
import org.ar.rtc.IRtcEngineEventHandler
import org.ar.rtc.RtcEngine
import kotlin.properties.Delegates

class App : Application() {

    public lateinit var rtcEngine: RtcEngine
    lateinit var mediaProjection: MediaProjection
    private val eventList = mutableListOf<IRtcEngineEventHandler>()

    companion object{
        var app : App by Delegates.notNull()
    }

    override fun onCreate() {
        super.onCreate()
        app = this
        rtcEngine = RtcEngine.create(this,Config.appId,RtcListener())
    }

    inner class RtcListener : IRtcEngineEventHandler(){

        override fun onJoinChannelSuccess(channel: String?, uid: String?, elapsed: Int) {
            eventList.forEach {
                it.onJoinChannelSuccess(channel,uid,elapsed)
            }
        }

        override fun onError(err: Int) {
            eventList.forEach {
                it.onError(err)
            }
        }

        override fun onFirstRemoteVideoDecoded(
            uid: String?,
            width: Int,
            height: Int,
            elapsed: Int
        ) {
            eventList.forEach {
                it.onFirstRemoteVideoDecoded(uid,width,height,elapsed)
            }
        }

        override fun onUserOffline(uid: String?, reason: Int) {
            eventList.forEach {
                it.onUserOffline(uid,reason)
            }
        }

    }

    fun registerRtcEvent(listener: IRtcEngineEventHandler){
        if (listener !in eventList){
            eventList.add(listener)
        }
    }

    fun unRegisterRtcEvent(listener: IRtcEngineEventHandler){
        eventList.remove(listener)
    }



}