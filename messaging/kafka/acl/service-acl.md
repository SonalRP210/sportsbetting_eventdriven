# Kafka topic ACL policy (service-account placeholder)
# Replace principals with your real auth mechanism.

principals:
  - name: svc-betting
    allow:
      produce: ["betting.bet.placed.v1", "betting.bet.cancelled.v1"]
      consume: ["odds.odds.updated.v1", "wallet.wallet.credited.v1"]

  - name: svc-odds
    allow:
      produce: ["odds.odds.updated.v1"]

  - name: svc-settlement
    allow:
      produce: ["settlement.event.settled.v1"]
      consume: ["betting.bet.placed.v1"]

  - name: svc-wallet
    allow:
      produce: ["wallet.wallet.credited.v1", "wallet.wallet.debited.v1"]
      consume: ["settlement.event.settled.v1"]

  - name: svc-risk
    allow:
      produce: ["risk.exposure.updated.v1"]
      consume: ["betting.bet.placed.v1", "betting.bet.cancelled.v1", "settlement.event.settled.v1"]

  - name: svc-notification
    allow:
      consume: ["betting.bet.placed.v1", "betting.bet.cancelled.v1", "wallet.wallet.credited.v1", "settlement.event.settled.v1"]
      produce: ["notification.events.v1"]
