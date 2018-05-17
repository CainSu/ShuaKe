package com.example.lyh_adt.shuake;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ShareHelper {
    private Context mContext;

    public ShareHelper(){}

    public ShareHelper(Context mContext){
        super();
        this.mContext=mContext;
    }

    public void save(String username,String cookies){
        SharedPreferences sp=mContext.getSharedPreferences("data",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor=sp.edit();
        editor.putString("username",username);
        editor.putString("cookies",cookies);
        editor.commit();
    }

    public Map<String, String> read(){
        Map<String,String>data=new HashMap<String,String>();
        SharedPreferences sp=mContext.getSharedPreferences("data",Context.MODE_PRIVATE);
        data.put("username",sp.getString("username",""));
        data.put("cookies",sp.getString("cookies",""));
        return data;
    }

    public void saveprogress(String title,String progress){
        SharedPreferences sp=mContext.getSharedPreferences("data",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor=sp.edit();
        editor.putString(title,progress);
        editor.commit();
    }

    public String readprogress(String title){
        SharedPreferences sp=mContext.getSharedPreferences("data",Context.MODE_PRIVATE);
        return sp.getString(title,"114");
    }

    public void cleanprogress(String title){
        SharedPreferences sp=mContext.getSharedPreferences("data",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor=sp.edit();
        editor.remove(title);
        editor.commit();
    }

    public void clean(){
        save("","");
    }
}
