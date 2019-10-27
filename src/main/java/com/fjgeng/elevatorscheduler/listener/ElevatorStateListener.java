package com.fjgeng.elevatorscheduler.listener;

import com.fjgeng.elevatorscheduler.Elevator;

/**
 * Created by gengfangjie on 2019/10/25.
 * 电梯厢状态监听接口
 */
public interface ElevatorStateListener {
    void stateChanged(Elevator elevator);
}
