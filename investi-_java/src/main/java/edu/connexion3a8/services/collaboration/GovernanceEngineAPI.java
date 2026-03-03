package edu.connexion3a8.services.collaboration;

import edu.connexion3a8.entities.collaboration.Investment;
import edu.connexion3a8.entities.collaboration.Project;

public class GovernanceEngineAPI {

    /**
     * Calculates the Compatibility Index before an offer is accepted.
     * Evaluates if the investor and entrepreneur match.
     */
    public static double calculateCompatibilityIndex(Investment offer, Project project) {
        double score = 50.0; // Start neutral

        // Guard against invalid data
        if (offer.getEquityRequested() <= 0 || project.getEquityOffered() <= 0
                || offer.getTotalAmount() <= 0 || project.getAmountRequested() <= 0) {
            return 50.0;
        }

        // Rule 1: Valuation comparison
        double impliedValuation = offer.getTotalAmount() / (offer.getEquityRequested() / 100.0);
        double targetValuation = project.getAmountRequested() / (project.getEquityOffered() / 100.0);

        double valuationRatio = impliedValuation / targetValuation;
        if (valuationRatio >= 1.0) {
            score += 25;
        } else if (valuationRatio >= 0.75) {
            score += 10;
        } else if (valuationRatio < 0.5) {
            score -= 25;
        } else {
            score -= 10;
        }

        // Rule 2: Amount coverage
        double coverageRatio = offer.getTotalAmount() / project.getAmountRequested();
        if (coverageRatio >= 1.0) {
            score += 20;
        } else if (coverageRatio >= 0.75) {
            score += 10;
        } else if (coverageRatio < 0.4) {
            score -= 15;
        }

        // Rule 3: Equity reasonableness
        if (offer.getEquityRequested() > 50) {
            score -= 25;
        } else if (offer.getEquityRequested() > 30) {
            score -= 10;
        } else if (offer.getEquityRequested() <= 15) {
            score += 10;
        }

        // Rule 4: Equity gap check — large difference between investor ask and project
        // offer
        double equityGap = Math.abs(offer.getEquityRequested() - project.getEquityOffered());
        if (equityGap > 30) {
            score -= 15;
        } else if (equityGap > 15) {
            score -= 5;
        }

        return Math.max(0, Math.min(100, score));
    }

    /**
     * Calculates the overall health score of an ACTIVE collaboration.
     * Formula: 0.6 * Payment Discipline + 0.4 * Progress Consistency
     */
    public static double calculateHealthScore(Investment activeInvestment) {
        if (activeInvestment.getDurationMonths() == 0)
            return 100.0;

        double expectedMonthsPaid = calculateExpectedMonthsPaid(activeInvestment);
        double actualMonthsPaid = activeInvestment.getPaymentMonthsCompleted();

        double paymentScore = 100.0;
        if (expectedMonthsPaid > 0) {
            double ratio = actualMonthsPaid / expectedMonthsPaid;
            paymentScore = Math.min(100.0, ratio * 100.0);
        }

        double progressScore = activeInvestment.getProgressPercentage();

        double healthScore = (0.6 * paymentScore) + (0.4 * progressScore);
        return Math.max(0, Math.min(100, healthScore));
    }

    /**
     * Generates a "Temperature" visual state based on Health Score.
     */
    public static String getTemperature(double healthScore) {
        if (healthScore >= 75)
            return "HOT";
        if (healthScore >= 40)
            return "WARM";
        return "COLD";
    }

    /**
     * Predicts the probability of default based on late payments or stagnation.
     */
    public static double calculateDefaultProbability(Investment activeInvestment) {
        double expectedMonthsPaid = calculateExpectedMonthsPaid(activeInvestment);
        double actualMonthsPaid = activeInvestment.getPaymentMonthsCompleted();

        double missedPayments = Math.max(0, expectedMonthsPaid - actualMonthsPaid);

        // Base Risk: 5% floor + 0.1% per month (capped at 15%) + 0.1% per equity point
        // (capped at 10%)
        double durationRisk = Math.min(15.0, activeInvestment.getDurationMonths() * 0.1);
        double equityRisk = Math.min(10.0, activeInvestment.getEquityRequested() * 0.1);
        double prob = 5.0 + durationRisk + equityRisk;

        // Payment discipline penalty
        if (missedPayments >= 1)
            prob += 20.0;
        if (missedPayments >= 2)
            prob += 25.0;
        if (missedPayments >= 3)
            prob += 30.0;

        // Progress stagnation penalty – only if substantial time has elapsed
        if (activeInvestment.getProgressPercentage() == 0 && expectedMonthsPaid >= 2) {
            prob += 15.0;
        }

        return Math.min(100.0, prob);
    }

