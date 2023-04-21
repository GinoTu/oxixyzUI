package com.dope.ooxixyz.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dope.ooxixyz.databinding.ItemMemberListBinding
import com.dope.ooxixyz.userInfoResponse.Members

class MemberListAdapter : RecyclerView.Adapter<MemberListAdapter.MemberListViewHolder>() {
    private var memberList: List<Members> = ArrayList()

    // Item點擊
    lateinit var onItemClickCallback: ((Int, Members) -> Unit)

    // 設定資料進來。
    fun setterData(memberList: List<Members>) {
        this.memberList = memberList
        notifyItemChanged(0, memberList.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberListViewHolder {
        return MemberListViewHolder(ItemMemberListBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int = memberList.size

    override fun onBindViewHolder(holder: MemberListViewHolder, position: Int) {
        val item = memberList[position]
        //Log.e("memberList", item.userName)
        holder.binding.apply {
            tvName.text = item.userName

            root.setOnClickListener{ onItemClickCallback.invoke(position, item) }
        }
    }

    inner class MemberListViewHolder(val binding: ItemMemberListBinding) : RecyclerView.ViewHolder(binding.root)
}