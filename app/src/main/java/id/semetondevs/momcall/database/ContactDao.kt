package id.semetondevs.momcall.database

import android.arch.persistence.room.*
import android.arch.persistence.room.OnConflictStrategy.REPLACE

@Dao interface ContactDao {

    @Query("SELECT * FROM contact")
    fun getAllContact(): List<Contact>

    @Insert(onConflict = REPLACE)
    fun saveContact(contact: Contact)

    @Delete
    fun deleteContact(contact: Contact)

    @Update(onConflict = REPLACE)
    fun updateContact(contact: Contact)

}