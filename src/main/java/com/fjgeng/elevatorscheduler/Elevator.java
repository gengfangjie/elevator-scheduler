package com.fjgeng.elevatorscheduler;

import com.fjgeng.elevatorscheduler.enums.Direction;
import com.fjgeng.elevatorscheduler.listener.ElevatorStateListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by gengfangjie on 2019/10/25.
 * 电梯厢类
 * 对电梯的运行速度做了如下假设
 * 1. 从启动到加速到最大只需要1.5s并且加速完成时走过的路程刚好为1层
 * 2. 相应的从最大速度减速到停止需要1.5s并且减速完成时走过的路程也为1层
 * 3. 以最大速度穿过1层的时间为1s
 * 4. 从启动到停止只走1层的话需要2s(因为无法达到最大速度)
 *
 * 没有时间修正的问题：有时候发现用户没有乘上电梯，原因是这个电梯满员后离开了，入口按钮熄灭，但是用户没有重新按钮
 */
public class Elevator {
    // 假设电梯容量为11人
    public static final int CAPACITY = 11;

    // 假设加速或减速通过一层的运行时间为1.5s
    private static final long ACC_OR_DSC_ONE_FLOOR = 1500;
    // 假设加速并减速通过一层的运行时间为2s
    private static final long ACC_AND_DSC_ONE_FLOOR = 2000;
    // 假设匀速穿过一层的运行时间为1s
    private static final long PASS_FLOOR_TIME = 1000;
    // 假设电梯每次等待下客时间为4s
    private static final long WAITING_OUT_TIME = 4000;
    // 假设电梯每次等待上客时间为4s
    private static final long WAITING_IN_TIME = 4000;

    private final Object lock;

    // 电梯标识
    private final String mark;
    // 电梯入口
    private final List<Entrance> entranceList;

    // 电梯状态
    private ElevatorState elevatorState;
    // 电梯内部乘客
    private Queue<Passenger> passengerQueue;
    // 监听电梯状态变化的对象
    private Set<ElevatorStateListener> stateChangeListenerSet;

    // 目的楼层
    private TreeSet<Integer> destinationSet;
    // scheduler分配的向上楼层
    private TreeSet<Integer> upRequestSet;
    // scheduler分配的向下楼层
    private TreeSet<Integer> downRequestSet;
    // 电梯下一步动作计划
    private NextStep nextStep;

    public Elevator(String mark, List<Entrance> entranceList) {
        this.lock = new Object();
        this.mark = mark;
        this.entranceList = entranceList;
        this.stateChangeListenerSet = new CopyOnWriteArraySet<>();
        this.elevatorState = new ElevatorState();
        this.passengerQueue = new LinkedBlockingQueue<>(CAPACITY);
        this.destinationSet = new TreeSet<>();
        this.upRequestSet = new TreeSet<>();
        this.downRequestSet = new TreeSet<>();
        this.nextStep = new NextStep(0, null);
    }

    public void init() {
        Executors.newSingleThreadExecutor().submit(new Engine());
    }

    public String getMark() {
        return mark;
    }

    public ElevatorState getElevatorState() {
        return elevatorState;
    }

    public List<Entrance> getEntranceList() {
        return entranceList;
    }

    public boolean passengerIn(Passenger passenger) {
        boolean result = this.passengerQueue.offer(passenger);
        if (result) {
            System.out.println(String.format("用户 %s 进入成功", passenger.getId()));
            return true;
        }
        System.out.println("用户进入失败");
        return false;
    }

    public boolean passengerOut(Passenger passenger) {
        if (canExit()) {
            this.passengerQueue.remove(passenger);
            return true;
        }
        return false;
    }

    public boolean canExit() {
        return this.elevatorState.getWorkingState().equals(ElevatorState.WorkingState.Idle)
                || this.elevatorState.getWorkingState().equals(ElevatorState.WorkingState.Waiting_out);
    }

    // 乘客在电梯内按下目的楼层
    public boolean addDestination(int floor) {
        synchronized (lock) {
            return this.destinationSet.add(floor);
        }
    }

