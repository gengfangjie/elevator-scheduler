package com.fjgeng.elevatorscheduler.enums;

/**
 * Created by gengfangjie on 2019/10/25.
 */
public enum ElevatorMark {
    /**
     * 电梯标识
     */
    A(0, "A"),
    B(1, "B"),
    C(2, "C"),
    D(3, "D");

    private final Integer index;
    private final String mark;

    ElevatorMark(Integer index, String mark) {
        this.index = index;
        this.mark = mark;
    }

    public Integer getIndex() {
        return index;
    }

    public String getMark() {
        return mark;
    }

    public static ElevatorMark getByIndex(Integer index) {
        for (ElevatorMark mark : values()) {
            if (index.equals(mark.getIndex())) {
                return mark;
            }
        }
        return null;
    }
}
