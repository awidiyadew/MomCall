package id.semetondevs.momcall.database

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "contact")
data class Contact(
        @ColumnInfo(name = "id") @PrimaryKey(autoGenerate = true) val id: Long,
        @ColumnInfo(name = "voice_call_Id") val voiceCallId: Long,
        @ColumnInfo(name = "video_call_id") val videoCallId: Long,
        @ColumnInfo(name = "name") val name: String,
        @ColumnInfo(name = "number") val number: String,
        @ColumnInfo(name = "photo_path") val photo: String?,
        val photoUrl: String?)