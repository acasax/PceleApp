package com.ResivoJe.PceleV3;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

public class btsetings extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private static final String TAG = "BT";
    BluetoothAdapter mBluetoothAdapter;
    Button btnEnableDisable_Discoverable;

    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    Thread pairingThread = new Thread();
    public DeviceListAdapter mDeviceListAdapter;

    ListView lvNewDevices;
    ProgressDialog dialog = null;
    Pattern sPattern = Pattern.compile("^BS(RAM|ram)((([A-Z])|([a-z])|([0-9])){0,18})$");

    boolean isValid(CharSequence s) {
        return sPattern.matcher(s).matches();
    }
    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch(state){
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }

            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //change button back to "Start"
                if (dialog != null) {
                    if (dialog.isShowing())
                        dialog.dismiss();
                }
                //report user
                Log.d(TAG,"Finished");
            }
        }
    };

    /**
     * Broadcast Receiver for changes made to bluetooth states such as:
     * 1) Discoverability mode on/off or expire.
     */
    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {

                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    //Device is in Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    //Device not in discoverable mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: Connecting....");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: Connected.");
                        break;
                }

            }
        }
    };




    /**
     * Broadcast Receiver for listing devices that are not yet paired
     * -Executed by btnDiscover() method.
     */
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)) {

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device == null) {
                    return;
                }
                if (device.getName() == null) {
                    return;
                }
                if (isValid(device.getName())) {
                    Log.d(TAG, "onReceive: " + device.getName());
                    for (int i = 0; i < mBTDevices.size(); i++) {
                        if (mBTDevices.get(i).getName().equals(device.getName())) {
                            return;
                        }
                    }
                    if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                        Log.d(TAG, "onReceive: " + device.getName() + " is PAIRED");
                        return;
                    }
                    device.setPin("7214".getBytes());
                    mBTDevices.add(device);
                    mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                    lvNewDevices.setAdapter(mDeviceListAdapter);

                }
            }

        }
    };


    /**
     * Broadcast Receiver that detects bond state changes (Pairing status changes)
     */
    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    synchronized (pairingThread) {
                        if (pairingThread != null) {
                            pairingThread.notifyAll();
                            Log.d(TAG, "NOTIFY");
                        }
                    }
                    //getResources().getString(R.string.upali_blutut);
                }
                //case2: creating a bone
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    checkBTPermissions();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < 5; i++) {
                                List<Fragment> fragments = btsetings.this.getSupportFragmentManager().getFragments();
                                if (fragments != null) {
                                    for (Fragment fragment : fragments) {
                                        if (fragment instanceof DialogFragment) {
                                            ((DialogFragment) fragment).dismiss();
                                        }
                                    }
                                }

                                if (mDevice.setPin("7214".getBytes())) {
                                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING. ENTERING THE PIN SUCCESSFULLY");
                                    break;
                                } else {
                                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING. ENTERING THE PIN UNSUCCESSFULLY");
                                }
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }).start();

                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
            }
        }
    };



    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        if (mBroadcastReceiver1 != null)
            try {
                unregisterReceiver(mBroadcastReceiver1);
            }catch (IllegalArgumentException e){
                e.printStackTrace();
            }
        if (mBroadcastReceiver3 != null)
            try {
                unregisterReceiver(mBroadcastReceiver3);
            }catch (IllegalArgumentException e){
                e.printStackTrace();
            }
        if (mBroadcastReceiver4 != null)
            try {
                unregisterReceiver(mBroadcastReceiver4);
            }catch (IllegalArgumentException e){
                e.printStackTrace();
            }
        if(mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.btsetings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(Color.BLACK);
        Button btnONOFF = (Button) findViewById(R.id.btnONOFF);
        btnEnableDisable_Discoverable = (Button) findViewById(R.id.btnDiscoverable_on_off);
        lvNewDevices = (ListView) findViewById(R.id.lvNewDevices);
        mBTDevices = new ArrayList<>();

        //Broadcasts when bond state changes (ie:pairing)

        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver1, BTIntent);

