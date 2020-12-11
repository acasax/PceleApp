package com.ResivoJe.PceleV3;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.ListFragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

public class DevicesFragment extends ListFragment {

    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDevice> AllDevice = new ArrayList<>();
    private ArrayList<BluetoothDevice> ValidDevice = new ArrayList<>();
    private ArrayAdapter<BluetoothDevice> listAdapter;
    Pattern sPattern = Pattern.compile("^BSRAM(\\d{5,7})$");
    Pattern mPattern = Pattern.compile("^BSram(\\d{5,7})$");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                // There are paired devices. Get the name and address of each paired device.
                for (BluetoothDevice device : pairedDevices) {
                    String deviceName = device.getName();
                    if (isValid(deviceName)) {
                        ValidDevice.add(device);
                    }
                }
            }
            listAdapter = new ArrayAdapter<BluetoothDevice>(getActivity(), 0, ValidDevice) {
                @Override
                public View getView(int p, View view, ViewGroup parent) {

                        if (view == null) {
                            view = getActivity().getLayoutInflater().inflate(R.layout.device_list_item, parent, false);
                        }
                        BluetoothDevice device = ValidDevice.get(p);
                        TextView text1 = view.findViewById(R.id.text1);
                        TextView text2 = view.findViewById(R.id.text2);
                        text1.setText(device.getName());
                        text2.setText(device.getAddress());

                    return view;
                }
            };

        }
    }

    boolean isValid(CharSequence s) {
        return sPattern.matcher(s).matches() || mPattern.matcher(s).matches();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(null);
        View header = getActivity().getLayoutInflater().inflate(R.layout.device_list_header, null, false);
        getListView().addHeaderView(header, null, false);
        setEmptyText("initializing...");
        ((TextView) getListView().getEmptyView()).setTextSize(18);
        setListAdapter(listAdapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_devices, menu);
        if(bluetoothAdapter == null)
            menu.findItem(R.id.bt_settings).setEnabled(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(bluetoothAdapter == null)
            setEmptyText("<Blutut nije dostupan na ovom urećaju>");
        else if(!bluetoothAdapter.isEnabled())
            setEmptyText("<Blutut je ugašen>");
        else
            setEmptyText("<Nema pronaćenih urećaja>");
        refresh();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.bt_settings) {
            Intent intent = new Intent();
            intent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
     //dodaj dugme za refres
    void refresh() {
        AllDevice.clear();
        if(bluetoothAdapter != null) {
            for (BluetoothDevice device : bluetoothAdapter.getBondedDevices())
                if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE)
                    AllDevice.add(device);
        }
        Collections.sort(AllDevice, DevicesFragment::compareTo);
        listAdapter.notifyDataSetChanged();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        BluetoothDevice device = ValidDevice.get(position-1);
        Bundle args = new Bundle();
        args.putString("device", device.getAddress());
        String[] devices = new String[ValidDevice.size()];
        for (int i = 0; i < ValidDevice.size(); i++)
            devices[i] = ValidDevice.get(i).getAddress();
        args.putStringArray("devices",devices);
        Fragment fragment = new TerminalFragment();
        fragment.setArguments(args);
        getFragmentManager().beginTransaction().replace(R.id.fragment, fragment, "terminal").addToBackStack(null).commit();
    }

    /**
     * sort by name, then address. sort named devices first
     */
    static int compareTo(BluetoothDevice a, BluetoothDevice b) {
        boolean aValid = a.getName()!=null && !a.getName().isEmpty();
        boolean bValid = b.getName()!=null && !b.getName().isEmpty();
        if(aValid && bValid) {
            int ret = a.getName().compareTo(b.getName());
            if (ret != 0) return ret;
            return a.getAddress().compareTo(b.getAddress());
        }
        if(aValid) return -1;
        if(bValid) return +1;
        return a.getAddress().compareTo(b.getAddress());
    }
}
