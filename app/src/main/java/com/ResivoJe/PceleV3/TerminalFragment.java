package com.ResivoJe.PceleV3;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener {

    private enum Connected { False, Pending, True }

    private String deviceAddress;
    private String newline = "\r\n";

    private TextView receiveText;

    private SerialSocket socket;
    private SerialService service;
    private boolean initialStart = true;
    private Connected connected = Connected.False;

    //Data for send
    String start = "s";
    String stop  = "x";

    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceAddress = getArguments().getString("device");
    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if(service != null && !getActivity().isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation") // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try { getActivity().unbindService(this); } catch(Exception ignored) {}
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(initialStart && service !=null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        if(initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());

        View sendBtn         = view.findViewById(R.id.send_btn);
        View stopBtn         = view.findViewById(R.id.stop_btn);
        View plusTimeBtn     = view.findViewById(R.id.plusTimeBtn);
        View minusTimeBtn    = view.findViewById(R.id.minusTimeBtn);
        View plusFrekBtn     = view.findViewById(R.id.plusFrekBtn);
        View minusFrekBtn    = view.findViewById(R.id.minusFrekBtn);
        View plusImpulsBtn   = view.findViewById(R.id.plusImpulsBtn);
        View minusImpulsBtn  = view.findViewById(R.id.minusImpulsBtn);
        View plusPauseBtn    = view.findViewById(R.id.plusPauseBtn);
        View minusPauseBtn   = view.findViewById(R.id.minusPauseBtn);
        View plusVoltageBtn  = view.findViewById(R.id.plusVoltageBtn);
        View minusVoltageBtn = view.findViewById(R.id.minusVoltageBtn);
        TextView time        = view.findViewById(R.id.timeTxt);
        TextView frek        = view.findViewById(R.id.frekTxt);
        TextView impuls      = view.findViewById(R.id.impulsTxt);
        TextView pause        = view.findViewById(R.id.pauseTxt);
        TextView voltage      = view.findViewById(R.id.voltageTxt);

        plusTimeBtn.setOnClickListener(v -> Steps(time, "+", 10, 0, 120));
        minusTimeBtn.setOnClickListener(v -> Steps(time, "-", 10, 0, 120));
        plusFrekBtn.setOnClickListener(v -> Steps(frek, "+", 10, 100, 1200));
        minusFrekBtn.setOnClickListener(v -> Steps(frek, "-", 10, 100, 1200));
        plusImpulsBtn.setOnClickListener(v -> Steps(impuls, "+", 1, 0, 25));
        minusImpulsBtn.setOnClickListener(v -> Steps(impuls, "-", 1, 0, 25));
        plusPauseBtn.setOnClickListener(v -> Steps(pause, "+", 1, 0, 25));
        minusPauseBtn.setOnClickListener(v -> Steps(pause, "-", 1, 0, 25));
        plusVoltageBtn.setOnClickListener(v -> Steps(voltage, "+", 1, 10, 35));
        minusVoltageBtn.setOnClickListener(v -> Steps(voltage, "-", 1, 10, 35));

        stopBtn.setOnClickListener(v -> send(stop));
        sendBtn.setOnClickListener(v -> send(start + "t" + time.getText() + "f" + frek.getText() + "i" + impuls.getText() + "p" + pause.getText() + "n" + voltage.getText()));

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
    }



    /*
     * Serial + UI
     */
    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            String deviceName = device.getName() != null ? device.getName() : device.getAddress();
            status("Povezivanje...");
            connected = Connected.Pending;
            socket = new SerialSocket();
            service.connect(this, "Povezano sa " + deviceName);
            socket.connect(getContext(), service, device);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
        socket.disconnect();
        socket = null;
    }

    private void send(String str) {
        if(connected != Connected.True) {
            Toast.makeText(getActivity(), "nije povezano", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            SpannableStringBuilder spn = new SpannableStringBuilder(str+'\n');
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            receiveText.setText(spn);
            byte[] data = (str + newline).getBytes();
            socket.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    private void receive(byte[] data) {
        receiveText.setText(new String(data));
    }

    private void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str+'\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.setText(spn);
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("Povezano");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("Greška: " + e.getMessage());
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        receive(data);
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("Konekcija prekinuta: " + e.getMessage());
        disconnect();
    }

    public void Steps(TextView a, String s, int Step, int minValue, int maxValue){
        String currentValue = (String) a.getText();
        int time = Integer.parseInt(currentValue);
        if (s == "+"){
            if (time < maxValue){
                time = time + Step;
                currentValue = String.valueOf(time);
                a.setText(currentValue);
            }
        }else if (s == "-"){
            if (time > minValue){
                time = time - Step;
                currentValue = String.valueOf(time);
                a.setText(currentValue);
            }
        }


    }

}