    /**
     * Helper to estimate how many months should have been paid by now.
     * Calculated strictly from the investmentDate.
     */
    private static double calculateExpectedMonthsPaid(Investment inv) {
        java.util.Date referenceDate = inv.getInvestmentDate();
        if (referenceDate == null) {
            return 0.0;
        }

        long daysSinceRef = (System.currentTimeMillis() - referenceDate.getTime()) / (1000 * 60 * 60 * 24);
        double expected = daysSinceRef / 30.0;
        return Math.min(expected, inv.getDurationMonths());
    }

    // ─── Adaptive Deal Balance Engine (Fairness Drift) ─────────────────────────

    public static class FairnessReport {
        public double idealEquity;
        public double deviation;
        public double fairnessScore;
        public String status;
    }

    /**
     * Evaluates if the current equity still reflects the true economic reality.
     * Also flags an equity gap if deviation from the ideal is too large.
     */
    public static FairnessReport evaluateFairnessDrift(Investment activeInvestment) {
        FairnessReport report = new FairnessReport();

        // 1. Calculate inputs
        double riskScore = calculateDefaultProbability(activeInvestment) / 100.0;
        double progressPercentage = activeInvestment.getProgressPercentage() / 100.0;

        double expectedMonthsPaid = calculateExpectedMonthsPaid(activeInvestment);
        double actualMonthsPaid = activeInvestment.getPaymentMonthsCompleted();
        double paymentDiscipline = expectedMonthsPaid > 0 ? Math.min(1.0, actualMonthsPaid / expectedMonthsPaid) : 1.0;

        // 2. Identify Performance Lags
        double duration = activeInvestment.getDurationMonths();
        double timeProgress = duration > 0 ? Math.min(1.0, expectedMonthsPaid / duration) : 1.0;

        // entrepreneurLag: if time progresses faster than actual project completion %
        double entrepreneurLag = timeProgress > 0 ? Math.max(0, timeProgress - progressPercentage) : 0.0;

        // investorLag: if payment discipline is less than 1.0
        double investorLag = 1.0 - paymentDiscipline;

        // Drift Factor: positive favors Investor (needs more equity), negative favors
        // Entrepreneur
        double driftFactor = entrepreneurLag - investorLag;

        // Multiplier: Anchor is 1.0. Max theoretical drift impact is roughly +/- 50%
        double multiplier = 1.0 + (driftFactor * 0.5) + (riskScore * 0.2);

        double baseEquity = activeInvestment.getEquityRequested();
        report.idealEquity = Math.max(0, Math.min(100.0, baseEquity * multiplier));

        // 3. Equity Deviation
        report.deviation = Math.abs(baseEquity - report.idealEquity);

        // 4. Fairness Score: 100 - (deviationRatio * 150)
        double deviationRatio = baseEquity > 0 ? (report.deviation / baseEquity) : 0;
        double score = 100.0 - (deviationRatio * 150.0);
        report.fairnessScore = Math.max(0, Math.min(100.0, score));

        // 5. Status Thresholds
        if (report.fairnessScore >= 80) {
            report.status = "BALANCED";
        } else if (report.fairnessScore >= 60) {
            report.status = "DRIFT_WARNING";
        } else {
            report.status = "UNBALANCED";
        }

        return report;
    }

    // ─── Advanced Financial Intelligence ─────────────────────────────────────

    /**
     * Calculates estimated Monthly Burn Rate = total capital / duration.
     */
    public static double calculateBurnRate(double requestedCapital, int proposedDuration) {
        if (proposedDuration <= 0 || requestedCapital <= 0)
            return 0.0;
        return requestedCapital / proposedDuration;
    }

