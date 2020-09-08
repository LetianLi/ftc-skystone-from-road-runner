package org.firstinspires.ftc.teamcode.yoda_code

import android.util.Log
import com.acmerobotics.roadrunner.localization.Localizer

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.acmerobotics.roadrunner.kinematics.Kinematics
import com.acmerobotics.roadrunner.util.Angle
import com.qualcomm.robotcore.util.ElapsedTime
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.DecompositionSolver
import org.apache.commons.math3.linear.LUDecomposition
import org.apache.commons.math3.linear.MatrixUtils

/**
 * Localizer based on two unpowered tracking omni wheels and an orientation sensor.
 * Supports extraction of Velocity.
 *
 * @param wheelPoses wheel poses relative to the center of the robot (positive X points forward on the robot)
 */
abstract class TwoTrackingWheelVelocityMeasurer(
    wheelPoses: List<Pose2d>
) : Localizer {
    private var _poseEstimate = Pose2d()
    override var poseEstimate: Pose2d
        get() = _poseEstimate
        set(value) {
            lastWheelPositions = emptyList()
            lastHeading = Double.NaN
            _poseEstimate = value
        }
    var robotPoseDelta = Pose2d()
    var lastMeasurementInterval = Double.NaN
    private var lastWheelPositions = emptyList<Double>()
    private var lastHeading = Double.NaN
    private var clock = ElapsedTime()

    private val forwardSolver: DecompositionSolver

    init {
        require(wheelPoses.size == 2) { "2 wheel poses must be provided" }

        val inverseMatrix = Array2DRowRealMatrix(3, 3)
        for (i in 0..1) {
            val orientationVector = wheelPoses[i].headingVec()
            val positionVector = wheelPoses[i].vec()
            inverseMatrix.setEntry(i, 0, orientationVector.x)
            inverseMatrix.setEntry(i, 1, orientationVector.y)
            inverseMatrix.setEntry(i, 2,
                    positionVector.x * orientationVector.y - positionVector.y * orientationVector.x)
        }
        inverseMatrix.setEntry(2, 2, 1.0)

        forwardSolver = LUDecomposition(inverseMatrix).solver

        require(forwardSolver.isNonSingular) { "The specified configuration cannot support full localization" }

        clock.reset()
    }

    fun updatePoseDelta() {
        val wheelPositions = getWheelPositions()
        val heading = getHeading()
        if (lastWheelPositions.isNotEmpty()) {
            val wheelDeltas = wheelPositions
                    .zip(lastWheelPositions)
                    .map { it.first - it.second }
            val headingDelta = Angle.normDelta(heading - lastHeading)
            val rawPoseDelta = forwardSolver.solve(MatrixUtils.createRealMatrix(
                    arrayOf((wheelDeltas + headingDelta).toDoubleArray())
            ).transpose())
            lastMeasurementInterval = clock.seconds()
            clock.reset()
            robotPoseDelta = Pose2d(
                    rawPoseDelta.getEntry(0, 0),
                    rawPoseDelta.getEntry(1, 0),
                    rawPoseDelta.getEntry(2, 0)
            )
            _poseEstimate = Kinematics.relativeOdometryUpdate(_poseEstimate, robotPoseDelta)
            Log.i("TwoTrackingWheelVelocityMeasurer", "Data: Velocity: " + robotPoseDelta.toString() + " | Measuring Interval: " + lastMeasurementInterval*1000 + " ms")
        }
        lastWheelPositions = wheelPositions
        lastHeading = heading
    }

    fun getPoseVelocity(): Pose2d {
        return robotPoseDelta.div(lastMeasurementInterval)
    }

    /**
     * returns the measuring interval (in ms) used in the last updatePoseDelta() call
     */
    fun getMeasuringInterval(): Double {
        return lastMeasurementInterval * 1000
    }

    /**
     * Returns the positions of the tracking wheels in the desired distance units (not encoder counts!)
     */
    abstract fun getWheelPositions(): List<Double>

    /**
     * Returns the heading of the robot (usually from a gyroscope or IMU).
     */
    abstract fun getHeading(): Double
}
