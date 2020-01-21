package org.firstinspires.ftc.teamcode.yoda_opmode;

import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.geometry.Vector2d;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.yoda_code.AutonomousBase;
import org.firstinspires.ftc.teamcode.yoda_enum.ArmSide;
import org.firstinspires.ftc.teamcode.yoda_enum.ArmStage;
import org.firstinspires.ftc.teamcode.yoda_enum.SkystonePos;
import org.firstinspires.ftc.teamcode.yoda_enum.TeamColor;

@Autonomous(group = "auto", name = "MZ (testing)")
public class MZ extends AutonomousBase {

    private double[] blueForwardOffsets = {0, 8, 16}; // left, middle, right
    private double[] redForwardOffsets  = {16, 8, 0}; // left middle, right
    private double forwardOffset = 0;
    private double neg = 1;

    private double stoneY = 33;
    private double foundationY = 0;
    private double adjustDistanceToFoundation = 0;
    private double lastDistanceToFoundation = 0;

    private double[] armOrder;
    private double frontArm = -5;
    private double backArm = 7;

    private double thirdStoneOffset = blueForwardOffsets[0];

    private Pose2d buildingZone = new Pose2d(20, 41, 0);
    private Pose2d loadingZone = new Pose2d(-12, 41, 0);
    private Pose2d centerLineStone = new Pose2d(0, 41, 0);
    private Pose2d centerLinePlain = new Pose2d(0, 38, 0);

    private Pose2d startingPos;
    private double baseHeading = 0;
    private boolean reversed = false;

    @Override
    public void runOpMode() throws InterruptedException {
        initialize();

        adjustDistanceToFoundation = 0;
        lastDistanceToFoundation = 0;

        if (isStopRequested()) return;
        if (strategist != null) {

            setUpVariables();

            drive.setLogTag("main");
            logCurrentPos("*** Plan to move to pick up 1st stone");
            drive.resetLatencyTimer();

            double expected_x = -27 - forwardOffset + armOrder[0] * neg;
            double expected_y =  stoneY * neg;
            drive.log("splineTo(new Pose2d(" + expected_x + "," + expected_y);
            drive.followTrajectorySync(drive.trajectoryBuilder()
                    .addMarker(0, () -> {
                        strategist.moveSkystoneArms(getArmSide(armOrder[0]), ArmStage.LOWERARM);
                        strategist.moveSkystoneArms(getArmSide(armOrder[0]), ArmStage.OPENGRABBER);
                        return null;})
                    .strafeTo(new Vector2d(expected_x, expected_y))
                    .addMarker(new Vector2d(expected_x, expected_y + 4 * neg), () -> { strategist.moveSkystoneArms(getArmSide(armOrder[0]), ArmStage.CLOSEGRABBER); return null;})
                    .build());
            logError("*** stone 1 | after moving to stone", expected_x, expected_y);

            drive.setLogTag("main");
            strategist.moveSkystoneArms(getArmSide(armOrder[0]), ArmStage.GRAB);
            logCurrentPos("*** Grabbed 1st stone");

            double y_error = moveAndDrop(
                    1,
                    59 + armOrder[0] * neg,
                    getArmSide(armOrder[0]),
                    teamColor == TeamColor.BLUE ? -1: -3);
            logCurrentPos("After dropping 1st stone");
            double offset = y_error;

            // go back for 2nd stone
            foundationToGrab(2,
                    -49 - forwardOffset + armOrder[1] * neg,
                    getArmSide(armOrder[1]),
                    teamColor == TeamColor.BLUE ? offset : 1,
                    1);
            logCurrentPos("Grabbed 2nd stone");

            y_error = moveAndDrop(
                    2,
                    50 + armOrder[1] * neg,
                    getArmSide(armOrder[1]),
                    teamColor == TeamColor.BLUE ? 1 : -1);
            logCurrentPos("After dropping 2nd stone");
            offset = y_error;

            // go back for 3rd stone
            foundationToGrab(
                    3,
                    -26 - thirdStoneOffset + armOrder[2] * neg,
                    getArmSide(armOrder[2]),
                    teamColor == TeamColor.BLUE ? offset : 2,
                    teamColor == TeamColor.BLUE ? 2 : 3);
            logCurrentPos("Grabbed 3rd stone");

            moveAndDrop(
                    3,
                    39 + armOrder[2] * neg,
                    getArmSide(armOrder[2]),
                    teamColor == TeamColor.BLUE ? offset : 0);
            logCurrentPos("After dropping 3rd stone");

            // Move Foundation
            drive.strafeLeft(3);
            drive.turnToRadians(Math.toRadians(-90 * neg), drive.getHeading());

            drive.followTrajectorySync(drive.trajectoryBuilder()
                    .addMarker(0, () -> {
                        strategist.resetSkystoneArms(); // get arm back to position.
                        drive.parkingArm.setPosition(1);
                        strategist.moveFoundationServos(0.7);
                        return null;})
                    .strafeTo(new Vector2d(50, drive.getY() - 5 * neg)) // forward
                    .addMarker(new Vector2d(50, drive.getY() - 4.5 * neg), () -> { strategist.moveFoundationServos(1); return null; }) //put mover down while moving
                    .build());
            drive.followTrajectorySync(drive.trajectoryBuilder()
                    .setReversed(true)
                    .splineTo(new Pose2d(35, (teamColor == TeamColor.BLUE ? 50 : 45) * neg, Math.toRadians(20 * neg))) // turn
                    .build());

            // Push to wall and Park
            drive.followTrajectorySync(drive.trajectoryBuilder()
                    .forward(10) // push foundation to wall
                    .addMarker(0.1, () -> { strategist.moveFoundationServos(0.7); return null;}) // open mover
                    .build());

            drive.followTrajectorySync(drive.trajectoryBuilder()
                    .strafeTo(new Vector2d(0, 38 * neg))
                    .build());

        }
    }

