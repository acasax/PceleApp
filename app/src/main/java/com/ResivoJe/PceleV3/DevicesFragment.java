package com.ResivoJe.PceleV3;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.ListFragment;

import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ResivoJe.PceleV3.databinding.DeviceListItemBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class DevicesFragment extends ListFragment {

    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDevice> allDevices = new ArrayList<>();
    private ArrayList<BluetoothDevice> validDevices = new ArrayList<>();
    private ArrayAdapter<BluetoothDevice> listAdapter;
    Pattern sPattern = Pattern.compile("^BS(RAM|ram)((([A-Z])|([a-z])|([0-9])){0,18})$");
    private static final String TAG = "DEVICES";
    private Menu menu;
    private MenuItem i0, i1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                Toast.makeText(getContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();
            } else if (!bluetoothAdapter.isEnabled()) {
                Toast.makeText(getContext(), "Turn on bluetooth", Toast.LENGTH_LONG).show();
                Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnBTon, 1);
            } else {
                listPairedDevices();
            }
        }
    }

    private void listPairedDevices() {
        bluetoothAdapter.startDiscovery();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                if (isValid(deviceName)) {
                    validDevices.add(device);
                }
            }
        }
        listAdapter = new ArrayAdapter<BluetoothDevice>(getActivity(), 0, validDevices) {
            @Override
            public View getView(int p, View view, ViewGroup parent) {
                if (view == null) {
                    view = getActivity().getLayoutInflater().inflate(R.layout.device_list_item, parent, false);
                }
                DeviceListItemBinding deviceListItemBinding = DeviceListItemBinding.bind(view);
                BluetoothDevice device = validDevices.get(p);
                deviceListItemBinding.text1.setText(device.getName());
                deviceListItemBinding.text2.setText(device.getAddress());

                return view;
            }
        };
        setListAdapter(listAdapter);
    }

    private boolean isValid(CharSequence s) {
        return sPattern.matcher(s).matches();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(null);
        View header = getActivity().getLayoutInflater().inflate(R.layout.device_list_header, null, false);
        getListView().addHeaderView(header, null, false);
        setEmptyText(getResources().getText(R.string.initializing));
        ((TextView) getListView().getEmptyView()).setTextSize(18);
        setListAdapter(listAdapter);
        if (validDevices.size() > 0) {
            Button button = header.findViewById(R.id.chooseAll_btn2);
            button.setOnClickListener(v -> chooseAllButton());
            //Button button1 = header.findViewById(R.id.start_bt_property);
            //button1.setOnClickListener(v -> openNewActivity());
        }
    }

    public void openNewActivity() {
        Intent intent = new Intent(getActivity(), BTSettings.class);
        startActivity(intent);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_devices, menu);
        this.menu = menu;
        this.i0 = menu.getItem(2);
        this.i1 = menu.getItem(3);
    }

    @Override
    public void onResume() {
        super.onResume();

        refresh();
    }

    private void setAppLocale(String localeCode) {
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            conf.setLocale(new Locale(localeCode.toLowerCase()));
        } else {
            conf.locale = new Locale(localeCode.toLowerCase());
        }
        res.updateConfiguration(conf, dm);
        getActivity().onConfigurationChanged(getResources().getConfiguration());
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.drkajBlutut:
                openNewActivity();
                return true;
            case R.id.clear:
                refresh();
                return true;
            case R.id.en_lang:
                setAppLocale("en");
                i0.setTitle(R.string.engelski);
                i1.setTitle(R.string.srpski);
                return true;
            case R.id.sr_lang:
                setAppLocale("rs");
                i0.setTitle(R.string.engelski);
                i1.setTitle(R.string.srpski);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //dodaj dugme za refres
    void refresh() {
        allDevices.clear();
        if (bluetoothAdapter == null)
            setEmptyText(getResources().getText(R.string.Blutut));
        else if (!bluetoothAdapter.isEnabled())
            setEmptyText(getResources().getText(R.string.Blutut1));
        else {
            if (getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                validDevices.clear();
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                bluetoothAdapter.startDiscovery();
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                if (pairedDevices.size() > 0) {
                    // There are paired devices. Get the name and address of each paired device.
                    for (BluetoothDevice device : pairedDevices) {
                        String deviceName = device.getName();
                        if (isValid(deviceName)) {
                            validDevices.add(device);
                        }
                    }
                }
                listAdapter = new ArrayAdapter<BluetoothDevice>(getActivity(), 0, validDevices) {
                    @Override
                    public View getView(int p, View view, ViewGroup parent) {

                        if (view == null) {
                            view = getActivity().getLayoutInflater().inflate(R.layout.device_list_item, parent, false);
                        }
                        BluetoothDevice device = validDevices.get(p);
                        TextView text1 = view.findViewById(R.id.text1);
                        TextView text2 = view.findViewById(R.id.text2);
                        text1.setText(device.getName());
                        text2.setText(device.getAddress());

                        return view;
                    }
                };
                setListAdapter(listAdapter);

            }
            if (validDevices.size() == 0) {
                setEmptyText(getResources().getText(R.string.Blutut2));
            }
        }
        if (bluetoothAdapter != null) {
            for (BluetoothDevice device : bluetoothAdapter.getBondedDevices())
                if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE)
                    allDevices.add(device);
        }
        Collections.sort(allDevices, DevicesFragment::compareTo);
        if (listAdapter != null)
            listAdapter.notifyDataSetChanged();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        BluetoothDevice device = validDevices.get(position - 1);
        Bundle args = new Bundle();
        args.putBoolean("sendAll", false);
        args.putString("device", device.getAddress());
        String[] devices = new String[1];
        String[] devicesNames = new String[1];
        devices[0] = device.getAddress();
        devicesNames[0] = device.getName();
        args.putStringArray("devices", devices);

        // Patch
        Intent i = new Intent(getActivity(), TerminalActivity.class);
        i.putExtra(TerminalActivity.DEVICES_EXTRA, devices);
        i.putExtra(TerminalActivity.DEVICES_NAMES_EXTRA, devicesNames);
        i.putExtra(TerminalActivity.SEND_ALL_EXTRA, false);
        startActivity(i);

