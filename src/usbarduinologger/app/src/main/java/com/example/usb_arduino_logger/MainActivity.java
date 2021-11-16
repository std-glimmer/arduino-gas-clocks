package com.example.usb_arduino_logger;

import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity implements Runnable
{
    private UsbManager mUsbManager;
    UsbAccessory mAccessory;
    FileInputStream mInputStream;
    FileOutputStream mOutputStream;

    private ArrayAdapter<String> listAdapter = null;
    private ListView listView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listView);
        listAdapter = new ArrayAdapter<String>(this,  android.R.layout.simple_list_item_1);
        listView.setAdapter(listAdapter);
        listAdapter.add("Готовы к принятию сообщений");
    }

    private void openAccessory(UsbAccessory accessory)
    {
        ParcelFileDescriptor mFileDescriptor = mUsbManager.openAccessory(accessory);

        if (mFileDescriptor != null)
        {
            mAccessory = accessory;
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();

            mInputStream = new FileInputStream(fd);
            //mOutputStream = new FileOutputStream(fd);

            Thread thread = new Thread(null, this, "AccessoryThread");
            thread.start();
        }
    }

    public void run()
    {
        int ret = 0;
        byte[] buffer = new byte[16384];
        int i;

        while (ret >= 0) {
            // получение входящих сообщений
            try
            {
                ret = mInputStream.read(buffer);

                // DEBUG
                if (ret >= 3)
                {
                    int command = buffer[0];
                    int target = buffer[1];
                    byte value = buffer[2];

                    listAdapter.add(
                            "Команда " + command + "\n" +
                            "Модуль " + target + "\n" +
                            "Значение " + value + "\n");
                }

            } catch (IOException e) {
                listAdapter.add(e.toString());
            }
        }
    }

    // пример использования - включить красный светодиод на полную яркость:
    // mActivity.sendCommand((byte)2, (byte)0, (byte)255)
//    public void sendCommand(byte command, byte target, int value)
//    {
//        byte[] buffer = new byte[3];
//        if (value > 255)
//            value = 255;
//
//        buffer[0] = command;
//        buffer[1] = target;
//        buffer[2] = (byte) value;
//        if (mOutputStream != null && buffer[1] != -1)
//        {
//            try
//            {
//                mOutputStream.write(buffer);
//            }
//            catch (IOException e)
//            {
//
//            }
//        }
//    }
}