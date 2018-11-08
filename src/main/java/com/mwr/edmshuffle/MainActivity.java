package com.mwr.edmshuffle;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import static com.mwr.edmshuffle.R.layout.activity_main;

public class MainActivity extends AppCompatActivity {

    WebView myWebView;
    Bundle myWebViewBundle;
    boolean logged;
    boolean newpage;
    boolean prevpage;
    boolean loadhtml;
    boolean firstime;
    boolean timercancel;
    boolean html2Bool;
    private GestureDetector mGestureDetector;
    private String html;
    private int seconds;
    private CountDownTimer songcountdown;
    private ProgressDialog pageProgress;
    // URL to get JSON
    private String url = "";
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
    // onfling log tag.
    private static final String TAG = "onfling";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //On creation of the activity
        super.onCreate(savedInstanceState);
        setContentView(activity_main);
        //Upper toolbar.
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        if(getSupportActionBar() != null){
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        CustomGestureDetector customGestureDetector = new CustomGestureDetector();
        mGestureDetector = new GestureDetector(this, customGestureDetector);
        // Attach listeners that'll be called for double-tap and related gestures
        mGestureDetector.setOnDoubleTapListener(customGestureDetector);

        myWebView = (WebView) findViewById(R.id.EDM);
        myWebViewBundle = new Bundle();
        WebSettings myWebSettings = myWebView.getSettings();
        myWebSettings.setJavaScriptEnabled(true);
        myWebView.addJavascriptInterface(new WebAppInterface(this), "Android");
        myWebSettings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:61.0) Gecko/20100101 Firefox/61.0");
        myWebSettings.setMediaPlaybackRequiresUserGesture(false);
        myWebView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                //Receiving cookies to use to check state of login
                String cookies = CookieManager.getInstance().getCookie(url);
                if (cookies != null) {
                    System.out.println(cookies);
                    if (cookies.contains("DRUPAL_UID=") && !cookies.contains("DRUPAL_UID=-1")) {
                        logged = true;
                        System.out.print("You're logged in");
                    } else {
                        logged = false;
                        System.out.print("You're logged out");
                    }
                }
                String weburl = myWebView.getUrl();
                System.out.println(weburl);
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

    @Override
    protected void onUserLeaveHint(){
        if (timercancel){
            stopcd(myWebView);
        }
        super.onUserLeaveHint();
    }
    @Override
    public void onBackPressed(){
        if (myWebView.canGoBack()){
            if (timercancel){
                stopcd(myWebView);
            }
            myWebView.goBack();
        }
        else {
            super.onBackPressed();
        }
    }
    public class WebAppInterface {
        Context mContext;

        // Instantiate the interface and set the context
        WebAppInterface(Context c) {
            mContext = c;
        }

        // Show a toast from the web page
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

    //Async task class to get json by making HTTP call
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
            String jsonStr = webreq.makeWebServiceCall(url, WebRequest.GETRequest,null);
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
                loadhtml = true;
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

    private void lefttoright(){
        Animation slideRightAnimIn = AnimationUtils.loadAnimation(getBaseContext(), R.anim.lefttoright_in);
        myWebView.setVisibility(View.VISIBLE);
        myWebView.startAnimation(slideRightAnimIn);
        pageProgress.dismiss();
    }

    private void righttoleft(){
        Animation slideLeftAnimIn = AnimationUtils.loadAnimation(getBaseContext(), R.anim.righttoleft_in);
        myWebView.setVisibility(View.VISIBLE);
        myWebView.startAnimation(slideLeftAnimIn);
        pageProgress.dismiss();
    }