    private void setUpVariables() {
        if (teamColor == TeamColor.RED) {
            neg = -1;
            baseHeading = Math.toRadians(180);
            reversed = true;
            startingPos = new Pose2d(-39.5, -61.5, baseHeading);

            if (getSkystonePos() == SkystonePos.RIGHT) thirdStoneOffset = redForwardOffsets[1];
            else thirdStoneOffset = redForwardOffsets[2];
            forwardOffset = strategist.getForwardOffset(getSkystonePos(), redForwardOffsets);

            if (getSkystonePos() == SkystonePos.LEFT)        armOrder = new double[] {backArm, frontArm, frontArm};
            else if (getSkystonePos() == SkystonePos.MIDDLE) armOrder = new double[] {backArm, frontArm, frontArm};
            else if (getSkystonePos() == SkystonePos.RIGHT)  armOrder = new double[] {backArm, frontArm, frontArm};
            foundationY = 30.5;
        }
        else if (teamColor == TeamColor.BLUE) {
            neg = 1;
            baseHeading = 0;
            reversed = false;
            startingPos = new Pose2d(-32, 61.5, baseHeading);

            if (getSkystonePos() == SkystonePos.LEFT) thirdStoneOffset = blueForwardOffsets[1];
            else thirdStoneOffset = blueForwardOffsets[0];
            forwardOffset = strategist.getForwardOffset(getSkystonePos(), blueForwardOffsets);

            if (getSkystonePos() == SkystonePos.LEFT)        armOrder = new double[] {frontArm, frontArm, backArm};
            else if (getSkystonePos() == SkystonePos.MIDDLE) armOrder = new double[] {frontArm, backArm, frontArm};
            else if (getSkystonePos() == SkystonePos.RIGHT)  armOrder = new double[] {frontArm, backArm, backArm};
            foundationY = 34;
        }
        drive.setPoseEstimate(startingPos);

        buildingZone = new Pose2d(buildingZone.getX(), buildingZone.getY() * neg, baseHeading);
        loadingZone = new Pose2d(loadingZone.getX(), loadingZone.getY() * neg, baseHeading);
        centerLineStone = new Pose2d(centerLineStone.getX(), centerLineStone.getY() * neg, baseHeading);
        centerLinePlain = new Pose2d(centerLinePlain.getX(), centerLinePlain.getY() * neg, baseHeading);

        drive.turnLedOff();
    }


    private double moveAndDrop(int stoneIndex, double foundationX, ArmSide arm, double center_y_offset) {
        auto_timer.reset();
        drive.setLogTag("moveAndDrop for stone " + stoneIndex);
        double expected_x = foundationX;
        drive.log("adjustDistanceToFoundation: " + adjustDistanceToFoundation);
        double expected_y = (foundationY + adjustDistanceToFoundation) * neg;
        drive.followTrajectorySync(drive.trajectoryBuilder()
                .strafeTo(new Vector2d(drive.getX(), drive.getY() + 3 * neg))
                .setReversed(reversed)
                .splineTo(centerLineStone.plus(new Pose2d(0, center_y_offset * neg)))
                .addMarker(buildingZone.vec(), () -> { strategist.moveSkystoneArms(arm, ArmStage.PREPDROP); return null;})
                .splineTo(new Pose2d(expected_x - 6, expected_y + 6 * neg, baseHeading))
                .strafeTo(new Vector2d(expected_x, expected_y))
                .build());
        double y_error = logError("after moving to foundation", expected_x, expected_y);
        lastDistanceToFoundation = drive.getRightDistance();
        if (lastDistanceToFoundation >= 1.9 && lastDistanceToFoundation < 10) { // too far
            adjustDistanceToFoundation = 1.9 - lastDistanceToFoundation ;
        } else if (lastDistanceToFoundation >=1.5) {
            adjustDistanceToFoundation = 0;
        } else if (lastDistanceToFoundation < 1.5) { // too close, move away from foundation
            if ( adjustDistanceToFoundation <= 0) {
                adjustDistanceToFoundation = 2;
            } else {
                adjustDistanceToFoundation += 2;
            }
        } else {
            adjustDistanceToFoundation = 0;
        }
        drive.log("current distance right to foundation: " + lastDistanceToFoundation);

        if (lastDistanceToFoundation > 1.9 && lastDistanceToFoundation < 10) {
            drive.log("strafeRight: " + (lastDistanceToFoundation - 1.8));
            drive.followTrajectorySync(drive.trajectoryBuilder()
                    .strafeRight(lastDistanceToFoundation - 1.8)
                    .build());
        }
        strategist.moveSkystoneArms(arm, ArmStage.DROP);
        //double leftDistance = drive.getLeftDistance();
        //drive.log("current distance left to wall: " + leftDistance);
        //drive.log("Calculated Y: " + drive.getCalculatedY(startingPos.getY()));
        drive.log("Cycle time:" + auto_timer.milliseconds());
        return Math.abs(y_error);
    }

