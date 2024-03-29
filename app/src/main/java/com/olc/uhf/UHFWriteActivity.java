package com.olc.uhf;

import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.olc.reader.BaseActivity;
import com.olc.reader.R;
import com.olc.reader.ReaderCtrlApp;
import com.olc.reader.service.uhf.IReadCallback;
import com.olc.reader.service.uhf.ITagSelectCallback;
import com.olc.reader.service.uhf.IWriteCallback;
import com.olc.util.CLog;
import com.olc.util.Cmd;
import com.olc.util.ErrorCodeInfo;
import com.olc.util.Util;

public class UHFWriteActivity extends BaseActivity implements View.OnClickListener {

    static final String TAG = "UHFWriteActivity";
    byte[] mEPC = null;

    TextView tv_selected_epc;
    TextView tv_info;
    RadioGroup rg;
    RadioButton rb_RESERVED,rb_EPC,rb_TID,rb_USER;
    EditText et_WordAdd, et_WordCnt, et_PassWord;
    EditText et_writeData;
    Button btn_write;

    boolean TagSelectState;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.uhf_write);

            setTitle(R.string.uhf_function_write);

            mEPC = getIntent().getByteArrayExtra("EPC");
            tv_selected_epc = (TextView) findViewById(R.id.tv_selected_epc);
            if(mEPC != null)
                tv_selected_epc.setText(Util.bytesToHexString(mEPC));

            tv_info = (TextView)findViewById(R.id.tv_info);
            tv_info.setMovementMethod(ScrollingMovementMethod.getInstance());

            rg = (RadioGroup)findViewById(R.id.rg);
            rb_RESERVED = (RadioButton)findViewById(R.id.rb_RESERVED) ;
            rb_EPC = (RadioButton)findViewById(R.id.rb_EPC) ;
            rb_TID = (RadioButton)findViewById(R.id.rb_TID) ;
            rb_USER = (RadioButton)findViewById(R.id.rb_USER) ;

            et_WordAdd = (EditText)findViewById(R.id.et_WordAdd);
            et_WordCnt = (EditText)findViewById(R.id.et_WordCnt);
            et_PassWord = (EditText)findViewById(R.id.et_PassWord);
            et_writeData = (EditText)findViewById(R.id.et_writeData);

            btn_write = (Button) findViewById(R.id.btn_write);
            findViewById(R.id.btn_tag_select).setOnClickListener(this);
            findViewById(R.id.btn_write).setOnClickListener(this);
            findViewById(R.id.btn_readTest).setOnClickListener(this);
            findViewById(R.id.btn_clear).setOnClickListener(this);

            btn_write.setEnabled(false);
            findViewById(R.id.btn_readTest).setEnabled(false);
            rb_EPC.setChecked(true);

            rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    switch (checkedId){
                        case R.id.rb_RESERVED:
                            et_WordAdd.setText("0");
                            et_WordCnt.setText("4");
                            et_writeData.setHint("1111222233334444");
                            break;
                        case R.id.rb_EPC:
                            et_WordAdd.setText("2");
                            et_WordCnt.setText("6");
                            et_writeData.setHint("111122223333444455556666");
                            break;
                        case R.id.rb_TID:
                            et_WordAdd.setText("0");
                            et_WordCnt.setText("6");
                            et_writeData.setHint("111122223333444455556666");
                            break;
                        case R.id.rb_USER:
                            et_WordAdd.setText("0");
                            et_WordCnt.setText("2");
                            et_writeData.setHint("11112222");
                            break;
                    }
                }
            });
        }catch (Exception e){
            CLog.e(TAG, "", e);
        }

    }

    @Override
    public void onClick(View v) {
        try {
            switch (v.getId()){
                case R.id.btn_tag_select:
                    tagSelect();
                    break;
                case R.id.btn_write:
                    write();
                    break;
                case R.id.btn_readTest:
                    read();
                    break;
                case R.id.btn_clear:
                    tv_info.setText("");
                    break;
            }
        }catch (Exception e){
            CLog.e(TAG, "", e);
        }

    }

    private void tagSelect() {
        try {
            if(mEPC == null){
                Toast.makeText(this, R.string.uhf_read_toast_tagselect, Toast.LENGTH_SHORT).show();
                return;
            }
            if(ReaderCtrlApp.iService != null){
                ITagSelectCallback tagSelectCallback = new ITagSelectCallback.Stub() {
                    @Override
                    public void response(final byte ErrorCode)  {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
	                            String ErrorCodeStr = ErrorCodeInfo.getErrorInfo(UHFWriteActivity.this, ErrorCode);
	                            ErrorCodeStr = String.format(getResources().getString(R.string.uhf_read_tagseecct_info), ErrorCodeStr);
	                            String info = tv_info.getText().toString().trim();
                                if(ErrorCode == Cmd.COMMOND_SUCCESS){
                                    btn_write.setEnabled(true);
                                    findViewById(R.id.btn_readTest).setEnabled(true);
                                    TagSelectState = true;
                                }else{
                                    btn_write.setEnabled(false);
                                    findViewById(R.id.btn_readTest).setEnabled(false);
                                    TagSelectState = false;
                                }
                                if(!"".equals(info)){
                                    info = info + "\r\n";
                                }
                                tv_info.setText(info + ErrorCodeStr);
                            }
                        });
                    }
                };
                ReaderCtrlApp.iService.SetTagSelected(tagSelectCallback, mEPC);
            }
        }catch (Exception e){
            CLog.e(TAG, "", e);
        }

    }

    private void write() {
        try {
            String PassWordStr = et_PassWord.getText().toString().trim();
            if(PassWordStr.length() != 8){
                Toast.makeText(this, R.string.uhf_kill_toast_pwd, Toast.LENGTH_SHORT).show();
                return;
            }
            PassWordStr = "".equals(PassWordStr) ? "00000000" : PassWordStr;
            byte[] PassWord = Util.stringToBytes(PassWordStr);

            if(!rb_RESERVED.isChecked()
                    && !rb_EPC.isChecked()
                    && !rb_TID.isChecked()
                    && !rb_USER.isChecked()){
                Toast.makeText(this, "Please check MemBank", Toast.LENGTH_SHORT).show();
                return;
            }

            byte MemBank = 0x01;
            if(rb_RESERVED.isChecked())
                MemBank = 0x00;
            if(rb_EPC.isChecked())
                MemBank = 0x01;
            if(rb_TID.isChecked())
                MemBank = 0x02;
            if(rb_USER.isChecked())
                MemBank = 0x03;

            String WordAddStr = et_WordAdd.getText().toString().trim();
            if("".equals(WordAddStr)){
                Toast.makeText(this, R.string.uhf_read_toast_wordadd, Toast.LENGTH_SHORT).show();
                return;
            }
	        WordAddStr = "".equals(WordAddStr) ? "0" : WordAddStr;
			byte[] WordAdd = Util.shortToByte(Short.parseShort(WordAddStr));

	        String WordCntStr = et_WordCnt.getText().toString().trim();
            if("".equals(WordCntStr)){
                Toast.makeText(this, R.string.uhf_read_toast_wordcnt, Toast.LENGTH_SHORT).show();
                return;
            }
            byte WordCnt =  Byte.parseByte(WordCntStr);

            String DataStr = et_writeData.getText().toString().trim();
            if(DataStr.length() == 0){
                Toast.makeText(this, R.string.uhf_write_data_toast, Toast.LENGTH_SHORT).show();
                return;
            }

            byte[] Data = Util.stringToBytes(DataStr);
            if(Data.length != WordCnt * 2){
                Toast.makeText(this, R.string.uhf_write_data_match_toast, Toast.LENGTH_SHORT).show();
                return;
            }

            if(ReaderCtrlApp.iService != null){
                IWriteCallback writeCallback = new IWriteCallback.Stub() {
                    @Override
                    public void success(byte[] PC, byte[] EPC, byte ErrCode)  {
                        result(ErrCode);
                    }

                    @Override
                    public void failed(byte ErrorCode)  {
                        result(ErrorCode);
                    }

                    void result(final byte ErrorCode){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String info = tv_info.getText().toString().trim();
                                if(!"".equals(info)){
                                    info = info + "\r\n";
                                }

                                String type = "EPC";
                                if(rb_RESERVED.isChecked())
                                    type = "RESERVED";
                                if(rb_EPC.isChecked())
                                    type = "EPC";
                                if(rb_TID.isChecked())
                                    type = "TID";
                                if(rb_USER.isChecked())
	                                type = "USER";
								String ErrorCodeStr = ErrorCodeInfo.getErrorInfo(UHFWriteActivity.this, ErrorCode);
								ErrorCodeStr = String.format(getResources().getString(R.string.uhf_write_info), type, ErrorCodeStr);
	                            tv_info.setText(info + ErrorCodeStr);
                            }
                        });
                    }
                };
                ReaderCtrlApp.iService.Write(writeCallback, PassWord, MemBank, WordAdd, WordCnt, Data);
            }
        }catch (Exception e){
            CLog.e(TAG, "", e);
        }

    }

    private void read(){
        try {
            if(!TagSelectState){
                Toast.makeText(this, R.string.uhf_read_toast_tagselect, Toast.LENGTH_SHORT).show();
                return;
            }

            if(!rb_RESERVED.isChecked()
                    && !rb_EPC.isChecked()
                    && !rb_TID.isChecked()
                    && !rb_USER.isChecked()){
                Toast.makeText(this, R.string.uhf_read_toast_membank, Toast.LENGTH_SHORT).show();
                return;
            }

            byte MemBank = 0x01;
            if(rb_RESERVED.isChecked())
                MemBank = 0x00;
            if(rb_EPC.isChecked())
                MemBank = 0x01;
            if(rb_TID.isChecked())
                MemBank = 0x02;
            if(rb_USER.isChecked())
                MemBank = 0x03;

            String WordAddStr = et_WordAdd.getText().toString().trim();
            if ("".equals(WordAddStr)) {
                Toast.makeText(this, R.string.uhf_read_toast_wordcnt, Toast.LENGTH_SHORT).show();
                return;
	        }
	        WordAddStr = "".equals(WordAddStr) ? "0" : WordAddStr;
			byte[] WordAdd = Util.shortToByte(Short.parseShort(WordAddStr));

            String WordCntStr = et_WordCnt.getText().toString().trim();
            if ("".equals(WordCntStr)) {
                Toast.makeText(this, R.string.uhf_read_toast_wordcnt, Toast.LENGTH_SHORT).show();
                return;
            }
            byte WordCnt =  Byte.parseByte(WordCntStr);

            String PassWordStr = et_PassWord.getText().toString().trim();
            if(PassWordStr.length() != 8){
                Toast.makeText(this, R.string.uhf_read_toast_pwd, Toast.LENGTH_SHORT).show();
                return;
            }
            PassWordStr = "".equals(PassWordStr) ? "00000000" : PassWordStr;
            byte[] PassWord = Util.stringToBytes(PassWordStr);

            if(ReaderCtrlApp.iService != null){
                final IReadCallback readCallback = new IReadCallback.Stub() {
                    @Override
                    public void success(byte[] PC, final byte[] EPC, final byte[] ReadData)  {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(!Util.bytesToHexString(EPC).equals(tv_selected_epc.getText().toString().trim())){
                                    return;
                                }
                                String info = tv_info.getText().toString().trim();
                                if(!"".equals(info)){
                                    info = info + "\r\n";
                                }
                                String type = "EPC";
                                if(rb_RESERVED.isChecked())
                                    type = "RESERVED";
                                if(rb_EPC.isChecked())
                                    type = "EPC";
                                if(rb_TID.isChecked())
                                    type = "TID";
                                if(rb_USER.isChecked())
                                    type = "USER";
                                tv_info.setText(String.format(getResources().getString(R.string.uhf_read_success_info), info, type, Util.bytesToHexString(ReadData)));
                            }
                        });
                    }

                    @Override
                    public void failed(final byte ErrorCode) {
                        result(ErrorCode);
                    }

                    void result(final byte ErrorCode){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String info = tv_info.getText().toString().trim();
                                if(!"".equals(info)){
                                    info = info + "\r\n";
                                }

                                String type = "EPC";
                                if(rb_RESERVED.isChecked())
                                    type = "RESERVED";
                                if(rb_EPC.isChecked())
                                    type = "EPC";
                                if(rb_TID.isChecked())
                                    type = "TID";
                                if(rb_USER.isChecked())
                                    type = "USER";
								String ErrorCodeStr = ErrorCodeInfo.getErrorInfo(UHFWriteActivity.this, ErrorCode);
								ErrorCodeStr = String.format(getResources().getString(R.string.uhf_read_failed_info), type, ErrorCodeStr);
	                            tv_info.setText(info + ErrorCodeStr);
                            }
                        });
                    }
                };

                ReaderCtrlApp.iService.Read(readCallback, MemBank, WordAdd, WordCnt, PassWord);
            }
        }catch (Exception e){
            CLog.e(TAG, "", e);
        }

    }

}
