package org.firstinspires.ftc.teamcode.yoda_opmode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.yoda_code.AutonomousBase;

@Autonomous(group = "auto", name = "M9: 2 Skystones -> foundation, Park")
public class M9_TwoSkystoneF_Foundation_Park extends AutonomousBase {

    @Override
    public void runOpMode() throws InterruptedException {
        initialize();
        if (isStopRequested()) return;
        if (strategist != null) {
            strategist.grabSkyStone();
            strategist.moveAndDropSkystoneOnFoundation(10);
            strategist.goBackGrabDeliverSecondSkystone();
            //strategist.turnAndMoveFoundationAndPark();
            strategist.fromFoundationToPark();
        }
    }
}
