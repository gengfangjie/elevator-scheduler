package com.fjgeng.elevatorscheduler.listener;

import com.fjgeng.elevatorscheduler.enums.Direction;

/**
 * Created by gengfangjie on 2019/10/25.
 * 电梯入口按钮监听接口
 */
public interface EntranceButtonListener {
    void entranceButtonHit(int floor, Direction direction);
}
