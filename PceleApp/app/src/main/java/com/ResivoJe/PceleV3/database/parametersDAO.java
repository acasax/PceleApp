package com.ResivoJe.PceleV3.database;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import com.ResivoJe.PceleV3.database.Parameters;

@Dao
public abstract class parametersDAO {

    @Insert
    public abstract long insert(Parameters parameters);

    @Query("Select * from parameters")
    public abstract LiveData<List<Parameters>> getAll();

    @Query("Update parameters set time = :time, frequency = :frequency, impuls = :impuls, pause = :pause, " +
            "voltage = :voltage where parameters.ID = :ID")
    public abstract void updateState(long ID, int time, int frequency, int impuls, int pause, int voltage);
}
