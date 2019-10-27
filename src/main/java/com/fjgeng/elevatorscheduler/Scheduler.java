package com.fjgeng.elevatorscheduler;

import com.fjgeng.elevatorscheduler.enums.Direction;
import com.fjgeng.elevatorscheduler.enums.ElevatorMark;
import com.fjgeng.elevatorscheduler.listener.ElevatorStateListener;
import com.fjgeng.elevatorscheduler.listener.EntranceButtonListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by gengfangjie on 2019/10/25.
 * 电梯调度器类
 */
public class Scheduler implements EntranceButtonListener, ElevatorStateListener {
    private BlockingQueue<RideRequest> rideRequestQueue;
    private Set<RideRequest> rideRequestPool;

    public Scheduler() {
        this.rideRequestPool = new HashSet<>();
        this.rideRequestQueue = new LinkedBlockingQueue<>();
    }

    public void init() {
        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        RideRequest request = rideRequestQueue.take();
//
//                        Building.INSTANCE.getElevator(ElevatorMark.A).receiveRideRequest(request);
//                        System.out.println("size" + rideRequestPool.size());
//                        if (true) continue;

                        int floor = request.getFloor();
                        Direction direction = request.getDirection();

                        List<Score> scores = new ArrayList<>();

                        for (ElevatorMark elevatorMark : ElevatorMark.values()) {
                            Elevator elevator = Building.INSTANCE.getElevator(elevatorMark);
                            Score score = new Score(elevatorMark, 0);

                            int scorePoint = 0;

                            if (elevator.isFullLoad() || elevator.isOverload()) {
                                score.setScore(Integer.MIN_VALUE);
                                continue;
                            }

                            scorePoint += (Building.TOP_FLOOR - Math.abs(floor - elevator.getElevatorState().getFloor()));
                            if (approaching(floor, direction, elevator.getElevatorState())) {
                                scorePoint += 50;
                            }
                            if (ElevatorState.WorkingState.Idle.equals(elevator.getElevatorState().getWorkingState())) {
                                scorePoint += 50;
                            }

                            score.setScore(scorePoint);
                            scores.add(score);
                        }

                        scores.sort(Comparator.comparingInt(Score::getScore));

                        if (scores.get(scores.size() - 1).getScore() < 0) {
                            System.out.println("当前电梯已满员，稍后重试调度...");
                            continue;
                        }

                        Building.INSTANCE.getElevator(scores.get(scores.size() - 1).getMark()).receiveRideRequest(request);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }

            private boolean approaching(int floor, Direction direction, ElevatorState elevatorState) {
                if (Direction.Up.equals(elevatorState.getDirection())) {
                    return Direction.Up.equals(direction) && floor - elevatorState.getFloor() > 1;
                } else if (Direction.Down.equals(elevatorState.getDirection())) {
                    return Direction.Down.equals(direction) && elevatorState.getFloor() - floor > 1;
                }
                return false;
            }
        });
    }

    @Override
    public synchronized void entranceButtonHit(int floor, Direction direction) {
        RideRequest rideRequest = new RideRequest(floor, direction);
        if (rideRequestPool.add(rideRequest)) {
            rideRequestQueue.offer(rideRequest);
        }
    }

    @Override
    public synchronized void stateChanged(Elevator elevator) {
        if (elevator.getElevatorState().getWorkingState().equals(ElevatorState.WorkingState.Waiting_in)) {
            int floor = elevator.getElevatorState().getFloor();
            if (Direction.Up.equals(elevator.getElevatorState().getDirection())) {
                rideRequestPool.remove(new RideRequest(floor, Direction.Up));
            } else if (Direction.Down.equals(elevator.getElevatorState().getDirection())) {
                rideRequestPool.remove(new RideRequest(floor, Direction.Down));
            }
        }
    }

    class Score {
        private ElevatorMark mark;
        private Integer score;

        public Score(ElevatorMark mark, Integer score) {
            this.mark = mark;
            this.score = score;
        }

        public ElevatorMark getMark() {
            return mark;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }
    }
}
