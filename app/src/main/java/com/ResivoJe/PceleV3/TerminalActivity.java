package com.ResivoJe.PceleV3;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ResivoJe.PceleV3.database.MyRoomDatabase;
import com.ResivoJe.PceleV3.database.Parameters;
import com.ResivoJe.PceleV3.databinding.ActivityTerminalBinding;

import java.util.ArrayList;

public class TerminalActivity extends AppCompatActivity {

    public final static String DEVICES_EXTRA = "devices";
    public final static String DEVICES_NAMES_EXTRA = "deviceNames";
    public final static String SEND_ALL_EXTRA = "sendAll";

    private ActivityTerminalBinding binding;
    private Handler handler;

    private int counter = 0;
    private int chosenState = 0;
    private Parameters deviceParams;
    private ProgressDialog progressDialog;

    private String[] devices;
    private String[] devicesNames;
    private boolean isSendAll = false;
    private boolean isGet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        binding = ActivityTerminalBinding.inflate(this.getLayoutInflater());
        setContentView(binding.getRoot());
        super.onCreate(savedInstanceState);
        handler = new Handler();

        Intent intent = getIntent();
        if(intent != null) {
            try {
                devices = intent.getStringArrayExtra(TerminalActivity.DEVICES_EXTRA);
                devicesNames = intent.getStringArrayExtra(TerminalActivity.DEVICES_NAMES_EXTRA);
                isSendAll = intent.getBooleanExtra(TerminalActivity.SEND_ALL_EXTRA, false);
            } catch (Exception e) {
                //
            }
        }

        deviceParams = new Parameters(1, 100, 1, 1, 10);

        //Voltage
        binding.plusVoltageBtn.setOnTouchListener((v, event) -> {
            return handleEvent(event, value -> deviceParams.setVoltage(value), deviceParams.getVoltage(), 1, 10, 35);
        });

        binding.minusVoltageBtn.setOnTouchListener((v, event) -> {
            return handleEvent(event, value -> deviceParams.setVoltage(value), deviceParams.getVoltage(), -1, 10, 35);
        });

        //Pauza
        binding.plusPauseBtn.setOnTouchListener((v, event) -> {
            return handleEvent(event, value -> deviceParams.setPause(value), deviceParams.getPause(), 1, 0, 10);
        });

        binding.minusPauseBtn.setOnTouchListener((v, event) -> {
            return handleEvent(event, value -> deviceParams.setPause(value), deviceParams.getPause(), -1, 0, 10);
        });

        //Impuls
        binding.plusImpulsBtn.setOnTouchListener((v, event) -> {
            return handleEvent(event, value -> deviceParams.setImpuls(value), deviceParams.getImpuls(), 1, 1, 10);
        });

        binding.minusImpulsBtn.setOnTouchListener((v, event) -> {
            return handleEvent(event, value -> deviceParams.setImpuls(value), deviceParams.getImpuls(), -1, 1, 10);
        });

        //Frekvencija
        binding.plusFrekBtn.setOnTouchListener((v, event) -> {
            return handleEvent(event, value -> deviceParams.setFrequency(value), deviceParams.getFrequency(), 1, 100, 1200);
        });

        binding.minusFrekBtn.setOnTouchListener((v, event) -> {
            return handleEvent(event, value -> deviceParams.setFrequency(value), deviceParams.getFrequency(), -1, 100, 1200);
        });

        //Vreme
        binding.plusTimeBtn.setOnTouchListener((v, event) -> {
            return handleEvent(event, value -> deviceParams.setTime(value), deviceParams.getTime(), 1, 1, 240);
        });

        binding.minusTimeBtn.setOnTouchListener((v, event) -> {
            return handleEvent(event, value -> deviceParams.setTime(value), deviceParams.getTime(), -1, 1, 240);
        });

        binding.saveParameters.setOnClickListener(v -> {
            AlertDialog.Builder builder1 = new AlertDialog.Builder(TerminalActivity.this);

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(TerminalActivity.this,
                    android.R.layout.select_dialog_multichoice);
            adapter.add((String) getResources().getText(R.string.stanje_1));
            adapter.add((String) getResources().getText(R.string.stanje_2));
            adapter.add((String) getResources().getText(R.string.stanje_3));

            builder1.setTitle(getResources().getText(R.string.saveState));
            builder1.setSingleChoiceItems(adapter, 0, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    chosenState = which;
                }
            });

