package botenanna.fitness;

import botenanna.game.Car;
import botenanna.game.Situation;
import botenanna.math.Vector3;
import botenanna.physics.Path;

import java.util.function.Function;

/** This class is used when you want a fitness value for "Shoot in direction". */
public class FitnessShootInDirection implements FitnessFunction {

    private final double ANGLE_SCALE = 5.756462;
    private final double DIST_SCALE = 1/450d;

    private Function<Situation, Vector3> targetPointFunc;
    private double angleDeviation;
    private double distDeviation;

    /** @param targetPointFunc the point to shoot towards
     *  @param angleDeviation an value that the angle is allowed to deviate.
     *  @param distDeviation an value that the distance is allowed to deviate. */
    public FitnessShootInDirection(Function<Situation, Vector3> targetPointFunc, double angleDeviation, double distDeviation){
        this.targetPointFunc = targetPointFunc;
        this.angleDeviation = angleDeviation;
        this.distDeviation = distDeviation;
    }

    /**	Takes a situation and time spent and returns a fitness value of that situation.
     *  This method extracts needed data from agentInput and passes in on to calculation.
     *  This is done to make the method testable.
     *  @param situation the situation to be evaluated.
     *  @param timeSpent the seconds used since origin of situation.
     *  @return a fitness value for the given situation. */
    @Override
    public double calculateFitness(Situation situation, double timeSpent) {
        return calculateFitnessValue(targetPointFunc.apply(situation), situation.getBall().getPosition(),
                situation.getBall().getVelocity(), situation.getMyCar().getPosition(), situation.getMyCar().getVelocity(), timeSpent);
    }

    /** Takes the needed information and calculates the fitness value.
     *  @param ballLocation ball location
     *  @param ballVelocity ball velocity
     *  @param carLocation the cars location.
     *  @param carVelocity the cars velocity.
     *  @param timeSpent the seconds used since origin of the situation.
     *  @return a fitness value for the given situation. */
    double calculateFitnessValue(Vector3 targetPoint, Vector3 ballLocation, Vector3 ballVelocity, Vector3 carLocation, Vector3 carVelocity, double timeSpent){

        Vector3 desiredShotDirection = targetPoint.minus(carLocation); //From car to desiredPoint
        Vector3 currentShotDirection = ballVelocity.plus(carVelocity);

        double distanceToBall = ballLocation.getDistanceTo(carLocation);
        double angleDiffernce = desiredShotDirection.getAngleTo(currentShotDirection);

        return (Math.pow(Math.E, -(timeSpent + (distanceToBall * DIST_SCALE) + angleDiffernce * ANGLE_SCALE)));
    }

    /** Checks if the deviations are fulfilled.
     *  @param situation the situation to be evaluated.
     *  @param timeSpent the time spend since origin.
     *  @return true if the variables are less or equal to the deviation. */
    @Override
    public boolean isDeviationFulfilled(Situation situation, double timeSpent) {

        Vector3 targetPoint = targetPointFunc.apply(situation);

        //Calculate function variables
        Car car = situation.getMyCar();
        double distToBall = car.getPosition().getDistanceTo(targetPoint); // Distance

        Vector3 desiredShotDirection = targetPoint.minus(car.getPosition()); //From car to desiredPoint
        Vector3 currentShotDirection = situation.getBall().getVelocity().plus(car.getVelocity());
        double angleDifference = desiredShotDirection.getAngleTo(currentShotDirection);

        return distToBall <= distDeviation && angleDifference <= angleDeviation;
    }
}
