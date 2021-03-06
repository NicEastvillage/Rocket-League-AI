package botenanna.game.simulation;

import botenanna.game.*;
import botenanna.math.Vector3;
import botenanna.physics.BallPhysics;
import botenanna.physics.Rigidbody;
import botenanna.physics.SimplePhysics;

import static botenanna.game.Car.*;

public class Simulation {

    /** Simulates the situation forward a stepsize measured in seconds.
     * The simulatio, simulates the player car, enemy car, ball and boostpads to create a new situation
     * @return A new simulated situation
     **/
    public static Situation  simulate(Situation situation, double stepsize, ActionSet action){
        if (stepsize < 0) throw new IllegalArgumentException("Step size must be more than zero. Current Step size is: "+stepsize);

        Rigidbody simulatedBall = simulateBall(situation.getBall(), stepsize);
        Car simulatedMyCar = simulateCarActions(situation.getMyCar(), action,  simulatedBall, stepsize);
        Car simulatedEnemyCar = steppedCar(situation.getEnemyCar(), stepsize);
        Boostpad[] simulatedBoostpads = simulateBoostpads(situation.getBoostpads(), simulatedEnemyCar, simulatedMyCar, stepsize);

        simulatedMyCar.setBallDependentVariables(simulatedBall.getPosition());
        simulatedEnemyCar.setBallDependentVariables(simulatedBall.getPosition());

        return new Situation(simulatedMyCar, simulatedEnemyCar, simulatedBall , simulatedBoostpads);
    }

    /** Simulates the boostpads, if any of the cars can pick up boost and they are stepped close to a pad deactivate them
     * @return an array of boostpads after simulation. */
    private static Boostpad[] simulateBoostpads(Boostpad[] boostpads, Car enemyCar, Car myCar, double stepsize) {

        for (int i = 0; i < boostpads.length; i++) {
            Boostpad pad = boostpads[i];

            simulatePickupBoostpad(pad, myCar);
            simulatePickupBoostpad(pad, enemyCar);

            pad.reduceRespawnTimeLeft(stepsize);
        }

        return boostpads;
    }

    /** Checks if car is touching pad. If they do, give the car boost and refresh boostpads respawn timer. */
    private static void simulatePickupBoostpad(Boostpad pad, Car car) {
        if (pad.getPosition().getDistanceTo(car.getPosition()) < Boostpad.PAD_RADIUS) {
            pad.refreshRespawnTimer();
            car.addBoost(pad.getBoostAmount());
        }
    }

    /** @return a new ball which has been moved forwards. */
    public static Rigidbody simulateBall(Rigidbody ball, double step)    {
        return BallPhysics.step(ball, step);
    }

    /** @return a new car which has been moved forwards. */
    private static Car steppedCar(Car car, double step) {
        Car newCar = SimplePhysics.step(car, step, car.isMidAir());
        Vector3 pos = newCar.getPosition();
        if (pos.z < Car.GROUND_OFFSET) {
            //Hit ground
            newCar.setPosition(pos.withZ(Car.GROUND_OFFSET));
            newCar.setVelocity(newCar.getVelocity().withZ(0));
            newCar.setIsMidAir(false);
        }
        return newCar;
    }

    /** Simulates a car with actions **
     * @param action the current actions from the Agent
     * @return a Car simulated forward in  the new situation     */
    private static Car simulateCarActions(Car car, ActionSet action, Rigidbody ball, double delta){

        boolean boosting = (action.isBoostDepressed() && car.getBoost() != 0);

        if (car.isMidAir()) {

        } else {
            // We are on the ground
            double newYaw = car.getRotation().yaw + getTurnRate(car) * action.getSteer() * delta;
            newYaw %= Math.PI; // Clamp to be between -PI and PI
            car.setRotation(car.getRotation().withYaw(newYaw));

            Vector3 acceleration = new Vector3();

            if (boosting) {
                acceleration = acceleration.plus(car.getFrontVector().scale(ACCELERATION_BOOST));
            } else if (action.getThrottle() != 0) {
                acceleration = acceleration.plus(car.getFrontVector().scale(getAccelerationStrength(car, (int)action.getThrottle(), false)));
            } else {
                // we assume our velocity is never sideways
                acceleration = acceleration.plus(car.getVelocity().getNormalized().scale(DECELERATION));
            }

            if (action.getSteer() != 0) {
                acceleration = acceleration.scale(TURN_ACCELERATION_DECREASE);
            }

            car.setAcceleration(acceleration);
        }

        car = steppedCar(car, delta);
        car.setBallDependentVariables(ball.getPosition());

        return car;
    }

    /** @param dir Direction of acceleration. 1 for forwards, -1 for backwards. */
    public static double getAccelerationStrength(Car car, int dir, boolean boosting) {
        Vector3 vel = car.getVelocity();
        Vector3 front = car.getFrontVector();

        double velProjFrontSize = vel.dot(front) / front.dot(front);
        double velDir = (velProjFrontSize >= 0) ? 1 : -1;
        Vector3 velParallelFront = front.scale(velProjFrontSize);
        double velLength = velParallelFront.getMagnitude();

        return MAX_VELOCITY_BOOST * dir - velLength * velDir;

    }

    /** @returns the turn rate of the car. */
    public static double getTurnRate(Car car) {

        double vel = car.getVelocity().getMagnitude();
        // See documentation "turnrate linear function.png" for math.
        return 1.325680896 + 0.0002869694124 * vel;
    }
}