    private void foundationToGrab(int stoneIndex, double stoneX, ArmSide arm, double center_y_offset, double stone_y_offset) {
        auto_timer.reset();
        drive.setLogTag("foundationToGrab for stone " + stoneIndex);
        double expected_x = stoneX;
        double expected_y = (stoneY + stone_y_offset) * neg;

        drive.log("strafeTo(new Vector2d( " + drive.getX() + ", " + (drive.getY() + 2 * neg) + ")");
        drive.log("addMarker(0.3, strategist.resetSkystoneArms()");
        drive.log("splineTo(centerLinePlain)");
        drive.log("splineTo(new Pose2d(" + stoneX + "," + (stoneY + 5 + center_y_offset) * neg + ", " + Math.toDegrees(baseHeading) + ")");
        drive.log("strafeTo(new Pose2d(" + stoneX + "," + stoneY * neg);
        drive.followTrajectorySync(drive.trajectoryBuilder()
                .strafeTo(new Vector2d(drive.getX(), drive.getY() + 2 * neg)) // move away from foundation
                .addMarker(0.3, () -> {
                    strategist.resetSkystoneArms();
                    return null;})
                .setReversed(!reversed)
                .splineTo(centerLinePlain.plus(new Pose2d(0, center_y_offset * neg)))
                .addMarker(centerLinePlain.vec().plus(new Vector2d(getArmOffset(arm) * neg - 5, center_y_offset * neg)), () -> {
                    strategist.moveSkystoneArms(arm, ArmStage.OPENGRABBER);
                    return null;})
                .addMarker(centerLinePlain.vec().plus(new Vector2d(getArmOffset(arm) * neg - 15, center_y_offset * neg)), () -> {
                    strategist.moveSkystoneArms(arm, ArmStage.PREPARM);
                    return null;})
                .strafeTo(new Vector2d(expected_x, centerLinePlain.getY() + stone_y_offset * neg))
                .addMarker(new Vector2d(expected_x, (stoneY  + stone_y_offset + 3) * neg), () -> {
                    strategist.moveSkystoneArms(arm, ArmStage.LOWERARM);
                    return null;})
                .strafeTo(new Vector2d(expected_x, expected_y))
                .build());
        logError("after moving from foundation to stones", expected_x, expected_y);
        double currentDistance = drive.getRightDistance();
        drive.log("current distance right:" + currentDistance);
        if (currentDistance > 2.5 && currentDistance < 8) {
            drive.log("strafeRight:"+ (currentDistance - 2.5));
            drive.followTrajectorySync(drive.trajectoryBuilder()
                    .strafeRight(currentDistance - 2.5)
                    .addMarker(new Vector2d(expected_x, expected_y + (currentDistance + 1) * neg), () -> {
                        strategist.moveSkystoneArms(arm, ArmStage.CLOSEGRABBER);
                        return null;})
                    .build());
            sleep(300);
            logError("after strafeRight", expected_x, expected_y - (currentDistance - 2.5));
        }
        else {
            strategist.moveSkystoneArms(arm, ArmStage.CLOSEGRABBER);
            sleep(300);
        }
        strategist.moveSkystoneArms(arm, ArmStage.GRAB);
        drive.log("Cycle time:" + auto_timer.milliseconds());
    }

    private void logCurrentPos(String context) {
        drive.update();
        Pose2d currentPos = drive.getPoseEstimate();

        drive.log(context + " current Position:" + currentPos.getX() + "," + currentPos.getY()
                + ", heading:" + Math.toDegrees(currentPos.getHeading()));
        //+ "\n imu:" + Math.toDegrees(drive.getRawExternalHeading()));
        ;
    }

    private ArmSide getArmSide(double side) {
        if (side == frontArm) return ArmSide.FRONT;
        else if (side == backArm) return ArmSide.BACK;
        return ArmSide.FRONT;
    }

    private double getArmOffset(ArmSide armSide) {
        switch (armSide){
            case FRONT: return frontArm;
            case BACK: return backArm;
            default: return frontArm;
        }
    }
}
