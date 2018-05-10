package com.example.lyh_adt.shuake;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.Map;


/**
 * Created by 10439 on 2018/3/22/0022.
 */

public class ShuaActivity extends AppCompatActivity implements View.OnClickListener {
    private int selectedCourse = -1;
    private String username;
    private TextView tv_usrname;
    private ListView listview;
    private ChaoXing chaoXing = new ChaoXing();
    private CourseListAdapter mAdapter = null;
    private LinkedList<String> courseList = null;
    private LinkedList<String> courseLinks = null;
    private Button btn_start;
    private Button btn_stop;
    private TextView tv_log;
    private Intent it1;
    private String cookies;
    private Boolean firstStart=true;
    public static Handler handler;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shua);

        //Intent
        Intent it = getIntent();
        Bundle bd = it.getExtras();

        username = bd.getCharSequence("username").toString();
        cookies =bd.getCharSequence("cookies").toString();

        Map<String,LinkedList<String>> map = chaoXing.getCourseList(cookies);
        courseList = map.get("courseList");
        courseLinks = map.get("courseLinks");
        Log.i("ADT","courseList"+courseList.toString());
        bindViews();

        tv_usrname.setText(username);
        mAdapter = new CourseListAdapter((LinkedList<String>)courseList,ShuaActivity.this);
        listview.setAdapter(mAdapter);

        handler=new Handler(){
            @Override
            public void handleMessage(Message msg){
                Log.i("ADT","get Message");
                    Bundle bd=msg.getData();
                    switch (msg.what){
                        case 1:
                            tv_log.append(bd.getString("log")+"\n");
                            tv_log.scrollTo(0,tv_log.getLineCount()*tv_log.getLineHeight()-tv_log.getHeight());
                        case 2:
                            tv_log.append(bd.getString("log"));
                    }

            }
        };
    }

    @Override
    public void onDestroy(){
        stopService(it1);
        super.onDestroy();
    }

    private void bindViews(){
        tv_log=(TextView)findViewById(R.id.tv_log);
        tv_usrname = (TextView)findViewById(R.id.tv_usrname);
        listview = (ListView)findViewById(R.id.listview);
        btn_start = (Button)findViewById(R.id.btn_start);
        btn_stop = (Button)findViewById(R.id.btn_stop);

        btn_start.setOnClickListener(this);
        btn_stop.setOnClickListener(this);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent,View view,int position,long id){
                mAdapter.select(position);
                selectedCourse = position;
            }
        });
    }

    @Override
    public void onClick(View v){
        Log.i("ADT","onClick");
        switch (v.getId()){
            case R.id.btn_start:
                it1 = new Intent(ShuaActivity.this,ChaoXing.class);

                Bundle b1 = new Bundle();
                b1.putString("myCookies",cookies);
                b1.putSerializable("courseLinks",courseLinks);
                b1.putInt("startIndex",selectedCourse);
                it1.putExtras(b1);
                //startService(it1);
                if(firstStart){
                    startForegroundService(it1);
                    firstStart=false;
                }else {
                    Toast.makeText(getApplicationContext(),"已有实例在进行，请停止后操作",Toast.LENGTH_LONG).show();
                }


                Log.i("ADT","btnstart");
                break;
            case R.id.btn_stop:
                stopService(it1);
                tv_log.setText("");
                break;
        }
    }
}
