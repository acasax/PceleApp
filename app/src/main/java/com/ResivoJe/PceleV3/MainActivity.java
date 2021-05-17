package com.ResivoJe.PceleV3;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        if (savedInstanceState == null)
            getSupportFragmentManager().beginTransaction().add(R.id.fragment, new DevicesFragment(), "devices").commit();
        else
            onBackStackChanged();
        toolbar.setTitleTextColor(Color.BLACK);
    }

    @Override
    public void onBackStackChanged() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount()>0);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // refresh your views here
        TextView tv = findViewById(R.id.devices);
        tv.setText(getResources().getText(R.string.devices));
        setTitle(getResources().getText(R.string.app_name));
        Button button = findViewById(R.id.chooseAll_btn2);
        button.setText(getResources().getText(R.string.izaberi_sve));
        super.onConfigurationChanged(newConfig);
    }
}
