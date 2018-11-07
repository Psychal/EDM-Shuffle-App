package com.mwr.edmshuffle;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

import static android.view.View.GONE;
import static com.mwr.edmshuffle.R.layout.activity_main;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "onfling";
    WebView myWebView;
    Bundle myWebViewBundle;
    boolean logged;
    boolean newpage;
    boolean prevpage;
    boolean loadhtml;
    boolean firstime;
    boolean timercancel;
    boolean html2Bool;
    private static String html;
    private static int seconds;
    private static CountDownTimer songcountdown;
    ProgressDialog pageProgress;

    // URL to get contacts JSON
    private static String url = "";

    // JSON Node names
    private static final String TAG_MUSICFEED = "musicfeed";
    private static final String TAG_ID = "objects";
    private static final String TAG_TITLE = "title";
    private static final String TAG_ARTIST = "field_artist";
    private static final String TAG_SONG = "Song";
    private static final String TAG_GENRE = "Genre(s):";
    private static final String TAG_LENGTH = "length_seconds";
    private static final String TAG_FULLURL = "field_fullurl";
    private static final String TAG_CHANNEL = "Channel";
    private static final String TAG_MODE = "Psychedelic Mode";
    private static final String TAG_YTIMG = "field_youtubeimg";

    public void finish() {
        super.finish();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (timercancel) {
                stopcd(myWebView);

            }
            System.out.println("Notification");
            unregisterReceiver(mReceiver);
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //on creation of the activity
        super.onCreate(savedInstanceState);
        setContentView(activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        // Create an object of our Custom Gesture Detector Class
        CustomGestureDetector customGestureDetector = new CustomGestureDetector();
        // Create a GestureDetector
        mGestureDetector = new GestureDetector(this, customGestureDetector);
        // Attach listeners that'll be called for double-tap and related gestures
        mGestureDetector.setOnDoubleTapListener(customGestureDetector);

        final NotificationCompat.Builder mBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_gear_edm)
                        .setContentTitle("EDM Shuffle")
                        .setAutoCancel(true)
                        .setContentText("Press me to get back to the frontpage!");

        IntentFilter filter = new IntentFilter("android.intent.CLOSE_ACTIVITY");
        registerReceiver(mReceiver, filter);
        unregisterReceiver(mReceiver);

        if (timercancel) {
            stopcd(myWebView);
        }

        myWebView = (WebView) findViewById(R.id.EDM);
        myWebViewBundle = new Bundle();
        WebSettings myWebSettings = myWebView.getSettings();
        myWebSettings.setJavaScriptEnabled(true);
        myWebView.addJavascriptInterface(new WebAppInterface(this), "Android");

        myWebSettings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:61.0) Gecko/20100101 Firefox/61.0");
        myWebSettings.setMediaPlaybackRequiresUserGesture(false);
        myWebView.setWebViewClient(new WebViewClient() {

            Animation slideLeftAnimIn = AnimationUtils.loadAnimation(getBaseContext(), R.anim.righttoleft_in);
            Animation slideRightAnimIn = AnimationUtils.loadAnimation(getBaseContext(), R.anim.lefttoright_in);

            @Override
            public void onPageFinished(WebView view, String url) {
                // Inject CSS when page is done loading.

                String cookies = CookieManager.getInstance().getCookie(url);
                System.out.println(url);
                if (url.contains("DRUPAL_UID=")) {
                    logged = true;
                    System.out.print("You're logged in" + logged);
                } else {
                    logged = false;
                    System.out.print("You're logged out");
                }


                new AsyncLogin().execute();
                Toolbar myToolbar2 = (Toolbar) findViewById(R.id.toolbar2);
                String weburl = myWebView.getUrl();
                System.out.println(weburl);
                //Hide bottom toolbar.
                if (weburl.contains("http://edm-shuffle.com/")) {
                    myToolbar2.setVisibility(GONE);
                } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    myToolbar2.setVisibility(View.GONE);
                }


                if (newpage) {
                    righttoleft();
                    newpage = false;
                }
                if (prevpage){
                    lefttoright();
                    prevpage = false;
                }
                System.out.println("onpagefinished");


                super.onPageFinished(view, url);
            }
        });


        Intent intent = new Intent("android.intent.CLOSE_ACTIVITY");
        final PendingIntent pIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        Intent resultIntent = new Intent(this, MainActivity.class);
