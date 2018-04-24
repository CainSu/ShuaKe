package com.example.lyh_adt.shuake;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaCodec;
import android.os.Bundle;
import android.os.Parcel;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

import java.net.CookieStore;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.regex.Pattern.compile;

/**
 * Created by 10439 on 2018/3/21/0021.
 */

public class ChaoXing extends IntentService implements Serializable {
    private Bitmap bitmap=null;//验证码
    private String username=null;//用户名
    private CookieManager cookieManager = new CookieManager( null, CookiePolicy.ACCEPT_ALL );
    private boolean flag=false;
    private String missionList;


    public ChaoXing(){
        super("ChaoXing");
        CookieHandler.setDefault(cookieManager);
    }

    @Override
    public void onCreate(){
        super.onCreate();

        NotificationManager mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel mChannel = new NotificationChannel("ShuaKeChaoXing","超星刷课",NotificationManager.IMPORTANCE_HIGH);
        mChannel.setDescription("ShuaKeChaoXing");
        mNotificationManager.createNotificationChannel(mChannel);



        Notification notification = new Notification.Builder(this)
        .setAutoCancel(false)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("超星刷课")
        .setContentText("正在运行...")
        .setChannelId("ShuaKeChaoXing")
        .build();

        mNotificationManager.notify(1,notification);
    }

    @Override
    protected void onHandleIntent(Intent intent){
        flag=false;
        Log.i("ADT","ChaoxingonHandleIntent");
        Bundle bundle = intent.getExtras();
        Character action = bundle.getChar("param");
        if (action.equals('s'))
        {
            Log.i("ADT","开始");

            start(bundle.getInt("startindex"),bundle.getString("myCookies"),(ArrayList<String>) bundle.getSerializable("courseLinks"));
        }
        else if (action.equals('t'))
        {
            Log.i("ADT","停止");
            flag = true;
            stopSelf();
        }
    }

