package org.firstinspires.ftc.teamcode.drive.opmode;

import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.geometry.Vector2d;
import com.acmerobotics.roadrunner.trajectory.Trajectory;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.teamcode.yoda_code.YodaMecanumDrive;

/*
 * This is a simple routine to test translational drive capabilities.
 */
@Disabled
@Config
@Autonomous(group = "drive")
public class StrafeTest extends LinearOpMode {
    public static double X = 20;
    public static double Y = 0;

    @Override
    public void runOpMode() throws InterruptedException {
        YodaMecanumDrive drive = new YodaMecanumDrive(telemetry, hardwareMap);
        drive.setPoseEstimate(new Pose2d());

        waitForStart();

        if (isStopRequested()) return;

        while (!isStopRequested()) {
            drive.followTrajectory(drive.trajectoryBuilder()
                    .strafeTo(new Vector2d(X, Y))
                    .build());

            while (!gamepad1.a && !isStopRequested()) {
                drive.update();
                telemetry.addData("IMU", Math.toDegrees(drive.getRawExternalHeading()));
                telemetry.addData("Heading", Math.toDegrees(drive.getPoseEstimate().getHeading()));
                telemetry.addData("X", drive.getPoseEstimate().getX());
                telemetry.addData("Y", drive.getPoseEstimate().getY());
                telemetry.addData("Left Encoder", encoderTicksToInches(drive.leftEncoder.getCurrentPosition()));
                telemetry.addData("Right Encoder", encoderTicksToInches(drive.rightEncoder.getCurrentPosition()));
                telemetry.addData("Front Encoder", encoderTicksToInches(drive.frontEncoder.getCurrentPosition()));
                telemetry.update();
            }
        }
    }
    public static double encoderTicksToInches(int ticks) {
        return 2.9/2.54 * 2 * Math.PI * 1 * ticks / 2880;
    }
}
