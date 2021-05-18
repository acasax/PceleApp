package com.ResivoJe.PceleV3;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ResivoJe.PceleV3.database.MyRoomDatabase;
import com.ResivoJe.PceleV3.database.Parameters;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.*;

public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener {
    private static final String TAG = "MainActivity";

    private enum Connected {False, Pending, True}

    private String deviceAddress;
    private String[] devicesAddresses;
    private String newline = "\r\n";
    private boolean sendAllBoolean = false;
    Button sendBtn;
    Button sendBtnAll;
    Button stopBtn;
    Button getBtn;
    Button startBtBtn;
    Object syncObject = new Object();
    ArrayList<BluetoothDevice> failedDeviceNames = new ArrayList<>();
    ArrayList<BluetoothDevice> successDeviceNames = new ArrayList<>();
    private boolean isGet = false;
    private int chosenState = 0;

    private String deviceToConnect;
    private String messageToSend;
    private boolean sendAllStarted = false;
    private int counter = 0;
    private TextView receiveText;

    private SerialSocket socket;
    private SerialService service;
    private boolean initialStart = true;
    private Connected connected = Connected.False;
    private String answerText = "";
    //Data for send
    String start = "S";
    String stop = "X";
    String get = "G";
    String begin = "<";
    String end = ">";

    ArrayList<Parameters> parameters = new ArrayList<>();


    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceAddress = getArguments().getString("device");
        devicesAddresses = getArguments().getStringArray("devices");
        sendAllBoolean = getArguments().getBoolean("sendAll");
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
        if (service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if (service != null && !getActivity().isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation")
    // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try {
            getActivity().unbindService(this);
        } catch (Exception ignored) {
        }
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (initialStart && service != null) {
            initialStart = false;
            if (!sendAllBoolean)
                getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        if (initialStart && isResumed()) {
            initialStart = false;
            if (!sendAllBoolean)
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

    public boolean checkEvent (MotionEvent event){
        if (event.getAction() == MotionEvent.ACTION_DOWN){
            counter = 0;
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            counter++;
            if (counter > 8 && counter % 2 == 1) {
                return true;
            }
        }
        return false;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());


        sendBtn = view.findViewById(R.id.send_btn);
        sendBtnAll = view.findViewById(R.id.sendAll_btn);
        stopBtn = view.findViewById(R.id.stop_btn);
        getBtn = view.findViewById(R.id.get_btn);
        View plusTimeBtn = view.findViewById(R.id.plusTimeBtn);
        View minusTimeBtn = view.findViewById(R.id.minusTimeBtn);
        View plusFrekBtn = view.findViewById(R.id.plusFrekBtn);
        View minusFrekBtn = view.findViewById(R.id.minusFrekBtn);
        View plusImpulsBtn = view.findViewById(R.id.plusImpulsBtn);
        View minusImpulsBtn = view.findViewById(R.id.minusImpulsBtn);
        View plusPauseBtn = view.findViewById(R.id.plusPauseBtn);
        View minusPauseBtn = view.findViewById(R.id.minusPauseBtn);
        View plusVoltageBtn = view.findViewById(R.id.plusVoltageBtn);
        View minusVoltageBtn = view.findViewById(R.id.minusVoltageBtn);
        TextView time = view.findViewById(R.id.timeTxt);
        TextView frek = view.findViewById(R.id.frekTxt);
        TextView impuls = view.findViewById(R.id.impulsTxt);
        TextView pause = view.findViewById(R.id.pauseTxt);
        TextView voltage = view.findViewById(R.id.voltageTxt);

        View saveParameters = view.findViewById(R.id.save_parameters);
        saveParameters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder1 = new AlertDialog.Builder(getContext());

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(TerminalFragment.this.getContext(),
                        android.R.layout.select_dialog_multichoice);
                adapter.add((String) getResources().getText(R.string.stanje_1));
                adapter.add((String) getResources().getText(R.string.stanje_1));
                adapter.add((String) getResources().getText(R.string.stanje_1));

                builder1.setTitle(getResources().getText(R.string.saveState));
                builder1.setSingleChoiceItems(adapter, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        chosenState = which;
                    }
                });

                builder1.setPositiveButton(
                        getResources().getText(R.string.save),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                saveState();
                                dialog.cancel();
                            }
                        });
                builder1.setNegativeButton(
                        getResources().getText(R.string.Odustani),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                AlertDialog alert11 = builder1.create();
                alert11.show();
            }
        });




        //plusTimeBtn.setOnClickListener(v -> Steps(time, "+", 1, 1, 255));
        //minusTimeBtn.setOnClickListener(v -> Steps(time, "-", 1, 1, 255));

        //plusFrekBtn.setOnClickListener(v -> Steps(frek, "+", 10, 100, 1200));
        //minusFrekBtn.setOnClickListener(v -> Steps(frek, "-", 10, 100, 1200));

        //plusImpulsBtn.setOnClickListener(v -> Steps(impuls, "+", 1, 1, 25));
        //minusImpulsBtn.setOnClickListener(v -> Steps(impuls, "-", 1, 1, 25));

        //plusPauseBtn.setOnClickListener(v -> Steps(pause, "+", 1, 0, 10));
        //minusPauseBtn.setOnClickListener(v -> Steps(pause, "-", 1, 0, 10));

        //plusVoltageBtn.setOnClickListener(v -> Steps(voltage, "+", 1, 10, 35));
        //minusVoltageBtn.setOnClickListener(v -> Steps(voltage, "-", 1, 10, 35));


        //Voltage
        plusVoltageBtn.setOnTouchListener((v, event) -> {
            if (checkEvent(event)){
                Steps(voltage, "+", 1, 10, 35);
                return true;
            }
            return false;
        });

        minusVoltageBtn.setOnTouchListener((v, event) -> {
            if (checkEvent(event)){
                Steps(voltage, "-", 1, 10, 35);
                return true;
            }
            return false;
        });


        //Pauza
        plusPauseBtn.setOnTouchListener((v, event) -> {
            if (checkEvent(event)){
                Steps(pause, "+", 1, 1, 10);
                return true;
            }
            return false;
        });

        minusPauseBtn.setOnTouchListener((v, event) -> {
            if (checkEvent(event)){
                Steps(pause, "-", 1, 1, 10);
                return true;
            }
            return false;
        });

        //Impuls
        plusImpulsBtn.setOnTouchListener((v, event) -> {
            if (checkEvent(event)){
                Steps(impuls, "+", 1, 1, 10);
                return true;
            }
            return false;
        });

        minusImpulsBtn.setOnTouchListener((v, event) -> {
            if (checkEvent(event)){
                Steps(impuls, "-", 1, 1, 10);
                return true;
            }
            return false;
        });

        //Frekvencija
        plusFrekBtn.setOnTouchListener((v, event) -> {
            if (checkEvent(event)){
                Steps(frek, "+", 10, 100, 1200);
                return true;
            }
            return false;
        });

        minusFrekBtn.setOnTouchListener((v, event) -> {
            if (checkEvent(event)){
                Steps(frek, "-", 10, 100, 1200);
                return true;
            }
            return false;
        });

        //Vreme
        plusTimeBtn.setOnTouchListener((v, event) -> {
            if (checkEvent(event)){
                Steps(time, "+", 1, 1, 240);
                return true;
            }
            return false;
        });

        minusTimeBtn.setOnTouchListener((v, event) -> {
            if (checkEvent(event)){
                Steps(time, "-", 1, 1, 240);
                return true;
            }
            return false;
        });

        if (sendAllBoolean) {
            stopBtn.setOnClickListener(v -> sendAll(begin + stop + end, true, false));
            getBtn.setOnClickListener(v -> sendAll(begin + get + end, true, true));
        }
        else {
            stopBtn.setOnClickListener(v -> send(begin + stop + end));
            getBtn.setOnClickListener(v -> send(begin + get + end));
        }
        sendBtn.setOnClickListener(v -> send(begin +
                start +  ";" +
                "T" + time.getText() + ";" +
                "I" + impuls.getText() + ";" +
                "P" + pause.getText() + ";" +
                "F" + frek.getText() + ";" +
                "V" + (Integer.parseInt(voltage.getText().toString()) * 7 + (Integer.parseInt(voltage.getText().toString()) / 10)) +
                end));

        sendBtnAll.setOnClickListener(v -> sendAll(begin +
                start +  ";" +
                "T" + time.getText() + ";" +
                "I" + impuls.getText() + ";" +
                "P" + pause.getText() + ";" +
                "F" + frek.getText() + ";" +
                "V" + (Integer.parseInt(voltage.getText().toString()) * 7 + (Integer.parseInt(voltage.getText().toString()) / 10)) +
                end, true, false));

        if (sendAllBoolean) {
            sendBtn.setBackgroundColor(Color.RED);
            sendBtn.setEnabled(false);
        } else {
            sendBtnAll.setBackgroundColor(Color.RED);
            sendBtnAll.setEnabled(false);
        }

        // promeniti brojeveeeee
        //
        MyRoomDatabase myRoomDatabase = MyRoomDatabase.getDatabase(getContext());
        final LiveData<List<Parameters>> listLiveData = myRoomDatabase.parametersDAO().getAll();
        listLiveData.observe(TerminalFragment.this, new Observer<List<Parameters>>(){
            @Override
            public void onChanged(List<Parameters> parameters) {
                TerminalFragment.this.parameters.clear();
                for (Parameters p : parameters) {
                    TerminalFragment.this.parameters.add(p);
                }
            }
        });
        return view;
    }


    private void saveState() {
        chosenState += 2;
        TextView time = getActivity().findViewById(R.id.timeTxt);
        TextView frek = getActivity().findViewById(R.id.frekTxt);
        TextView impuls = getActivity().findViewById(R.id.impulsTxt);
        TextView pause = getActivity().findViewById(R.id.pauseTxt);
        TextView voltage = getActivity().findViewById(R.id.voltageTxt);
        int t = Integer.valueOf(time.getText().toString());
        int f = Integer.valueOf(frek.getText().toString());
        int i = Integer.valueOf(impuls.getText().toString());
        int p = Integer.valueOf(pause.getText().toString());
        int v = Integer.valueOf(voltage.getText().toString());
        new Thread(new Runnable() {
            @Override
            public void run() {
                MyRoomDatabase.getDatabase(getContext()).parametersDAO().updateState(
                        chosenState,t,f,i,p,v);
            }
        }).start();
    }

    void setNewParameters(int i){
        TextView time = getActivity().findViewById(R.id.timeTxt);
        TextView frek = getActivity().findViewById(R.id.frekTxt);
        TextView impuls = getActivity().findViewById(R.id.impulsTxt);
        TextView pause = getActivity().findViewById(R.id.pauseTxt);
        TextView voltage = getActivity().findViewById(R.id.voltageTxt);

        time.setText(String.valueOf(parameters.get(i).getTime()));
        frek.setText(String.valueOf(parameters.get(i).getFrequency()));
        impuls.setText(String.valueOf(parameters.get(i).getImpuls()));
        pause.setText(String.valueOf(parameters.get(i).getPause()));
        voltage.setText(String.valueOf(parameters.get(i).getVoltage()));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.default_state) {
            setNewParameters(0);
            return true;
        }

        if (id == R.id.state_1) {
            setNewParameters(1);
            return true;
        }

        if (id == R.id.state_2) {
            setNewParameters(2);
            return true;
        }

        if (id == R.id.state_3) {
            setNewParameters(3);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*
     * Serial + UI
     */
    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            String deviceName = device.getName() != null ? device.getName() : device.getAddress();
            status((String) getResources().getText(R.string.Povezivanj));
            connected = Connected.Pending;
            socket = new SerialSocket();
            service.connect(this, getResources().getText(R.string.Povezano) + deviceName);
            socket.connect(getContext(), service, device);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    /*
     * Serial + UI
     */
    private void connectAndSend(BluetoothDevice device) {
        try {
            String deviceName = device.getName() != null ? device.getName() : device.getAddress();
            status((String) getResources().getText(R.string.Povezivanj));
            connected = Connected.Pending;
            socket = new SerialSocket();
            service.connect(this, getResources().getText(R.string.Povezano) + deviceName);
            socket.connect(getContext(), service, device);

            synchronized(syncObject) {
                try {
                    // Calling wait() will block this thread until another thread
                    // calls notify() on the object.
                    syncObject.wait();
                } catch (InterruptedException e) {
                    // Happens if someone interrupts your thread.
                }
            }

            if (connected == Connected.True) {
                send(messageToSend);
                successDeviceNames.add(device);
                sleep(2000);

                disconnect();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), getResources().getText(R.string.poslata) + device.getName(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            else {
                failedDeviceNames.add(device);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), getResources().getText(R.string.poslataN) + device.getName(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        if (service != null)
            service.disconnect();
        if (socket != null)
            socket.disconnect();
        socket = null;
    }

    private void send(String str) {
        if (connected != Connected.True) {
            Toast.makeText(getActivity(), getResources().getText(R.string.NijePovezano), Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    receiveText.setText(spn);
                }
            });

            answerText = "";
            byte[] data = (str + newline).getBytes();
            socket.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }


    public void sendAll(String str, boolean all, boolean isGet) {

        this.isGet = isGet;
        ArrayList<BluetoothDevice> failedDevicesCopy = (ArrayList<BluetoothDevice>) failedDeviceNames.clone();
        failedDeviceNames.clear();

        ArrayList<BluetoothDevice> successDevicesCopy = (ArrayList<BluetoothDevice>) successDeviceNames.clone();
        successDeviceNames.clear();

        messageToSend = str;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (all) {
                    for (int i = 0; i < devicesAddresses.length; i++) {
                        deviceToConnect = devicesAddresses[i];
                        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceToConnect);
                        connectAndSend(device);
                    }
                }
                else{
                    for (int i = 0 ; i < failedDevicesCopy.size(); i++){
                        connectAndSend(failedDevicesCopy.get(i));
                    }
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder1 = new AlertDialog.Builder(getContext());

                        String positiveMessage = (String) getResources().getText(R.string.PokusajPonovo);
                        String negativeMessage = (String) getResources().getText(R.string.Odustani);

                        String message = "";
                        if(successDeviceNames.size() > 0) {
                            message += "Uređaji, koji su uspešno primili komandu su: \n";
                            for (int i = 0; i < successDeviceNames.size(); i++) {
                                message += successDeviceNames.get(i).getName() + "\n";
                            }
                        }

                        if(failedDeviceNames.size() > 0) {
                            message += "\n";
                            message += "Uređaji, koji nisu uspešno primili komandu su: \n";
                            for (int i = 0; i < failedDeviceNames.size(); i++) {
                                message += failedDeviceNames.get(i).getName() + "\n";
                            }
                        }
                        else {
                            positiveMessage = "U redu";
                        }

                        builder1.setMessage(message);
                        builder1.setCancelable(true);

                        builder1.setPositiveButton(
                                positiveMessage,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        if (failedDeviceNames.size() > 0) {
                                            sendAll(str, false, isGet);
                                        }
                                        dialog.cancel();
                                    }
                                });
                        if (failedDeviceNames.size() > 0) {

                            builder1.setNegativeButton(
                                    negativeMessage,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });
                        }
                        AlertDialog alert11 = builder1.create();
                        alert11.show();
                    }
                });
            }
        }).start();
    }

    private void receive(byte[] data) {
        answerText += new String (data);
        checkAnswer();
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                receiveText.setText(answerText);
            }
        });
    }

    private void checkAnswer() {
        if (answerText.contains(">")){
            answerText = answerText.replace("<","");
            answerText = answerText.replace(">","");
            String[] parts = answerText.split(";");
            // error handling
            if (parts.length < 1) answerText = "Nepoznata greska.";
            parts[0] = parts[0].replace("E","");
            String error = "";
            switch (parts[0]) {
                case "0": error = "Uspesno ste poslali komandu. \n\n"; break;
                case "1": error = "Poruka nije u dobrom formatu. \n"; break;
                case "2": error = "Greska u zadatim parametrima. \n"; break;
                case "3": error = "Komanda se ne moze izvrsiti. \n"; break;
                default : error = "Nepoznata greska.";
            }
            if (parts[0].equals("0")) {
                String deviceState = "";
                parts[1] = parts[1].replace("A", "");
                if (parts[1].equals("0")) {
                    deviceState = "Uredjaj ne radi. \n";
                } else {
                    deviceState = "";
                }
                parts[2] = parts[2].replace("R", "");

                if (!sendAllBoolean || (sendAllBoolean && isGet)) {
                    String timeLeft = "Preostalo " + parts[2] + " min rada uredjaja. \n";
                    String cycle;
                    if (parts.length < 4) {
                        cycle = "";
                    } else {
                        parts[3] = parts[3].replace("C", "");
                        if (parts[3].equals("0")) {
                            cycle = "\n";
                        } else {
                            cycle = "Uredjaj je izvrsio komandu do kraja.\n";
                            deviceState = "";
                            timeLeft = "";
                        }
                    }
                    answerText = error + deviceState + timeLeft + cycle;
                }
                else {
                    answerText = error + deviceState;
                }
            }
            else {
                answerText = error;
            }
        }
    }

    private void status(String str) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
                spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                receiveText.setText(spn);
            }
        });
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("Povezano");
        connected = Connected.True;
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized(syncObject) {
                    syncObject.notify();
                }
            }
        }).start();

    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("Greška: " + e.getMessage());
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized(syncObject) {
                    syncObject.notify();
                }
            }
        }).start();
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

    public void Steps(TextView a, String s, int Step, int minValue, int maxValue) {
        String currentValue = (String) a.getText();
        int value = Integer.parseInt(currentValue);
        if (s == "+") {
            if (value < maxValue) {
                value = value + Step;
                currentValue = String.valueOf(value);
                a.setText(currentValue);
            }
        } else if (s == "-") {
            if (value > minValue) {
                value = value - Step;
                currentValue = String.valueOf(value);
                a.setText(currentValue);
            }
        }
    }

}