// Because clicking the notification opens a new ("special") activity, there's
// no need to create an artificial back stack.
        final PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

// Inflate a menu to be displayed in the toolbar
        Toolbar myToolbar2 = (Toolbar) findViewById(R.id.toolbar2);
        myToolbar2.inflateMenu(R.menu.toolbarmenubottom);

        // Set an OnMenuItemClickListener to handle menu item clicks
        myToolbar2.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // Handle the bottom menu item

                switch (item.getItemId()) {
                    case R.id.action_navigation:
                        if (timercancel) {
                            stopcd(myWebView);
                        }
                        if (html2Bool) {
                            setLoadHtml2(true);
                        }
                        new GetJSON().execute();
                        return true;
                    case R.id.action_navigation2:
                        if (timercancel) {
                            stopcd(myWebView);
                        }

                        finish();
                        return true;
                    case R.id.action_navigation3:
                        mBuilder.setContentIntent(resultPendingIntent);
                        int mNotificationId = 001;
// Gets an instance of the NotificationManager service
                        NotificationManager mNotifyMgr =
                                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
// Builds the notification and issues it.
                        mNotifyMgr.notify(mNotificationId, mBuilder.build());
                        Intent startMain = new Intent(Intent.ACTION_MAIN);
                        startMain.addCategory(Intent.CATEGORY_HOME);
                        startMain.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(startMain);

                        if (timercancel) {
                            stopcd(myWebView);
                        }
                        if (html2Bool) {
                            setLoadHtml2(true);
                        }
                        new GetJSON().execute();

                        return true;
                    case R.id.action_navigation4:
                        String appendurl = myWebView.getUrl();
                        if (appendurl.endsWith("?psychedelic")) {
                            myWebView.loadUrl(appendurl.replace("?psychedelic", ""));
                        } else {
                            myWebView.loadUrl(appendurl + "?psychedelic");
                        }
                        return true;
                }
                return true;
            }
        });

        myWebView.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, final MotionEvent event) {
                mGestureDetector.onTouchEvent(event);
                return MainActivity.super.onTouchEvent(event);
            }
        });
        if (loadhtml) {
            myWebView.loadData(TAG_SONG, "text/html", null);
        } else {
            myWebView.loadUrl("http://edm-shuffle.com/");
        }

    }
    public class WebAppInterface {
        Context mContext;

        /** Instantiate the interface and set the context */
        WebAppInterface(Context c) {
            mContext = c;
        }

        /** Show a toast from the web page */
        @JavascriptInterface
        public void showToast(String toast) {
            myWebView.post(new Runnable() {
                @Override
                public void run() {
                    if (timercancel) {
                        stopcd(myWebView);
                    }
                    if (html2Bool) {
                        setLoadHtml2(true);
                    }
                    new GetJSON().execute();

                }
            });

            Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Async task class to get json by making HTTP call
     */
    private class GetJSON extends AsyncTask<Void, Void, Void> {

        // Hashmap for ListView
        ArrayList<HashMap<String, String>> objectList;
        ProgressDialog proDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
// Showing progress loading dialog
            proDialog = new ProgressDialog(MainActivity.this);
            proDialog.setMessage("Please wait...");
            proDialog.setCancelable(false);
            proDialog.show();


        }

        @Override
        protected Void doInBackground(Void... arg0) {
// Creating service handler class instance
            WebRequest webreq = new WebRequest();

// Making a request to url and getting response
            String jsonStr = webreq.makeWebServiceCall(url, WebRequest.GETRequest);
            System.out.println(jsonStr + "jsonStr");

            Log.d("Response: ", "> " + jsonStr);

            objectList = ParseJSON(jsonStr);
            System.out.println(objectList + "objectlist");


            return null;
        }

        @Override
        protected void onPostExecute(Void requestresult) {
            super.onPostExecute(requestresult);
// Dismiss the progress dialog
            if (proDialog.isShowing())
                proDialog.dismiss();
            if ((!html2Bool) && (seconds != 0)) {
                html2Bool = true;
                setLoadHtml1();
            }

// Updating received data from JSON into ListView
            if (objectList != null) {

                    ListView listView = (ListView) findViewById(R.id.list1);
                    ListAdapter adapter = new SimpleAdapter(
                            MainActivity.this, objectList,
                            R.layout.list_item, new String[]{TAG_TITLE, TAG_ARTIST, TAG_SONG},
                            new int[]{R.id.fulltitle, R.id.artist, R.id.song});
                    listView.setAdapter(adapter);
            }
        }
    }
    public void lefttoright(){
        Animation slideRightAnimIn = AnimationUtils.loadAnimation(getBaseContext(), R.anim.lefttoright_in);

        myWebView.setVisibility(View.VISIBLE);
        myWebView.startAnimation(slideRightAnimIn);
        pageProgress.dismiss();
    };

    public void righttoleft(){
        Animation slideLeftAnimIn = AnimationUtils.loadAnimation(getBaseContext(), R.anim.righttoleft_in);

        myWebView.setVisibility(View.VISIBLE);
        myWebView.startAnimation(slideLeftAnimIn);
        pageProgress.dismiss();
    };

    private ArrayList<HashMap<String, String>> ParseJSON(String json) {
        if (json != null) {
            try {
// Hashmap for ListView
                ArrayList<HashMap<String, String>> objectList = new ArrayList<HashMap<String, String>>();

                JSONObject jsonObj = new JSONObject(json);

// Getting JSON Array node
                JSONArray musicfeed = jsonObj.getJSONArray(TAG_MUSICFEED);
                System.out.println(musicfeed + "musicfeed");

// looping through All
                for (int i = 0; i < musicfeed.length(); i++) {
                    JSONObject c = musicfeed.getJSONObject(i);
                    System.out.println(c + "c");
                    // objects node is JSON Object
                    JSONObject items = c.getJSONObject(TAG_ID);

                    String length = items.getString(TAG_LENGTH);
                    String title = items.getString(TAG_TITLE);
                    String artist = items.getString(TAG_ARTIST);
                    String Song = items.getString(TAG_SONG);
                    String genre = items.getString(TAG_GENRE);

                    String fullurl = items.getString(TAG_FULLURL);
                    String channel = items.getString(TAG_CHANNEL);
                    String mode = items.getString(TAG_MODE);
                    String YTimg = items.getString(TAG_YTIMG);
                    System.out.println(length + "objects");
// tmp hashmap for single
                    HashMap<String, String> object = new HashMap<>();

// adding every child node to HashMap key => value
                    object.put(TAG_LENGTH, length);
                    object.put(TAG_TITLE, title);
                    object.put(TAG_ARTIST, artist);
                    object.put(TAG_SONG, Song);

// adding student to students list_item
                    objectList.add(object);
                    System.out.println(object);
                    String embedurl = fullurl.replace("watch?v=", "embed/") + "?&#10;&#10;autoplay=1&amp;feature=player_detailpage&amp;VQ=HD1080&amp;wmode=transparent&amp;iv_load_policy=3\"frameborder=\"0\" style=\"pointer-events:none;\"";
                    String number = length.replaceAll("\\D+", "");
                    seconds = 3000 + Integer.parseInt(number + "000");


                    InputStream is = getAssets().open("myhtml.html");
                    StringBuilder htmlbuilder = new StringBuilder();
                    byte[] buffer = new byte[1024];
                    while (is.read(buffer) != -1) {
                        htmlbuilder.append(new String(buffer));
                    }
                    is.close();
                    String rawHtml = htmlbuilder.toString();
                    html = rawHtml.replace("%VIDEO%", embedurl)
                            .replace("%TITLE%", title)
                            .replace("%ARTIST%",artist)
                            .replace("%SONG%",Song)
                            .replace("%GENRE%",genre)
                            .replace("%CHANNEL%",channel);

                }
                return objectList;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            Log.e("ServiceHandler", "No data received from HTTP Request");
            return null;
        }
    }

    void setLoadHtml1(){
        boolean loadhtml = true;
        //do the myWebView stuff here
        if (loadhtml) {
            firstime = true;
            myWebView.loadDataWithBaseURL("file:///android_asset/",html,"text/html","utf-8",null);
            songtimer();
            startcd(myWebView);
        }
    }
    void setLoadHtml2(boolean loadhtml){
        loadhtml = true;
        //do the myWebView stuff here
        if (loadhtml) {
            firstime = true;
            myWebView.loadDataWithBaseURL("file:///android_asset/",html,"text/html","utf-8",null);

            songtimer();
            startcd(myWebView);
        }
    }

    void songtimer(){
        System.out.println(seconds);
        timercancel = true;
        songcountdown = new CountDownTimer(seconds,1000) {
            public void onTick(long millisUntilFinished) {
System.out.println(millisUntilFinished);
            }

            public void onFinish() {
                System.out.println("finished");
                if (html2Bool){
                    setLoadHtml2(true);
                }
                new GetJSON().execute();
            }
        };
    }
    public void startcd(View v) {
        System.out.println("start");
        songcountdown.start();
    }

    public void stopcd(View v) {
        System.out.println("cancel");
        songcountdown.cancel();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e){
        super.dispatchTouchEvent(e);
        return mGestureDetector.onTouchEvent(e);
    }

    class CustomGestureDetector implements GestureDetector.OnGestureListener,
            GestureDetector.OnDoubleTapListener {
        Animation slideLeftAnim = AnimationUtils.loadAnimation(getBaseContext (), R.anim.righttoleft_out);
        Animation slideRightAnim = AnimationUtils.loadAnimation(getBaseContext(), R.anim.lefttoright_out);

        private static final int SWIPE_MIN_DISTANCE = 220;
        private static final int SWIPE_MAX_OFF_PATH = 250;
        private static final int SWIPE_THRESHOLD_VELOCITY = 200;

        public boolean onSingleTapConfirmed(MotionEvent e) {
            Log.d(TAG, "onSingleTapConfirmed =)");
            final String weburl2 = myWebView.getUrl();
            final Toolbar myToolbar2 = (Toolbar) findViewById(R.id.toolbar2);
            new CountDownTimer(5000, 1000){
                public void onTick(long millisUntilFinished){

                }
                public void onFinish(){
                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE &&
                            html.contains("Song")){
                        myToolbar2.setVisibility(GONE);
                    }
                }
            }.start();

            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE &&
                    html.contains("Song"))
            {
                myToolbar2.setVisibility(View.VISIBLE);
            }
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.d(TAG, "onDoubleTap =)");
            return true;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            Log.d(TAG, "onDoubleTapEvent =)");
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            Log.d(TAG, "onDown =)");

            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            Log.d(TAG, "onShowPress =)");
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Log.d(TAG, "onSingleTapUp =)");
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.d(TAG, "onScroll");
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            Toast.makeText(MainActivity.this, "",
                    Toast.LENGTH_LONG).show();
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.d(TAG, "onFling " + e1.getX() +" - " + e2.getX());
            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                return false;

            if ((e1.getX() - e2.getX() < -SWIPE_MIN_DISTANCE) &&
            (Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) &&
                    (!url.equals("http://edm-shuffle.com"))) {
                Log.d(TAG, "Left to Right swipe performed");
                slideRightAnim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation arg0) {
                        prevpage = true;
                    }

                    @Override
                    public void onAnimationEnd(Animation arg0) {
                        myWebView.setVisibility(View.INVISIBLE);
                        pageProgress = new ProgressDialog(MainActivity.this);
                        pageProgress.setMessage("Loading");
                        pageProgress.setCancelable(false);
                        pageProgress.hide();
                        System.out.println (html);
                    }

                    @Override
                    public void onAnimationRepeat(Animation arg0) {
                    }
                });
                myWebView.startAnimation(slideRightAnim);
            }

            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                return false;
            if((e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE) &&
                    (Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) &&
                    (myWebView.getUrl().equals("about:blank")))
            {
                Log.d(TAG, "Right to Left swipe performed");
                slideLeftAnim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation arg0) {
                       newpage = true;
                    }
                    @Override
                    public void onAnimationRepeat(Animation arg0) {
                    }
                    @Override
                    public void onAnimationEnd(Animation arg0) {
                        myWebView.setVisibility(View.INVISIBLE);
                        pageProgress = new ProgressDialog(MainActivity.this);
                        pageProgress.setMessage("Loading");
                            pageProgress.setCancelable(false);
                            pageProgress.hide();
                    }
                });
                if (timercancel) {
                    stopcd(myWebView);
                }
                if (html2Bool) {
                    setLoadHtml2(true);
                }
                new GetJSON().execute();
                myWebView.startAnimation(slideLeftAnim);
            }

            if (e1.getY() < e2.getY()) {
                Log.d(TAG, "Up to Down swipe performed");
            }

            if (e1.getY() > e2.getY()) {
                Log.d(TAG, "Down to Up swipe performed");
            }
            return true;
        }
    }
    private GestureDetector mGestureDetector;

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        //Hiding toolbars when orientation changed to landscape.
        super.onConfigurationChanged(newConfig);
        Toolbar myToolbar2 = (Toolbar) findViewById(R.id.toolbar2);
        String weburl = myWebView.getUrl();

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getSupportActionBar().hide();
            myToolbar2.setVisibility(GONE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            getSupportActionBar().show();
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            {
//                if(!html.contains("Song"))
//                {
//                    myToolbar2.setVisibility(GONE);
//                }
//                else{
//                    myToolbar2.setVisibility(View.GONE);
//                }
            }

        }

    }
    @Override
    public boolean onSupportNavigateUp(){
        if( myWebView.canGoBack()){
            myWebView.goBack();
        }
        else
        finish();


        // or call finish()
        return true;
    }

