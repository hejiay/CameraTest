package com.wildma.wildmaidcardcamera;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.wildma.idcardcamera.camera.IDCardCamera;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Author   wildma
 * Github   https://github.com/wildma
 * Date     2018/6/24
 * Desc     ${身份证相机使用例子}
 */
public class MainActivity extends AppCompatActivity {
    public ImageView mIvFront;
    private EditText numberCharacters;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mIvFront = (ImageView) findViewById(R.id.iv_front);
        numberCharacters = (EditText)findViewById(R.id.number_character_editText);
        editor = getSharedPreferences("number_characters", MODE_PRIVATE).edit();
        //mIvBack = (ImageView) findViewById(R.id.iv_back);
    }


    /**
     * 身份证正面
     */
    public void front(View view) {
        String numberString = numberCharacters.getText().toString();
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher matcher = pattern.matcher(numberString);
        if (!numberString.equals("")) {
            if (matcher.matches()) {
                int numberInt = Integer.parseInt(numberString);
                editor.putInt("number_character", numberInt);
                editor.apply();
                IDCardCamera.create(this).openCamera(numberInt);
            } else {
                Toast.makeText(this, "请输入字符个数(数字类型)", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "请输入字符个数(数字类型)", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 身份证反面
     */
//    public void back(View view) {
//        IDCardCamera.create(this).openCamera(IDCardCamera.TYPE_IDCARD_BACK);
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == IDCardCamera.RESULT_CODE) {
            //获取图片路径，显示图片
            final String path = IDCardCamera.getImagePath(data);
            Log.d("图片路径：", path);
            if (!TextUtils.isEmpty(path)) {
                if (requestCode == IDCardCamera.TYPE_IDCARD_FRONT) {
                    mIvFront.setImageBitmap(BitmapFactory.decodeFile(path));
                }
            }
        }
    }
}
