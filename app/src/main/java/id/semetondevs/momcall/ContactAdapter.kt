package id.semetondevs.momcall

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView


class ContactAdapter(context: Context) : ArrayAdapter<Contact>(context, 0) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val holder: ContactVH?
        var view = convertView

        if (view == null) {
            view = LayoutInflater.from(context)
                    .inflate(R.layout.item_tinder_contact, parent, false)
            holder = ContactVH(view!!)
            view.tag = holder
        } else {
            holder = view.tag as ContactVH
        }

        val contact: Contact = getItem(position)
        holder.tvContactName.text = contact.name
        holder.tvContactNum.text = contact.number
        return view
    }

    internal class ContactVH(view: View) {
        val tvContactName: TextView = view.findViewById<TextView>(R.id.tv_contact_name) as TextView
        val tvContactNum: TextView = view.findViewById<TextView>(R.id.tv_contact_number) as TextView
    }

}