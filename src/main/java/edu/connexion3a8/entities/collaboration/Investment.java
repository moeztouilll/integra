package edu.connexion3a8.entities.collaboration;

import java.util.Date;
import java.util.Objects;

public class Investment {

    private int investmentId;
    private int projectId;
    private String investorId; // Changed from int to String (UUID)
    private double totalAmount;
    private int durationMonths;
    private double amountPerPeriod;
    private double equityRequested;
    private String status;
    private Date investmentDate;

    // Collaboration & Progress Tracking Fields (Idea 5)
    private int progressPercentage; // 0 to 100
    private String latestProgressLog; // What the entrepreneur did lately
    private int paymentMonthsCompleted; // Investor payments made so far
    private Date lastPaymentDate; // Automatically map SQL Timestamp to Date

    public Investment() {
    }

    public Investment(int projectId, String investorId, double totalAmount,
            int durationMonths, double amountPerPeriod,
            double equityRequested, String status) {
        this.projectId = projectId;
        this.investorId = investorId;
        this.totalAmount = totalAmount;
        this.durationMonths = durationMonths;
        this.amountPerPeriod = amountPerPeriod;
        this.equityRequested = equityRequested;
        this.status = status;
    }

    public int getInvestmentId() {
        return investmentId;
    }

    public void setInvestmentId(int investmentId) {
        this.investmentId = investmentId;
    }

    public int getProjectId() {
        return projectId;
    }

    public String getInvestorId() {
        return investorId;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public int getDurationMonths() {
        return durationMonths;
    }

    public double getAmountPerPeriod() {
        return amountPerPeriod;
    }

    public double getEquityRequested() {
        return equityRequested;
    }

    public String getStatus() {
        return status;
    }

    public Date getInvestmentDate() {
        return investmentDate;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public void setInvestorId(String investorId) {
        this.investorId = investorId;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setDurationMonths(int durationMonths) {
        this.durationMonths = durationMonths;
    }

    public void setAmountPerPeriod(double amountPerPeriod) {
        this.amountPerPeriod = amountPerPeriod;
    }

    public void setEquityRequested(double equityRequested) {
        this.equityRequested = equityRequested;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setInvestmentDate(Date investmentDate) {
        this.investmentDate = investmentDate;
    }

    // Getters and Setters for Progress Tracking
    public int getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(int progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public String getLatestProgressLog() {
        return latestProgressLog;
    }

    public void setLatestProgressLog(String latestProgressLog) {
        this.latestProgressLog = latestProgressLog;
    }

    public int getPaymentMonthsCompleted() {
        return paymentMonthsCompleted;
    }

    public void setPaymentMonthsCompleted(int paymentMonthsCompleted) {
        this.paymentMonthsCompleted = paymentMonthsCompleted;
    }

    public Date getLastPaymentDate() {
        return lastPaymentDate;
    }

    public void setLastPaymentDate(Date lastPaymentDate) {
        this.lastPaymentDate = lastPaymentDate;
    }

    @Override
    public String toString() {
        return "Investment{" +
                "investmentId=" + investmentId +
                ", projectId=" + projectId +
                ", investorId=" + investorId +
                ", totalAmount=" + totalAmount +
                ", durationMonths=" + durationMonths +
                ", equityRequested=" + equityRequested +
                ", status='" + status + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Investment))
            return false;
        Investment that = (Investment) o;
        return investmentId == that.investmentId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(investmentId);
    }
}
