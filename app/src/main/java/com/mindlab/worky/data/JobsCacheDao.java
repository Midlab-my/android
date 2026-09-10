package com.mindlab.worky.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface JobsCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(JobsCacheEntity entity);

    @Query("SELECT * FROM jobs_cache WHERE queryKey = :queryKey LIMIT 1")
    JobsCacheEntity findByQuery(String queryKey);

    @Query("SELECT * FROM jobs_cache ORDER BY updatedAt DESC LIMIT 20")
    List<JobsCacheEntity> listRecent();
}
