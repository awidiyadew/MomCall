package id.semetondevs.momcall.database

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "contact")
data class Contact(
        @ColumnInfo(name = "id") @PrimaryKey(autoGenerate = true) val id: Long?,
        @ColumnInfo(name = "voice_call_Id") var voiceCallId: Long?,
        @ColumnInfo(name = "video_call_id") var videoCallId: Long?,
        @ColumnInfo(name = "name") var name: String,
        @ColumnInfo(name = "number") var number: String,
        @ColumnInfo(name = "photo_path") var photo: String?,
        val photoUrl: String?) {

    @Ignore constructor() : this(0, 0, 0, "", "", "", "")

}