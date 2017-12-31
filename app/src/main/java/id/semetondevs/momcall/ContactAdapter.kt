package id.semetondevs.momcall

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide


class ContactAdapter(context: Context) : ArrayAdapter<Contact>(context, 0) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val holder: ContactVH?
        var view = convertView

        if (view == null) {
            view = LayoutInflater.from(context)
                    .inflate(R.layout.item_contact, parent, false)
            holder = ContactVH(view!!)
            view.tag = holder
        } else {
            holder = view.tag as ContactVH
        }

        holder.bind(getItem(position))
        return view
    }

    internal class ContactVH(view: View) {
        private val tvContactName: TextView = view.findViewById<TextView>(R.id.tv_contact_name) as TextView
        private val tvContactNum: TextView = view.findViewById<TextView>(R.id.tv_contact_number) as TextView
        private val ivContactPhoto: ImageView = view.findViewById<ImageView>(R.id.iv_contact_photo) as ImageView

        fun bind(contact: Contact) {
            tvContactName.text = contact.name
            tvContactNum.text = contact.number

            if (contact.photoUrl != null) {
                Glide.with(ivContactPhoto.context)
                        .load(contact.photoUrl)
                        .into(ivContactPhoto)
            }
        }
    }

}