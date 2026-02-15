# Dealio Trust Rating System – Technical Reference

## Overview

This document defines the Trust Rating model for Dealio.

The goal of this system is to ensure:

- Trust is **slow to build**
- Trust is **quick to damage**
- Trust is **hard to fake**
- Small sample sizes do NOT produce artificially high scores
- High-volume, consistent behavior is rewarded

All scores are normalized to a **0–100 scale**.

---

# 1. High-Level Model

The overall trust score is composed of:

- Review Score (35%)
- Transaction Score (30%)
- Verification Score (20%)
- Profile Score (15%)
  overallScore =
  reviewScore * 0.35 +
  transactionScore * 0.30 +
  verificationScore * 0.20 +
  profileScore * 0.15

All scores must:
- Use BigDecimal
- Be scaled to 2 decimal places
- Use RoundingMode.HALF_UP

---

# 2. Review Score (Bayesian Weighted)

## Problem
Raw averages are misleading:
- 1 review of 5★ should NOT equal 100% trust
- 20×5★ + 1×1★ should NOT collapse reputation

## Solution: Bayesian Average
weightedRating = (v / (v + m)) * R + (m / (v + m)) * C

Where:

- R = user's average rating
- v = total number of reviews
- m = minimum review threshold (use 10)
- C = global platform average rating (e.g. 4.2)

### Convert to 0–100 scale:
reviewScore = (weightedRating / 5.0) * 100
### Key Effects

- 1 review → pulled toward global average
- 20+ reviews → reflects real behavior
- Large review counts dominate the prior

---

# 3. Transaction Score

Transactions represent real-world execution.

## Step 1 – Completion Rate
completionRate = successfulTransactions / totalTransactions
completionScore = completionRate * 60
Max: 60 points

---

## Step 2 – Volume Boost (Log Scaled)
volumeScore = min(log10(totalTransactions + 1) * 15, 15)
Max: 15 points

### Why log scale?

Because:
- 1 → 10 transactions = major difference
- 100 → 110 = small difference

Prevents inflation from spam transactions.

---

## Final Transaction Score
Cap at 100.

---

# 4. Profile Score (Completeness)

Profile score encourages users to build legitimate accounts.

## Scoring Rules

| Feature | Points |
|----------|--------|
| Profile picture | +15 |
| Bio present | +10 |
| Email verified | +15 |
| Phone verified | +20 |
| Location present | +10 |
| Account age > 3 months | +10 |
| Account age > 1 year | +20 |

Cap at 100.

---

# 5. Verification Score

Strong but not overpowering.

| Verification | Score |
|--------------|--------|
| ID verified | 70 |
| Otherwise | 0 |

Optional future expansion:
- Face match
- Address verification
- Bank verification

---

# 6. Edge Case Behavior

### Brand new user
- No reviews
- No transactions
- No verification

→ VERY_POOR trust (realistic)

---

### 1 review (5★)
Bayesian formula pulls toward global average.
→ Moderate score, not 100%.

---

### 20 great reviews + 1 bad review
Minimal drop in score.
→ Stability against review bombing.

---

### High transaction volume but few reviews
Transaction score still builds trust.

---

# 7. Trust Level Mapping
90+ → EXCELLENT
80–89 → VERY_GOOD
70–79 → GOOD
60–69 → FAIR
50–59 → POOR
<50 → VERY_POOR

---

# 8. Implementation Rules

## DO NOT:
- Calculate trust inside the entity
- Use raw averages
- Allow easy max trust
- Allow trust to reach 95+ too easily

## DO:
- Centralize logic in TrustRatingService
- Use aggregation queries (NOT loading full lists)
- Recalculate after:
    - Review creation
    - Transaction completion
    - Verification changes
- Optionally run nightly recalculation job

---

# 9. Service Responsibilities

TrustRatingService should:

1. Fetch:
    - Review stats (avg + count)
    - Transaction stats
    - User profile state
    - Verification state

2. Calculate:
    - Bayesian review score
    - Transaction score
    - Profile score
    - Verification score
    - Weighted overall score

3. Persist updated TrustRating

---

# 10. Future Enhancements (Phase 2)

Not required for MVP, but supported by current design:

- Time decay for reviews
- Dispute penalties
- Fraud flags
- Weight configuration stored in DB
- Trust badges (TOP_SELLER, VERIFIED_PRO, etc.)
- Separate buyer vs seller trust

---

# 11. Design Philosophy

Trust should be:

- Slow to build
- Fast to damage
- Resistant to manipulation
- Based on behavior over time
- Transparent but not easily gameable

---
