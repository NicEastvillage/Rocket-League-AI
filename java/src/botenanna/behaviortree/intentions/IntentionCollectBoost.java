package botenanna.behaviortree.intentions;

import botenanna.fitness.FitnessDriveOverPointWithAngle;
import botenanna.fitness.FitnessFunction;
import botenanna.game.Boostpad;
import botenanna.game.Situation;
import botenanna.physics.BallPhysics;
import botenanna.physics.Path;

/** The IntentionCollectBoost will collect the best boost pad and continue towards the balls position.
 * Its signature is {@code "IntentionCollectBoost"}*/
public class IntentionCollectBoost extends Intention {

    /** The IntentionCollectBoost will collect the best boost pad and continue towards the balls position.
     * Its signature is {@code "IntentionCollectBoost"}*/
    public IntentionCollectBoost(String[] arguments) throws IllegalArgumentException {
        super(arguments);
    }

    @Override
    protected FitnessFunction getFitnessFunction(Situation input) {
        return new FitnessDriveOverPointWithAngle(new Path(input.getBestBoostPad().getPosition()), BallPhysics.getPath(input.getBall(), 50, STEPSIZE), 0.45, Boostpad.PAD_RADIUS, false);
    }

    @Override
    protected boolean shouldInterrupt(Situation input) {
        return false;
    }
}
