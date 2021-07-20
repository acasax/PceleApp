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

import com.ResivoJe.PceleV3.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(this.getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        if (savedInstanceState == null)
            getSupportFragmentManager().beginTransaction().add(R.id.fragment, new DevicesFragment(), "devices").commit();
        else
            onBackStackChanged();
        binding.toolbar.setTitleTextColor(Color.BLACK);
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
