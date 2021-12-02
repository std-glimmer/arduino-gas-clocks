package com.example.usb_arduino_logger;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity
{
    public final int DEFAULT_VENDOR_ID = 0x1A86;
    public final String ACTION_USB_PERMISSION = "USB_PERMISSION";

    private ArrayAdapter<String> listAdapter = null;
    private Button connectButton = null;
    private TextView clockDisplay = null;
    private EditText vendorIDEdit = null;

    boolean _isConnected = false;

    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        listAdapter = new ArrayAdapter<>(this,  android.R.layout.simple_list_item_1);
        ListView logsListView = findViewById(R.id.logsList);
        logsListView.setAdapter(listAdapter);
        connectButton = findViewById(R.id.connectButton);
        clockDisplay = findViewById(R.id.clockDisplay);
        vendorIDEdit = findViewById(R.id.vendorIDEdit);

        usbManager = (UsbManager) getSystemService(USB_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);
    }

    public int getVendorID()
    {
        String userInput = vendorIDEdit.getText().toString();

        if (userInput.isEmpty())
        {
            return DEFAULT_VENDOR_ID;
        }
        else
        {
            return Integer.decode(userInput);
        }
    }

    public void onClickConnect(View view)
    {
        try
        {
            if (!_isConnected)
            {
                int neededID = getVendorID();

                listAdapter.add("Ищем " + neededID);

                HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
                if (!usbDevices.isEmpty())
                {
                    boolean keep = true;
                    for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet())
                    {
                        device = entry.getValue();
                        int deviceVID = device.getVendorId();

                        if (deviceVID == neededID)
                        {
                            listAdapter.add("Устройство найдено");

                            final Intent piIntent = new Intent(ACTION_USB_PERMISSION);
                            piIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                            PendingIntent pi = PendingIntent.getBroadcast(this, 0, piIntent, 0);
                            usbManager.requestPermission(device, pi);
                            keep = false;
                        }
                        else
                        {
                            connection = null;
                            device = null;
                        }

                        if (!keep)
                            break;
                    }
                }
            }
            else
            {
                serialPort.close();
                _isConnected = false;
                connectButton.setText("Подключиться");
                clockDisplay.setText("");
            }
        }
        catch (Exception exception)
        {
            listAdapter.add(exception.getMessage());
        }
    }

    public void onClickClear(View view)
    {
        listAdapter.clear();
    }

    UsbSerialInterface.UsbReadCallback usbReadCallback = new UsbSerialInterface.UsbReadCallback()
    {
        @Override
        public void onReceivedData(byte[] arg0)
        {
            try
            {
                String data = new String(arg0, StandardCharsets.UTF_8);
                clockDisplay.setText(data);
            }
            catch (Exception exception)
            {
                listAdapter.add(exception.getMessage());
            }
        }
    };

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            try
            {
                if (intent.getAction().equals(ACTION_USB_PERMISSION))
                {
                    boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                    if (granted)
                    {
                        listAdapter.add("Разрешение на использование устройства получено");
                        connection = usbManager.openDevice(device);
                        serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);

                        if (serialPort != null)
                        {
                            if (serialPort.open())
                            {
                                serialPort.setBaudRate(9600);
                                serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                                serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                                serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                                serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                                serialPort.read(usbReadCallback);

                                listAdapter.add("Serial Connection Opened!");

                                _isConnected = true;
                                connectButton.setText("Отключиться");
                            }
                            else
                            {
                                Log.d("SERIAL", "PORT NOT OPEN");
                                listAdapter.add("PORT NOT OPEN");
                            }
                        }
                        else
                        {
                            Log.d("SERIAL", "PORT IS NULL");
                            listAdapter.add("PORT IS NULL");
                        }
                    }
                    else
                    {
                        Log.d("SERIAL", "PERM NOT GRANTED");
                        listAdapter.add("Отказано в доступе");
                    }
                }
            }
            catch (Exception exception)
            {
                listAdapter.add(exception.getMessage());
            }
        }
    };
}