    @Override
    public void onDestroy(){
        flag = true;
        stopSelf();
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

    public Map<String,LinkedList<String>> getCourseList(final String myCookies){
        final LinkedList<String> courseLinks = new LinkedList<String>();
        final LinkedList<String> courseList = new LinkedList<String>();
        Map<String,LinkedList<String>> map = new HashMap<String,LinkedList<String>>();
        Log.i("ADT","getCourseList"+flag);
        //final LinkedList<String> CourseList=null;


        new Thread(){
            public void run(){

                try{
                    HttpURLConnection getCourseList = (HttpURLConnection)new URL("http://mooc1-2.chaoxing.com/visit/interaction").openConnection();
                    getCourseList.setRequestMethod("GET");
                    getCourseList.addRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
                    getCourseList.addRequestProperty("Referer", "http://i.mooc.chaoxing.com/space/index");

                    getCourseList.addRequestProperty("Cookie",myCookies);

                    if (getCourseList.getResponseCode() == 200){
                        InputStream is = getCourseList.getInputStream();
                        Log.i("ADT","Content="+getCourseList.getContent().toString());
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                        StringBuilder sb = new StringBuilder();
                        String line = null;
                        while ((line = reader.readLine()) != null) {
                            //Log.i("ADT","line="+line);
                            sb.append(line + "\n");
                        }
                        is.close();
                        getCourseList.disconnect();
                        String resp = sb.toString();
                        //Log.i("ADT","resp="+sb.toString());
                        //Log.i("ADT","resp="+resp);



                        Matcher m = Pattern.compile("<div class=\"Mconright httpsClass\">[\\n\\s]*<h3 class=\"clearfix\" >[\\n\\s]*<a  href='([\\w\\/?=&]*)'[\\n\\s]*target=\"_blank\" title=\"([\\u4e00-\\u9fa5()（）\\w-]*)\">").matcher(resp);
                        while (m.find()){
                            //Log.i("ADT","total:"+m.groupCount());
                            Log.i("ADT","link:"+m.group(1)+"title:"+m.group(2));
                            courseLinks.add("https://mooc1-2.chaoxing.com"+m.group(1));
                            courseList.add(m.group(2));
                        }
                        flag = true;
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        }.start();

        while(flag == false);
        flag = false;
        map.put("courseList",courseList);
        map.put("courseLinks",courseLinks);
        return map;
    }

    public String getCoookies(){
        List<URI> uri = cookieManager.getCookieStore().getURIs();
        List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
        Log.i("ADT","URIString="+uri.toString());
        Log.i("ADT","newCookies"+cookies.toString());

        return cookies.toString().substring(1,cookies.toString().lastIndexOf(']')).replace(',',';');

    }

    public void start(int m,final String myCookies,ArrayList<String> courseLinks){

        Log.i("ADT","m="+m);
        //进入章节目录寻找未完成的任务
        Log.i("ADT","courseLinsk is Empty"+courseLinks.isEmpty());
        flag=false;

        for (int i=m;i<courseLinks.size()&&!flag;i+=1){
            Map<String,String> mission = findmission(courseLinks.get(i),myCookies);
            Log.i("ADT","missionlink="+mission.get("link"));
            while(mission.get("link")!=null&&!flag)
            {
                Log.i("ADT","flag=="+flag);
                missionList = mission.get("list");
                Log.i("ADT","进入play循环");
                play(mission.get("link"),myCookies);
                mission = findmission(courseLinks.get(i),myCookies);
            }
            break;
        }
    }

    private Map<String,String> findmission(final String url,final String myCookies){
        Map<String,String> mission = new HashMap<String,String>();
                try {
                    HttpURLConnection getMission = (HttpURLConnection)new URL(url).openConnection();
                    getMission.setRequestMethod("GET");
                    getMission.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
                    getMission.setRequestProperty("Cookie",myCookies);

                    if (getMission.getResponseCode() == 200) {
                        InputStream is = getMission.getInputStream();
                        //Log.i("ADT", "Content=" + getMission.getContent().toString());
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                        StringBuilder sb = new StringBuilder();
                        String line = null;
                        while ((line = reader.readLine()) != null) {
                            //Log.i("ADT", "line=" + line);
                            sb.append(line + "\n");
                        }
                        is.close();
                        getMission.disconnect();
                        String resp = sb.toString();

                        Matcher m = Pattern.compile("<em class=\"orange\">(\\d)</em>[\\s\\n]*</span>[\\s\\n]*<span class=\"articlename\">[\\s\\n]*<a href='([\\/\\w?=&;]+)' title=\"([\\u4e00-\\u9fa5\\d（）]+)\"\\s*>").matcher(resp);
                        if (m.find()){
                            Log.i("ADT","missioncount="+m.group(1)+" link:"+m.group(2)+" title:"+m.group(3));
                            mission.put("list",m.group(3));
                            mission.put("link",m.group(2));
                        }
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }
        return mission;

    }

    private void play(final String url,final String myCookies){
        Log.i("ADT","url="+url);
        //获取chapterid、courseid、clazzid
        Matcher m = Pattern.compile("chapterId=(\\d+)&courseId=(\\d+)&clazzid=(\\d+)").matcher(url);
        m.find();
        final String knowledgeid = m.group(1);
        final String courseId = m.group(2);
        final String clazzid = m.group(3);

        try{
            //获取objectid，jobid，otherinfo，fid
            HttpURLConnection gettovideo = (HttpURLConnection) new URL("https://mooc1-2.chaoxing.com/knowledge/cards?clazzid=" + clazzid + "&courseid=" + courseId + "&knowledgeid=" + knowledgeid + "&num=0&v=20160407-1").openConnection();
            gettovideo.setRequestMethod("GET");
            gettovideo.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
            gettovideo.setRequestProperty("Cookie", myCookies);

            if (gettovideo.getResponseCode() == 200) {
                InputStream is = gettovideo.getInputStream();
                Log.i("ADT", "Content=" + gettovideo.getContent().toString());
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    //Log.i("ADT", "line=" + line);
                    sb.append(line + "\n");
                }
                is.close();
                gettovideo.disconnect();
                String resp = sb.toString();

                m = Pattern.compile("jobid\":\"(\\d+)").matcher(resp);
                m.find();
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

                if (getstatus.getResponseCode() == 200) {
                    is = getstatus.getInputStream();
                    Log.i("ADT", "Content=" + getstatus.getContent().toString());
                    reader = new BufferedReader(new InputStreamReader(is));
                    sb = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        //Log.i("ADT", "line=" + line);
                        sb.append(line + "\n");
                    }
                    is.close();
                    getstatus.disconnect();
                    resp = sb.toString();

                    m = Pattern.compile("duration\":(\\d+),").matcher(resp);
                    m.find();
                    String duration = m.group(1);
                    m = Pattern.compile("dtoken\":\"(\\w+)\"").matcher(resp);
                    m.find();
                    String dtoken = m.group(1);

                    String playingTime = "114";
                    String enc=new String();
                    HttpURLConnection getwork=(HttpURLConnection)new URL("https://mooc1-2.chaoxing.com/multimedia/log/"+dtoken+"?userid="+userid + "&rt=0.9&jobid=" + jobid + "&dtype=Video&objectId=" + objectid + "&clazzId=" + clazzid + "&clipTime=" + "0_" + duration + "&otherInfo=" + otherInfo + "&duration=" + duration + "&view=pc&playingTime=" +
                            playingTime + "&isdrag=3&enc=" + enc).openConnection();

                    resp="{\"isPassed\":false}";

                    while(resp.equals("{\"isPassed\":false}")&&!flag){
                        enc = getenc(duration,playingTime,clazzid,userid,jobid,objectid);
                        Log.i("ADT","enc="+enc);
                        getwork = (HttpURLConnection)new URL("https://mooc1-2.chaoxing.com/multimedia/log/"+dtoken+"?userid="+userid + "&rt=0.9&jobid=" + jobid + "&dtype=Video&objectId=" + objectid + "&clazzId=" + clazzid + "&clipTime=" + "0_" + duration + "&otherInfo=" + otherInfo + "&duration=" + duration + "&view=pc&playingTime=" +
                                playingTime + "&isdrag=3&enc=" + enc).openConnection();
                        getwork.setRequestMethod("GET");
                        getwork.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
                        getwork.setRequestProperty("Referer", "https://mooc1-2.chaoxing.com/ananas/modules/video/index.html?v=2018-0126-1905");
                        getwork.setRequestProperty("X - Requested - With", "XMLHttpRequest");
                        getwork.setRequestProperty("Cookie",myCookies);

                        int code =getwork.getResponseCode();
                        Log.i("ADT","code="+code);
                        if ( code == 200) {
                            is = getwork.getInputStream();
                            reader = new BufferedReader(new InputStreamReader(is));
                            sb = new StringBuilder();
                            while ((line = reader.readLine()) != null) {
                                //Log.i("ADT", "line=" + line);
                                sb.append(line);
                            }
                            is.close();
                            getwork.disconnect();
                            resp = sb.toString();

                            Log.i("ADT","resp="+resp);
                            playingTime = String.valueOf(Integer.parseInt(playingTime)+114);
                            Log.i("ADT","playingTime="+playingTime);
                        }
                        refreshNotification((int)(Float.parseFloat(playingTime)/Integer.parseInt(duration)*100),missionList);

                        if(!flag) Thread.sleep(114000);
                    }
                    Log.i("ADT","出循环");
                    Log.i("ADT","resp="+resp);


                    Log.i("ADT","章节测验");
                    dotest(url,myCookies,clazzid,courseId,knowledgeid);

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
        //获取NotificationManager实例
        NotificationManager notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //实例化NotificationCompat.Builde并设置相关属性
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                //设置小图标
                .setSmallIcon(R.mipmap.ic_launcher_round)
                //设置通知标题
                .setContentTitle("超星刷课")
                //设置通知内容
                .setContentText(list)
                .setProgress(100,progress,false)
                .setChannelId("ShuaKeChaoXing");
        //通过builder.build()方法生成Notification对象,并发送通知,id=1
        notifyManager.notify(1, builder.build());
    }

    private void dotest(String url,final String myCookies,final String clazzid,final String courseid,final String knowledgeid){
        String msg;
        try{
            HttpURLConnection resp = (HttpURLConnection) new URL("https://mooc1-2.chaoxing.com"+url).openConnection();
            resp.setRequestMethod("GET");
            resp.setRequestProperty("Cookie",myCookies);
            resp.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");

            if (resp.getResponseCode()==200) {
                InputStream is = resp.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
                is.close();
                resp.disconnect();
                msg = sb.toString();

                Matcher m = Pattern.compile("utEnc=\"(\\w+)\"").matcher(msg);
                if(!m.find())
                    return;
                String utenc = m.group(1);


                resp = (HttpURLConnection) new URL("https://mooc1-2.chaoxing.com/knowledge/cards?clazzid="+clazzid+"&courseid="+courseid+"&knowledgeid="+knowledgeid+"&num=1&v=20160407-1").openConnection();
                resp.setRequestMethod("GET");
                resp.setRequestProperty("Cookie",myCookies);
                resp.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
                resp.setRequestProperty("Referer","https://mooc1-2.chaoxing.com"+url);

                if (resp.getResponseCode()==200) {
                    is = resp.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(is));
                    sb = new StringBuilder();
                    line = null;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    is.close();
                    resp.disconnect();
                    msg = sb.toString();
                    //Log.i("ADT",msg);

                    m = Pattern.compile("\"workid\":\"(\\w+)\"").matcher(msg);
                    m.find();
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

                    if (resp.getResponseCode()==200) {
                        is = resp.getInputStream();
                        reader = new BufferedReader(new InputStreamReader(is));
                        sb = new StringBuilder();
                        line = null;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line + "\n");
                            //Log.i("ADT",line);
                        }
                        is.close();
                        resp.disconnect();
                        msg = sb.toString();

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
                        while(m.find()){

                            answer = getAnswer(m.group(1));
                            Log.i("ADT","answer="+answer);
                            if (answer.contains("√")){
                                mm = Pattern.compile("name=\"answer(\\d+)\" value=\"true\"").matcher(msg);
                                if(mm.find()){
                                    params.append("&").append("answer"+mm.group(1)+"=").append("true").append("&").append("answertype"+mm.group(1)+"=3");
                                    answerwqbid += mm.group(1)+",";
                                }
                            }else if(answer.contains("×")){
                                mm = Pattern.compile("name=\"answer(\\d+)\" value=\"false\"").matcher(msg);
                                if(mm.find()){
                                    params.append("&").append("answer"+mm.group(1)+"=").append("false").append("&").append("answertype"+mm.group(1)+"=3");
                                    answerwqbid += mm.group(1)+",";
                                }
                            }else{
                                mm = Pattern.compile("<input name=\"answer(\\d+)\" type=\"radio\" value=\"(\\w)\"[\\D\\d]+style=\"padding-left:10px;\">.*"+answer+".*</a>").matcher(msg);
                                if(mm.find()){
                                    params.append("&").append("answer"+mm.group(1)+"=").append(mm.group(2)).append("&").append("answertype"+mm.group(1)+"=0");
                                    answerwqbid += mm.group(1)+",";
                                }
                            }
                            //Log.i("ADT-------------------","answer"+mm.group(1)+" value="+mm.group(2));
                        }
                        Log.i("ADT","dotestfinish");
                        params.append("&answerwqbid=").append(answerwqbid);
                        Log.i("ADT","params="+params.toString());


                        resp = (HttpURLConnection) new URL("https://mooc1-2.chaoxing.com/work/addStudentWorkNewWeb?_classId="+clazzid+"&courseid="+courseid+"&token="+enc_work+"&totalQuestionNum="+totalQuestionNum+"&version=1&ua=pc&formType=post&saveStatus=1&pos="+enc+"&value=(290|658)").openConnection();
                        resp.setRequestMethod("POST");
                        resp.setRequestProperty("Cookie",myCookies);
                        resp.setRequestProperty("Referer","https://mooc1-2.chaoxing.com/work/doHomeWorkNew?courseId="+courseid+"&workId="+workId+"&api=1&knowledgeid="+knowledgeid+"&classId="+clazzid+"&oldWorkId="+workId+"&jobid=work-"+workId+"&type=&isphone=false&enc="+enc);
                        resp.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
                        resp.setUseCaches(false);
                        byte[] bytes = params.toString().getBytes();
                        resp.getOutputStream().write(bytes);
                        if (resp.getResponseCode()==200) {
                            is = resp.getInputStream();
                            reader = new BufferedReader(new InputStreamReader(is));
                            sb = new StringBuilder();
                            line = null;
                            while ((line = reader.readLine()) != null) {
                                sb.append(line + "\n");
                                Log.i("ADT",line);
                            }
                            is.close();
                            resp.disconnect();
                            msg = sb.toString();
                        }
                        Log.i("ADT","msg="+msg);
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            Log.i("ADT","没打开答题页面？");
        }


    }

    private String getAnswer(String question){
        try{
            HttpURLConnection resp = (HttpURLConnection)new URL("https://www.zhengjie.com/s?type=question&q="+question).openConnection();
            resp.setRequestMethod("GET");
            resp.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");

            if (resp.getResponseCode()==200) {
                InputStream is = resp.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                    //Log.i("ADT", line);
                }
                is.close();
                resp.disconnect();
                String msg = sb.toString();

                Matcher m = Pattern.compile("<input type='hidden' class='resource_url_for_copy' value='([\\w:/.]+)'>").matcher(msg);
                if (m.find()){
                    resp = (HttpURLConnection)new URL(m.group(1)).openConnection();
                    resp.setRequestMethod("GET");
                    resp.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");

                    if (resp.getResponseCode()==200) {
                        is = resp.getInputStream();
                        reader = new BufferedReader(new InputStreamReader(is));
                        sb = new StringBuilder();
                        line = null;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line + "\n");
                            //Log.i("ADT", line);
                        }
                        is.close();
                        resp.disconnect();
                        msg = sb.toString();

                        m = Pattern.compile("<div class='resource_content short' style='display: none;'>(.+)</div>").matcher(msg);
                        if (m.find()){
                            msg = m.group(1);
                            //Log.i("ADT","return_answer_msg="+msg);
                            msg = msg.split("<")[0];
                            String[] t = msg.split(" ");
                            msg = t[t.length-1];
                            //Log.i("ADT","return_answer_msg="+msg);
                            return msg;
                        }
                        resp.disconnect();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    return null;
    }

}
