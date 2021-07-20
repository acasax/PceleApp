package com.ResivoJe.PceleV3.database;


import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity(tableName = "parameters")
public class Parameters {

    @PrimaryKey(autoGenerate = true)
    private long ID;

    @ColumnInfo(name = "time")
    private int time;

    @ColumnInfo (name = "frequency")
    private int frequency;

    @ColumnInfo (name = "impuls")
    private int impuls;

    @ColumnInfo (name = "pause")
    private int pause;

    @ColumnInfo (name = "voltage")
    private int voltage;

    public Parameters(int time, int frequency, int impuls, int pause, int voltage) {
        this.time = time;
        this.frequency = frequency;
        this.impuls = impuls;
        this.pause = pause;
        this.voltage = voltage;
    }

    public long getID() {
        return ID;
    }

    public void setID(long ID) {
        this.ID = ID;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public int getImpuls() {
        return impuls;
    }

    public void setImpuls(int impuls) {
        this.impuls = impuls;
    }

    public int getPause() {
        return pause;
    }

    public void setPause(int pause) {
        this.pause = pause;
    }

    public int getVoltage() {
        return voltage;
    }

    public void setVoltage(int voltage) {
        this.voltage = voltage;
    }
}
