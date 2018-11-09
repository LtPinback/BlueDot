package com.stuffaboutcode.bluedot;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.view.Gravity;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;
import android.content.Intent;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.view.View;
import android.net.Uri;

import java.util.Set;
import java.util.ArrayList;

public class Devices extends AppCompatActivity {

    ListView devicelist;
    ImageButton infoButton;
    NoDefaultSpinner profileSpinner;
    public int commPort = 0;

    private BluetoothAdapter myBluetooth = null;
    public static String EXTRA_ADDRESS = "device_address";
    public static String EXTRA_NAME = "device_name";
    public static String EXTRA_PORT = "bt_port";
    public static int BT_ENABLE_REQUEST = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices);

        devicelist = (ListView)findViewById(R.id.listView);
        infoButton = (ImageButton) findViewById(R.id.infoButton);
        profileSpinner = (NoDefaultSpinner) findViewById(R.id.profileSpinner);

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, R.layout.simple_spinner_item, new ArrayList<>(Data.ConnProfiles.keySet()));
        dataAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        profileSpinner.setAdapter(dataAdapter);
        profileSpinner.setSelection(-1);

        profileSpinner.setOnItemSelectedListener(new mySpinnerListener());

        //if the device has bluetooth
        myBluetooth = BluetoothAdapter.getDefaultAdapter();

        if(myBluetooth == null) {
            Toast toast = Toast.makeText(getApplicationContext(), "Bluetooth Device Not Available.", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            toast.show();

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Devices.this.finish();
                    System.exit(0);
                    // Do nothing.
                }
            }, 2500);

        } else if(!myBluetooth.isEnabled()) {
            //Ask to the user turn the bluetooth on
            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBTon,BT_ENABLE_REQUEST);
        } else {

            pairedDevicesList();

        }

        infoButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Uri uri = Uri.parse("https://bluedot.readthedocs.io");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == BT_ENABLE_REQUEST && resultCode == RESULT_OK) {

            pairedDevicesList();

        } else {

            Toast toast = Toast.makeText(getApplicationContext(), "Permission To Use Bluetooth Device Not Granted.", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            toast.show();

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Devices.this.finish();
                    System.exit(0);
                    // Do nothing.
                }
            }, 2500);
        }
    }


    class mySpinnerListener implements Spinner.OnItemSelectedListener
    {
        @Override
        public void onItemSelected(AdapterView parent, View v, int position, long id) {
            commPort = Data.ConnProfiles.get(parent.getItemAtPosition(position).toString());
            Toast toast = Toast.makeText(parent.getContext(), parent.getItemAtPosition(position).toString() + " Selected.", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM, 0, 300);
            toast.show();
        }

        @Override
        public void onNothingSelected(AdapterView parent) {
            // TODO Auto-generated method stub
            // Do nothing.
        }
    }

    private void pairedDevicesList() {

        Toast toastA = Toast.makeText(getApplicationContext(), "Looking For Devices...", Toast.LENGTH_SHORT);
        toastA.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        toastA.show();

        Set<BluetoothDevice> pairedDevices = myBluetooth.getBondedDevices();
        ArrayList list = new ArrayList();
        String msg;

        if (pairedDevices.size()>0) {
            // create a list of paired bluetooth devices
            for(BluetoothDevice bt : pairedDevices)
            {
                list.add(bt.getName() + "\n" + bt.getAddress()); //Get the device's name and the address
            }
            if (pairedDevices.size()==1) {
                msg = "One Paired Bluetooth Device Found.";
            } else {
                msg = list.size() + " Paired Bluetooth Devices Found.";
            }
            Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            toast.show();
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            toast.show();
        }

        final ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
        devicelist.setAdapter(adapter);
        devicelist.setOnItemClickListener(myListClickListener); //Method called when the device from the list is clicked
    }

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener()
    {
        public void onItemClick (AdapterView<?> av, View v, int arg2, long arg3)
        {

            if (commPort==0) {

                Toast toast = Toast.makeText(getApplicationContext(), "Profile Not Selected. You Must Choose A Profile Before Trying To Connect To A Device.", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                toast.show();

            } else {

                // Get the device MAC address, the last 17 chars in the View
                String info = ((TextView) v).getText().toString();
                String deviceName = info.split("\n")[0];
                String address = info.split("\n")[1];

                // Make an intent to start next activity.
                Intent i = new Intent(Devices.this, Button.class);

                //Change the activity.
                i.putExtra(EXTRA_NAME, deviceName);
                i.putExtra(EXTRA_ADDRESS, address);
                i.putExtra(EXTRA_PORT, commPort);
                startActivity(i);
            }
        }
    };
}

