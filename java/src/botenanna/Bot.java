package botenanna;

import botenanna.behaviortree.*;
import botenanna.behaviortree.composites.Selector;
import botenanna.behaviortree.composites.Sequencer;
import botenanna.behaviortree.decorators.Invert;
import botenanna.behaviortree.tasks.*;
import botenanna.behaviortree.guards.*;
import botenanna.math.RLMath;
import botenanna.math.Vector2;
import botenanna.math.Vector3;
import botenanna.overlayWindow.StatusWindow;
import botenanna.physics.Rigidbody;
import botenanna.physics.Boostpads;
import rlbot.api.GameData;

public class Bot {

    public enum Team {
        BLUE, ORANGE
    }

    private final Team team;
    private final int playerIndex;
    private BehaviorTree behaviorTree;

    /** An Rocket League agent. */
    public Bot(int playerIndex, int teamIndex) {
        this.playerIndex = playerIndex;
        team = (teamIndex == 0 ? Team.BLUE : Team.ORANGE);
        behaviorTree = buildBehaviourTree();
    }

    /** Hardcoded building of a BehaviourTree */
    public BehaviorTree buildBehaviourTree() {

        /* Current tree is:
           Selector
             Sequencer0
               GuardIsKickoff
               Invert0
                 Sequencer01
                   GuardHasBoost 0
                   TaskGoTowardsPoint ball_land_pos false true
               TaskDashForward
             Sequencer1
               GuardIsMidAir
               TaskAdjustAirRotation ball_land_pos
             Sequencer2
               GuardIsDistanceLessThan my_pos ball_pos 320
               GuardIsDoubleLessThan ang_ball 0.05 true
               TaskDashForward
             Sequence3
               Selector
                 GuardIsBallOnMyHalf
                 GuardIsDistanceLessThan my_pos ball_pos 1200
                 GuardIsDistanceLessThan my_pos ball_land_pos 1800
               TaskGoTowardsPoint ball_land_pos
             Sequence4
                Invert
                    GuardHasBoot (70)
                TaskGoTowards best_boostpad
             TaskGoTowardsPoint my_goal_box
        */

        Node sequence01 = new Sequencer();
        sequence01.addChild(new GuardHasBoost(new String[] {"20"}));
        sequence01.addChild(new TaskGoTowardsPoint(new String[] {"ball_pos", "false", "true"}));

        Node invert0 = new Invert();
        invert0.addChild(sequence01);

        Node sequence0 = new Sequencer();
        sequence0.addChild(new GuardIsKickoff(new String[0]));
        sequence0.addChild(invert0);
        sequence0.addChild(new TaskDashForward(new String[0]));

        Node sequence1 = new Sequencer();
        sequence1.addChild(new GuardIsMidAir(new String[0]));
        sequence1.addChild(new TaskAdjustAirRotation(new String[] {"ball_land_pos"}));

        Node sequence2 = new Sequencer();
        sequence2.addChild(new GuardIsDistanceLessThan(new String[] {"my_pos", "ball_pos", "520"}));
        sequence2.addChild(new GuardIsDoubleLessThan(new String[] {"ang_ball", "0.05", "true"}));
        sequence2.addChild(new TaskDashForward(new String[] {}));

        Node selector = new Selector(); // low selector
        selector.addChild(new GuardIsBallOnMyHalf(new String[0]));
        selector.addChild(new GuardIsDistanceLessThan(new String[] {"my_pos", "ball_pos", "1200"}));
        selector.addChild(new GuardIsDistanceLessThan(new String[] {"my_pos", "ball_land_pos", "1800"}));

        Node sequence3 = new Sequencer();
        sequence3.addChild(selector);
        sequence3.addChild(new TaskGoTowardsPoint(new String[] {"ball_land_pos", "true", "true"}));

        Node sequence4 = new Sequencer();
        Node invert1 = new Invert();
        invert1.addChild(new GuardHasBoost(new String[] {"70"}));
        sequence4.addChild(invert1);
        sequence4.addChild(new TaskGoTowardsPoint(new String[] {"best_boost", "true", "false"}));

        selector = new Selector(); // upper selector
        selector.addChild(sequence0);
        selector.addChild(sequence1);
        selector.addChild(sequence2);
        selector.addChild(sequence3);
        selector.addChild(sequence4);
        selector.addChild(new TaskGoTowardsPoint(new String[] {"my_goal_box"}));

        BehaviorTree bhtree = new BehaviorTree();
        bhtree.addChild(selector);

        return bhtree;
    }

    /** Let the bot process the information from the input packet
     * @param packet the game tick packet from the game
     * @return an AgentOutput of what the agent want to do
     */
    public AgentOutput process(AgentInput packet) {
        return behaviorTree.evaluate(packet);
    }
}