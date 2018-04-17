package botenanna.physics;

import botenanna.math.Vector3;

import static botenanna.Ball.*;

public class BallPhysics {

    /** Get the path which the Ball will travel. This includes bounces of walls and on the floor.
     * @param duration must be zero or positive.
     * @param stepsize must be positive. A smaller step size will increase the accuracy of the Path. */
    public Path getPath(Rigidbody ball, double duration, double stepsize) {
        if (duration < 0) throw new IllegalArgumentException("Duration must be zero or positive.");
        if (stepsize <= 0) throw new IllegalArgumentException("Step size must be positive.");

        if (ball.getVelocity().isZero()) {
            return new Path(ball.getPosition());
        }

        ball = ball.clone();

        Path path = new Path();
        double timeSpent = 0;
        double timeLeft = duration;
        double nextWallHit;
        double nextGroundHit;

        path.addTimeStep(0, ball.getPosition());

        do {

            nextWallHit = SimplePhysics.predictArrivalAtAnyWall(ball, RADIUS);
            nextGroundHit = SimplePhysics.predictArrivalAtHeight(ball, RADIUS, true);

            // Check if ball doesn't hits anything
            if (timeLeft < nextGroundHit && timeLeft < nextWallHit) {
                extendPathWithNoCollision(path, ball, true, timeSpent, timeLeft, stepsize);
                return path;
            } else if (nextWallHit < nextGroundHit) {
                // Simulate until ball it hits wall
                extendPathWithNoCollision(path, ball, true, timeSpent, nextWallHit, stepsize);
                timeSpent += nextWallHit;
                Vector3 vel = ball.getVelocity();
                if (SimplePhysics.willHitSideWallNext(ball, RADIUS)) {
                    ball.setVelocity(new Vector3(vel.x * BALL_WALL_BOUNCINESS, vel.y, vel.z));
                } else {
                    ball.setVelocity(new Vector3(vel.x, vel.y * BALL_WALL_BOUNCINESS, vel.z));
                }
            } else if (nextGroundHit == 0) {
                // Simulate ball rolling until it hits wall
                ball.setVelocity(new Vector3(ball.getVelocity().x, ball.getVelocity().y, 0));

                extendPathWithNoCollision(path, ball, false, timeSpent, Math.min(nextWallHit, timeLeft), stepsize);
                timeSpent += nextWallHit;
                Vector3 vel = ball.getVelocity();
                if (SimplePhysics.willHitSideWallNext(ball, RADIUS)) {
                    ball.setVelocity(new Vector3(vel.x * BALL_WALL_BOUNCINESS, vel.y, vel.z));
                } else {
                    ball.setVelocity(new Vector3(vel.x, vel.y * BALL_WALL_BOUNCINESS, vel.z));
                }
            } else {
                // Simulate until ball it hits ground
                extendPathWithNoCollision(path, ball, true, timeSpent, nextGroundHit, stepsize);
                timeSpent += nextGroundHit;
                Vector3 vel = ball.getVelocity();
                ball.setVelocity(new Vector3(vel.x, vel.y, vel.z * BALL_GROUND_BOUNCINESS));
            }

            timeLeft = duration - timeSpent;
        } while (timeSpent <= duration);

        return path;
    }

    /** Simulate and extend already existing path. Helper function to {@link #getPath(Rigidbody, double, double)}. */
    private void extendPathWithNoCollision(Path path, Rigidbody body, boolean affectedByGravity, double timeSpent, double timeLeft, double stepsize) {
        while (timeLeft - stepsize > 0) {
            timeSpent += stepsize;
            timeLeft -= stepsize;

            SimplePhysics.step(body, stepsize, affectedByGravity);
            path.addTimeStep(timeSpent, body.getPosition());
        }

        SimplePhysics.step(body, timeLeft, affectedByGravity);
        path.addTimeStep(timeSpent + timeLeft, body.getPosition());
    }
}
