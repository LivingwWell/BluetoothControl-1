package com.example.bluetoothcontrol;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

public class recyclerviewAdapter extends BaseQuickAdapter<HomeItem, BaseViewHolder> {

    /**
     *
     * @param layoutResId  这个填你Item界面的Id
     * @param data    这个代表填充到Item里的数据,类型为List<实体类>
     */
    public recyclerviewAdapter(int layoutResId, @Nullable List<HomeItem> data) {
        super(R.layout.item_recyclerview, data);
    }


    @Override
    protected void convert(@NonNull BaseViewHolder helper, HomeItem item) {
        //将Item里面的控件和数据绑定
        helper.setText(R.id.txt, item.getName());
    }
}