
# Backlog du Sprint 2


# Objectif du Sprint

Mettre en place deux nouveaux microservices :

- **customer-service**
- **subscription-service**

Chaque service doit inclure :

- Modèle de domaine
- API REST
- Pipeline CI/CD avec Jenkins
- Analyse de qualité avec SonarQube
- Image Docker publiée sur Docker Hub
- Evènements Kafka publiés

Le **subscription-service** doit vérifier l'existence du client et de l'offre avant la création d'un abonnement.

---

# (Definition of Done)

Une User Story est considérée **Terminée** lorsque :

- Le code est  sur le dépôt Git
- Le build Maven réussit
- Le pipeline Jenkins s'exécute avec succès
- L’API REST est accessible et testée manuellement
- L’image Docker est disponible sur Docker Hub

---

# Backlog du Sprint

| ID | User Story | Points | Statut |
|----|----------------------|--------|--------|
| US-11 | En tant qu'agent commercial, je veux créer un nouveau client afin de pouvoir lui associer des abonnements | 5 | Terminé |
| US-12 | En tant qu’agent commercial, je veux consulter le profil d’un client afin de visualiser ses informations | 3 | Terminé |
| US-13 | En tant que responsable boutique, je veux mettre à jour les informations d’un client afin de maintenir les données à jour | 3 | À faire |
| US-14 | En tant qu’administrateur, je veux consulter l’historique des paiements d’un client afin d'assurer la traçabilité des transactions | 3 | À faire |
| US-15 | En tant qu’agent commercial, je veux créer un abonnement reliant un client à une offre afin de lui permettre d'utiliser les services télécom | 8 | Terminé |
| US-16 | En tant que responsable boutique, je veux consulter les abonnements d’un client afin d’avoir une vue globale de son compte | 3 | Terminé |
| US-17 | US-17: En tant qu’administrateur ou responsable boutique, je veux gérer le cycle de vie d’un abonnement (Réactiver, suspendre) afin de contrôler l’accès aux services. | 5 | À faire |
| US-18 | En tant qu’administrateur, je veux résilier un abonnement afin d'arrêter définitivement le service | 3 | En cours |
| US-19| En tant que responsable boutique, je veux consulter les détails d’un abonnement afin de visualiser son état et ses paramètres | 3 | À faire |
| US-20 | En tant qu'agent commercial, je veux définir la fréquence de facturation d’un abonnement choisi par le client afin d’adapter le cycle de facturation | 5 | Terminé |
| US-21 | En tant qu’administrateur, je veux consulter l'historique de facturation d'un client afin d'assurer la traçabilité | 5 | À faire



---
