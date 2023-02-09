package com.sygic.adas.visiontestapp.ui.vision.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sygic.adas.vision.objects.Sign
import com.sygic.adas.visiontestapp.R
import com.sygic.adas.visiontestapp.core.getDrawableId

private val GROUP_COLORS = arrayOf(Color.GREEN, Color.CYAN, Color.YELLOW, Color.LTGRAY, Color.MAGENTA)
private const val MAX_NUMBER_OF_SIGNS = 20

class DetectedSignsAdapter : RecyclerView.Adapter<DetectedSignsAdapter.ViewHolder>() {

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgSign)
        val text: TextView = view.findViewById(R.id.tvConfidence)
    }

    private val items: MutableList<Sign> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_sign, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sign = items[position]

        holder.text.text = "[${sign.confidence.toInt()}] ${sign.signConfidence.toInt()}%"
        if(sign.group != 0) {
            val colorIndex = sign.group % GROUP_COLORS.size
            holder.text.background = ColorDrawable(GROUP_COLORS[colorIndex])
        }

        holder.img.setImageResource(sign.getDrawableId())
    }

    fun addSign(sign: Sign) {
        items.add(0, sign)
        notifyItemInserted(0)

        while(items.size > MAX_NUMBER_OF_SIGNS) {
            val index = items.size -1
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}