    /**
     * Calculates the Runway (in months) based on remaining capital vs burn rate.
     * Returns 999.0 if burn rate is zero (effectively infinite runway).
     */
    public static double calculateRunwayMonths(double totalInjectedCapital, double estimatedBurnRate) {
        if (estimatedBurnRate <= 0)
            return 999.0;
        return totalInjectedCapital / estimatedBurnRate;
    }

    /**
     * Calculates remaining capital (total investment minus what's already paid
     * out).
     */
    public static double calculateRemainingCapital(Investment inv) {
        int monthsPaid = inv.getPaymentMonthsCompleted();
        double amountPerPeriod = inv.getAmountPerPeriod();
        if (amountPerPeriod <= 0 && inv.getDurationMonths() > 0) {
            amountPerPeriod = inv.getTotalAmount() / inv.getDurationMonths();
        }
        double alreadyPaid = monthsPaid * amountPerPeriod;
        return Math.max(0.0, inv.getTotalAmount() - alreadyPaid);
    }

    /**
     * Calculates Capital Velocity Score (0-100) combining funding momentum and
     * progress execution speed.
     */
    public static double calculateCapitalVelocity(Investment offer, Project project) {
        if (offer == null || project == null)
            return 50.0;

        double duration = offer.getDurationMonths() > 0 ? offer.getDurationMonths() : 1.0;
        double actualProgress = offer.getProgressPercentage();
        double expectedProgress = Math.min(1.0, calculateExpectedMonthsPaid(offer) / duration) * 100.0;

        // Execution ratio: How ahead/behind schedule (capped at 1.5 to avoid extremes)
        double executionRatio = expectedProgress > 0 ? Math.min(1.5, actualProgress / expectedProgress) : 1.0;

        // Payment discipline component
        double expectedMonthsPaid = calculateExpectedMonthsPaid(offer);
        double actualMonthsPaid = offer.getPaymentMonthsCompleted();
        double paymentRatio = expectedMonthsPaid > 0 ? Math.min(1.0, actualMonthsPaid / expectedMonthsPaid) : 1.0;

        // Weighted: 60% execution, 40% payment discipline
        double velocityScore = (executionRatio * 60.0) + (paymentRatio * 40.0);
        return Math.max(0, Math.min(100, velocityScore));
    }

    /**
     * Calculates the Investor's Reputation Score based on their history of
     * investments.
     */
    public static double calculateInvestorReputation(java.util.List<Investment> investorHistory) {
        if (investorHistory == null || investorHistory.isEmpty()) {
            return 50.0;
        }

        double totalDisciplineScore = 0;
        int activeCount = 0;
        int completedCount = 0;
        int pendingCount = 0;
        int breachedCount = 0;

        for (Investment inv : investorHistory) {
            if ("ACCEPTED".equalsIgnoreCase(inv.getStatus())) {
                double expected = calculateExpectedMonthsPaid(inv);
                double actual = inv.getPaymentMonthsCompleted();
                if (expected > 0) {
                    double discipline = Math.min(1.0, actual / expected);
                    totalDisciplineScore += discipline * 100.0;
                    activeCount++;
                    if (actual < expected - 2) {
                        breachedCount++;
                    }
                } else {
                    totalDisciplineScore += 100.0;
                    activeCount++;
                }
            } else if ("COMPLETED".equalsIgnoreCase(inv.getStatus()) || "CLOSED".equalsIgnoreCase(inv.getStatus())) {
                completedCount++;
            } else if ("PENDING".equalsIgnoreCase(inv.getStatus())) {
                pendingCount++;
            }
        }

        double baseScore = 50.0;
        baseScore += Math.min(30.0, completedCount * 8.0);
        baseScore += Math.min(10.0, pendingCount * 2.0);

        if (activeCount > 0) {
            double avgDiscipline = totalDisciplineScore / activeCount;
            baseScore += ((avgDiscipline - 50.0) / 50.0) * 25.0;
        }

        baseScore -= (breachedCount * 15.0);

        return Math.max(0, Math.min(100, baseScore));
    }
}
