package com.zsj.tactimind.match.model;

public class TeamStats {
    private int goals;
    private int shots;
    private int shotsOnTarget;
    private int yellowCards;
    private int corners;
    private int dangerousAttacks;
    private int possessionRate;

    public int getGoals() {
        return goals;
    }

    public void setGoals(int goals) {
        this.goals = goals;
    }

    public int getShots() {
        return shots;
    }

    public void setShots(int shots) {
        this.shots = shots;
    }

    public int getShotsOnTarget() {
        return shotsOnTarget;
    }

    public void setShotsOnTarget(int shotsOnTarget) {
        this.shotsOnTarget = shotsOnTarget;
    }

    public int getYellowCards() {
        return yellowCards;
    }

    public void setYellowCards(int yellowCards) {
        this.yellowCards = yellowCards;
    }

    public int getCorners() {
        return corners;
    }

    public void setCorners(int corners) {
        this.corners = corners;
    }

    public int getDangerousAttacks() {
        return dangerousAttacks;
    }

    public void setDangerousAttacks(int dangerousAttacks) {
        this.dangerousAttacks = dangerousAttacks;
    }

    public int getPossessionRate() {
        return possessionRate;
    }

    public void setPossessionRate(int possessionRate) {
        this.possessionRate = possessionRate;
    }
}
