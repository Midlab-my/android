package com.mindlab.worky.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CareerCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(CareerCacheEntity entity);

    @Query("SELECT * FROM career_cache WHERE cargoKey = :cargoKey LIMIT 1")
    CareerCacheEntity findByCargo(String cargoKey);

    @Query("SELECT * FROM career_cache ORDER BY updatedAt DESC LIMIT 20")
    List<CareerCacheEntity> listRecent();

    @Query("DELETE FROM career_cache")
    void clearAll();
}
