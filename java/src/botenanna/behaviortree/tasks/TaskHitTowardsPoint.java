package botenanna.behaviortree.tasks;

import botenanna.AgentInput;
import botenanna.AgentOutput;
import botenanna.ArgumentTranslator;
import botenanna.behaviortree.*;
import botenanna.math.RLMath;
import botenanna.math.Vector2;
import botenanna.math.Vector3;

import java.util.Vector;
import java.util.function.Function;

import static java.lang.Math.acos;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;


public class TaskHitTowardsPoint extends Leaf{

private static double carPotentialSpeed = 46;
private double precision = 100;

private static final double SLIDE_ANGLE = 1.7;
private Function<AgentInput, Object> pointFunc;

    public TaskHitTowardsPoint(String[] arguments) throws IllegalArgumentException {
        super(arguments);
        if (arguments.length == 0 || arguments.length > 2) {
            throw new IllegalArgumentException();
        }

        pointFunc = ArgumentTranslator.get(arguments[0]);

        if (arguments.length == 2) {
            precision = Double.parseDouble(arguments[1]);
        }

    }

    //old
    @Override
    public void reset() {
        // Irrelevant
    }

    @Override
    public NodeStatus run(AgentInput input) throws MissingNodeException {


        //TODO Change current target to target aquired though arguments
        //TODO Change precicion in arguments
        //TODO Version 2 add 3d and expand on ballPoint
        Vector3 target = (Vector3) pointFunc.apply(input);
        ;

        // Finds the target based on the given aim
        Vector3 ballVelocity = input.ballVelocity;
        Vector3 myPos = input.myLocation;
        //Predicts the ball position
        Vector2 ballPos = input.ballLocation.plus(input.ballVelocity.scale(RLMath.predictSeconds(ballVelocity, input.ballLocation, input.myVelocity,myPos,input.myFrontVector))).asVector2();
        Vector2 point =  findCirclePoint(myPos,ballPos);

       if (angleToTarget(ballPos, ballVelocity, point, target.asVector2())){
           point = searchAngle(input,point);
       }

        //Same as drive towards point, it will  turn toward the point and drive there.
        double ang = RLMath.carsAngleToPoint(input.myLocation.asVector2(), input.myRotation.yaw, point);
        // Smooth the angle to a steering amount - this avoids wobbling
        double steering = RLMath.steeringSmooth(ang);
        AgentOutput output = new AgentOutput().withAcceleration(1).withSteer(steering);

        //Sharp turning with slide
        if (ang > SLIDE_ANGLE || ang < -SLIDE_ANGLE) {
                output.withSlide();
            }
        //Speed boost with boost if the car is too slow
        if (input.ballLocation.z<100 && input.myVelocity.asVector2().getMagnitude()<carPotentialSpeed
                && RLMath.carsAngleToPoint(ballPos, input.myRotation.yaw,input.ballLandingPosition.asVector2())>0.2){
            output.withBoost();
        }

        return new NodeStatus(Status.RUNNING, output,  this);
    }


    boolean angleToTarget(Vector2 ballPos, Vector3 ballV, Vector2 ballPoint, Vector2 target) {

        // Direction of the force, vector  from point to ball CoM
        Vector2 direction = ballPoint.minus(ballPos);
        double directionMag = direction.getMagnitude();

        if (direction.isZero()) {
            directionMag = 1;
        }

        // Calculate it as a unit vector
        Vector2 unitVector = new Vector2(direction.x / directionMag, direction.y / directionMag);

            //Find the possible momentum.
        Vector2 maxMomentum = new Vector2(unitVector.x * carPotentialSpeed, unitVector.y * carPotentialSpeed);

        // Momentum vector gives the power from the current velocity to the direction of the ball
        Vector2 appliedForce = maxMomentum.plus(ballV.asVector2());//maxMomentum.plus(input.ballVelocity.asVector2());

        // Finds the angle between the aim and the force
        double atanForce = atan2(appliedForce.y - ballPos.y, appliedForce.x - ballPos.x);
        double atanTarget = atan2(target.y - ballPos.y, target.x - ballPos.x);
        double ang =  atanTarget - atanForce;


        return ang < Math.toRadians(precision/2);

    }

    Vector2 searchAngle(AgentInput input, Vector2 target){
        Vector2 angleVector = input.ballVelocity.asVector2().minus(target).scale(-1).getNormalized();
        return angleVector.scale(3);
    }

    Vector2 findCirclePoint(Vector3 myPos, Vector2 ballPos){
        // The size of a circle point
        double circlePoint = new Vector2(ballPos.x+94*cos(0),ballPos.y+94*cos(0)).getMagnitude();

        // Vector car to ball
        Vector2  carToBall = myPos.asVector2().minus(ballPos);

        // Finds the unitvector of the car and adds the size of the circlepoint Circlevektor
        Vector2 carUnit = new Vector2((carToBall.x/carToBall.getMagnitude())*circlePoint,(carToBall.y/carToBall.getMagnitude())*circlePoint);

        //Finds the vector from
        return myPos.asVector2().plus(carUnit.scale(-1));
    }

}