package com.example.user.work9;

import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener{
    final String mainDir = "diary";
    public static boolean externalGrant;
    private boolean isModifyMode = false;
    String oldFileName = "";
    FileInputStream fin;
    FileOutputStream fout;

    LinearLayout linear1, linear2;
    TextView tvCount;
    Button btnSave,btnCancel,btnRegister;
    DatePicker datePicker;
    EditText editText;
    ListView listview;
    ArrayList<String> list;
    ArrayAdapter<String> arrayAdapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        linear1 = (LinearLayout) findViewById(R.id.linear1);
        linear2 = (LinearLayout) findViewById(R.id.linear2);
        tvCount = (TextView) findViewById(R.id.tvCount);
        datePicker = (DatePicker) findViewById(R.id.datePicker);
        editText = (EditText) findViewById(R.id.editText);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnCancel=(Button) findViewById(R.id.btnCancel);
        btnRegister=(Button)findViewById(R.id.btnRegister);
        btnSave.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
        btnRegister.setOnClickListener(this);
        listview = (ListView)findViewById(R.id.listview);
        list = readInitData();
        arrayAdapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,list);
        listview.setAdapter(arrayAdapter);
        listview.setOnItemClickListener(this);
        listview.setOnItemLongClickListener(this);
        arrayAdapter.notifyDataSetChanged();
    }

    private ArrayList<String> readInitData() {
        ArrayList<String> list = new ArrayList<>();
        if(isAvailableExternal() && getRWPermission()) {
            File dir = new File(getSDpath() + mainDir);
            if (!dir.exists() || !dir.isDirectory())
                dir.mkdir();
            File[] files = dir.listFiles();
            for (File f : files) {
                String[] s = f.getName().split(".");
                if (s.length >= 2 && s[s.length - 1].equals("memo")) ;
                list.add(f.getName());
            }
            setCount(list.size());
            Collections.sort(list);
        }else{
            Toast.makeText(getApplicationContext(), "외장메모리로 부터 데이터를 읽을 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
        return list;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btnRegister:
                isModifyMode = false;
                btnSave.setText("저장");
                editText.setText("");
                linear2.setVisibility(View.VISIBLE);
                linear1.setVisibility(View.INVISIBLE);
                break;
            case R.id.btnSave:
                String f = datePicker.getYear() + "-" + datePicker.getMonth() + "-" + datePicker.getDayOfMonth() + ".memo";
                if(list.contains(f) && !isModifyMode) {
                        oldFileName = f;
                        modify(f);
                }
                else {
                    byte[] data = editText.getText().toString().getBytes();
                    add(f,data);
                    Toast.makeText(getApplicationContext(), "성공적으로 " + ((isModifyMode)?"수정":"저장") + "했습니다.", Toast.LENGTH_SHORT).show();
                    arrayAdapter.notifyDataSetChanged();
                    linear2.setVisibility(View.INVISIBLE);
                    linear1.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.btnCancel:
                linear2.setVisibility(View.INVISIBLE);
                linear1.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(externalGrant){
            oldFileName = list.get(position);
            modify(oldFileName);
            linear2.setVisibility(View.VISIBLE);
            linear1.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
        if(externalGrant){
            AlertDialog.Builder dlg = new AlertDialog.Builder(this);
            final int index = position;
            dlg.setTitle("삭제")
                    .setMessage("정말 삭제하시겠습니까?")
                    .setNegativeButton("취소",null)
                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            remove(list.get(index));
                            list.remove(index);
                            setCount(list.size());
                            arrayAdapter.notifyDataSetChanged();
                        }
                    })
                    .show();
        }
        return false;
    }

    private void add(String file, byte[] data){
        writeByteToExternal(mainDir + "/" + file,data,false);
        if(isModifyMode) {
            File oldfile = new File(getSDpath() + mainDir + "/" + oldFileName);
            oldfile.renameTo(new File(getSDpath() + mainDir + "/" + file));
            list.remove(oldFileName);
        }
        list.add(file);
        Collections.sort(list);
        setCount(list.size());
    }

    private void remove(String file){
        File f = new File(getSDpath() + mainDir + "/" + file);
        f.delete();
    }

    private void modify(String file){
        Log.e("FILE NAME " , file);
        String[] date = (file.split(".memo")[0]).split("-");
        try {
            isModifyMode = true;
            btnSave.setText("수정");
            datePicker.updateDate(Integer.parseInt(date[0]),Integer.parseInt(date[1]),Integer.parseInt(date[2]));
            String s = "";
            try {
                s = new String(readByteFromExternal(mainDir + "/" + file), "UTF-8");
            } catch (UnsupportedEncodingException e) {}
            editText.setText(s);
        }catch(RuntimeException e){
            for(StackTraceElement k : e.getStackTrace())
                Log.e("E",k.toString());
            Toast.makeText(getApplicationContext(),"파일 형식이 잘못되었습니다.",Toast.LENGTH_SHORT).show();
        }

    }

    private void setCount(int i){
        tvCount.setText("등록된 메모 개수: " + i);
    }

    private byte[] readByte(String dir){
        try{
            fin = new FileInputStream(dir);
            byte[] data = new byte[fin.available()];
            fin.read(data);
            return data;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e){
        }
        return null;
    }

    public byte[] readByteFromExternal(String dir){
        if(isAvailableExternal()&&getRWPermission())
            return readByte(getSDpath() + dir);
        else
            return null;
    }

    public byte[] readByteFromInternal(String dir){
        return readByte(getFilesDir() + dir);
    }

    private void writeByte(String dir, byte[] data, boolean isAppend){
        try{
            fout = new FileOutputStream(dir,isAppend);
            fout.write(data);
            fout.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch(IOException e){
        }
    }

    public void writeByteToExternal(String dir, byte[] data, boolean isAppend){
        if(isAvailableExternal() && getRWPermission())
            writeByte(getSDpath() + dir, data, isAppend);
    }

    public void writeByteToInternal(String dir, byte[] data, boolean isAppend){
        writeByte(getFilesDir() + dir, data, isAppend);
    }


    public byte[] readFromRaw(int id){
        try {
            InputStream is = getResources().openRawResource(id);
            byte[] data = new byte[is.available()];
            is.read(data);
            return data;
        }catch(IOException e){
            return null;
        }
    }

    private boolean isAvailableExternal(){
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    private String getSDpath(){
        String sdPath = "";
        if(isAvailableExternal())
            sdPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
        else
            sdPath = getFilesDir().toString();
        return sdPath;
    }

    private boolean getRWPermission(){
        int permissioninfo = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if(permissioninfo == PackageManager.PERMISSION_GRANTED){
            externalGrant = true;
            return true;
        }else{
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                return false;
            }
            else{
                ActivityCompat.requestPermissions(this,new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},300);
            }
        }
        return externalGrant;
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode){
            case 300:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "권한 획득 성공", Toast.LENGTH_SHORT).show();
                    externalGrant = true;
                }
                else{
                    Toast.makeText(getApplicationContext(),"권한 획득 실패",Toast.LENGTH_SHORT).show();
                    externalGrant = false;
                }
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}
