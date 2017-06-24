package controller.ui.ui.components;

import common.TotalScaleLayout;
import controller.action.ActionBoard;
import controller.action.ui.CardIncrease;
import controller.net.RobotOnlineStatus;
import controller.net.RobotWatcher;
import controller.ui.helper.FontHelper;
import controller.ui.localization.Localization;
import controller.ui.localization.LocalizationManager;
import controller.ui.ui.customized.Button;
import controller.ui.ui.customized.CountDownCircle;
import data.Helper;
import data.PlayerInfo;
import data.Rules;
import data.hl.HL;
import data.spl.SPL;
import data.states.AdvancedData;
import data.values.Penalties;
import data.values.Side;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Created by rkessler on 2017-06-11.
 */
public class Robot extends AbstractComponent {

    private ImageIcon lanIcon;
    private Side side;
    private int id;
    private JButton robot;
    private JLabel robotLabel;

    private JButton yellowCard;
    private JButton redCard;


    protected ImageIcon lanOnline;
    protected ImageIcon lanHighLatency;
    protected ImageIcon lanOffline;
    protected ImageIcon lanUnknown;


    protected static final String ICONS_PATH = "config/icons/";
    protected static final String ONLINE = "wlan_status_green.png";
    protected static final String OFFLINE = "wlan_status_red.png";
    protected static final String HIGH_LATENCY = "wlan_status_yellow.png";
    protected static final String UNKNOWN_ONLINE_STATUS = "wlan_status_grey.png";
    public static final Color COLOR_HIGHLIGHT = Color.YELLOW;

    protected static final int UNPEN_HIGHLIGHT_SECONDS = 10;

    private CountDownCircle robotTime;


    public Robot(Side side, int id) {
        this.side = side;
        this.id = id;
        lanOnline = new ImageIcon(ICONS_PATH + ONLINE);
        lanHighLatency = new ImageIcon(ICONS_PATH + HIGH_LATENCY);
        lanOffline = new ImageIcon(ICONS_PATH + OFFLINE);
        lanUnknown = new ImageIcon(ICONS_PATH + UNKNOWN_ONLINE_STATUS);

        setup();
    }


    public void updateLayout(double aspectRatio){
        TotalScaleLayout robotLayout = new TotalScaleLayout(robot);
        robot.setLayout(robotLayout);
        robot.removeAll();

        // Figure out a way to make this easier
        double rightOffset = 0.05;

        double robotTimeWidth = 0.8 / aspectRatio;
        robotLayout.add(1 - robotTimeWidth - rightOffset, 0.1, robotTimeWidth, 0.8, robotTime);

        double cardWidth = 0.4 / aspectRatio;
        robotLayout.add(1-robotTimeWidth-cardWidth - rightOffset, 0.1, cardWidth, 0.8, yellowCard);
        robotLayout.add(1-robotTimeWidth-2*cardWidth - rightOffset, 0.1, cardWidth, 0.8, redCard);

        double restWidth = 1 - robotTimeWidth-2*cardWidth - rightOffset;

        robotLayout.add(0, 0, restWidth, 1, robotLabel);
    }

