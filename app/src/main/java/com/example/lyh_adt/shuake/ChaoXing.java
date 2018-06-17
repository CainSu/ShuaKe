package com.example.lyh_adt.shuake;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.os.Binder;
import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.telecom.Call;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import javax.security.auth.callback.Callback;

/**
 * Created by 10439 on 2018/3/21/0021.
 */

public class ChaoXing extends Service implements Serializable {
    private Bitmap bitmap=null;//验证码
    private String username=null;//用户名
    private CookieManager cookieManager = new CookieManager( null, CookiePolicy.ACCEPT_ALL );
    private boolean flag=false;
    private String missionList;
    private String myCookies;
    private ArrayList<String> courseLinks;
    private int startindex;
    private String url,duration,newplayingTime,clazzid,userid,jobid,objectid,dtoken,courseId,knowledgeid,otherInfo;
    private PowerManager.WakeLock wakeLock;
    private String answerString=null;

    public ChaoXing(){
        super();
        CookieHandler.setDefault(cookieManager);
    }

    @Override
    public IBinder onBind(Intent intent){
        return null;
    }
    @Override
    public void onCreate(){
        super.onCreate();

        Intent shuaIntent = new Intent(this,ShuaActivity.class);
        Log.i("ADT","SDK"+Build.VERSION.SDK_INT);

        if(Build.VERSION.SDK_INT>26) {
            NotificationChannel notificationChannel = null;
            notificationChannel = new NotificationChannel("ChaoXingShuaKeid", "ShuaKeChaoXing", NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(notificationChannel);
        }

        PendingIntent shuaPendingIntent = PendingIntent.getActivity(this,0,shuaIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this,"ChaoXingShuaKeid")
        .setAutoCancel(false)
        .setOngoing(true)
        .setSmallIcon(R.mipmap.pikaqiu_foreground)
        .setContentTitle("超星刷课")
        .setContentText("正在运行...")
        .setContentIntent(shuaPendingIntent)
        .build();

        startForeground(1,notification);
        //manager.notify(1,notification);
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag");
        wakeLock.acquire();
    }

    @Override
    public int onStartCommand(Intent intent,int flag,int startId){
        Log.i("ADT","onStartCommand");

        if(intent.getAction().equals("init")){
            this.flag=false;
            Bundle bd = intent.getExtras();
            myCookies=bd.getString("myCookies");
            courseLinks=(ArrayList<String>)bd.getSerializable("courseLinks");
            startindex=bd.getInt("startIndex");
            startshua(startindex,myCookies,courseLinks);
        }
        else if(intent.getAction().equals("playvideo")){
            new Thread(){
                public void run(){playVideo();}
            }.start();
        }

        return super.onStartCommand(intent,flag,startId);
    }

    @Override
    public void onDestroy(){
        flag = true;
        stopForeground(STOP_FOREGROUND_REMOVE);
        wakeLock.release();
        super.onDestroy();
        Log.i("ADT","onDestory");
    }

    public Bitmap getValocde() {
        Log.i("ADT","获取验证码图片");
        //获取验证码图片
         new Thread(){
            public void run() {
                try {
                    HttpURLConnection getvalcode =(HttpURLConnection)  new URL("http://passport2.chaoxing.com/num/code?1515388254551").openConnection();
                    getvalcode.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
                    getvalcode.setRequestMethod("GET");
                    if (getvalcode.getResponseCode() == 200){
                        InputStream is = getvalcode.getInputStream();
                        bitmap = BitmapFactory.decodeStream(is);
                        Log.i("ADT","Cookie是空的吗？"+cookieManager.getCookieStore().getCookies().toString());
                        getvalcode.disconnect();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
        Log.i("ADT","返回Bitmap");
        while (bitmap == null);
        return bitmap;
    }

    public String login(final String usernumber, final String paswd, final String valcode){
        new Thread(){
            public void run(){
                String msg="";
                try{
                    HttpURLConnection postlogin = (HttpURLConnection)new URL("http://passport2.chaoxing.com/login?refer=http%3A%2F%2Fi.mooc.chaoxing.com").openConnection();
                    postlogin.setRequestMethod("POST");
                    postlogin.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
                    postlogin.setRequestProperty("Referer","http://passport2.chaoxing.com/login?refer=http%3A%2F%2Fi.mooc.chaoxing.com");
                    postlogin.setRequestProperty("Origin","http://passport2.chaoxing.com");
                    postlogin.setConnectTimeout(5000);
                    postlogin.setDoOutput(true);
                    postlogin.setUseCaches(false);
                    StringBuffer params = new StringBuffer();
                    params.append("refer_0x001=http%3A%2F%2Fi.mooc.chaoxing.com").append("&")
                            .append("pid=-1").append("&")
                            .append("pidName=").append("&")
                            .append("fid=1867").append("&")
                            .append("fidName=南华大学").append("&")
                            .append("allowJoin=0").append("&")
                            .append("isCheckNumCode=1").append("&")
                            .append("f=0").append("&")
                            .append("productid=").append("&")
                            .append("verCode=").append("&")
                            .append("uname=").append(usernumber).append("&")
                            .append("password=").append(paswd).append("&")
                            .append("numcode=").append(valcode);

                    byte[] loginbytes = params.toString().getBytes();
                    postlogin.getOutputStream().write(loginbytes);


                    if (postlogin.getResponseCode()==200)
                    {
                        postlogin.disconnect();
                        HttpURLConnection getspace = (HttpURLConnection)new URL("http://i.mooc.chaoxing.com/space/index").openConnection();
                        getspace.setRequestMethod("GET");
                        getspace.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
                        if (getspace.getResponseCode()==200)
                        {
                            InputStream is = getspace.getInputStream();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                            StringBuilder sb = new StringBuilder();
                            String line = null;
                            while ((line = reader.readLine()) != null) {
                                sb.append(line + "\n");
                            }
                            is.close();
                            getspace.disconnect();
                            msg = sb.toString();

                            Matcher m = Pattern.compile("<p class=\"personalName\" title=\"([\\u4e00-\\u9fa5]*)\"").matcher(msg);
                            if (m.find()){
                                username=m.group(1);
                            }
                            else{
                                username="0";
                            }


                    }
                    //Log.i("ADT","Cookie是空的吗？"+cookieManager.getCookieStore().getCookies().toString());
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }

            };
        }.start();
        while (username==null);
        return username;
    }

    public void getCourseList(final String myCookies){
        final LinkedList<String> courseLinks = new LinkedList<String>();
        final LinkedList<String> courseList = new LinkedList<String>();
        Map<String,LinkedList<String>> map = new HashMap<String,LinkedList<String>>();
        Log.i("ADT","getCourseList's flag=="+flag);
        //final LinkedList<String> CourseList=null;


        new Thread(){
            public void run(){

                try{
                    HttpURLConnection getCourseList = (HttpURLConnection)new URL("http://mooc1-2.chaoxing.com/visit/interaction").openConnection();
                    getCourseList.setRequestMethod("GET");
                    getCourseList.addRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
                    getCourseList.addRequestProperty("Referer", "http://i.mooc.chaoxing.com/space/index");

                    getCourseList.addRequestProperty("Cookie",myCookies);

                    String resp = getResponeText(getCourseList);

                    getCourseList.disconnect();

//                    if (getCourseList.getResponseCode() == 200){
//                        InputStream is = getCourseList.getInputStream();
//                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
//                        StringBuilder sb = new StringBuilder();
//                        String line = null;
//                        while ((line = reader.readLine()) != null) {
//                            //Log.i("ADT","line="+line);
//                            sb.append(line + "\n");
//                        }
//                        is.close();
//                        getCourseList.disconnect();
//                        String resp = sb.toString();
//                        //Log.i("ADT","resp="+sb.toString());
//                        //Log.i("ADT","resp="+resp);



                    Matcher m = Pattern.compile("<div class=\"Mconright httpsClass\">[\\n\\s]*<h3 class=\"clearfix\" >[\\n\\s]*<a  href='([\\w\\/?=&]*)'[\\n\\s]*target=\"_blank\" title=\"([\\u4e00-\\u9fa5()（）\\w-]*)\">").matcher(resp);
                    while (m.find()){
                        //Log.i("ADT","total:"+m.groupCount());
                        Log.i("ADT","link:"+m.group(1)+"title:"+m.group(2));
                        courseLinks.add("https://mooc1-2.chaoxing.com"+m.group(1));
                        courseList.add(m.group(2));
                    }
                    Bundle bd = new Bundle();
                    bd.putSerializable("CourseList",courseList);
                    bd.putSerializable("CourseLinks",courseLinks);

                    Message msg = new Message();
                    msg.what=3;
                    msg.setData(bd);
                    ShuaActivity.handler.sendMessage(msg);
                    //flag = true;
                  //}

                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        }.start();

//        while(flag == false);
//        flag = false;
//        map.put("courseList",courseList);
//        map.put("courseLinks",courseLinks);
    }

    public String getCoookies(){
        List<URI> uri = cookieManager.getCookieStore().getURIs();
        List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
        Log.i("ADT","URIString="+uri.toString());
        Log.i("ADT","newCookies"+cookies.toString());

        return cookies.toString().substring(1,cookies.toString().lastIndexOf(']')).replace(',',';');

    }

    public void startshua(final int m,final String myCookies,final ArrayList<String> courseLinks){

        if(flag)return;
        new Thread(){
            @Override
            public void run(){
                message("开始",1);
                //进入章节目录寻找未完成的任务
                Log.i("ADT","courseLinsk is Empty"+courseLinks.isEmpty());
                message("courseLinsk is Empty=="+courseLinks.isEmpty(),1);
                if(courseLinks.isEmpty())return;

                Map<String,String> mission = findmission(courseLinks.get(startindex),myCookies);
                if(mission.get("link")==null){
                    message("startindex+1",1);
                    startindex+=1;
                    if(startindex>=courseLinks.size()){
                        message("结束",2);
                        stopSelf();
                        return;
                    }
                    mission = findmission(courseLinks.get(startindex),myCookies);
                }
                if(mission.get("link")!=null&&!flag) {
                    missionList = mission.get("list");
                    play(mission.get("link"),myCookies,"0");
                }
            }
        }.start();
    }

    private Map<String,String> findmission(final String url,final String myCookies){
        Map<String,String> mission = new HashMap<String,String>();
                try {
                    HttpURLConnection getMission = (HttpURLConnection)new URL(url).openConnection();
                    getMission.setRequestMethod("GET");
                    getMission.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
                    getMission.setRequestProperty("Referer","http://mooc1-2.chaoxing.com/visit/interaction");
                    getMission.setRequestProperty("Cookie",myCookies);
                    getMission.setConnectTimeout(5000);

//                    if (getMission.getResponseCode() == 200) {
//                        InputStream is = getMission.getInputStream();
//                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
//                        StringBuilder sb = new StringBuilder();
//                        String line = null;
//                        while ((line = reader.readLine()) != null) {
//                            //Log.i("ADT", line);
//                            sb.append(line);
//                        }
//                        is.close();
//                        getMission.disconnect();
//                        String resp = sb.toString();

                    String resp = getResponeText(getMission);
                    getMission.disconnect();

                    //Log.i("ADT","findmissionresp="+resp);
                    Matcher m = Pattern.compile("<em class=\"orange\">(\\d)</em>[\\s\\n]*</span>[\\s\\n]*<span class=\"articlename\">[\\s\\n\\d.]*<a href='([\\/\\w?=&;]+)' title=\"(.+?)\"\\s*>").matcher(resp);
                    if (m.find()){
                        Log.i("ADT","missioncount="+m.group(1)+" link:"+m.group(2)+" title:"+m.group(3).substring(0,25));
                        mission.put("list",m.group(3).replaceAll("[^\\u4e00-\\u9fa5]",""));
                        mission.put("link",m.group(2));
                    }
                    //}

                }catch (Exception e){
                    e.printStackTrace();
                }
        return mission;

    }

    private void play(final String url,final String myCookies,final String num){

        Log.i("ADT","url="+url);
        try{

            //获取chapterid、courseid、clazzid
            Matcher m = Pattern.compile("chapterId=(\\d+)&courseId=(\\d+)&clazzid=(\\d+)").matcher(url);
            m.find();
            final String knowledgeid = m.group(1);
            final String courseId = m.group(2);
            final String clazzid = m.group(3);


            //获取objectid，jobid，otherinfo，fid
            HttpURLConnection gettovideo = (HttpURLConnection) new URL("https://mooc1-2.chaoxing.com/knowledge/cards?clazzid=" + clazzid + "&courseid=" + courseId + "&knowledgeid=" + knowledgeid + "&num="+num+"&v=20160407-1").openConnection();
            gettovideo.setRequestMethod("GET");
            gettovideo.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
            gettovideo.setRequestProperty("Cookie", myCookies);
            gettovideo.setConnectTimeout(5000);

//            if (gettovideo.getResponseCode() == 200) {
//                InputStream is = gettovideo.getInputStream();
//                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
//                StringBuilder sb = new StringBuilder();
//                String line = null;
//                while ((line = reader.readLine()) != null) {
//                    //Log.i("ADT", "line=" + line);
//                    sb.append(line + "\n");
//                }
//                is.close();
//                gettovideo.disconnect();
//                String resp = sb.toString();

            String resp = getResponeText(gettovideo);
            gettovideo.disconnect();

            m = Pattern.compile("jobid\":\"(\\d+)").matcher(resp);
            if(m.find()){
                String jobid=m.group(1);
                m = Pattern.compile("otherInfo\":\"(\\w+)\"").matcher(resp);
                m.find();
                String otherInfo=m.group(1);
                m = Pattern.compile("objectid\":\"(\\w+)\"").matcher(resp);
                m.find();
                String objectid=m.group(1);
                m = Pattern.compile("fid\":\"(\\d+)").matcher(resp);
                m.find();
                String fid=m.group(1);
                m = Pattern.compile("userid\":\"(\\d+)\"").matcher(resp);
                m.find();
                String userid=m.group(1);

                Log.i("ADT","jobid="+jobid+" otherInfo="+otherInfo+" objectid="+objectid+" fid="+fid+" userid="+userid);

                //获取duration,dtoken
                HttpURLConnection getstatus = (HttpURLConnection)new URL("https://mooc1-2.chaoxing.com/ananas/status/"+objectid+"?k="+fid).openConnection();
                getstatus.setRequestMethod("GET");
                getstatus.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
                getstatus.setRequestProperty("Referer", "https://mooc1-2.chaoxing.com/ananas/modules/video/index.html?v=2018-0126-1905");
                getstatus.setRequestProperty("Cookie",myCookies);
                getstatus.setConnectTimeout(5000);

//                if (getstatus.getResponseCode() == 200) {
//                    is = getstatus.getInputStream();
//                    reader = new BufferedReader(new InputStreamReader(is));
//                    sb = new StringBuilder();
//                    while ((line = reader.readLine()) != null) {
//                        //Log.i("ADT", "line=" + line);
//                        sb.append(line + "\n");
//                    }
//                    is.close();
//                    getstatus.disconnect();
//                    resp = sb.toString();

                resp = getResponeText(getstatus);

                getstatus.disconnect();

                m = Pattern.compile("duration\":(\\d+),").matcher(resp);
                m.find();
                String duration = m.group(1);
                m = Pattern.compile("dtoken\":\"(\\w+)\"").matcher(resp);
                m.find();
                String dtoken = m.group(1);

                Log.i("ADT","duration="+duration);

                this.url=url;
                this.duration=duration;
                ShareHelper sp=new ShareHelper(getApplicationContext());
                this.newplayingTime=sp.readprogress(missionList);
                this.clazzid=clazzid;
                this.userid=userid;
                this.jobid=jobid;
                this.objectid=objectid;
                this.dtoken=dtoken;
                this.courseId=courseId;
                this.knowledgeid=knowledgeid;
                this.otherInfo=otherInfo;

                playVideo();
                //}
                }
                else{
                    Log.e("ADT","num=0 unfind video");
                    play(url,myCookies,"1");
                }

            //}
        }catch (Exception e){
                e.printStackTrace();
        }

    }

    private void playVideo(){
        String enc;
        HttpURLConnection getwork;
        enc = getenc(duration,newplayingTime,clazzid,userid,jobid,objectid);
        try{
            Log.i("ADT","enc="+enc);
            getwork = (HttpURLConnection)new URL("https://mooc1-2.chaoxing.com/multimedia/log/"+dtoken+"?userid="+userid + "&rt=0.9&jobid=" + jobid + "&dtype=Video&objectId=" + objectid + "&clazzId=" + clazzid + "&clipTime=" + "0_" + duration + "&otherInfo=" + otherInfo + "&duration=" + duration + "&view=pc&playingTime=" +
                    newplayingTime + "&isdrag=3&enc=" + enc).openConnection();
            getwork.setRequestMethod("GET");
            getwork.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
            getwork.setRequestProperty("Referer", "https://mooc1-2.chaoxing.com/ananas/modules/video/index.html?v=2018-0126-1905");
            getwork.setRequestProperty("X - Requested - With", "XMLHttpRequest");
            getwork.setRequestProperty("Cookie",myCookies);
            getwork.setConnectTimeout(5000);

            int code = getwork.getResponseCode();
            Log.i("ADT","code="+code);

            ;
            if ( code == 200) {
                InputStream is = getwork.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    //Log.i("ADT", "line=" + line);
                    sb.append(line);
                }
                is.close();
                getwork.disconnect();
                String resp = sb.toString();

                //Log.i("ADT","resp="+resp);
                message("resp="+resp,1);
                newplayingTime = String.valueOf(Integer.parseInt(newplayingTime)+114);
                if(Integer.parseInt(newplayingTime)>Integer.parseInt(duration))newplayingTime=duration;
                ShareHelper sp=new ShareHelper(getApplicationContext());
                sp.saveprogress(missionList,newplayingTime);
                Log.i("ADT","playingTime="+newplayingTime);
                message("playingTime="+newplayingTime,1);
                refreshNotification((int)(Float.parseFloat(newplayingTime)/Integer.parseInt(duration)*100),duration+":"+missionList);
                if(resp.equals("{\"isPassed\":false}")){
                    Intent intent=new Intent(ChaoXing.this,ChaoXing.class);
                    intent.setAction("playvideo");
                    PendingIntent pendingIntent=PendingIntent.getService(ChaoXing.this,0,intent,0);
                    AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+114000,pendingIntent);
                }else {
                    sp.cleanprogress(missionList);
                    dotest(url,myCookies,clazzid,courseId,knowledgeid,"1");
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private String getenc(String duration,String playingTime,String clazzid,String userid,String jobid,String objectid){
        String clipTime = "0_"+duration;
        String enc = "[" + clazzid + "]" + "[" + userid + "]" + "[" + jobid + "]" + "[" + objectid + "]" + "[" + Integer.parseInt(
                playingTime) * 1000 + "]" + "[d_yHJ!$pdA~5]" + "[" + Integer.parseInt(duration) * 1000 + "]" + "[" + clipTime + "]";
        return MD5(enc).toLowerCase();
    }

    private String MD5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(s.getBytes("utf-8"));
            return toHex(bytes);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String toHex(byte[] bytes) {

        final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();
        StringBuilder ret = new StringBuilder(bytes.length * 2);
        for (int i=0; i<bytes.length; i++) {
            ret.append(HEX_DIGITS[(bytes[i] >> 4) & 0x0f]);
            ret.append(HEX_DIGITS[bytes[i] & 0x0f]);
        }
        return ret.toString();
    }

    private void refreshNotification(int progress,String list) {
        Log.i("ADT","progress="+progress);
        message("progress="+progress,1);
        Intent shuaIntent = new Intent(this,ShuaActivity.class);
        PendingIntent shuaPendingIntent = PendingIntent.getActivity(this,0,shuaIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        //获取NotificationManager实例
        //实例化NotificationCompat.Builde并设置相关属性
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,"ChaoXingShuaKeid")
                .setContentIntent(shuaPendingIntent)
                //设置小图标
                .setSmallIcon(R.mipmap.pikaqiu_foreground)
                //设置通知标题1
                .setContentTitle("超星刷课")
                //设置通知内容
                .setContentText(list)
                .setProgress(100,progress,false);
        //通过builder.build()方法生成Notification对象,并发送通知,id=1
        startForeground(1,builder.build());
    }

    private void dotest(String url,final String myCookies,final String clazzid,final String courseid,final String knowledgeid,final String num){
        String msg;
        try{
            HttpURLConnection resp = (HttpURLConnection) new URL("https://mooc1-2.chaoxing.com"+url).openConnection();
            resp.setRequestMethod("GET");
            resp.setRequestProperty("Cookie",myCookies);
            resp.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
            resp.setConnectTimeout(5000);

//            if (resp.getResponseCode()==200) {
//                InputStream is = resp.getInputStream();
//                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
//                StringBuilder sb = new StringBuilder();
//                String line = null;
//                while ((line = reader.readLine()) != null) {
//                    sb.append(line + "\n");
//                }
//                is.close();
//                resp.disconnect();
//                msg = sb.toString();

            msg = getResponeText(resp);
            resp.disconnect();

            Matcher m = Pattern.compile("utEnc=\"(\\w+)\"").matcher(msg);
            if(!m.find())
                return;
            String utenc = m.group(1);


            resp = (HttpURLConnection) new URL("https://mooc1-2.chaoxing.com/knowledge/cards?clazzid="+clazzid+"&courseid="+courseid+"&knowledgeid="+knowledgeid+"&num="+num+"&v=20160407-1").openConnection();
            resp.setRequestMethod("GET");
            resp.setRequestProperty("Cookie",myCookies);
            resp.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
            resp.setRequestProperty("Referer","https://mooc1-2.chaoxing.com"+url);
            resp.setConnectTimeout(5000);

//            if (resp.getResponseCode()==200) {
//                is = resp.getInputStream();
//                reader = new BufferedReader(new InputStreamReader(is));
//                sb = new StringBuilder();
//                line = null;
//                while ((line = reader.readLine()) != null) {
//                    sb.append(line + "\n");
//                }
//                is.close();
//                resp.disconnect();
//                msg = sb.toString();
//                //Log.i("ADT",msg);
            msg = getResponeText(resp);
            resp.disconnect();

            m = Pattern.compile("\"workid\":\"(\\w+)\"").matcher(msg);
            if(m.find()){
                String workId = m.group(1);
                String jobId = "work-"+workId;
                m = Pattern.compile("\"enc\":\"(\\w+)\"").matcher(msg);
                m.find();
                String enc = m.group(1);

                //获取问题
                resp = (HttpURLConnection) new URL("https://mooc1-2.chaoxing.com/workHandle/handle?workId="+workId+"&courseid="+courseid+"&knowledgeid="+knowledgeid+"&userid=&ut=s&classId="+clazzid+"&jobid="+jobId+"&type=&isphone=false&submit=false&enc="+enc+"&utenc="+utenc).openConnection();
                resp.setRequestMethod("GET");
                resp.setRequestProperty("Referer","https://mooc1-2.chaoxing.com/ananas/modules/work/index.html?v=2018-0126-1905");
                resp.setRequestProperty("Cookie",myCookies);
                resp.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
                resp.setInstanceFollowRedirects(true);
                resp.setConnectTimeout(5000);

//                if (resp.getResponseCode()==200) {
//                    is = resp.getInputStream();
//                    reader = new BufferedReader(new InputStreamReader(is));
//                    sb = new StringBuilder();
//                    line = null;
//                    while ((line = reader.readLine()) != null) {
//                        sb.append(line + "\n");
//                        //Log.i("ADT",line);
//                    }
//                    is.close();
//                    resp.disconnect();
//                    msg = sb.toString();
                msg = getResponeText(resp);
                resp.disconnect();

                m = Pattern.compile("totalQuestionNum=([\\w]+)\"").matcher(msg);
                m.find();
                String totalQuestionNum = m.group(1);

                m = Pattern.compile("id=\"workRelationId\" value=\"(\\d+)\"").matcher(msg);
                m.find();
                String workRelationId = m.group(1);

                m = Pattern.compile("id=\"enc_work\" value=\"(\\w+)\"").matcher(msg);
                m.find();
                String enc_work = m.group(1);

                m = Pattern.compile("name=\"userId\" value=\"(\\d+)\"").matcher(msg);
                m.find();
                String userid = m.group(1);

                String answer;
                m = Pattern.compile("<div class=\"clearfix\" style=\"line-height: 35px; font-size: 14px;padding-right:15px;\">(.+)</div>").matcher(msg);
                StringBuffer params = new StringBuffer();
                params.append("pyFlag=").append("&")
                        .append("api=1").append("&")
                        .append("workAnswerId=").append("&")
                        .append("oldSchoolId=").append("&")
                        .append("enc=").append("&")
                        .append("courseId=").append(courseid).append("&")
                        .append("classId=").append(clazzid).append("&")
                        .append("totalQuestionNum=").append(totalQuestionNum).append("&")
                        .append("fullScore=100.0").append("&")
                        .append("knowledgeid=").append(knowledgeid).append("&")
                        .append("oldWorkId=").append(workId).append("&")
                        .append("jobid=").append("work-"+workId).append("&")
                        .append("workRelationId=").append(workRelationId).append("&")
                        .append("enc_work=").append(enc_work).append("&")
                        .append("userId=").append(userid);

                Matcher mm;
                String answerwqbid=null;
                String Answer=null;
                int Panduan=0;
                while(m.find()){

                    Log.i("ADT","question="+m.group(1));
                    message("question="+m.group(1),1);

                    answer = getAnswer(m.group(1),0);

                    if(answer==null)throw new Exception("Empty return answer");
                    Answer += answer;
                    Log.i("ADT","answer="+answer);
                    message("answer="+answer,1);
                    if (answer.contains("√")){
                        int i=0;
                        mm = Pattern.compile("name=\"answer(\\d+)\" value=\"true\"").matcher(msg);
                        while(mm.find()&&(i+=1)<=Panduan);
                        {
                            Log.i("ADT","question number="+mm.group(1));
                            params.append("&").append("answer"+mm.group(1)+"=").append("true").append("&").append("answertype"+mm.group(1)+"=3");
                            answerwqbid += mm.group(1)+",";
                        }
                        Panduan+=1;
                    }else if(answer.contains("×")){
                        int i=0;
                        mm = Pattern.compile("name=\"answer(\\d+)\" value=\"false\"").matcher(msg);
                        while(mm.find()&&(i+=1)<=Panduan);
                        {
                            Log.i("ADT","question number="+mm.group(1));
                            params.append("&").append("answer"+mm.group(1)+"=").append("false").append("&").append("answertype"+mm.group(1)+"=3");
                            answerwqbid += mm.group(1)+",";
                        }
                        Panduan+=1;
                    }else{
                        mm = Pattern.compile("<input name=\"answer(\\d+)\" type=\"radio\" value=\"(\\w)\"   />&nbsp;&nbsp;(\\w)[\\n\\s]+</label>[\\n\\s]+<a href=\"javascript:void\\(0\\);\" class=\"fl after\" style=\"padding-left:10px;\">"+answer+"</a>").matcher(msg);
                        if(mm.find()){
                            Log.i("ADT","question number="+mm.group(1)+"option="+mm.group(2));
                            params.append("&").append("answer"+mm.group(1)+"=").append(mm.group(2)).append("&").append("answertype"+mm.group(1)+"=0");
                            answerwqbid += mm.group(1)+",";
                        }
                    }
                    //Log.i("ADT-------------------","answer"+mm.group(1)+" value="+mm.group(2));
                }
                Log.i("ADT","answerwqbid="+answerwqbid);
                if(answerwqbid==null){
                    flag=true;
                    throw new Exception("Empty sending answer");
                }

                String[] a=answerwqbid.split(",");
                for (int i=1;i<a.length;i+=1) {
                    if(a[0].equals(a[i])){
                        flag=true;
                        throw new Exception("Equal answernum");
                    }
                }

                Log.i("ADT","发送答案");
                Log.i("ADT","dotestfinish");
                params.append("&answerwqbid=").append(answerwqbid.replace("null",""));
                Log.i("ADT","params="+params.toString());


                resp = (HttpURLConnection) new URL("https://mooc1-2.chaoxing.com/work/addStudentWorkNewWeb?_classId="+clazzid+"&courseid="+courseid+"&token="+enc_work+"&totalQuestionNum="+totalQuestionNum+"&version=1&ua=pc&formType=post&saveStatus=1&pos="+enc+"&value=(290|658)").openConnection();
                resp.setRequestMethod("POST");
                resp.setRequestProperty("Cookie",myCookies);
                resp.setRequestProperty("Referer","https://mooc1-2.chaoxing.com/work/doHomeWorkNew?courseId="+courseid+"&workId="+workId+"&api=1&knowledgeid="+knowledgeid+"&classId="+clazzid+"&oldWorkId="+workId+"&jobid=work-"+workId+"&type=&isphone=false&enc="+enc);
                resp.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
                resp.setUseCaches(false);
                resp.setConnectTimeout(5000);
                byte[] bytes = params.toString().getBytes();
                resp.getOutputStream().write(bytes);

                if (resp.getResponseCode()==200) {
                    InputStream is = resp.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder sb = new StringBuilder();
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line + "\n");
                        //Log.i("ADT",line);
                    }
                    is.close();
                    resp.disconnect();
                    msg = sb.toString();
                    startshua(startindex,myCookies,courseLinks);
                }
                //}
            }else{
                dotest(url,myCookies,clazzid,courseid,knowledgeid,"2");
            }

            //}
            //}
        }catch (PatternSyntaxException e){
            e.printStackTrace();
            message("无法找到答案,结束",2);
            stopSelf();

        }catch (Exception e){
            e.printStackTrace();
            Log.i("ADT","没打开答题页面？");
        }

    }

    private String getAnswer(String question,final int n){
        Log.i("ADT","getAnswer");
        try{
            HttpURLConnection resp = (HttpURLConnection)new URL("https://www.baidu.com/s?ie=utf-8&wd="+question).openConnection();
            resp.setRequestMethod("GET");
            resp.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
            resp.setConnectTimeout(5000);


//            if (resp.getResponseCode()==200) {
//                InputStream is = resp.getInputStream();
//                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
//                StringBuilder sb = new StringBuilder();
//                String line = null;
//                while ((line = reader.readLine()) != null) {
//                    line=line.replaceAll("\\s","");
//                    line=line.replaceAll("\\n","");
//                    sb.append(line);
//                    //Log.i("ADT", line);
//                }
//                is.close();
//                resp.disconnect();
//                String msg = sb.toString();
            String msg = getResponeText(resp);
            resp.disconnect();
            Log.i("ADT","------------------------------------------------------------");
            //Log.i("ADT", msg);
            Matcher m = Pattern.compile("href=\"(http://www.baidu.com/link\\?url=[\\w\\d-_]+)\"").matcher(msg);

            int i=0;
            while (m.find()&&(i+=1)<3);
            Log.i("ADT","link="+m.group(1));
            resp = (HttpURLConnection)new URL(m.group(1)).openConnection();
            resp.setRequestMethod("GET");
            resp.setInstanceFollowRedirects(true);
            resp.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
            resp.setConnectTimeout(5000);

            Log.i("ADT","responsecode="+resp.getResponseCode());
            if(resp.getResponseCode()==302){
                String location = resp.getHeaderField("Location");
                resp = (HttpURLConnection)new URL(location).openConnection();
                resp.setRequestMethod("GET");
                resp.setInstanceFollowRedirects(true);
                resp.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
                resp.setConnectTimeout(5000);
            }
            Log.i("ADT","responsecode2="+resp.getResponseCode());
//            if (resp.getResponseCode()==200) {
//                is = resp.getInputStream();
//                reader = new BufferedReader(new InputStreamReader(is));
//                sb = new StringBuilder();
//                line = null;
//                while ((line = reader.readLine()) != null) {
//                    sb.append(line);
//                    //Log.i("ADT", line);
//                }
//                is.close();
//                resp.disconnect();
//                msg = sb.toString();
            msg = getResponeText(resp);
            resp.disconnect();

            m = Pattern.compile("<div class='resource_content short' style='display: none;'>(.+)</div>").matcher(msg);
            i=0;
            while (m.find()&&i<=n){
                msg = m.group(1);
                //Log.i("ADT","return_answer_msg="+msg);
                msg = msg.split("<")[0];
                String[] t = msg.split(" ");
                msg = t[t.length-1];
                //Log.i("ADT","return_answer_msg="+msg);
                i+=1;
            }
            if(msg==null)msg=getAnswer1(question,n);
            return msg;
            //}

            //}
        }catch (Exception e){
            e.printStackTrace();
        }
    return null;
    }

    private String getAnswer1(String question,final int n){
        Log.i("ADT","getAnswer1");
        try{
            HttpURLConnection resp = (HttpURLConnection)new URL("https://www.zhengjie.com/s?type=question&q="+question).openConnection();
            resp.setRequestMethod("GET");
            resp.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
            resp.setConnectTimeout(5000);


//            if (resp.getResponseCode()==200) {
//                InputStream is = resp.getInputStream();
//                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
//                StringBuilder sb = new StringBuilder();
//                String line = null;
//                while ((line = reader.readLine()) != null) {
//                    line=line.replaceAll("\\s","");
//                    line=line.replaceAll("\\n","");
//                    sb.append(line);
//                    //Log.i("ADT", line);
//                }
//                is.close();
//                resp.disconnect();
//                String msg = sb.toString();
            String msg = getResponeText(resp);
            resp.disconnect();
            Log.i("ADT","------------------------------------------------------------");
            //Log.i("ADT", msg);
            Matcher m = Pattern.compile("<input class=\"resource_url_for_copy\" type=\"hidden\" value=\"([\\w\\/:.\\d]+)\">").matcher(msg);

            int i=0;
            while (m.find()&&(i+=1)<1);
            Log.i("ADT","link="+m.group(1));
            resp = (HttpURLConnection)new URL(m.group(1)).openConnection();
            resp.setRequestMethod("GET");
            resp.setInstanceFollowRedirects(true);
            resp.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
            resp.setConnectTimeout(5000);

            Log.i("ADT","responsecode="+resp.getResponseCode());
            if(resp.getResponseCode()==302){
                String location = resp.getHeaderField("Location");
                resp = (HttpURLConnection)new URL(location).openConnection();
                resp.setRequestMethod("GET");
                resp.setInstanceFollowRedirects(true);
                resp.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
                resp.setConnectTimeout(5000);
            }
            Log.i("ADT","responsecode2="+resp.getResponseCode());
//            if (resp.getResponseCode()==200) {
//                is = resp.getInputStream();
//                reader = new BufferedReader(new InputStreamReader(is));
//                sb = new StringBuilder();
//                line = null;
//                while ((line = reader.readLine()) != null) {
//                    sb.append(line);
//                    //Log.i("ADT", line);
//                }
//                is.close();
//                resp.disconnect();
//                msg = sb.toString();

            msg = getResponeText(resp);
            resp.disconnect();

            m = Pattern.compile("<div class='resource_content short' style='display: none;'>(.+)</div>").matcher(msg);
            i=0;
            while (m.find()&&i<=n){
                msg = m.group(1);
                //Log.i("ADT","return_answer_msg="+msg);
                msg = msg.split("<")[0];
                String[] t = msg.split(" ");
                msg = t[t.length-1];
                //Log.i("ADT","return_answer_msg="+msg);
                i+=1;
            }
            resp.disconnect();
            return msg;
            //}

            //}
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private void message(String string,int what){
        Message msg=new Message();
        Bundle bd=new Bundle();
        bd.putString("log",string);
        msg.what=what;
        msg.setData(bd);
        ShuaActivity.handler.sendMessage(msg);
    }

    private String getResponeText(HttpURLConnection getCourseList) {
        try {
            if (getCourseList.getResponseCode() == 200) {
                InputStream is = getCourseList.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    //Log.i("ADT","line="+line);
                    sb.append(line + "\n");
                }
                is.close();
                getCourseList.disconnect();
                return sb.toString();
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "检查网络链接", Toast.LENGTH_LONG).show();
            message("网络问题导致终止",2);
        }
        return null;
    }
}