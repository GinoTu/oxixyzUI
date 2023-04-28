package com.dope.ooxixyz.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dope.ooxixyz.databinding.ItemMemberRequestListBinding
import com.dope.ooxixyz.userInfoResponse.MembersRequest

class MemberRequestListAdapter: RecyclerView.Adapter<MemberRequestListAdapter.MemberRequestListViewHolder>(){
    private var memberRequestList: List<MembersRequest> = ArrayList()

    // Item點擊。
    lateinit var onItemClickCallback: ((Int, MembersRequest) -> Unit)

    // 設定資料進來。
    fun setterData(memberRequestList: List<MembersRequest>) {
        this.memberRequestList = memberRequestList
        notifyItemChanged(0, memberRequestList.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberRequestListViewHolder {
        return MemberRequestListViewHolder(ItemMemberRequestListBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int = memberRequestList.size

    override fun onBindViewHolder(holder: MemberRequestListViewHolder, position: Int) {
        val item = memberRequestList[position]
        holder.binding.apply {
            tvName.text = item.userName

            root.setOnClickListener{ onItemClickCallback.invoke(position, item) }
        }
    }

    inner class MemberRequestListViewHolder(val binding: ItemMemberRequestListBinding) : RecyclerView.ViewHolder(binding.root)
}