//        Fragment fragment = new TerminalFragment();
//        fragment.setArguments(args);
//        getFragmentManager().beginTransaction().replace(R.id.fragment, fragment, "terminal").addToBackStack(null).commit();
    }

    public void chooseAllButton() {
        BluetoothDevice device = validDevices.get(0);
        Bundle args = new Bundle();
        args.putBoolean("sendAll", true);
        args.putString("device", device.getAddress());
        String[] devices = new String[validDevices.size()];
        String[] devicesNames = new String[validDevices.size()];
        for (int i = 0; i < validDevices.size(); i++) {
            devices[i] = validDevices.get(i).getAddress();
            devicesNames[i] = validDevices.get(i).getName();
        }
        args.putStringArray("devices", devices);

        // Patch
        Intent i = new Intent(getActivity(), TerminalActivity.class);
        i.putExtra(TerminalActivity.DEVICES_EXTRA, devices);
        i.putExtra(TerminalActivity.DEVICES_NAMES_EXTRA, devicesNames);
        i.putExtra(TerminalActivity.SEND_ALL_EXTRA, true);
        startActivity(i);

//        Fragment fragment = new TerminalFragment();
//        fragment.setArguments(args);
//        getFragmentManager().beginTransaction().replace(R.id.fragment, fragment, "terminal").addToBackStack(null).commit();
    }

    /**
     * sort by name, then address. sort named devices first
     */
    static int compareTo(BluetoothDevice a, BluetoothDevice b) {
        boolean aValid = a.getName() != null && !a.getName().isEmpty();
        boolean bValid = b.getName() != null && !b.getName().isEmpty();
        if (aValid && bValid) {
            int ret = a.getName().compareTo(b.getName());
            if (ret != 0) return ret;
            return a.getAddress().compareTo(b.getAddress());
        }
        if (aValid) return -1;
        if (bValid) return +1;
        return a.getAddress().compareTo(b.getAddress());
    }

}
