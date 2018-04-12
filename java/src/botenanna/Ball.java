package botenanna;

import botenanna.math.Vector3;
import botenanna.physics.Path;
import botenanna.physics.Rigidbody;
import rlbot.api.GameData;

/** A ball */
public class Ball extends Rigidbody {

    public static final double RADIUS = 92.2;
    public static final double DIAMETER = RADIUS * 2;
    public static final double SLIDE_DECCELERATION = -230;
    public static final double BALL_GROUND_BOUNCINESS = -0.6;
    public static final double BALL_WALL_BOUNCINESS = -0.6;

    public Ball() {
        setAffectedByGravity(true);
    }

    /** Create a ball from the GameData's BallInfo. This way position, velocity,
     * acceleration, rotation and gravity is set immediately. */
    public Ball(GameData.BallInfo ball) {
        setPosition(Vector3.convert(ball.getLocation()));
        setVelocity(Vector3.convert(ball.getVelocity()));
        setAcceleration(Vector3.convert(ball.getAcceleration()));
        setRotation(Vector3.convert(ball.getRotation()));
        setAffectedByGravity(true);
    }

    /** Get the path which the Ball will travel. This includes bounces of walls and on the floor.
     * @param duration must be zero or positive.
     * @param stepsize must be positive. A smaller step size will increase the accuracy of the Path. */
    @Override
    public Path getPath(double duration, double stepsize) {
        if (duration < 0) throw new IllegalArgumentException("Duration must be zero or positive.");
        if (stepsize <= 0) throw new IllegalArgumentException("Step size must be positive.");

        Rigidbody simulation = this.clone();

        Path path = new Path();
        double timeSpent = 0;
        double timeLeft = duration;
        double nextWallHit;
        double nextGroundHit;

        path.addTimeStep(0, simulation.getPosition());

        do {

            nextWallHit = simulation.predictArrivalAtAnyWall(RADIUS);
            nextGroundHit = simulation.predictArrivalAtHeight(RADIUS);

            // Check if ball doesn't hits anything
            if (timeLeft < nextGroundHit && timeLeft < nextWallHit) {
                extendPathWithNoCollision(path, simulation, timeSpent, timeLeft, stepsize);
                return path;
            } else if (nextWallHit < nextGroundHit) {
                // Simulate until ball it hits wall
                extendPathWithNoCollision(path, simulation, timeSpent, nextWallHit, stepsize);
                timeSpent += nextWallHit;
                Vector3 vel = simulation.getVelocity();
                if (willHitSideWallNext(RADIUS)) {
                    simulation.setVelocity(new Vector3(vel.x * BALL_WALL_BOUNCINESS, vel.y, vel.z));
                } else {
                    simulation.setVelocity(new Vector3(vel.x, vel.y * BALL_WALL_BOUNCINESS, vel.z));
                }
            } else if (nextGroundHit == 0) {
                // Simulate ball rolling until it hits wall
                simulation.setAffectedByGravity(false);
                simulation.setVelocity(new Vector3(simulation.getVelocity().x, simulation.getVelocity().y, 0));

                extendPathWithNoCollision(path, simulation, timeSpent, Math.min(nextWallHit, timeLeft), stepsize);
                timeSpent += nextWallHit;
                Vector3 vel = simulation.getVelocity();
                if (willHitSideWallNext(RADIUS)) {
                    simulation.setVelocity(new Vector3(vel.x * BALL_WALL_BOUNCINESS, vel.y, vel.z));
                } else {
                    simulation.setVelocity(new Vector3(vel.x, vel.y * BALL_WALL_BOUNCINESS, vel.z));
                }
            } else {
                // Simulate until ball it hits ground
                extendPathWithNoCollision(path, simulation, timeSpent, nextGroundHit, stepsize);
                timeSpent += nextGroundHit;
                Vector3 vel = simulation.getVelocity();
                simulation.setVelocity(new Vector3(vel.x, vel.y, vel.z * BALL_GROUND_BOUNCINESS));
            }

            timeLeft = duration - timeSpent;
        } while (timeSpent <= duration);

        return path;
    }

    /** Simulate and extend already existing path. Helper function to {@link #getPath(double, double)}. */
    private void extendPathWithNoCollision(Path path, Rigidbody ballCopy, double timeSpent, double timeLeft, double stepsize) {
        while (timeLeft - stepsize > 0) {
            timeSpent += stepsize;
            timeLeft -= stepsize;

            ballCopy.step(stepsize);
            path.addTimeStep(timeSpent, ballCopy.getPosition());
        }

        ballCopy.step(timeLeft);
        path.addTimeStep(timeSpent + timeLeft, ballCopy.getPosition());
    }
}