    // 乘客在电梯内取消目的楼层 逻辑无时间支持验证。。。
    public boolean removeDestination(int floor) {
        synchronized (lock) {
            return this.destinationSet.remove(floor);
        }
    }

    // 接收到调度指令
    public void receiveRideRequest(RideRequest request) {
        synchronized (lock) {
            if (request.getDirection().equals(Direction.Up)) {
                upRequestSet.add(request.getFloor());
            } else if (request.getDirection().equals(Direction.Down)) {
                downRequestSet.add(request.getFloor());
            }
        }
    }

    public boolean isOverload() {
        return this.passengerQueue.size() > CAPACITY;
    }

    public boolean isFullLoad() {
        return this.passengerQueue.size() == CAPACITY;
    }

    public void registerListener(ElevatorStateListener listener) {
        this.stateChangeListenerSet.add(listener);
    }

    public void removeListener(ElevatorStateListener listener) {
        this.stateChangeListenerSet.remove(listener);
    }

    private void stateChanged() {
        System.out.println(String.format("电梯 %s 状态: %s 乘客: %s", mark, elevatorState, passengerQueue.size()));
        synchronized (lock) {
            this.stateChangeListenerSet.forEach(listener -> listener.stateChanged(this));
        }
    }

    class Engine implements Runnable {
        public void run() {
            while (true) {
                try {
                    while (nextStep.getNextFloor() > 0) {
                        synchronized (lock) {
                            doWork();
                            setNext();
                        }
                    }
                    elevatorState.setWorkingState(ElevatorState.WorkingState.Idle);
                    elevatorState.setDirection(null);
                    TimeUnit.MILLISECONDS.sleep(100);
                    synchronized (lock) {
                        setNext();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // 电梯进入stall状态才可以进行转向
        private void directionCorrect() {
            if (elevatorState.getStall()) {
                if (nextStep.getNextFloor() - elevatorState.getFloor() > 0) {
                    elevatorState.setDirection(Direction.Up);
                } else if (nextStep.getNextFloor() - elevatorState.getFloor() < 0) {
                    elevatorState.setDirection(Direction.Down);
                } else {
                    elevatorState.setDirection(null);
                }
            }
        }

        private void doSetNext(int result, Direction direction) {
            nextStep.setNextFloor(result);
            nextStep.setNextDirection(direction);
            if (elevatorState.getWorkingState().equals(ElevatorState.WorkingState.Up)) {
                if (Direction.Up.equals(elevatorState.getDirection())) {
                    if (nextFloorBelow(nextStep.getNextFloor())) {
                        System.out.println("运行错误");
                    }
                }
            }
            directionCorrect();
        }

        private int setNext() {
            int result = 0;
            int currentFloor = elevatorState.getFloor();

            Integer nearestDestinationHigher = Utils.getNearestHigher(destinationSet, currentFloor);
            Integer nearestDestinationLower = Utils.getNearestLower(destinationSet, currentFloor);
            Integer nearestUpRequestHigher = Utils.getNearestHigher(upRequestSet, currentFloor);
            Integer nearestUpRequestLower = Utils.getNearestLower(upRequestSet, currentFloor);
            Integer nearestDownRequestHigher = Utils.getNearestHigher(downRequestSet, currentFloor);
            Integer nearestDownRequestLower = Utils.getNearestLower(downRequestSet, currentFloor);

            if (Direction.Up.equals(elevatorState.getDirection())) {
                // 取 最近 更高的 向上楼层
                result = Utils.lower(nearestDestinationHigher, nearestUpRequestHigher);
                if (result != 0) {
                    doSetNext(result, Direction.Up);
                    return result;
                }
                // 取 最远 更高的 向下楼层
                result = downRequestSet.isEmpty() ? 0 : downRequestSet.last();
                if (result != 0 && result > currentFloor) {
                    doSetNext(result, Direction.Down);
                    return result;
                }
                // 取 最近 更低的 向下楼层
                result = Math.max(nearestDestinationLower, nearestDownRequestLower);
                if (result != 0) {
                    doSetNext(result, Direction.Down);
                    return result;
                }
                // 取 最远 更低的 向上楼层
                result = upRequestSet.isEmpty() ? 0 : upRequestSet.first();
                if (result != 0 && result < currentFloor) {
                    doSetNext(result, Direction.Up);
                    return result;
                }
            }
            if (Direction.Down.equals(elevatorState.getDirection())) {
                // 取 最近 更低的 向下楼层
                result = Math.max(nearestDestinationLower, nearestDownRequestLower);
                if (result != 0) {
                    doSetNext(result, Direction.Down);
                    return result;
                }
                // 取 最远 更低的 向上楼层
                result = upRequestSet.isEmpty() ? 0 : upRequestSet.first();
                if (result != 0 && result < currentFloor) {
                    doSetNext(result, Direction.Up);
                    return result;
                }
                // 取 最近 更高的 向上楼层
                result = Utils.lower(nearestDestinationHigher, nearestUpRequestHigher);
                if (result != 0) {
                    doSetNext(result, Direction.Up);
                    return result;
                }
                // 取 最远 更高的 向下楼层
                result = downRequestSet.isEmpty() ? 0 : downRequestSet.last();
                if (result != 0 && result > currentFloor) {
                    doSetNext(result, Direction.Down);
                    return result;
                }
            }
            if (upRequestSet.contains(currentFloor)) {
                if (elevatorState.getStall()) {
                    elevatorState.setDirection(Direction.Up);
                    nextStep.setNextFloor(currentFloor);
                    nextStep.setNextDirection(Direction.Up);
                    return result;
                }
            }
            if (downRequestSet.contains(currentFloor)) {
                if (elevatorState.getStall()) {
                    elevatorState.setDirection(Direction.Down);
                    nextStep.setNextFloor(currentFloor);
                    nextStep.setNextDirection(Direction.Down);
                    return result;
                }
            }

            List<Integer> floorList = new ArrayList<>();
            floorList.add(nearestDestinationHigher);
            floorList.add(nearestDestinationLower);
            floorList.add(nearestUpRequestHigher);
            floorList.add(nearestUpRequestLower);
            floorList.add(nearestDownRequestHigher);
            floorList.add(nearestDownRequestLower);

            result = Utils.getNearest(floorList, currentFloor);
            nextStep.setNextFloor(result);
            if (result == nearestUpRequestHigher || result == nearestUpRequestLower) {
                nextStep.setNextDirection(Direction.Up);
            } else if (result == nearestDownRequestHigher || result == nearestDownRequestLower) {
                nextStep.setNextDirection(Direction.Down);
            }
            directionCorrect();
            return Utils.getNearest(floorList, currentFloor);
        }

        /**
         * nextFloor与当前车厢距离(gap)大于等于2时，车厢加速或匀速穿过1层
         * nextFloor与当前车厢距离(gap)等于1时，车厢减速到达
         *
         * @throws InterruptedException
         */
        private void doWork() throws InterruptedException {
            elevatorState.setStall(false);
            int nextFloor = nextStep.getNextFloor();
            int gap = Math.abs(elevatorState.getFloor() - nextFloor);

            if (elevatorState.getWorkingState().equals(ElevatorState.WorkingState.Idle)
                    || elevatorState.getWorkingState().equals(ElevatorState.WorkingState.Waiting_in)) {
                if (nextFloorAbove(nextFloor)) {
                    startUp(gap);
                } else if (nextFloorBelow(nextFloor)) {
                    startDown(gap);
                }
            } else if (elevatorState.getWorkingState().equals(ElevatorState.WorkingState.Up)) {
                if (Direction.Up.equals(elevatorState.getDirection())) {
                    if (nextFloorAbove(nextFloor)) {
                        goUp(gap);
                    } else if (nextFloorBelow(nextFloor)) {
                        System.out.println("运行错误");
//                        System.out.println(String.format("电梯: %s 在 %s 层减速准备反向运行", mark, elevatorState.getFloor()));
//                        goUp(1);
//                        elevatorState.setWorkingState(ElevatorState.WorkingState.Stall);
                        return;
                    }
                }
            } else if (elevatorState.getWorkingState().equals(ElevatorState.WorkingState.Down)) {
                if (Direction.Down.equals(elevatorState.getDirection())) {
                    if (nextFloorBelow(nextFloor)) {
                        goDown(gap);
                    } else if (nextFloorAbove(nextFloor)) {
                        System.out.println("运行错误");
//                        System.out.println(String.format("电梯: %s 在 %s 层减速准备反向运行", mark, elevatorState.getFloor()));
//                        goDown(1);
//                        elevatorState.setWorkingState(ElevatorState.WorkingState.Stall);
                        return;
                    }
                }
            }
            gap = Math.abs(elevatorState.getFloor() - nextFloor);
            if (gap == 0) {
                waitOnEntrance();
            }
        }

        private boolean nextFloorAbove(int nextFloor) {
            return nextFloor > elevatorState.getFloor();
        }

        private boolean nextFloorBelow(int nextFloor) {
            return nextFloor < elevatorState.getFloor();
        }

        private void startUp(int gap) throws InterruptedException {
            elevatorState.setWorkingState(ElevatorState.WorkingState.Up);
            TimeUnit.MILLISECONDS.sleep(gap >= 2 ? ACC_OR_DSC_ONE_FLOOR : ACC_AND_DSC_ONE_FLOOR);
            elevatorState.setFloor(elevatorState.getFloor() + 1);
            stateChanged();
        }

        private void startDown(int gap) throws InterruptedException {
            elevatorState.setWorkingState(ElevatorState.WorkingState.Down);
            TimeUnit.MILLISECONDS.sleep(gap >= 2 ? ACC_OR_DSC_ONE_FLOOR : ACC_AND_DSC_ONE_FLOOR);
            elevatorState.setFloor(elevatorState.getFloor() - 1);
            stateChanged();
        }

        private void goUp(int gap) throws InterruptedException {
            TimeUnit.MILLISECONDS.sleep(gap >= 2 ? PASS_FLOOR_TIME : ACC_OR_DSC_ONE_FLOOR);
            elevatorState.setFloor(elevatorState.getFloor() + 1);
            stateChanged();
        }

        private void goDown(int gap) throws InterruptedException {
            TimeUnit.MILLISECONDS.sleep(gap >= 2 ? PASS_FLOOR_TIME : ACC_OR_DSC_ONE_FLOOR);
            elevatorState.setFloor(elevatorState.getFloor() - 1);
            stateChanged();
        }

        private void waitOnEntrance() throws InterruptedException {
            System.out.println(String.format("电梯: %s 到达 %s 层停靠", mark, elevatorState.getFloor()));
            elevatorState.setStall(true);
            elevatorState.setDirection(nextStep.getNextDirection());
            elevatorState.setWorkingState(ElevatorState.WorkingState.Waiting_out);
            stateChanged();
            TimeUnit.MILLISECONDS.sleep(WAITING_OUT_TIME);
            elevatorState.setWorkingState(ElevatorState.WorkingState.Waiting_in);
            stateChanged();
            TimeUnit.MILLISECONDS.sleep(WAITING_IN_TIME);
            destinationSet.remove(elevatorState.getFloor());

            if (elevatorState.getDirection().equals(Direction.Up)) {
                upRequestSet.remove(elevatorState.getFloor());
            } else if (elevatorState.getDirection().equals(Direction.Down)) {
                downRequestSet.remove(elevatorState.getFloor());
            }
        }
    }

    class NextStep {
        private volatile int nextFloor;
        private Direction nextDirection;

        public NextStep(int nextFloor, Direction nextDirection) {
            this.nextFloor = nextFloor;
            this.nextDirection = nextDirection;
        }

        public int getNextFloor() {
            return nextFloor;
        }

        public void setNextFloor(int nextFloor) {
            this.nextFloor = nextFloor;
        }

        public Direction getNextDirection() {
            return nextDirection;
        }

        public void setNextDirection(Direction nextDirection) {
            this.nextDirection = nextDirection;
        }
    }
}