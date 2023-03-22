package frc.robot.autos;

import java.util.HashMap;

import com.pathplanner.lib.PathConstraints;
import com.pathplanner.lib.PathPlanner;
import com.pathplanner.lib.PathPlannerTrajectory;
import com.pathplanner.lib.commands.FollowPathWithEvents;
import com.pathplanner.lib.commands.PPSwerveControllerCommand;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.ParallelCommandGroup;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.WaitCommand;
import frc.robot.Constants;
import frc.robot.commands.MoveToSetpoint;
//import frc.robot.commands.PIDRamp;
import frc.robot.subsystems.ArmSubsystem;
import frc.robot.subsystems.Swerve;
public class Auto extends SequentialCommandGroup {
    public Auto(Swerve s_Swerve, ArmSubsystem armSubsystem){
        HashMap<String, Command> eventMap = new HashMap<>();
        SequentialCommandGroup dropCube = new SequentialCommandGroup(new MoveToSetpoint(armSubsystem, 1, true),new MoveToSetpoint(armSubsystem, 7, true));
        dropCube.addCommands(new InstantCommand(() -> armSubsystem.extendExtender()), new WaitCommand(1),new InstantCommand(() -> armSubsystem.openClamper()), new InstantCommand(() -> armSubsystem.retractExtender()),new WaitCommand(.5));
        // wait command placeholder
        //eventMap.put("drop cube", dropCube);

        PathPlannerTrajectory path = PathPlanner.loadPath("HardPath", new PathConstraints(1, 3), true);
        var thetaController =
            new ProfiledPIDController(
                Constants.AutoConstants.kPThetaController, 0, 0, Constants.AutoConstants.kThetaControllerConstraints);
        thetaController.enableContinuousInput(-Math.PI, Math.PI);
        // SwerveControllerCommand swerveControllerCommand =
        //     new SwerveControllerCommand(
        //         path,
        //         s_Swerve::getPose,
        //         Constants.Swerve.swerveKinematics,
        //         new PIDController(Constants.AutoConstants.kPXController, 0, 0),
        //         new PIDController(Constants.AutoConstants.kPYController, 0, 0),
        //         thetaController,
        //         s_Swerve::setModuleStates,
        //         s_Swerve);
        PPSwerveControllerCommand swerveCommand = 
            new PPSwerveControllerCommand(
            path, 
            s_Swerve::getPose, 
            Constants.Swerve.swerveKinematics, 
            new PIDController(Constants.AutoConstants.kPXController, 0, 0), 
            new PIDController(Constants.AutoConstants.kPYController, 0, 0), 
            new PIDController(Constants.AutoConstants.kPThetaController, 0, .2), 
            s_Swerve::setModuleStates,
            true,
            s_Swerve);
            FollowPathWithEvents command = new FollowPathWithEvents(swerveCommand,path.getMarkers(),eventMap);
            ParallelCommandGroup driveAuto = new ParallelCommandGroup(new MoveToSetpoint(armSubsystem, 1), command);
            Field2d m_field = new Field2d();
            SmartDashboard.putData(m_field);

    // Push the trajectory to Field2d.
            m_field.getObject("traj").setTrajectory(path);

            addCommands(
                dropCube,
                new InstantCommand(() -> s_Swerve.resetOdometry(path.getInitialPose())),
                driveAuto
            );
    }
}