package frc.robot;

import com.revrobotics.CANSparkMax.IdleMode;

import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.PneumaticsModuleType;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.DoubleSolenoid.Value;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.Constants.*;

public class Robot extends TimedRobot {
  public Joystick stickL = new Joystick(JoystickConstants.leftUSBindex);
  public Joystick stickR = new Joystick(JoystickConstants.rightUSBindex);
  public Joystick controller = new Joystick(ControllerConstants.USBindex);
  public boolean usingController = false;
  public boolean targetingCube = true;
  public Drivetrain driveTrain = new Drivetrain();
  public Elevator elevator = new Elevator();
  public Lime limelight = new Lime();
  public Compressor pcmCompressor = new Compressor(PneumaticsModuleType.CTREPCM);
  public DoubleSolenoid handSolenoid = new DoubleSolenoid(PneumaticsModuleType.CTREPCM, 1, 0);
  public int maxPipelines = 3;

  @Override
  public void robotInit() {
    pcmCompressor.enableDigital();
  }

  @Override
  public void robotPeriodic() {
    SmartDashboard.putBoolean("Tank Drive", Drivetrain.kTankFlag);
    SmartDashboard.putBoolean("Using Controller", usingController);
    SmartDashboard.putBoolean("Using Joystick", !usingController);
    SmartDashboard.putNumber("Elevator Average", elevator.getAveragePosition());
    SmartDashboard.putNumber("Pulley", elevator.getPulleyPosition()); 
    SmartDashboard.putNumber("Current", pcmCompressor.getCurrent());
    SmartDashboard.putBoolean("Hard Braking", driveTrain.getDriveIdle()==IdleMode.kBrake);
    SmartDashboard.putBoolean("Targeting Cube", targetingCube);
    SmartDashboard.putBoolean("Targeting Cone", !targetingCube);
    SmartDashboard.putNumber("Current Pipeline", LimelightHelpers.getCurrentPipelineIndex("limelight"));
    SmartDashboard.putNumber("Left Wheel", driveTrain.getLeftPosition());
    SmartDashboard.putNumber("Right Wheel", driveTrain.getRightPosition());
  }

  @Override
  public void autonomousInit() {}

  @Override
  public void autonomousPeriodic() {}

  @Override
  public void teleopInit() {
    usingController = false;
  }

  @Override
  public void teleopPeriodic() {
    driveTrain.diffDrive.feed();

    // manage driver control
    // default is joystick
    if (controller.getRawButtonPressed(ControllerConstants.toggleDriverControl)) usingController = usingController?false:true;

    if (controller.getRawButtonPressed(12)) {
      double currPipe = LimelightHelpers.getCurrentPipelineIndex("limelight");
      if (currPipe == maxPipelines-1) LimelightHelpers.setPipelineIndex("limelight",0);
      else LimelightHelpers.setPipelineIndex("limelight",(int)currPipe+1);
    }

    // targetting object
    if (controller.getRawButtonPressed(ControllerConstants.targetCube)) targetingCube = true;
    if (controller.getRawButtonPressed(ControllerConstants.targetCone)) targetingCube = false;

    // zero encoders
    if (controller.getRawButtonPressed(ControllerConstants.zeroEncoders)) {
      elevator.zeroEncoders();
      driveTrain.zeroDriveEncoders();
    }
    
    // controller drive code
    if (usingController) {
      // tank drive and reverse
      driveTrain.defaultFlags();
      // braking
      if (controller.getRawButtonPressed(ControllerConstants.brakingModeIndex)) driveTrain.toggleDriveIdle();

      // drive code
      double forward = controller.getRawAxis(ControllerConstants.arcadeForward);
      double rotation = controller.getRawAxis(ControllerConstants.arcadeRotation);

      driveTrain.arcade(forward, rotation);
    } else {  //joystick
      // tank drive and reverse buttons
      if (stickL.getRawButtonPressed(JoystickConstants.tankToggleButton)) driveTrain.toggleTankFlag();
      // tank drive
      if (stickR.getRawButton(JoystickConstants.limelightMode)) {
        double[] modifiedCommands = limelight.autoCenter();
        driveTrain.tank(modifiedCommands[0],modifiedCommands[1]);
      } else {
        if (Drivetrain.kTankFlag) {
          double left = stickL.getRawAxis(JoystickConstants.tankLeftAxis);
          double right = stickR.getRawAxis(JoystickConstants.tankRightAxis);
          driveTrain.tank(left, right);
        } else { // arcade drive
          double forward = stickL.getRawAxis(JoystickConstants.arcadeForwardAxis);
          double rotation = stickR.getRawAxis(JoystickConstants.arcadeRotationAxis);
          driveTrain.arcade(forward, rotation);
        }
      }

      // braking
      if (stickR.getRawButtonPressed(2)) driveTrain.toggleDriveIdle();
    }

    // auto pulley and elevator
    if (controller.getPOV()!=-1) {
      // manual pulley
      if (controller.getRawButton(ControllerConstants.rotateArmDownIndex)) {
        elevator.rotateArmDown(0.4);
      } else if (controller.getRawButton(ControllerConstants.rotateArmUpIndex)) {
        elevator.rotateArmUp(0.8);
      } else {
        elevator.stopPulley();
      }

      // manual elevator
      if (controller.getRawButton(ControllerConstants.elevatorExtend)) {
        elevator.extendArm(0.35);
      } else if (controller.getRawButton(ControllerConstants.elevatorRetract)) {
        elevator.retractArm(0.35);
      } else {
        elevator.stopElevator();
      }
    } else elevator.dealWithPOV(controller.getPOV(), targetingCube);

    // hand
    if (controller.getRawButtonPressed(ControllerConstants.handOpen)) {
      handSolenoid.set(Value.kForward);
    } else if (controller.getRawButtonPressed(ControllerConstants.handClose)) {
      handSolenoid.set(Value.kReverse);
    }
  }

  @Override
  public void disabledInit() {
    targetingCube = true;
    driveTrain.defaultFlags();
    driveTrain.stopMotor();
    elevator.stopAllMotors();
  }

  @Override
  public void disabledPeriodic() {
    driveTrain.stopMotor();
    elevator.stopAllMotors();
  }

  @Override
  public void testInit() {}

  @Override
  public void testPeriodic() {}

  @Override
  public void simulationInit() {}

  @Override
  public void simulationPeriodic() {}
}