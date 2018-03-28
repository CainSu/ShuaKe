package com.example.lyh_adt.shuake;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {
    private ImageView iv_valcode;
    private ChaoXing chaoXing;
    private Button btn_login;
    private EditText et_username;
    private EditText et_pasw;
    private EditText et_valcode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindviews();
        chaoXing = new ChaoXing();
        iv_valcode.setImageBitmap(chaoXing.getValocde());

        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String usernumber = et_username.getText().toString();
                String paswd = et_pasw.getText().toString();
                String valcode = et_valcode.getText().toString();
                String username = chaoXing.login(usernumber,paswd,valcode);
                Log.i("ADT","用户名为"+username);

                String coookies=chaoXing.getCoookies();

                Intent it = new Intent(MainActivity.this,ShuaActivity.class);
                //Bundle
                Bundle bd = new Bundle();
                bd.putCharSequence("username",username);
                bd.putCharSequence("cookies",coookies);
                //绑定
                it.putExtras(bd);
                startActivity(it);
            }
        });
    }

    private void bindviews(){
        iv_valcode = (ImageView)findViewById(R.id.iv_valcode);
        btn_login = (Button)findViewById(R.id.btn_login);
        et_username = (EditText)findViewById(R.id.et_username);
        et_pasw = (EditText)findViewById(R.id.et_pasw);
        et_valcode = (EditText)findViewById(R.id.et_valcode);
    }


}
