package com.stuffaboutcode.bluedot;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;
import android.app.ProgressDialog;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import java.io.IOException;

public class Button extends AppCompatActivity {

    String address = null;
    String deviceName = null;

    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    private boolean connectionLost = false;
    int btPort;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private ProgressDialog progress;
    private double last_x = 0;
    private double last_y = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_button);

        Intent newint = getIntent();
        deviceName = newint.getStringExtra(Devices.EXTRA_NAME);
        address = newint.getStringExtra(Devices.EXTRA_ADDRESS);
        btPort = newint.getIntExtra(Devices.EXTRA_PORT, 0);

        TextView statusView = (TextView)findViewById(R.id.status);

        final View roundButton = (View)findViewById(R.id.roundButton);

        statusView.setText("Connecting to " + deviceName + " via port " + btPort);

        new ConnectBT().execute();

        roundButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    pressed(roundButton, event);

                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    released(roundButton, event);

                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    moved(roundButton, event);
                }
                return false;
            }
        });

    }

    private void pressed(View roundButton, MotionEvent event) {
        double x = calcX(roundButton, event);
        double y = calcY(roundButton, event);
        send(buildMessage("1", x, y));
        last_x = x;
        last_y = y;
    }

    private void released(View roundButton, MotionEvent event) {
        double x = calcX(roundButton, event);
        double y = calcY(roundButton, event);
        send(buildMessage("0", x, y));
        last_x = x;
        last_y = y;
    }

    private void moved(View roundButton, MotionEvent event) {
        double x = calcX(roundButton, event);
        double y = calcY(roundButton, event);
        //has x or y changed?
        if ((x != last_x) || (y != last_y)) {
            send(buildMessage("2", x, y));
            last_x = x;
            last_y = y;
        }
    }

    private double calcX(View roundButton, MotionEvent event) {
        double x = (event.getX() - (roundButton.getWidth() / 2)) / (roundButton.getWidth() / 2);
        x = (double)Math.round(x * 10000d) / 10000d;
        return x;
    }

    private double calcY(View roundButton, MotionEvent event) {
        double y = (event.getY() - (roundButton.getHeight() / 2)) / (roundButton.getHeight() /2) * -1;
        y = (double)Math.round(y * 10000d) / 10000d;
        return y;
    }

    private String buildMessage(String operation, double x, double y) {
        return (operation + "," + String.valueOf(x) + "," + String.valueOf(y) + "\n");
    }

    private void send(String message) {
        if (btSocket!=null) {
            try {
                btSocket.getOutputStream().write(message.getBytes());
            } catch (IOException e) {
                msg("Error : " + e.getMessage());
                if(e.getMessage().contains("Broken pipe")) Disconnect();
            }
        } else {
            msg("Error : btSocket == null");
        }
    }

    private void Disconnect() {
        if (btSocket!=null) {
            try {
                isBtConnected = false;
                btSocket.close();
            } catch (IOException e) {
                msg("Error");
            }
        }
        Toast.makeText(getApplicationContext(),"Disconnected",Toast.LENGTH_LONG).show();
        finish();
    }

    private void msg(String message) {
        TextView statusView = (TextView)findViewById(R.id.status);
        statusView.setText(message);
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean ConnectSuccess = true;

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(Button.this, "Connecting", "Please wait...");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) { //while the progress dialog is shown, the connection is done in background
            try {
                if (btSocket == null || !isBtConnected) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    Method m = null;
                    try {
                        m = dispositivo.getClass().getMethod("createInsecureRfcommSocket", new Class[] {int.class});
                        btSocket = (BluetoothSocket) m.invoke(dispositivo,btPort);
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                    //btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            } catch (IOException e) {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) { //after the doInBackground, it checks if everything went fine
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                Toast.makeText(getApplicationContext(), "Failed to connect", Toast.LENGTH_LONG).show();
                finish();
            } else {
                msg("Connected to " + deviceName + " via port " + btPort);
                isBtConnected = true;
                // start the connection monitor
                new MonitorConnection().execute();
            }
            progress.dismiss();
        }
    }

    private class MonitorConnection extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... devices) {
            while (!connectionLost) {
                try {
                    //read from the buffer, when this errors the connection is lost
                    // this was the only reliable way I found of monitoring the connection
                    // .isConnected didnt work
                    // BluetoothDevice.ACTION_ACL_DISCONNECTED didnt fire
                    btSocket.getInputStream().read();
                } catch (IOException e) {
                    connectionLost = true;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            // if the bt is still connected, the connection must have been lost
            if (isBtConnected) {
                try {
                    isBtConnected = false;
                    btSocket.close();
                } catch (IOException e) {
                    // nothing doing, we are ending anyway!
                }
                Toast.makeText(getApplicationContext(), "Connection lost", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        Disconnect();
    }
}