//
//        IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
//        registerReceiver(mBroadcastReceiver2, discoverDevicesIntent);

        IntentFilter discoverIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mBroadcastReceiver3, discoverIntent);

        IntentFilter finishIntent = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mBroadcastReceiver1, finishIntent);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        lvNewDevices.setOnItemClickListener(btsetings.this);

        btnONOFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: enabling/disabling bluetooth.");
                enableDisableBT();
            }
        });

    }

    public void enableDisableBT(){
        if(mBluetoothAdapter == null){
            Log.d(TAG, "enableDisableBT: Does not have BT capabilities.");
        }
        if(!mBluetoothAdapter.isEnabled()){
            Log.d(TAG, "enableDisableBT: enabling BT.");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

        }
        if(mBluetoothAdapter.isEnabled()){
            Log.d(TAG, "enableDisableBT: disabling BT.");
            mBluetoothAdapter.disable();
        }

    }

    public void PairAll(View view) {
//        Log.d(TAG, "btnEnableDisable_Discoverable: Making device discoverable for 300 seconds.");
//
//        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
//        startActivity(discoverableIntent);
//
//        IntentFilter intentFilter = new IntentFilter(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
//        registerReceiver(mBroadcastReceiver2,intentFilter);

        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Pairing devices");
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setProgress(0);
        progress.setMax(mBTDevices.size());
        progress.show();
        checkBTPermissions();

        pairingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    if (mBTDevices != null) {

                        for (int i = 0; i < mBTDevices.size(); i++) {
                            mBTDevices.get(i).setPin("7214".getBytes());
                            mBTDevices.get(i).createBond();
                            mBTDevices.get(i).setPin("7214".getBytes());

                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            mBTDevices.get(i).setPin("7214".getBytes());
                            progress.setProgress(i+1);
                            progress.show();
                            Log.d("BT", "Index " + i + " Number of devices" + mBTDevices.size());
                        }
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.dismiss();
                                mBTDevices.clear();
                                lvNewDevices.clearChoices();
                            }
                        });
                    }
                }

                pairingThread = new Thread();
            }
        });
        pairingThread.start();
    }

    public void btnDiscover(View view) {
        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");

        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "btnDiscover: Canceling discovery.");

            //check BT permissions in manifest
            checkBTPermissions();

            mBTDevices.clear();
            lvNewDevices.clearChoices();
            mBluetoothAdapter.startDiscovery();

            dialog = ProgressDialog.show(btsetings.this, "",
                    getResources().getString(R.string.loading), true);
            new Thread(new Runnable() {
                @Override
                public void run() {

                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (dialog != null && dialog.isShowing()){
                        dialog.dismiss();
                    }
                }
            }).start();
        }
        if(!mBluetoothAdapter.isDiscovering()){


            //check BT permissions in manifest
            checkBTPermissions();

            mBTDevices.clear();
            mBluetoothAdapter.startDiscovery();

            dialog = ProgressDialog.show(btsetings.this, "",
                    getResources().getString(R.string.loading), true);
            new Thread(new Runnable() {
                @Override
                public void run() {

                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (dialog != null && dialog.isShowing()){
                        dialog.dismiss();
                    }
                }
            }).start();
        }
    }

    /**
     * This method is required for all devices running API23+
     * Android must programmatically check the permissions for bluetooth. Putting the proper permissions
     * in the manifest is not enough.
     *
     * NOTE: This will only execute on versions > LOLLIPOP because it is not needed otherwise.
     */

    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M){

            if (this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION")  != PackageManager.PERMISSION_GRANTED)
            {
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 5250); //Any number
            }

            if (this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION")  != PackageManager.PERMISSION_GRANTED)
            {
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 5251); //Any number
            }

            if (this.checkSelfPermission("Manifest.permission.BLUETOOTH")  != PackageManager.PERMISSION_GRANTED)
            {
                this.requestPermissions(new String[]{Manifest.permission.BLUETOOTH}, 5253); //Any number
            }

            if (this.checkSelfPermission("Manifest.permission.BLUETOOTH_ADMIN")  != PackageManager.PERMISSION_GRANTED)
            {
                this.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 5252); //Any number
            }

        }else{
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        //first cancel discovery because its very memory intensive.
        mBluetoothAdapter.cancelDiscovery();

        Log.d(TAG, "onItemClick: You Clicked on a device.");
        String deviceName = mBTDevices.get(i).getName();
        String deviceAddress = mBTDevices.get(i).getAddress();

        Log.d(TAG, "onItemClick: deviceName = " + deviceName);
        Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

        //create the bond.
        //NOTE: Requires API 17+? I think this is JellyBean
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
            Log.d(TAG, "Trying to pair with " + deviceName);

            String pin = "7214";

            new Thread(new Runnable() {
                @Override
                public void run() {
                    mBTDevices.get(i).setPin("7214".getBytes());
                    mBTDevices.get(i).createBond();
                    mBTDevices.get(i).setPin("7214".getBytes());

                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mBTDevices.get(i).setPin("7214".getBytes());
                }
            }).start();

        }
    }


}
