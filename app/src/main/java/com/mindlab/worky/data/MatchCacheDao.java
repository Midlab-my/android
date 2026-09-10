package com.mindlab.worky.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MatchCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(MatchCacheEntity entity);

    @Query("SELECT * FROM match_cache WHERE queryKey = :queryKey LIMIT 1")
    MatchCacheEntity findByQuery(String queryKey);

    @Query("SELECT * FROM match_cache ORDER BY updatedAt DESC LIMIT 20")
    List<MatchCacheEntity> listRecent();
}
