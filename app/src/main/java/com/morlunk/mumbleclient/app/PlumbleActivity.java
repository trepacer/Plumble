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

package com.morlunk.mumbleclient.app;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.text.InputType;
import android.text.format.Formatter;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.morlunk.jumble.Constants;
import com.morlunk.jumble.IJumbleService;
import com.morlunk.jumble.JumbleService;
import com.morlunk.jumble.model.Server;
import com.morlunk.jumble.protobuf.Mumble;
import com.morlunk.jumble.util.JumbleException;
import com.morlunk.jumble.util.JumbleObserver;
import com.morlunk.jumble.util.MumbleURLParser;
import com.morlunk.mumbleclient.BuildConfig;
import com.morlunk.mumbleclient.R;
import com.morlunk.mumbleclient.Settings;
import com.morlunk.mumbleclient.channel.AccessTokenFragment;
import com.morlunk.mumbleclient.channel.ChannelFragment;
import com.morlunk.mumbleclient.channel.ServerInfoFragment;
import com.morlunk.mumbleclient.db.DatabaseCertificate;
import com.morlunk.mumbleclient.db.DatabaseProvider;
import com.morlunk.mumbleclient.db.PlumbleDatabase;
import com.morlunk.mumbleclient.db.PlumbleSQLiteDatabase;
import com.morlunk.mumbleclient.db.PublicServer;
import com.morlunk.mumbleclient.internet.RequestListener;
import com.morlunk.mumbleclient.internet.SendRequest;
import com.morlunk.mumbleclient.preference.PlumbleCertificateGenerateTask;
import com.morlunk.mumbleclient.preference.Preferences;
import com.morlunk.mumbleclient.servers.FavouriteServerListFragment;
import com.morlunk.mumbleclient.servers.PublicServerListFragment;
import com.morlunk.mumbleclient.servers.ServerEditFragment;
import com.morlunk.mumbleclient.service.PlumbleOverlay;
import com.morlunk.mumbleclient.service.PlumbleService;
import com.morlunk.mumbleclient.util.JumbleServiceFragment;
import com.morlunk.mumbleclient.util.JumbleServiceProvider;
import com.morlunk.mumbleclient.util.Log;
import com.morlunk.mumbleclient.util.PlumbleTrustStore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;


import info.guardianproject.netcipher.proxy.OrbotHelper;

public class PlumbleActivity extends ActionBarActivity implements ListView.OnItemClickListener,
        FavouriteServerListFragment.ServerConnectHandler, JumbleServiceProvider, DatabaseProvider,
        SharedPreferences.OnSharedPreferenceChangeListener, DrawerAdapter.DrawerDataProvider,
        ServerEditFragment.ServerEditListener {
    /**
     * If specified, the provided integer drawer fragment ID is shown when the activity is created.
     */
    public static final String EXTRA_DRAWER_FRAGMENT = "drawer_fragment";

    private PlumbleService mService;
    private PlumbleDatabase mDatabase;
    private Settings mSettings;

    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private DrawerAdapter mDrawerAdapter;

    private ProgressDialog mConnectingDialog;
    private AlertDialog mErrorDialog;
    private AlertDialog.Builder mDisconnectPromptBuilder;

    /** List of fragments to be notified about service state changes. */
    private List<JumbleServiceFragment> mServiceFragments = new ArrayList<JumbleServiceFragment>();
    public static MediaPlayer mPlayer;
    public static String branchcd = null;
    public static String acountcd = null;
    public static String serverip = null;
    public static String moveTaskToBack = null;
    private PlumbleOverlay.MyBoardCast receiver;
//    public void initPlayer(){
//        PlumbleActivity.mPlayer = MediaPlayer.create(this, R.raw.ptt);
//        PlumbleActivity.mPlayer.setLooping(true);
//    }
    public void hideActivity(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(moveTaskToBack == null){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                powerWake();
                moveTaskToBack(true);
            }
        }).start();
    }


    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //2. 隐藏界面
            Log.getClassInfo("onServiceConnected");
