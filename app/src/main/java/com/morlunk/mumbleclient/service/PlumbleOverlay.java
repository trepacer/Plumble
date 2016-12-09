/*
 * Copyright (C) 2014 Andrew Comminos
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.morlunk.mumbleclient.service;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.morlunk.mumbleclient.R;
import com.morlunk.mumbleclient.Settings;
import com.morlunk.mumbleclient.app.PlumbleActivity;
import com.morlunk.mumbleclient.channel.ChannelAdapter;
import com.morlunk.mumbleclient.util.Log;

import static com.morlunk.mumbleclient.util.Log.getClassInfo;

/**
 * An onscreen interactive overlay displaying the users in the current channel.
 * Created by andrew on 26/09/13.
 */
public class PlumbleOverlay {

    public static final int DEFAULT_WIDTH = 200;
    public static final int DEFAULT_HEIGHT = 240;


    private View mOverlayView;
    private ChannelAdapter mChannelAdapter;
    private ImageView mTalkButton;
    private ImageView mCloseButton;
    private WindowManager.LayoutParams mOverlayParams;
    private boolean mShown = false;

    private static PlumbleService mService;

    public PlumbleOverlay(PlumbleService service) {
        //7-3-5-1.初始化对讲窗口
        getClassInfo();
        mService = service;
        mOverlayView = View.inflate(service, R.layout.overlaycustom, null);//自定义页面
        mTalkButton = (ImageView) mOverlayView.findViewById(R.id.overlay_talk);
        mCloseButton = (ImageView) mOverlayView.findViewById(R.id.overlay_close);

        //小窗口按键按下事件
        mTalkButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(MotionEvent.ACTION_DOWN == event.getAction()) {
                    mTalkButton.setBackgroundResource(R.drawable.ic_action_microphone);
                    mService.setTalkingState(true);
                    PlumbleActivity.mPlayer.start();
                    return true;
                } else if(MotionEvent.ACTION_UP == event.getAction()) {
                    mTalkButton.setBackgroundResource(R.drawable.ic_action_microphone_dark);
                    mService.setTalkingState(false);
                    PlumbleActivity.mPlayer.pause();
                    return true;
                } else if(MotionEvent.ACTION_CANCEL == event.getAction()) {
                    mTalkButton.setBackgroundResource(R.drawable.ic_action_microphone_dark);
                    mService.setTalkingState(false);
                    PlumbleActivity.mPlayer.pause();
                    return true;
                }
                return false;
            }
        });
        //7-3-5-2.加载设置
        Settings settings = Settings.getInstance(service);
        //7-3-5-3.选择ptt模式  3种模式
        boolean usingPtt = Settings.ARRAY_INPUT_METHOD_PTT.equals(settings.getInputMethod());
        //7-3-5-4.设置ptt按钮显示
        setPushToTalkShown(usingPtt);

        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hide();
            }
        });

        DisplayMetrics metrics = mService.getResources().getDisplayMetrics();
        mOverlayParams = new WindowManager.LayoutParams((int)(DEFAULT_WIDTH*metrics.density),
                (int)(DEFAULT_HEIGHT*metrics.density),
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        mOverlayParams.gravity = Gravity.CENTER_VERTICAL | Gravity.CENTER;
        mOverlayParams.windowAnimations = android.R.style.Animation_Dialog;
    }

    public boolean isShown() {
        return mShown;
    }

    public void show() {
        getClassInfo();
        if(mShown)
            return;
        mShown = true;
        WindowManager windowManager = (WindowManager) mService.getSystemService(Context.WINDOW_SERVICE);
        windowManager.addView(mOverlayView, mOverlayParams);
    }

    public void hide() {
        getClassInfo();
        if(!mShown)
            return;
        mShown = false;
        try {
            WindowManager windowManager = (WindowManager) mService.getSystemService(Context.WINDOW_SERVICE);
            windowManager.removeView(mOverlayView);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public void setPushToTalkShown(boolean showPtt) {
        getClassInfo();
        mTalkButton.setVisibility(showPtt ? View.VISIBLE : View.GONE);
    }

    public static class MyBoardCast  extends BroadcastReceiver {

        public static boolean talkStatue = false;
        @Override
        public void onReceive(Context context, Intent intent) {
            getClassInfo();
            String action = intent.getAction();
            Log.getClassInfo("action: "+action);

            if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
                if (intent.hasExtra("state")) {
                    int state = intent.getIntExtra("state", 0);
                    if (state == 1) {
                        //1. 注册耳机监听
                        ((AudioManager)context.getSystemService(context.AUDIO_SERVICE)).registerMediaButtonEventReceiver(new ComponentName(
                                context,PlumbleOverlay.MyBoardCast.class));
                    } else if(state == 0){
                        ((AudioManager)context.getSystemService(context.AUDIO_SERVICE)).unregisterMediaButtonEventReceiver(new ComponentName(
                                context,PlumbleOverlay.MyBoardCast.class));
                    }
                }
            }else if(Intent.ACTION_MEDIA_BUTTON.equals(action)){
                Log.getClassInfo("action: "+action);
                KeyEvent keyEvent = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
                if(keyEvent.getAction() == KeyEvent.ACTION_DOWN){
                    Log.getClassInfo("talkStatue: "+talkStatue);
                    //mPlayer is null
                    if (talkStatue){
                        mService.setTalkingState(false);
                        PlumbleActivity.mPlayer.pause();
                        talkStatue = false;
                    }else{
                        mService.setTalkingState(true);
                        //10085467 mPlayer is null
//                        if(PlumbleActivity.mPlayer == null){
//                            PlumbleActivity.mPlayer = MediaPlayer.create(context, R.raw.ptt);
//                            PlumbleActivity.mPlayer.setLooping(true);
//                        }
                        PlumbleActivity.mPlayer.start();
                        talkStatue = true;
                    }
                }
            }else if(Intent.ACTION_BOOT_COMPLETED.equals(action)){
                Intent mainActivityIntent = new Intent(context, PlumbleActivity.class);  // 要启动的Activity
                mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(mainActivityIntent);
            }else if("com.tre.android.ptt_press".equals(action)){
                Log.getClassInfo("action: "+action);
                mService.setTalkingState(false);
                PlumbleActivity.mPlayer.pause();
            }else if("com.tre.android.ptt_release".equals(action)){
                Log.getClassInfo("action: "+action);
                mService.setTalkingState(true);
                PlumbleActivity.mPlayer.start();
            }
        }
    }
}
