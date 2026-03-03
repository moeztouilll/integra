package edu.connexion3a8.services.collaboration;

import edu.connexion3a8.entities.collaboration.Project;

/**
 * Advanced Business Logic API (API métier avancée)
 * Evaluates the risk and potential of a project based on quantitative metrics.
 */
public class ProjectRiskAPI {

    public enum RiskLevel {
        LOW_RISK, MODERATE_RISK, HIGH_RISK, EXTREME_RISK
    }

    public static class RiskReport {
        public RiskLevel level;
        public double healthScore; // 0 to 100
        public String advice;

        public RiskReport(RiskLevel level, double healthScore, String advice) {
            this.level = level;
            this.healthScore = healthScore;
            this.advice = advice;
        }
    }

    /**
     * Algorithm to assess risk purely based on mathematical heuristics.
     */
    public static RiskReport calculateRiskScore(Project p) {
        if (p == null)
            return new RiskReport(RiskLevel.EXTREME_RISK, 0, "Invalid project.");

        double requested = p.getAmountRequested();
        double equity = p.getEquityOffered();

        if (requested <= 0 || equity <= 0) {
            return new RiskReport(RiskLevel.EXTREME_RISK, 0, "Suspicious zero-values.");
        }

        // Calculate implicit valuation: V = Amount / Equity
        // e.g., $100,000 for 10% -> $1,000,000 Valuation
        double impliedValuation = requested / (equity / 100.0);

        double score = 100.0;
        RiskLevel rLevel = RiskLevel.MODERATE_RISK;
        String rec = "";

        if (impliedValuation > 10_000_000) {
            score -= 30; // Very high valuation for a platform startup
            rLevel = RiskLevel.HIGH_RISK;
            rec = "High valuation. Confirm significant traction.";
        } else if (impliedValuation < 100_000) {
            score -= 10;
            rLevel = RiskLevel.MODERATE_RISK;
            rec = "Low valuation. Ensure the business model is scalable.";
        } else {
            rLevel = RiskLevel.LOW_RISK;
            rec = "Balanced valuation.";
        }

        if (equity > 50) {
            score -= 20;
            rec += " Founder giving up majority control is a red flag.";
            if (rLevel == RiskLevel.LOW_RISK)
                rLevel = RiskLevel.MODERATE_RISK;
        }

        if (score < 60)
            rLevel = RiskLevel.HIGH_RISK;

        return new RiskReport(rLevel, Math.max(0, score), rec);
    }
}
