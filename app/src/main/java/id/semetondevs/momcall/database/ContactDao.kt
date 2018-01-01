package id.semetondevs.momcall.database

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import android.arch.persistence.room.Query

@Dao interface ContactDao {

    @Query("SELECT * FROM contact")
    fun getAllContact(): List<Contact>

    @Insert(onConflict = REPLACE)
    fun saveContact(contact: Contact)

}