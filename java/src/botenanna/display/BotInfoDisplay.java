package botenanna.display;

import botenanna.BotenAnna;
import botenanna.behaviortree.BehaviorTree;
import botenanna.game.Situation;
import botenanna.Bot;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.io.IOException;

/** Display for bot info */
public class BotInfoDisplay extends InfoDisplay {

    public static final Color BLUE = new Color(.34, .42, 1, 1);
    public static final Color ORANGE = new Color(1, 0.7, 0.3, 1);

    private Bot bot;

    public BotInfoDisplay(Bot bot) {
        super("Car #" + bot.getPlayerIndex(), bot.getPlayerIndex() == 0 ? BLUE : ORANGE);
        this.bot = bot;

        addChangeBtButton();
    }

    /** Add button to header that allow changing of behaviour tree. */
    public void addChangeBtButton() {
        Button changeBt = new Button("Tree");
        changeBt.setFont(new Font(10));
        changeBt.setPadding(new Insets(1, 4, 1, 4));
        changeBt.setPrefHeight(16);
        changeBt.setOnAction(e -> changeBehaviourTree());
        header.getChildren().add(changeBt);
    }

    /** Update info displayed. */
    public void update() {
        Situation input = bot.getLastInputReceived();
        if (input == null || input.myCar.getPosition() == null)
            return;

        infoLabel.setText(String.format(
                "Pos: %s\n" +
                "Vel: %s\n" +
                "Rot: %s\n" +
                "AngToBall: %f\n" +
                "HasPossession: %b",
                input.myCar.getPosition().toStringFixedSize(),
                input.myCar.getVelocity().toStringFixedSize(),
                input.myCar.getRotation().toStringFixedSize(),
                input.myCar.getAngleToBall(),
                input.whoHasPossession()));
    }

    /** Change the behaviour of the bot connected to this display. */
    private void changeBehaviourTree() {
        try {
            BehaviorTree tree = BotenAnna.defaultBTBuilder.buildFromFileChooser();
            if (tree != null) {
                bot.setBehaviorTree(tree);
            }
        } catch (IOException e) {
            // Do nothing
        }
    }
}
