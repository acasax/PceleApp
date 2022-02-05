package com.ResivoJe.PceleV3.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.Date;
import java.util.concurrent.Executors;

@Database(version = 2,
        entities = {
                Parameters.class
        },
        exportSchema = false
)
public abstract class MyRoomDatabase extends RoomDatabase {

    private static MyRoomDatabase singletonInstance;

    public abstract parametersDAO parametersDAO();

    public static MyRoomDatabase getDatabase(final Context context) {
        if (singletonInstance == null) {
            synchronized (MyRoomDatabase.class) {
                if (singletonInstance == null) {
                    singletonInstance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            MyRoomDatabase.class,
                            "myDatabase")
                            .fallbackToDestructiveMigration()
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    Executors.newSingleThreadScheduledExecutor().execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            getDatabase(context).parametersDAO().insert(new Parameters(1, 100, 1, 1, 10));

                                            getDatabase(context).parametersDAO().insert(new Parameters(2, 200, 2, 1, 10));
                                            getDatabase(context).parametersDAO().insert(new Parameters(3, 300, 3, 2, 20));
                                            getDatabase(context).parametersDAO().insert(new Parameters(4, 400, 4, 3, 30));
                                        }
                                    });
                                }
                            })
                            .build();
                }
            }
        }
        return singletonInstance;
    }
}