    public void setup() {
        robot = new JButton();
        robot.addActionListener(ActionBoard.robot[side.value()][this.id]);
        robot.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                // This is only called when the user releases the mouse button.
                double w = e.getComponent().getSize().getWidth();
                double h = e.getComponent().getSize().getHeight();
                double aspect = w / h;
                updateLayout(aspect);
            }
        });

        TotalScaleLayout robotLayout = new TotalScaleLayout(robot);
        robot.setLayout(robotLayout);

        robotLabel = new JLabel();
        robotLabel.setHorizontalAlignment(JLabel.CENTER);
        robotLabel.setIcon(lanIcon);

        robotTime = new CountDownCircle();
        robotTime.setVisible(false);

        yellowCard = new Button("Yellow");
        yellowCard.addActionListener(new CardIncrease(side, this.id, Color.YELLOW));

        redCard = new Button("Red");
        redCard.addActionListener(new CardIncrease(side, this.id, Color.RED));

        robotLabel.setVisible(true);

        robot.setVisible(true);

        updateLayout(1);

        this.setLayout(new TotalScaleLayout(this));
        ((TotalScaleLayout) this.getLayout()).add(0, 0, 1, 1, robot);
        this.setVisible(true);
    }

    private void updateRobotOnlineStatus(){
        int sideValue = this.side.value();
        int j = this.id;

        //** Online state **/
        RobotOnlineStatus[][] onlineStatus = RobotWatcher.updateRobotOnlineStatus();
        ImageIcon currentLanIcon;
        if (onlineStatus[sideValue][j] == RobotOnlineStatus.ONLINE) {
            currentLanIcon = lanOnline;
        } else if (onlineStatus[sideValue][j] == RobotOnlineStatus.HIGH_LATENCY) {
            currentLanIcon = lanHighLatency;
        } else if (onlineStatus[sideValue][j] == RobotOnlineStatus.OFFLINE) {
            currentLanIcon = lanOffline;
        } else {
            currentLanIcon = lanUnknown;
        }
        robotLabel.setIcon(currentLanIcon);

    }

    @Override
    public void update(AdvancedData data) {
        int sideValue = this.side.value();
        int robotId = this.id;
        PlayerInfo robotInfo = data.team[sideValue].player[robotId];

        // First of all we update the Online Status of the Robot
        updateRobotOnlineStatus();

        // then we update the yellow and red card buttons with the number of cards
        updatePenaltyCards(robotInfo);

        if (ActionBoard.robot[sideValue][robotId].isCoach(data)) {
            if (data.team[sideValue].coach.penalty == Penalties.SPL_COACH_MOTION) {
                robot.setEnabled(false);
                robotLabel.setText(LocalizationManager.getLocalization().EJECTED);
            } else {
                robotLabel.setText(data.team[sideValue].teamColor + " " + LocalizationManager.getLocalization().COACH);
            }
        } else {
            if (robotInfo.penalty != Penalties.NONE) {
                if (!data.ejected[sideValue][robotId]) {
                    int seconds = data.getRemainingPenaltyTime(sideValue, robotId);
                    boolean pickup = ((Rules.league instanceof SPL &&
                            robotInfo.penalty == Penalties.SPL_REQUEST_FOR_PICKUP)
                            || (Rules.league instanceof HL &&
                            (robotInfo.penalty == Penalties.HL_PICKUP_OR_INCAPABLE
                                    || robotInfo.penalty == Penalties.HL_SERVICE))
                    );
                    boolean illegalMotion = Rules.league instanceof SPL
                            && robotInfo.penalty == Penalties.SPL_ILLEGAL_MOTION_IN_SET;
                    if (seconds == 0) {
                        if (pickup) {
                            robotLabel.setText(data.team[sideValue].teamColor + " " + (robotId + 1) + " (" + Penalties.SPL_REQUEST_FOR_PICKUP.toString() + ")");
                            highlight(robot, true);
                        } else if (illegalMotion) {
                            robotLabel.setText(data.team[sideValue].teamColor + " " + (robotId + 1) + " (" + Penalties.SPL_ILLEGAL_MOTION_IN_SET.toString() + ")");
                            highlight(robot, true);
                        } else if (robotInfo.penalty == Penalties.SUBSTITUTE) {
                            robotLabel.setText(data.team[sideValue].teamColor + " " + (robotId + 1) + " (" + Penalties.SUBSTITUTE.toString() + ")");
                            highlight(robot, false);
                        } else if (!(Rules.league instanceof SPL) ||
                                !(robotInfo.penalty == Penalties.SPL_COACH_MOTION)) {
                            robotLabel.setText(data.team[sideValue].teamColor + " " + (robotId + 1) + ": " + Helper.formatTime(seconds));
                            highlight(robot, seconds <= UNPEN_HIGHLIGHT_SECONDS && robot.getBackground() != COLOR_HIGHLIGHT);
                        }
                    } else {
                        robotLabel.setText(data.team[sideValue].teamColor + " " + (robotId + 1) + ": " + Helper.formatTime(seconds) + (pickup ? " (P)" : ""));
                        highlight(robot, seconds <= UNPEN_HIGHLIGHT_SECONDS && robot.getBackground() != COLOR_HIGHLIGHT);
                    }
                    // Update the robot time component
                    int penTime = (seconds + data.getSecondsSince(data.whenPenalized[sideValue][robotId]));
                    double percent = 100.0 * seconds / (double) penTime;
                    robotTime.updateValue(seconds, percent);
                } else {
                    robotLabel.setText(LocalizationManager.getLocalization().EJECTED);
                    robotTime.setVisible(false);
                    highlight(robot, false);
                }
            } else {
                robotLabel.setText(data.team[sideValue].teamColor + " " + (robotId + 1));
                robotTime.setVisible(false);
                highlight(robot, false);
            }
        }
    }

    private void updatePenaltyCards(PlayerInfo playerInfo) {
        yellowCard.setText(String.valueOf(playerInfo.yellowCardCount));
        yellowCard.setMargin(new Insets(0, 0, 0, 0));
        yellowCard.setFont(FontHelper.boldStandardFont());

        redCard.setText(String.valueOf(playerInfo.redCardCount));
        redCard.setMargin(new Insets(0, 0, 0, 0));
        redCard.setFont(FontHelper.boldStandardFont());

        if (playerInfo.yellowCardCount > 0){
            yellowCard.setBackground(new Color(255, 255, 0));
        } else {
            yellowCard.setBackground(new Color(255, 251, 181));
        }

        if (playerInfo.redCardCount > 0){
            redCard.setBackground(new Color(255, 0, 0));
        } else {
            redCard.setBackground(new Color(255, 174, 171));
        }
    }


}
