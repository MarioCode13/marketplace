package dev.marketplace.marketplace.service.dto;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * DTO carrying the component scores and final overall trust score.
 * All scores are expected to be on a 0-100 scale, using BigDecimal with scale 2
 * and RoundingMode.HALF_UP applied by the calculator.
 */
public class TrustComponentsDTO {

    private BigDecimal reviewScore;
    private BigDecimal transactionScore;
    private BigDecimal verificationScore;
    private BigDecimal profileScore;
    private BigDecimal overallScore;

    public TrustComponentsDTO() {
    }

    public TrustComponentsDTO(BigDecimal reviewScore,
                              BigDecimal transactionScore,
                              BigDecimal verificationScore,
                              BigDecimal profileScore,
                              BigDecimal overallScore) {
        this.reviewScore = reviewScore;
        this.transactionScore = transactionScore;
        this.verificationScore = verificationScore;
        this.profileScore = profileScore;
        this.overallScore = overallScore;
    }

    public BigDecimal getReviewScore() {
        return reviewScore;
    }

    public void setReviewScore(BigDecimal reviewScore) {
        this.reviewScore = reviewScore;
    }

    public BigDecimal getTransactionScore() {
        return transactionScore;
    }

    public void setTransactionScore(BigDecimal transactionScore) {
        this.transactionScore = transactionScore;
    }

    public BigDecimal getVerificationScore() {
        return verificationScore;
    }

    public void setVerificationScore(BigDecimal verificationScore) {
        this.verificationScore = verificationScore;
    }

    public BigDecimal getProfileScore() {
        return profileScore;
    }

    public void setProfileScore(BigDecimal profileScore) {
        this.profileScore = profileScore;
    }

    public BigDecimal getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(BigDecimal overallScore) {
        this.overallScore = overallScore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrustComponentsDTO that = (TrustComponentsDTO) o;
        return Objects.equals(reviewScore, that.reviewScore) &&
                Objects.equals(transactionScore, that.transactionScore) &&
                Objects.equals(verificationScore, that.verificationScore) &&
                Objects.equals(profileScore, that.profileScore) &&
                Objects.equals(overallScore, that.overallScore);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reviewScore, transactionScore, verificationScore, profileScore, overallScore);
    }

    @Override
    public String toString() {
        return "TrustComponentsDTO{" +
                "reviewScore=" + reviewScore +
                ", transactionScore=" + transactionScore +
                ", verificationScore=" + verificationScore +
                ", profileScore=" + profileScore +
                ", overallScore=" + overallScore +
                '}';
    }
}

