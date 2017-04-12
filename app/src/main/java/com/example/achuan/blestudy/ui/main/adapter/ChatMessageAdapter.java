package com.example.achuan.blestudy.ui.main.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.achuan.blestudy.R;
import com.example.achuan.blestudy.mode.bean.ChatMsgFmt;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by achuan on 16-10-5.
 * 功能：RY列表适配器类(这是个模板,大家可以在此基础上进行扩展)
 * MyBean是一个(模板)数据模型类,路径为：model/bean/
 * 注意：该文件具体使用时需移动到对应ui/modulexxx/adapter文件目录下,方便管理
 */
public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.ViewHolder> {



    private LayoutInflater mInflater;//创建布局装载对象来获取相关控件（类似于findViewById()）
    private Context mContext;//显示框面
    private List<ChatMsgFmt> mChatMsgFmtList;


    //定义两个接口引用变量
    private OnClickListener mOnClickListener;
    private OnLongClickListener mOnLongClickListener;


    //define interface
    public interface OnClickListener {
        void onClick(View view, int postion);
    }

    public interface OnLongClickListener {
        void onLongClick(View view, int postion);
    }

    //定义 set方法
    public void setOnClickListener(OnClickListener onClickListener) {
        mOnClickListener = onClickListener;
    }

    public void setOnLongClickListener(OnLongClickListener onLongClickListener) {
        mOnLongClickListener = onLongClickListener;
    }

    /*构造方法*/
    public ChatMessageAdapter(Context mContext, List<ChatMsgFmt> mChatMsgFmtList) {
        this.mContext = mContext;
        this.mChatMsgFmtList = mChatMsgFmtList;
        //通过获取context来初始化mInflater对象
        mInflater = LayoutInflater.from(mContext);
    }

    //适配器中数据集中的个数
    public int getItemCount() {
        return mChatMsgFmtList.size();
    }


    //设置item的type序号
    @Override
    public int getItemViewType(int position) {
        ChatMsgFmt msgFmt = mChatMsgFmtList.get(position);
        return msgFmt.getFrom() == ChatMsgFmt.MESSAGE_FROM.OTHERS ? 0 : 1;
    }

    /****
     * item第一次显示时,才创建其对应的viewholder进行控件存储,之后直接使用即可
     ****/
    //先创建ViewHolder
    public ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        //根据类别加载不同的消息布局
        View view;
        if (type == 0) {//别人发的
            view = mInflater.inflate(R.layout.msg_from_fmt, parent, false);
        } else {//发给别人的
            view = mInflater.inflate(R.layout.msg_to_fmt, parent, false);
        }
        ViewHolder viewHolder = new ViewHolder(view);//创建一个item的viewHoler实例
        return viewHolder;
    }

    /****
     * 当前界面中出现了item的显示更新时执行该方法（即有item加入或者移除界面）
     * <p/>
     * 该方法的执行顺序　　早于　　onScrolled（）方法
     ****/
    //绑定ViewHolder
    public void onBindViewHolder(final ViewHolder holder, final int postion) {
        //再通过viewHolder中缓冲的控件添加相关数据


        ChatMsgFmt msgFmt = mChatMsgFmtList.get(postion);
        //
        holder.mTvMsgName.setText(msgFmt.getName());
        holder.mTvMsgContent.setText(msgFmt.getMsg());


        /***为item设置点击监听事件***/
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mOnClickListener != null) {
                    //设置回调监听
                    mOnClickListener.onClick(view, postion);
                }
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mOnLongClickListener != null) {
                    //设置长时间点击回调监听
                    mOnLongClickListener.onLongClick(v, postion);
                }
                return false;//设置成false,这样就不会触发单击的监听事件
            }
        });
    }


    /*创建自定义的ViewHolder类*/
    public static class ViewHolder extends RecyclerView.ViewHolder {
        //使用butterknife来进行item中的控件加载,此处需要自己添加
        @BindView(R.id.tv_msg_name)
        TextView mTvMsgName;
        @BindView(R.id.tv_msg_content)
        TextView mTvMsgContent;
        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