//    private void injectCSS() {
//                try {
//                    InputStream inputStream = getAssets().open("style.css");
//                    byte[] buffer = new byte[inputStream.available()];
//                    inputStream.read(buffer);
//                    inputStream.close();
//                    String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);
//                    myWebView.loadUrl("javascript:(function() {" +
//                            "var parent = document.getElementsByTagName('head').item(0);" +
//                            "var style = document.createElement('style');" +
//                            "style.type = 'text/css';" +
//                            // Tell the browser to BASE64-decode the string into your script
//                            "style.innerHTML = window.atob('" + encoded + "');" +
//                            "parent.appendChild(style)" +
//                            "})()");
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem itemlogin = menu.findItem(R.id.action_settings);
        MenuItem itemregister = menu.findItem(R.id.action_settings2);
        MenuItem itemlogout = menu.findItem(R.id.action_settings9);
        MenuItem itemprofile = menu.findItem(R.id.action_settings10);

        if (logged)
        {
            itemlogin.setVisible(false);
            itemregister.setVisible(false);
            itemlogout.setVisible(true);
            itemprofile.setVisible(true);
        }
        else{
            itemlogin.setVisible(true);
            itemregister.setVisible(true);
            itemlogout.setVisible(false);
            itemprofile.setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbarmenu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI...
                myWebView.loadUrl("http://edm-shuffle.com/user");
                return true;
            case R.id.action_settings9:
                myWebView.loadUrl("http://edm-shuffle.com/user/logout");
                logged = false;
                return true;
            case R.id.action_settings2:
                myWebView.loadUrl("http://edm-shuffle.com/user/register");
                return true;
            case R.id.action_settings10:
                myWebView.loadUrl("http://edm-shuffle.com/user");
                return true;
            case R.id.action_settings3:
                myWebView.loadUrl("http://edm-shuffle.com/search");
                return true;
            case R.id.action_settings7:
                myWebView.loadUrl("http://edm-shuffle.com/music/privacy-policy");
                return true;
            case R.id.action_settings8:
                myWebView.loadUrl("http://edm-shuffle.com/music/copyrightlegal");
                return true;

            case R.id.action_menu:
                // User chose the "Radio" item, show radio menu.
                return true;
            case R.id.action_menu2:
                if (!url.equals("http://edm-shuffle.com/service/popular")){
                    url = "http://edm-shuffle.com/service/popular";
                    html2Bool = false;
                    firstime = false;
                }
                if (timercancel){
                    stopcd(myWebView);
                }
                if (html2Bool){
                    setLoadHtml2(true);
                }
                new GetJSON().execute();
                if (!firstime){
                    new GetJSON().execute();

                }
                return true;
            case R.id.action_menu3:
                if (!url.equals("http://edm-shuffle.com/service/allplus")){
                    url = "http://edm-shuffle.com/service/allplus";
                    html2Bool = false;
                    firstime = false;
                }
                if (timercancel){
                    stopcd(myWebView);
                }
                if (html2Bool){
                    setLoadHtml2(true);
                }
                new GetJSON().execute();
                if (!firstime){
                    new GetJSON().execute();

                }
                return true;
            case R.id.action_menu4:
                if (!url.equals("http://edm-shuffle.com/service/electro")){
                    url = "http://edm-shuffle.com/service/electro";
                    html2Bool = false;
                    firstime = false;
                }
                if (timercancel){
                    stopcd(myWebView);
                }
                if (html2Bool){
                    setLoadHtml2(true);
                }
                new GetJSON().execute();
                if (!firstime){
                    new GetJSON().execute();

                }
                return true;
            case R.id.action_menu5:
                if (!url.equals("http://edm-shuffle.com/service/drumandbass")){
                    url = "http://edm-shuffle.com/service/drumandbass";
                    html2Bool = false;
                    firstime = false;
                }
                if (timercancel){
                    stopcd(myWebView);
                }
                if (html2Bool){
                    setLoadHtml2(true);
                }
                new GetJSON().execute();
                if (!firstime){
                    new GetJSON().execute();

                }
                return true;
            case R.id.action_menu6:
                if (!url.equals("http://edm-shuffle.com/service/trance")){
                    url = "http://edm-shuffle.com/service/trance";
                    html2Bool = false;
                    firstime = false;
                }
                if (timercancel){
                    stopcd(myWebView);
                }
                if (html2Bool){
                    setLoadHtml2(true);
                }
                new GetJSON().execute();
                if (!firstime){
                    new GetJSON().execute();

                }
                return true;
            case R.id.action_menu7:
                if (!url.equals("http://edm-shuffle.com/service/house")){
                    url = "http://edm-shuffle.com/service/house";
                    html2Bool = false;
                    firstime = false;
                }
                if (timercancel){
                    stopcd(myWebView);
                }
                if (html2Bool){
                    setLoadHtml2(true);
                }
                new GetJSON().execute();
                if (!firstime){
                    new GetJSON().execute();

                }
                return true;
            case R.id.action_menu8:
                if (!url.equals("http://edm-shuffle.com/service/dubstep")){
                    url = "http://edm-shuffle.com/service/dubstep";
                    html2Bool = false;
                    firstime = false;
                }
                if (timercancel){
                    stopcd(myWebView);
                }
                if (html2Bool){
                    setLoadHtml2(true);
                }
                new GetJSON().execute();
                if (!firstime){
                    new GetJSON().execute();

                }
                return true;
            case R.id.action_menu9:
                if (!url.equals("http://edm-shuffle.com/service/drumstep")){
                    url = "http://edm-shuffle.com/service/drumstep";
                    html2Bool = false;
                    firstime = false;
                }
                if (timercancel){
                    stopcd(myWebView);
                }
                if (html2Bool){
                    setLoadHtml2(true);
                }
                new GetJSON().execute();
                if (!firstime){
                    new GetJSON().execute();

                }
                return true;
            case R.id.action_menu10:
                if (!url.equals("http://edm-shuffle.com/service/bounce")){
                    url = "http://edm-shuffle.com/service/bounce";
                    html2Bool = false;
                    firstime = false;
                }
                if (timercancel){
                    stopcd(myWebView);
                }
                if (html2Bool){
                    setLoadHtml2(true);
                }
                new GetJSON().execute();
                if (!firstime){
                    new GetJSON().execute();

                }
                return true;
            case R.id.action_menu11:
                if (!url.equals("http://edm-shuffle.com/service/chill")){
                    url = "http://edm-shuffle.com/service/chill";
                    html2Bool = false;
                    firstime = false;
                }
                if (timercancel){
                    stopcd(myWebView);
                }
                if (html2Bool){
                    setLoadHtml2(true);
                }
                new GetJSON().execute();
                if (!firstime){
                    new GetJSON().execute();

                }
                return true;
            case R.id.action_menu12:
                if (!url.equals("http://edm-shuffle.com/service/glitch")){
                    url = "http://edm-shuffle.com/service/glitch";
                    html2Bool = false;
                    firstime = false;
                }
                if (timercancel){
                    stopcd(myWebView);
                }
                if (html2Bool){
                    setLoadHtml2(true);
                }
                new GetJSON().execute();
                if (!firstime){
                    new GetJSON().execute();

                }
                return true;
            case R.id.action_menu13:
                if (!url.equals("http://edm-shuffle.com/service/trap")){
                    url = "http://edm-shuffle.com/service/trap";
                    html2Bool = false;
                    firstime = false;
                }
                if (timercancel){
                    stopcd(myWebView);
                }
                if (html2Bool){
                    setLoadHtml2(true);
                }
                new GetJSON().execute();
                if (!firstime){
                    new GetJSON().execute();

                }
                return true;
            case R.id.action_menu14:
                if (!url.equals("http://edm-shuffle.com/service/futurebass")){
                    url = "http://edm-shuffle.com/service/futurebass";
                    html2Bool = false;
                    firstime = false;
                }
                if (timercancel){
                    stopcd(myWebView);
                }
                if (html2Bool){
                    setLoadHtml2(true);
                }
                new GetJSON().execute();
                if (!firstime){
                    new GetJSON().execute();

                }
                return true;
            case R.id.action_menu15:
                if (!url.equals("http://edm-shuffle.com/service/funk")){
                    url = "http://edm-shuffle.com/service/funk";
                    html2Bool = false;
                    firstime = false;
                }
                if (timercancel){
                    stopcd(myWebView);
                }
                if (html2Bool){
                    setLoadHtml2(true);
                }
                new GetJSON().execute();
                if (!firstime){
                    new GetJSON().execute();

                }
                return true;
            case R.id.action_menu16:
                if (!url.equals("http://edm-shuffle.com/service/japanese")){
                    url = "http://edm-shuffle.com/service/japanese";
                    html2Bool = false;
                    firstime = false;
                }
                if (timercancel){
                    stopcd(myWebView);
                }
                if (html2Bool){
                    setLoadHtml2(true);
                }
                new GetJSON().execute();
                if (!firstime){
                    new GetJSON().execute();

                }
                return true;
            case R.id.action_menu17:
                if (!url.equals("http://edm-shuffle.com/service/hardstyle")){
                    url = "http://edm-shuffle.com/service/hardstyle";
                    html2Bool = false;
                    firstime = false;
                }
                if (timercancel){
                    stopcd(myWebView);
                }
                if (html2Bool){
                    setLoadHtml2(true);
                }
                new GetJSON().execute();
                if (!firstime){
                    new GetJSON().execute();

                }
                return true;
            case R.id.action_menu18:
                if (!url.equals("http://edm-shuffle.com/service/miscjson")){
                    url = "http://edm-shuffle.com/service/miscjson";
                    html2Bool = false;
                    firstime = false;
                }
                if (timercancel){
                    stopcd(myWebView);
                }
                if (html2Bool){
                    setLoadHtml2(true);
                }
                new GetJSON().execute();
                if (!firstime){
                    new GetJSON().execute();

                }
                return true;
            case R.id.action_menu19:
                if (!url.equals("http://edm-shuffle.com/service/bigroom")){
                    url = "http://edm-shuffle.com/service/bigroom";
                    html2Bool = false;
                    firstime = false;
                }
                if (timercancel){
                    stopcd(myWebView);
                }
                if (html2Bool){
                    setLoadHtml2(true);
                }
                new GetJSON().execute();
                if (!firstime){
                    new GetJSON().execute();

                }
                return true;
            case R.id.action_menu20:
                if (!url.equals("http://edm-shuffle.com/service/synchronised")){
                    url = "http://edm-shuffle.com/service/synchronised";
                    html2Bool = false;
                    firstime = false;
                }
                if (timercancel){
                    stopcd(myWebView);
                }
                if (html2Bool){
                    setLoadHtml2(true);
                }
                new GetJSON().execute();
                if (!firstime){
                    new GetJSON().execute();

                }

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }
    private class AsyncLogin extends AsyncTask<String, Integer, String> {
        ProgressDialog pdLoading = new ProgressDialog(MainActivity.this);
        HttpURLConnection conn;
        URL url = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //Method running on UI thread
            pdLoading.setMessage("\tLoading...");
            pdLoading.setCancelable(false);
            pdLoading.hide();
        }

        @Override
        protected String doInBackground(String... params) {
            try {

                // Enter URL address to php file
                url = new URL("http://edm-shuffle.com/?mobileview");

            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return "exception";
            }
            try {
                // Setup HttpURLConnection class to send and receive data from php and mysql
                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(15000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("POST");

                // setDoInput and setDoOutput method depict handling of both send and receive
                conn.setDoInput(true);
                conn.setDoOutput(true);

            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
                return "exception";
            }

            try {

                int response_code = conn.getResponseCode();

                StringBuilder builder2 = new StringBuilder();
                builder2.append(conn.getResponseCode())
                        .append(" ")
                        .append(conn.getResponseMessage())
                        .append("\n");

                Map<String, List<String>> map = conn.getHeaderFields();
                for (Map.Entry<String, List<String>> entry : map.entrySet())
                {
                    if (entry.getKey() == null)
                        continue;
                    builder2.append( entry.getKey())
                            .append(": ");

                    List<String> headerValues = entry.getValue();
                    Iterator<String> it = headerValues.iterator();
                    if (it.hasNext()) {
                        builder2.append(it.next());

                        while (it.hasNext()) {
                            builder2.append(", ")
                                    .append(it.next());
                        }
                    }

                    builder2.append("\n");
                }
                System.out.println(builder2);

                // Check if successful connection made
                if (response_code == HttpURLConnection.HTTP_OK) {

                    // Read data sent from server
                    InputStream input = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                        System.out.print(line);
                        String[] str1 = line.split("html front");
                        for(int i=1; i<str1.length; i++) {
                        System.out.println(str1[i]);
                        }

                }

                    // Pass data to onPostExecute method
                    return (result.toString());

                } else {

                    return ("unsuccessful");
                }

            } catch (IOException e) {
                e.printStackTrace();
                return "exception";
            } finally {
                conn.disconnect();
            }

        }
        protected void onProgressUpdate(Integer... progress) {

        }
            @Override
        protected void onPostExecute(String result) {

            //Method running on UI thread
                System.out.println(result);
                pdLoading.dismiss();
        }
    }
}