//            moveTaskToBack(true);
            hideActivity();
            mService = (PlumbleService)((JumbleService.JumbleBinder) service).getService();
            mService.registerObserver(mObserver);
            mService.clearChatNotifications(); // Clear chat notifications on resume.
            mDrawerAdapter.notifyDataSetChanged();

            for(JumbleServiceFragment fragment : mServiceFragments)
                fragment.setServiceBound(true);

            // Re-show server list if we're showing a fragment that depends on the service.
            if(getSupportFragmentManager().findFragmentById(R.id.content_frame) instanceof JumbleServiceFragment &&
                    !mService.isSynchronized()) {
                loadDrawerFragment(DrawerAdapter.ITEM_FAVOURITES);
            }
            Log.getClassInfo("ConnectionState: onServiceConnected");
            updateConnectionState(getService());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    private JumbleObserver mObserver = new JumbleObserver() {
        @Override
        public void onConnected() {
            Log.getClassInfo();
            loadDrawerFragment(DrawerAdapter.ITEM_SERVER);
            mDrawerAdapter.notifyDataSetChanged();
            supportInvalidateOptionsMenu();
            Log.getClassInfo("ConnectionState: onConnected");
            updateConnectionState(getService());
            //2. 隐藏界面
//            Log.getClassInfo("moveTaskToBack");
//            moveTaskToBack(true);
            hideActivity();
        }

        @Override
        public void onConnecting() {
            Log.getClassInfo("ConnectionState: onConnecting");
            updateConnectionState(getService());
        }

        @Override
        public void onDisconnected(JumbleException e) {
            Log.getClassInfo("ConnectionState: onDisconnected");
            // Re-show server list if we're showing a fragment that depends on the service.
            if(getSupportFragmentManager().findFragmentById(R.id.content_frame) instanceof JumbleServiceFragment) {
                loadDrawerFragment(DrawerAdapter.ITEM_FAVOURITES);
            }
            mDrawerAdapter.notifyDataSetChanged();
            supportInvalidateOptionsMenu();

            updateConnectionState(getService());
        }

        @Override
        public void onTLSHandshakeFailed(X509Certificate[] chain) {
            final Server lastServer = getService().getConnectedServer();

            if (chain.length == 0)
                return;

            try {
                final X509Certificate x509 = chain[0];

                AlertDialog.Builder adb = new AlertDialog.Builder(PlumbleActivity.this);
                adb.setTitle(R.string.untrusted_certificate);
                try {
                    MessageDigest digest = MessageDigest.getInstance("SHA-1");
                    byte[] certDigest = digest.digest(x509.getEncoded());
                    String hexDigest = new String(Hex.encode(certDigest));
                    adb.setMessage(getString(R.string.certificate_info,
                            x509.getSubjectDN().getName(),
                            x509.getNotBefore().toString(),
                            x509.getNotAfter().toString(),
                            hexDigest));
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    adb.setMessage(x509.toString());
                }
                /*adb.setPositiveButton(R.string.allow, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Try to add to trust store
                        try {
                            String alias = lastServer.getHost();
                            KeyStore trustStore = PlumbleTrustStore.getTrustStore(PlumbleActivity.this);
                            trustStore.setCertificateEntry(alias, x509);
                            PlumbleTrustStore.saveTrustStore(PlumbleActivity.this, trustStore);
                            Toast.makeText(PlumbleActivity.this, R.string.trust_added, Toast.LENGTH_LONG).show();
                            connectToServer(lastServer);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(PlumbleActivity.this, R.string.trust_add_failed, Toast.LENGTH_LONG).show();
                        }
                    }
                });
                adb.setNegativeButton(R.string.wizard_cancel, null);
                adb.show();*/

                try {
                    String alias = lastServer.getHost();
                    KeyStore trustStore = PlumbleTrustStore.getTrustStore(PlumbleActivity.this);
                    trustStore.setCertificateEntry(alias, x509);
                    PlumbleTrustStore.saveTrustStore(PlumbleActivity.this, trustStore);
                    Toast.makeText(PlumbleActivity.this, R.string.trust_added, Toast.LENGTH_LONG).show();
                    connectToServer(lastServer);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(PlumbleActivity.this, R.string.trust_add_failed, Toast.LENGTH_LONG).show();
                }
            } catch (CertificateException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onPermissionDenied(String reason) {
            AlertDialog.Builder adb = new AlertDialog.Builder(PlumbleActivity.this);
            adb.setTitle(R.string.perm_denied);
            adb.setMessage(reason);
            adb.show();
        }
    };
    private String getAvailMemory() {// 获取android当前可用内存大小
        Log.getClassInfo();

        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        MemoryInfo mi = new MemoryInfo();
        am.getMemoryInfo(mi);
        //mi.availMem; 当前系统的可用内存

        return Formatter.formatFileSize(getBaseContext(), mi.availMem);// 将获取的内存大小规格化
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //1-1.程序开始
        Log.getClassInfo();
        //1-2.唤醒屏幕
        powerWake();
        //1-3.可用内存
        Log.getClassInfo("可用内存： "+getAvailMemory());
        //1-4.加载设置
        mSettings = Settings.getInstance(this);
        //1-5.设置主题
        setTheme(mSettings.getTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //1-6.用户可见下，屏幕一直亮
        setStayAwake(mSettings.shouldStayAwake());
        //1-7.获取用户名
        getServerInfo();
        //1-8.软件核心设置
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        //监听SharedPreference是否改变,及时更新界面中preference的summary
        preferences.registerOnSharedPreferenceChangeListener(this);
        mDatabase = new PlumbleSQLiteDatabase(this); // TODO add support for cloud storage
        mDatabase.open();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setOnItemClickListener(this);
        //1-9.数组适配器
        mDrawerAdapter = new DrawerAdapter(this, this);
        mDrawerList.setAdapter(mDrawerAdapter);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                Log.getClassInfo();
                super.onDrawerStateChanged(newState);
                // Prevent push to talk from getting stuck on when the drawer is opened.
                if (getService() != null
                        && getService().isSynchronized()
                        && getService().isTalking() && !mSettings.isPushToTalkToggle()) {
                    getService().setTalkingState(false);
                }
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                supportInvalidateOptionsMenu();
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        //1-10.顶部导航栏
        int iconColor = getTheme().obtainStyledAttributes(new int[] { android.R.attr.textColorPrimaryInverse }).getColor(0, -1);
        Drawable logo = getResources().getDrawable(R.drawable.ic_home);
        logo.setColorFilter(iconColor, PorterDuff.Mode.MULTIPLY);
        getSupportActionBar().setLogo(logo);
        //1-11.替换一个 Fragment
        if(savedInstanceState == null) {
            if (getIntent() != null && getIntent().hasExtra(EXTRA_DRAWER_FRAGMENT)) {
                loadDrawerFragment(getIntent().getIntExtra(EXTRA_DRAWER_FRAGMENT,
                        DrawerAdapter.ITEM_FAVOURITES));
            } else {
                loadDrawerFragment(DrawerAdapter.ITEM_FAVOURITES);
            }
        }
        // If we're given a Mumble URL to show, open up a server edit fragment.
        if(getIntent() != null &&
                Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            String url = getIntent().getDataString();
            try {
                Server server = MumbleURLParser.parseURL(url);

                // Open a dialog prompting the user to connect to the Mumble server.
                DialogFragment fragment = (DialogFragment) ServerEditFragment.createServerEditDialog(
                        PlumbleActivity.this, server, ServerEditFragment.Action.CONNECT_ACTION, true);
                fragment.show(getSupportFragmentManager(), "url_edit");
            } catch (MalformedURLException e) {
                Toast.makeText(this, getString(R.string.mumble_url_parse_failed), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }

        setVolumeControlStream(mSettings.isHandsetMode() ?
                AudioManager.STREAM_VOICE_CALL : AudioManager.STREAM_MUSIC);
        //1-12.如果没有证书,就生成证书
        if(mSettings.isFirstRun()) showSetupWizard();
    }

    private void powerWake() {
        Log.getClassInfo();
        if (Build.VERSION.SDK_INT > 8) {
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(Context.POWER_SERVICE);
            boolean powerSaveMode = false;
            powerSaveMode = pm.isScreenOn();
            Log.getClassInfo("powerSaveMode: "+powerSaveMode);
            if(!powerSaveMode){
                // 10085467 唤醒屏幕
                exeCmd("adb shell input keyevent 26;adb shell input keyevent 82");
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        Log.getClassInfo();
        //1-13.onCreate方法彻底执行完毕的回调
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    protected void onResume() {
        Log.getClassInfo();
        //1-14.获得焦点
        super.onResume();
        Intent connectIntent = new Intent(this, PlumbleService.class);
        bindService(connectIntent, mConnection, 0);
        hideActivity();
    }

    @Override
    protected void onPause() {
        Log.getClassInfo();
        super.onPause();
        if (mErrorDialog != null)
            mErrorDialog.dismiss();
        if (mConnectingDialog != null)
            mConnectingDialog.dismiss();

        if(mService != null) {
            for (JumbleServiceFragment fragment : mServiceFragments) {
                fragment.setServiceBound(false);
            }
            mService.unregisterObserver(mObserver);
        }
        unbindService(mConnection);
    }

    @Override
    protected void onDestroy() {
        Log.getClassInfo("ConnectionState: onDisconnected");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.unregisterOnSharedPreferenceChangeListener(this);
        mDatabase.close();
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.getClassInfo();
        //1-16.初始化Menu菜单 创建activity时,调用一次
        MenuItem disconnectButton = menu.findItem(R.id.action_disconnect);
        disconnectButton.setVisible(mService != null && mService.isSynchronized());

        // Color the action bar icons to the primary text color of the theme.
        int foregroundColor = getSupportActionBar().getThemedContext()
                .obtainStyledAttributes(new int[] { android.R.attr.textColor })
                .getColor(0, -1);
        for(int x=0;x<menu.size();x++) {
            MenuItem item = menu.getItem(x);
            if(item.getIcon() != null) {
                Drawable icon = item.getIcon().mutate(); // Mutate the icon so that the color filter is exclusive to the action bar
                icon.setColorFilter(foregroundColor, PorterDuff.Mode.MULTIPLY);
            }
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //1-15.初始化Menu菜单 每次点击menu都会触发
        Log.getClassInfo();
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.plumble, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.getClassInfo();
        if(mDrawerToggle.onOptionsItemSelected(item))
            return true;

        switch (item.getItemId()) {
            case R.id.action_disconnect:
                getService().disconnect();
                return true;
        }

        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.getClassInfo();
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.getClassInfo();
        if (mService != null && keyCode == mSettings.getPushToTalkKey()) {
            mService.onTalkKeyDown();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.getClassInfo();
        if (mService != null && keyCode == mSettings.getPushToTalkKey()) {
            mService.onTalkKeyUp();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        Log.getClassInfo();
        if(mService != null && mService.isSynchronized()) {
//            mDisconnectPromptBuilder.show();

            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.getClassInfo();
        mDrawerLayout.closeDrawers();
        loadDrawerFragment((int) id);
    }

    /**
     * Shows a nice looking setup wizard to guide the user through the app's settings.
     * Will do nothing if it isn't the first launch.
     */
    private void showSetupWizard() {
        Log.getClassInfo();
        PlumbleCertificateGenerateTask generateTask = new PlumbleCertificateGenerateTask(PlumbleActivity.this) {
            @Override
            protected void onPostExecute(DatabaseCertificate result) {
                super.onPostExecute(result);
                if(result != null) mSettings.setDefaultCertificateId(result.getId());
            }
        };
        generateTask.execute();
        mSettings.setFirstRun(false);
    }

    /**
     * Loads a fragment from the drawer.
     */
    private void loadDrawerFragment(int fragmentId) {
        Log.getClassInfo();
        Class<? extends Fragment> fragmentClass = null;
        Bundle args = new Bundle();
        switch (fragmentId) {
            case DrawerAdapter.ITEM_SERVER:
                fragmentClass = ChannelFragment.class;
                break;
            case DrawerAdapter.ITEM_INFO:
                fragmentClass = ServerInfoFragment.class;
                break;
            case DrawerAdapter.ITEM_ACCESS_TOKENS:
                fragmentClass = AccessTokenFragment.class;
                args.putLong("server", mService.getConnectedServer().getId());
                args.putStringArrayList("access_tokens", (ArrayList<String>) mDatabase.getAccessTokens(mService.getConnectedServer().getId()));
                break;
            case DrawerAdapter.ITEM_PINNED_CHANNELS:
                fragmentClass = ChannelFragment.class;
                args.putBoolean("pinned", true);
                break;
            case DrawerAdapter.ITEM_FAVOURITES:
                fragmentClass = FavouriteServerListFragment.class;
                break;
            case DrawerAdapter.ITEM_PUBLIC:
                fragmentClass = PublicServerListFragment.class;
                break;
            case DrawerAdapter.ITEM_SETTINGS:
                Intent prefIntent = new Intent(this, Preferences.class);
                startActivity(prefIntent);
                return;
            default:
                return;
        }
        Fragment fragment = Fragment.instantiate(this, fragmentClass.getName(), args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, fragment, fragmentClass.getName())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
        //1-9-2.根据fragmentId返回对应的id,title
        setTitle(mDrawerAdapter.getItemWithId(fragmentId).title);
    }

    public void connectToServer(final Server server) {
        Log.getClassInfo();
        // Check if we're already connected to a server; if so, inform user.
        if(mService != null && mService.isConnectionEstablished()) {
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            adb.setMessage(R.string.reconnect_dialog_message);
            adb.setPositiveButton(R.string.connect, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Register an observer to reconnect to the new server once disconnected.
                    mService.registerObserver(new JumbleObserver() {
                        @Override
                        public void onDisconnected(JumbleException e) {
                            Log.getClassInfo("ConnectionState: onDisconnected");
                            Log.getClassInfo("Host"+server.getHost());
                            connectToServer(server);
                            mService.unregisterObserver(this);
                        }
                    });
                    mService.disconnect();
                }
            });
            adb.setNegativeButton(android.R.string.cancel, null);
            adb.show();
            return;
        }

        // Prompt to start Orbot if enabled but not running
        // TODO(acomminos): possibly detect onion address before connecting?
        if (mSettings.isTorEnabled()) {
            if (!OrbotHelper.isOrbotRunning(this)) {
                OrbotHelper.requestShowOrbotStart(this);
                return;
            }
        }
        //1-7-7-2.创建Task,执行异步线程 连接server
        ServerConnectTask connectTask = new ServerConnectTask(this, mDatabase);
        connectTask.execute(server);
    }

    public void connectToPublicServer(final PublicServer server) {
        Log.getClassInfo();
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);

        final Settings settings = Settings.getInstance(this);

        // Allow username entry
        final EditText usernameField = new EditText(this);
        usernameField.setHint(settings.getDefaultUsername());
        alertBuilder.setView(usernameField);

        alertBuilder.setTitle(R.string.connectToServer);

        alertBuilder.setPositiveButton(R.string.connect, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                PublicServer newServer = server;
                if(!usernameField.getText().toString().equals(""))
                    newServer.setUsername(usernameField.getText().toString());
                else
                    newServer.setUsername(settings.getDefaultUsername());
                connectToServer(newServer);
            }
        });

        alertBuilder.show();
    }

    private void setStayAwake(boolean stayAwake) {
        Log.getClassInfo();
        if (stayAwake) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    /**
     * Updates the activity to represent the connection state of the given service.
     * Will show reconnecting dialog if reconnecting, dismiss otherwise, etc.
     * Basically, this service will do catch-up if the activity wasn't bound to receive
     * connection state updates.
     * @param service A bound IJumbleService.
     */
    private void updateConnectionState(IJumbleService service) {
        Log.getClassInfo();
        Log.getClassInfo("ConnectionState: "+mService.getConnectionState());
        if (mConnectingDialog != null)
            mConnectingDialog.dismiss();
        if (mErrorDialog != null)
            mErrorDialog.dismiss();

        switch (mService.getConnectionState()) {
            case CONNECTING:
                Server server = service.getConnectedServer();
                mConnectingDialog = new ProgressDialog(this);
                mConnectingDialog.setIndeterminate(true);
                mConnectingDialog.setCancelable(true);
                mConnectingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        mService.disconnect();
                        Toast.makeText(PlumbleActivity.this, R.string.cancelled,
                                Toast.LENGTH_SHORT).show();
                    }
                });
                mConnectingDialog.setMessage(getString(R.string.connecting_to_server, server.getHost(),
                        server.getPort()));
                mConnectingDialog.show();
                break;
            case CONNECTION_LOST:
                // Only bother the user if the error hasn't already been shown.
                if (!getService().isErrorShown()) {
                    JumbleException error = getService().getConnectionError();
                    Log.getClassInfo("ConnectionState: begin");
                    Log.getClassInfo("ERROR: "+error.getMessage());
                    Log.getClassInfo("ConnectionState: begin");
                    AlertDialog.Builder ab = new AlertDialog.Builder(PlumbleActivity.this);
                    ab.setTitle(R.string.connectionRefused);
                    if (mService.isReconnecting()) {
                        ab.setMessage(getString(R.string.attempting_reconnect, error.getMessage()));
                        ab.setPositiveButton(R.string.cancel_reconnect, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (getService() != null) {
                                    getService().cancelReconnect();
                                    getService().markErrorShown();
                                }
                            }
                        });
                    } else if (error.getReason() == JumbleException.JumbleDisconnectReason.REJECT &&
                            (error.getReject().getType() == Mumble.Reject.RejectType.WrongUserPW ||
                                    error.getReject().getType() == Mumble.Reject.RejectType.WrongServerPW)) {
                        // FIXME(acomminos): Long conditional.
                        final EditText passwordField = new EditText(this);
                        passwordField.setInputType(InputType.TYPE_CLASS_TEXT |
                                InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        passwordField.setHint(R.string.password);
                        ab.setTitle(R.string.invalid_password);
                        ab.setMessage(error.getMessage());
                        ab.setView(passwordField);
                        ab.setPositiveButton(R.string.reconnect, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Server server = getService().getConnectedServer();
                                if (server == null)
                                    return;
                                String password = passwordField.getText().toString();
                                server.setPassword(password);
                                if (server.isSaved())
                                    mDatabase.updateServer(server);
                                connectToServer(server);
                            }
                        });
                        ab.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (getService() != null)
                                    getService().markErrorShown();
                            }
                        });
                    } else {
                        ab.setMessage(error.getMessage());
                        ab.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (getService() != null)
                                    getService().markErrorShown();
                            }
                        });
                    }
                    ab.setCancelable(false);
                    mErrorDialog = ab.show();
                }
                break;


        }
    }

    /*
     * HERE BE IMPLEMENTATIONS
     */

    @Override
    public PlumbleService getService() {
        return mService;
    }

    @Override
    public PlumbleDatabase getDatabase() {
        return mDatabase;
    }

    @Override
    public void addServiceFragment(JumbleServiceFragment fragment) {
        Log.getClassInfo();
        mServiceFragments.add(fragment);
    }

    @Override
    public void removeServiceFragment(JumbleServiceFragment fragment) {
        Log.getClassInfo();
//        mServiceFragments.remove(fragment);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        //1-8-2.SharedPreference改变
        Log.getClassInfo();
        if(Settings.PREF_THEME.equals(key)) {
            // Recreate activity when theme is changed
            if(Build.VERSION.SDK_INT >= 11)
                recreate();
            else {
                Intent intent = new Intent(this, PlumbleActivity.class);
                finish();
                startActivity(intent);
            }
        } else if (Settings.PREF_STAY_AWAKE.equals(key)) {
            setStayAwake(mSettings.shouldStayAwake());
        } else if (Settings.PREF_HANDSET_MODE.equals(key)) {
            setVolumeControlStream(mSettings.isHandsetMode() ?
                    AudioManager.STREAM_VOICE_CALL : AudioManager.STREAM_MUSIC);
        }
    }

    @Override
    public boolean isConnected() {
        return mService != null && mService.isSynchronized();
    }

    @Override
    public String getConnectedServerName() {
        Log.getClassInfo();
        if(mService != null && mService.isSynchronized()) {
            Server server = mService.getConnectedServer();
            return server.getName().equals("") ? server.getHost() : server.getName();
        }
        if (BuildConfig.DEBUG)
            throw new RuntimeException("getConnectedServerName should only be called if connected!");
        return "";
    }

    @Override
    public void onServerEdited(ServerEditFragment.Action action, Server server) {
        Log.getClassInfo();
        switch (action) {
            case ADD_ACTION:
                mDatabase.addServer(server);
                loadDrawerFragment(DrawerAdapter.ITEM_FAVOURITES);
                break;
            case EDIT_ACTION:
                mDatabase.updateServer(server);
                loadDrawerFragment(DrawerAdapter.ITEM_FAVOURITES);
                break;
            case CONNECT_ACTION:
                connectToServer(server);
                break;
        }
    }

    private void registerHeadsetPlugReceiver() {
        Log.getClassInfo();
        receiver = new PlumbleOverlay.MyBoardCast();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);
        intentFilter.setPriority(100);
        registerReceiver(receiver, intentFilter);
    }

    public void getServerInfo(){
        Log.getClassInfo();
        //1-7-1.注册耳机插拔
        registerHeadsetPlugReceiver();
        //1-7-2.注册耳机监听
        ((AudioManager)getSystemService(AUDIO_SERVICE)).registerMediaButtonEventReceiver(new ComponentName(
                this,
                PlumbleOverlay.MyBoardCast.class));
        if(acountcd != null && acountcd.equals("")){
            Log.getClassInfo("acountcd :"+acountcd);
            return;
        }
        //1-7-3.从服务器获取数据
        Map<String, String> param = new HashMap<String, String>(); // 参数
        param.clear();
        String SerialNumber = android.os.Build.SERIAL;  //url参数：序列号
        String ip = getLocalIpAddress();                 //url参数：ip地址
        param.put("staticip", ip);
        param.put("serialnumber",SerialNumber);
        param.put("formatType","json");
        //1-7-4.加载媒体声音
        mPlayer = MediaPlayer.create(this, R.raw.ptt);
        mPlayer.setLooping(true);
        SendRequest.openRequest(this, getString(R.string.GETPTTSERVER), param,
                new RequestListener() {
                    @Override
                    public void onSucess(Object info) throws JSONException {
                        Log.getClassInfo("register success");
                        //1-7-5.从服务器取得数据
                        JSONObject ds = (JSONObject) info;
                        if(ds.isNull("Table0")){
                            return;
                        }

                        JSONArray table0 = (JSONArray) ds.get("Table0");
                        JSONObject object = (JSONObject) table0.get(0);
                        branchcd = (String) object.get("branchcd");
                        acountcd = (String) object.get("account");
                        serverip = (String) object.get("serverip");
                        //1-7-6.创建server
                        Server server = createServer();
                        //1-7-7.连接server
                        connectToServer(server);
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Log.e("Volley","getEmpCD", e);
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("Volley","getEmpCD", e);
                    }
                });
    }

    /**
     * 获取当前ip地址
     *
     * @return
     */
    public static String getLocalIpAddress() {
        Log.getClassInfo();
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()
                            && !inetAddress.isLinkLocalAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public Server createServer() {
        Log.getClassInfo();
        String name = acountcd;
        String host = serverip;

        int port = Constants.DEFAULT_PORT;

        String username = acountcd;
        String password = "";
        return new Server(-1, name, host, port, username, password);

    }
    private void exeCmd(String cmd) {
        try {
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
