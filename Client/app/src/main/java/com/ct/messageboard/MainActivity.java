package com.ct.messageboard;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private EditText editText_name;
    private EditText editText_password;
    private EditText editText_content;
    private Button buuton;
    private Handler uiHandler;
    private String TAG="MainActivity";
    private ProgressDialog waitingDialog;
    private AlertDialog.Builder builder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText_name=findViewById(R.id.editText_name);
        editText_content=findViewById(R.id.editText_content);
        editText_password=findViewById(R.id.editText_password);
        buuton=findViewById(R.id.button_OK);
        buuton.setOnClickListener(this);

        uiHandler=new Handler(){
            public void handleMessage(Message msg){
                switch (msg.what){
                    case 1:
                        waitingDialog= new ProgressDialog(MainActivity.this);
                        waitingDialog.setMessage("连接中，请稍等...");
                        waitingDialog.setIndeterminate(true);
                        waitingDialog.setCancelable(false);
                        waitingDialog.show();
                        break;
                    case 2:                             //connecting to server succeed
                        waitingDialog.dismiss();
                        builder=new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("提示");
                        builder.setPositiveButton("知道了！", new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog,int whitch) {

                            }
                        });
                        Bundle bundle=msg.getData();
                        if(bundle.getString("result").equals("success")){
                            builder.setMessage("Succeed.");
                        }else if(bundle.getString("result").equals("failure")){
                            builder.setMessage("Failed."+bundle.getString("reason"));
                        }
                        builder.create().show();
                        break;
                    case 3:                             //connecting to server failed
                        waitingDialog.dismiss();
                        builder=new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("提示");
                        builder.setPositiveButton("知道了！", new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog,int whitch) {

                            }
                        });
                        builder.setMessage("Failed.Please check your network");
                        builder.show();
                        break;
                    default:
                        break;
                }
            }
        };
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.button_OK:
                String name=editText_name.getText().toString();
                String password=editText_password.getText().toString();
                String content=editText_content.getText().toString();
                new socketThread(name,password,content).start();
                break;
            default:
                break;
        }
    }

    class socketThread extends Thread{
        private String IP;
        private int PORT;
        private Socket socket;
        private String name;
        private String password;
        private String content;
        private InputStreamReader inputStreamReader;
        private OutputStreamWriter outputStreamWriter;
        private JsonWriter jsonWriter;
        private JsonReader jsonReader;

        public socketThread(String n,String p,String c){
            name=n;
            password=p;
            content=c;
            IP="95.163.206.203";
            PORT=30000;
        }

        public void run(){
            Message message;
            try{
                socket=new Socket();
                message=new Message();

                message.what=1;                                                 //连接
                uiHandler.sendMessage(message);

                socket.connect(new InetSocketAddress(IP,PORT),5*1000);
                inputStreamReader=new InputStreamReader(socket.getInputStream());
                outputStreamWriter=new OutputStreamWriter(socket.getOutputStream());
                jsonWriter = new JsonWriter(outputStreamWriter);
                jsonWriter.beginObject();
                jsonWriter.name("label").value("client");
                jsonWriter.name("name").value(name);
                jsonWriter.name("password").value(password);
                jsonWriter.name("content").value(content);
                jsonWriter.endObject();
                jsonWriter.flush();
                //接收消息
                Bundle bundle=new Bundle();
                jsonReader=new JsonReader(inputStreamReader);
                jsonReader.beginObject();
                while(jsonReader.hasNext()){
                    bundle.putString(jsonReader.nextName(),jsonReader.nextString());
                }
                jsonReader.endObject();

                message=new Message();                                          //连接结束
                message.what=2;
                message.setData(bundle);
                uiHandler.sendMessage(message);
                socket.close();
            }catch (IOException e) {
                message=new Message();
                e.printStackTrace();
                message.what=3;
                uiHandler.sendMessage(message);
            }
        }
    }
}