            builder1.setPositiveButton(
                    getResources().getText(R.string.save),
                    (dialog, id) -> {
                        saveState();
                        dialog.cancel();
                    });
            builder1.setNegativeButton(
                    getResources().getText(R.string.Odustani),
                    (dialog, id) -> dialog.cancel());
            AlertDialog alert11 = builder1.create();
            alert11.show();
        });

        binding.sendBtn.setOnClickListener(v -> start(devicesNames[0], devices[0]));
        binding.sendAllBtn.setOnClickListener(v -> startAll());
        binding.stopBtn.setOnClickListener(v -> stopAll());
        binding.getBtn.setOnClickListener(v -> getAll());

        if (isSendAll) {
            binding.sendBtn.setBackgroundColor(Color.RED);
            binding.sendBtn.setEnabled(false);
        } else {
            binding.sendAllBtn.setBackgroundColor(Color.RED);
            binding.sendAllBtn.setEnabled(false);
        }
    }

    interface ParamInterface {
        void setValue(int value);
    }

    private boolean handleEvent(MotionEvent event, ParamInterface paramInterface, int value, int step, int minValue, int maxValue) {
        if (checkEvent(event)) {
            paramInterface.setValue(increment(value, step, minValue, maxValue));
            displayParams();
            return true;
        }
        return false;
    }

    private boolean checkEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
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

    private int increment(int value, int step, int minValue, int maxValue) {
        value += step;
        if (value > maxValue) {
            value = maxValue;
        } else if(value < minValue) {
            value = minValue;
        }
        return value;
    }

    private void displayParams() {
        binding.timeTxt.setText(String.valueOf(deviceParams.getTime()));
        binding.frekTxt.setText(String.valueOf(deviceParams.getFrequency()));
        binding.impulsTxt.setText(String.valueOf(deviceParams.getImpuls()));
        binding.pauseTxt.setText(String.valueOf(deviceParams.getPause()));
        if (deviceParams.getPause() == 0){
            binding.minusImpulsBtn.setEnabled(false);
            binding.plusImpulsBtn.setEnabled(false);
            binding.impulsTitleTxt.setTextColor(Color.RED);
        }
        else {
            binding.impulsTitleTxt.setTextColor(Color.WHITE);
            binding.minusImpulsBtn.setEnabled(true);
            binding.plusImpulsBtn.setEnabled(true);
        }
        binding.voltageTxt.setText(String.valueOf(deviceParams.getVoltage()));
    }

    private void saveState() {
        chosenState += 2;
        new Thread(() -> MyRoomDatabase.getDatabase(TerminalActivity.this).parametersDAO().updateState(
                chosenState, deviceParams.getTime(), deviceParams.getFrequency(), deviceParams.getImpuls(), deviceParams.getPause(), deviceParams.getVoltage())).start();
    }

    private void start(String name, String address) {
        isGet = false;
        showWait();
        new Thread(() -> {
            String command = DeviceController.getParamCommand(deviceParams);
            String response = name + ": \n" + sendCommand(address, command);
            handler.post(() -> showDialog(response));
        }).start();
    }

    private void startAll() {
        String command = DeviceController.getParamCommand(deviceParams);
        isGet = false;
        sendCommandToAll(command);
    }

    private void stopAll() {
        String command = DeviceController.getStopCommand();
        isGet = false;
        sendCommandToAll(command);
    }

    private void getAll() {
        String command = DeviceController.getReadCommand();
        isGet = true;
        sendCommandToAll(command);
    }

    private void sendCommandToAll(String command) {
        showWait();
        new Thread(() -> {
            String response = "";
            for(int i = 0; i < devices.length; i++) {
                response += devicesNames[i] + ": \n" + sendCommand(devices[i], command) + "\n";
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    // Do nothing
                }
                int finalI = i + 1;
                handler.post(() -> setProgressValue(finalI));
            }
            String finalResponse = response;
            handler.post(() -> showDialog(finalResponse));
        }).start();
    }

    private String sendCommand(String address, String command) {
        String response = "Unknown default response";
        String checked = "NA";
        DeviceController deviceController = new DeviceController(TerminalActivity.this, address);
        if(deviceController.connect()) {
            response = deviceController.send(command);
            checked = checkResponse(response);
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                // Do nothing
            }
            deviceController.disconnect();
        } else {
            response = getResources().getString(R.string.NijePovezano);
        }
        return response + "\n" + checked;
    }

    private void displayToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private String checkResponse(String response) {
        if (response.contains(">")) {
            response = response.replace("<", "");
            response = response.replace(">", "");
            String[] parts = response.split(";");
            // error handling
            if (parts.length < 1)
                response = (String) getResources().getText(R.string.NepoznataGreska);
            parts[0] = parts[0].replace("E", "");
            String error = "";
            switch (parts[0]) {
                case "0":
                    error = (String) getResources().getText(R.string.E0);
                    break;
                case "1":
                    error = (String) getResources().getText(R.string.E1);
                    break;
                case "2":
                    error = (String) getResources().getText(R.string.E2);
                    break;
                case "3":
                    error = (String) getResources().getText(R.string.E3);
                    break;
                default:
                    error = (String) getResources().getText(R.string.NepoznataGreska);
            }
            if (parts[0].equals("0")) {
                String deviceState = "";
                parts[1] = parts[1].replace("A", "");
                if (parts[1].equals("0")) {
                    deviceState = (String) getResources().getText(R.string.R0);
                } else {
                    deviceState = "";
                }
                parts[2] = parts[2].replace("R", "");

                if (!isSendAll || (isSendAll && isGet)) {
                    String timeLeft = (String) getResources().getText(R.string.Preostalo) + parts[2] + (String) getResources().getText(R.string.MinRada);
                    String cycle;
                    if (parts.length < 4) {
                        cycle = "";
                    } else {
                        parts[3] = parts[3].replace("C", "");
                        if (parts[3].equals("0")) {
                            cycle = "\n";
                        } else {
                            cycle = (String) getResources().getText(R.string.KomandaDoKraja);
                            deviceState = "";
                            timeLeft = "";
                        }
                    }
                    response = error + deviceState + timeLeft + cycle;
                } else {
                    response = error + deviceState;
                }
            } else {
                response = error;
            }
        }
        return response;
    }

    private void showDialog(String message) {
        hideWait();
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Status");
        alert.setMessage(message);

        alert.setPositiveButton("Ok", (dialog, whichButton) -> {

        });
        alert.show();
    }

    private void showWait() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sending commands");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setProgress(0);
        progressDialog.setMax(devices.length);
        progressDialog.show();
    }

    private void setProgressValue(int value){
        progressDialog.setProgress(value);
    }

    private void hideWait() {
        progressDialog.dismiss();
    }

}
