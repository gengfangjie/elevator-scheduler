package com.fjgeng.elevatorscheduler;

/**
 * Created by gengfangjie on 2019/10/27.
 * 同一层的电梯入口共享按钮开关逻辑
 */
public class EntranceButton {
    private Boolean upButton;
    private Boolean downButton;

    public EntranceButton(Boolean upButton, Boolean downButton) {
        this.upButton = upButton;
        this.downButton = downButton;
    }

    public Boolean getUpButton() {
        return upButton;
    }

    public void setUpButton(Boolean upButton) {
        this.upButton = upButton;
    }

    public Boolean getDownButton() {
        return downButton;
    }

    public void setDownButton(Boolean downButton) {
        this.downButton = downButton;
    }
}
