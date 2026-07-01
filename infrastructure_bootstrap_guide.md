# Infrastructure Bootstrap & GitOps Setup Guide (From Zero to Production)

Ce guide détaille la chaîne de commandes complète pour initialiser et déployer l'intégralité de l'infrastructure de la plateforme TélécomBilling, depuis la configuration AWS/kubeconfig initiale jusqu'à la synchronisation finale via ArgoCD (GitOps).

---

## 📋 Prérequis Locaux

Avant de commencer, assurez-vous d'avoir installé les outils CLI requis sur votre terminal :
1. **AWS CLI v2** : Configuré avec les accès appropriés (`aws configure`).
2. **eksctl** : Le CLI officiel pour administrer EKS.
3. **kubectl** : Pour interagir avec le cluster Kubernetes.
4. **Helm v3** : Pour d'éventuels charts locaux.

---

## 🏗️ Étape 1 : Création du Cluster AWS EKS

Nous utilisons `eksctl` pour créer un cluster managé sur AWS EKS avec des nœuds de type **Spot** pour optimiser les coûts.

```bash
# Initialiser le cluster EKS avec 2 nœuds de travail (worker nodes) standard
eksctl create cluster \
  --name billing-cluster \
  --region us-east-1 \
  --nodegroup-name standard-nodes \
  --node-type t3.medium \
  --nodes 2 \
  --nodes-min 1 \
  --nodes-max 3 \
  --managed \
  --spot
```

> [!NOTE]
> Cette opération prend environ **15 à 20 minutes**. Elle orchestre la création du VPC AWS, des sous-réseaux, des groupes de sécurité, du plan de contrôle EKS et des nœuds EC2 autoscalés.

---

## 🔧 Étape 2 : Configuration du Contexte local kubectl

Une fois le cluster créé, nous devons lier notre environnement local au cluster EKS.

```bash
# Générer ou mettre à jour la configuration kubeconfig locale
aws eks update-kubeconfig --region us-east-1 --name billing-cluster

# Vérifier la connexion avec le cluster et l'état des nœuds
kubectl get nodes
```

Créer les namespaces nécessaires pour l'application et les outils compagnons :
```bash
# Namespace pour la production de nos microservices
kubectl create namespace billing-prod
```

---

## 🛡️ Étape 3 : Déploiement des Composants Système (Ingress & Sealed Secrets)

### 3.1 Nginx Ingress Controller
L'Ingress Controller gère le routage du trafic HTTP entrant depuis l'extérieur vers nos pod correspondants:

```bash
# Déployer Ingress Nginx pour AWS (il va créer un Network Load Balancer L4 automatique)
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.10.1/deploy/static/provider/aws/deploy.yaml

# Attendre que l'adresse DNS externe du NLB AWS soit assignée
kubectl get svc -n ingress-nginx -w
```

### 3.2 Sealed Secrets Controller (Bitnami)
Pour stocker nos secrets de production (comme `DB_PASSWORD` et `JWT_SECRET`) de manière sécurisée et versionnée sous Git :

```bash
# Installer le contrôleur Sealed Secrets
kubectl apply -f https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.24.5/controller.yaml

# Re-chiffrer les secrets locaux si nécessaire avec la nouvelle clé du cluster
kubeseal --fetch-cert > new-pub-cert.pem

kubectl create secret generic billing-secret \
  --namespace billing-prod \
  --from-literal=DB_PASSWORD=votre_mot_de_passe_rds \
  --from-literal=JWT_SECRET=votre_cle_secrete_jwt \
  --dry-run=client -o yaml | \
  kubeseal --cert new-pub-cert.pem -o yaml > sealedsecret.yaml

# Appliquer le SealedSecret chiffré
kubectl apply -f sealedsecret.yaml
```

---

## 🐙 Étape 4 : Déploiement et Configuration d'ArgoCD

ArgoCD est notre moteur GitOps. Il écoute en continu notre dépôt GitHub de configuration (`billing-app-config`) et reconcile l'état du cluster Kubernetes.

### 4.1 Installation d'ArgoCD
```bash
# Créer le namespace et appliquer les manifests officiels
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Attendre que tous les pods ArgoCD soient opérationnels
kubectl wait --namespace argocd \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/name=argocd-server \
  --timeout=90s
```

### 4.2 Récupération du Mot de passe Administrateur
```bash
# Décoder le mot de passe initial généré par ArgoCD
kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath="{.data.password}" | base64 -d; echo
```

### 4.3 Accès à la console Web
Pour accéder à la console d'ArgoCD depuis votre machine locale :
```bash
# Faire un port-forward local sur le port 8443
kubectl port-forward svc/argocd-server -n argocd 8443:443
```
*Naviguez sur `https://localhost:8443` avec pour identifiant `admin` et le mot de passe récupéré.*

### 4.4 Enregistrement des Applications ArgoCD (Bootstrap GitOps)
Nous déclarons à ArgoCD nos applications déclarées dans le dépôt `billing-app-config` :

```bash
# Déployer l'application d'infrastructure (Kafka, ConfigMaps, Ingress, ServiceMonitor)
kubectl apply -f https://raw.githubusercontent.com/ghassenchouchen/billing-app-config/main/argocd/infra-app.yaml

# Déployer l'ApplicationSet des microservices Backend (auto-détection des services)
kubectl apply -f https://raw.githubusercontent.com/ghassenchouchen/billing-app-config/main/argocd/backend-appSet.yaml

# Déployer le microservice Frontend
kubectl apply -f https://raw.githubusercontent.com/ghassenchouchen/billing-app-config/main/argocd/frontend-app.yaml
```

---

## 🛢️ Étape 5 : Initialisation et Seeding de la Base de Données

Les schémas des tables SQL sont créés automatiquement par Spring Boot (`ddl-auto: update`), mais le jeu de données pour la démonstration doit être inséré manuellement dans le serveur de base de données **AWS RDS** (`billing-db.cajugww62s1l.us-east-1.rds.amazonaws.com`).

```bash
# Exécuter un conteneur MySQL temporaire dans le cluster et y injecter notre script SQL de seeding
kubectl run mysql-seed --rm -it --restart=Never \
  --namespace billing-prod \
  --image=mysql:8.0 -- \
  bash -c "mysql -h billing-db.cajugww62s1l.us-east-1.rds.amazonaws.com -u root -p < /dev/stdin" < spring-backend/seed-data.sql
```

---

## 🔍 Étape 6 : Vérification Finale du Cluster

Consulter le statut global de tous les pods pour s'assurer du fonctionnement correct :

```bash
kubectl get pods -n billing-prod
```

### 🌍 Accéder à la plateforme de Billing
Récupérez l'URL publique de l'Ingress pour vous y connecter :
```bash
kubectl get svc -n ingress-nginx
# Copiez le DNS situé dans la colonne "EXTERNAL-IP" (ex: abc-123.us-east-1.elb.amazonaws.com)
```
*Ouvrez votre navigateur et accédez à cette URL externe pour interagir avec le portail client de TélécomBilling !*
