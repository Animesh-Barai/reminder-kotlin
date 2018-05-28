package com.elementary.tasks.core.data.dao;

import com.elementary.tasks.core.data.models.Group;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import static androidx.room.OnConflictStrategy.REPLACE;

/**
 * Copyright 2018 Nazar Suhovich
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@Dao
public interface GroupDao {

    @Query("SELECT * FROM `Group`")
    LiveData<List<Group>> loadAll();

    @Query("SELECT * FROM `Group` LIMIT 1")
    LiveData<Group> loadDefault();

    @Nullable
    @Query("SELECT * FROM `Group` LIMIT 1")
    Group getDefault();

    @Query("SELECT * FROM `Group`")
    List<Group> getAll();

    @Insert(onConflict = REPLACE)
    void insert(Group group);

    @Insert(onConflict = REPLACE)
    void insertAll(Group... groups);

    @Delete
    void delete(Group group);

    @Query("SELECT * FROM `Group` WHERE uuId=:id")
    LiveData<Group> loadById(String id);
}