    private ArrayList<HashMap<String, String>> ParseJSON(String json) {
        if (json != null) {
            try {
// Hashmap for ListView
                ArrayList<HashMap<String, String>> objectList = new ArrayList<>();
                JSONObject jsonObj = new JSONObject(json);
// Getting JSON Array node
                JSONArray musicfeed = jsonObj.getJSONArray(TAG_MUSICFEED);
                System.out.println(musicfeed + "musicfeed");

// Looping through all objects
                for (int i = 0; i < musicfeed.length(); i++) {
                    JSONObject c = musicfeed.getJSONObject(i);

                    // JSON object items
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

                    //String manipulation of the video url and length of video.
                    String embedurl = fullurl.replace("watch?v=", "embed/") +
                            "?&#10;&#10;autoplay=1&amp;feature=player_detailpage&amp;VQ=HD1080&amp;wmode=transparent&amp;iv_load_policy=3\"frameborder=\"0\" style=\"pointer-events:none;\"";
                    String number = length.replaceAll("\\D+", "");
                    seconds = 3000 + Integer.parseInt(number + "000");

                    //Parsing the values to html file
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

    private void setLoadHtml1(){
        //do the myWebView stuff here
        if (loadhtml) {
            firstime = true;
            myWebView.loadDataWithBaseURL("file:///android_asset/",html,"text/html","utf-8",null);
            songtimer();
            startcd(myWebView);
        }
    }
    private void setLoadHtml2(boolean loadhtml){
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
    private void startcd(View v) {
        System.out.println("start");
        songcountdown.start();
    }

    private void stopcd(View v) {
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
            Log.d(TAG, "onSingleTapConfirmed");
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.d(TAG, "onDoubleTap");
            return true;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            Log.d(TAG, "onDoubleTapEvent");
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            Log.d(TAG, "onDown");
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            Log.d(TAG, "onShowPress");
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Log.d(TAG, "onSingleTapUp");
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.d(TAG, "onScroll");
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            Log.d(TAG, "onLongPress");
            Toast.makeText(MainActivity.this, "onLongPressToast",
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
            if((e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE) && (Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) &&
                    (myWebView.getUrl().equals("about:blank"))) {
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

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        //Hiding toolbars when orientation changed to landscape.
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getSupportActionBar().hide();
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            getSupportActionBar().show();
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    public boolean onSupportNavigateUp(){
        if( myWebView.canGoBack()){
            if (timercancel){
                stopcd(myWebView);
            }
            myWebView.goBack();
        }
        else{
        if (timercancel){
            stopcd(myWebView);
        }
        finish();
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem itemlogin = menu.findItem(R.id.action_settings);
        MenuItem itemregister = menu.findItem(R.id.action_settings2);
        MenuItem itemlogout = menu.findItem(R.id.action_settings9);
        MenuItem itemprofile = menu.findItem(R.id.action_settings10);
        //Changing menu items if logged in
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
                // User chose the "Settings" item, show the app settings menu.
                myWebView.loadUrl("http://edm-shuffle.com/user");
                if (timercancel){
                    stopcd(myWebView);
                }
                return true;

            case R.id.action_settings9:
                myWebView.loadUrl("http://edm-shuffle.com/user/logout");
                logged = false;
                if (timercancel){
                    stopcd(myWebView);
                }
                return true;

            case R.id.action_settings2:
                myWebView.loadUrl("http://edm-shuffle.com/user/register");
                if (timercancel){
                    stopcd(myWebView);
                }
                return true;

            case R.id.action_settings10:
                myWebView.loadUrl("http://edm-shuffle.com/user");
                if (timercancel){
                    stopcd(myWebView);
                }
                return true;

            case R.id.action_settings3:
                myWebView.loadUrl("http://edm-shuffle.com/search");
                if (timercancel){
                    stopcd(myWebView);
                }
                return true;

            case R.id.action_settings7:
                myWebView.loadUrl("http://edm-shuffle.com/music/privacy-policy");
                if (timercancel){
                    stopcd(myWebView);
                }
                return true;

            case R.id.action_settings8:
                myWebView.loadUrl("http://edm-shuffle.com/music/copyrightlegal");
                if (timercancel){
                    stopcd(myWebView);
                }
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
                return super.onOptionsItemSelected(item);
        }
    }
}
