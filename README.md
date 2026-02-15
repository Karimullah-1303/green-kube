# 🍃 Green Kube: Enterprise Kubernetes Resource Optimizer

![Java](https://img.shields.io/badge/Java-Spring_Boot-green?style=for-the-badge&logo=spring)
![Kubernetes](https://img.shields.io/badge/Kubernetes-Operator-blue?style=for-the-badge&logo=kubernetes)
![Terraform](https://img.shields.io/badge/Terraform-IaC-623CE4?style=for-the-badge&logo=terraform)
![Prometheus](https://img.shields.io/badge/Prometheus-Metrics-e6522c?style=for-the-badge&logo=prometheus)
![Grafana](https://img.shields.io/badge/Grafana-Dashboards-orange?style=for-the-badge&logo=grafana)
![ArgoCD](https://img.shields.io/badge/ArgoCD-GitOps-lightgrey?style=for-the-badge&logo=argo)
![Helm](https://img.shields.io/badge/Helm-Charts-0F1689?style=for-the-badge&logo=helm)

**Green Kube** is a highly available, custom Kubernetes Operator built in Java (Spring Boot) designed to detect, analyze, and alert on cloud resource waste. Fully bootstrapped with Terraform and deployed via ArgoCD, it actively monitors cluster workloads, comparing strict Kyverno security limits against live Prometheus usage metrics, and fires real-time Slack alerts when pods hoard memory or CPU they don't need.

## 🌟 The Problem it Solves
In enterprise Kubernetes clusters, developers often "over-provision" their pods with massive resource limits just to be safe. This creates a massive gap between *allocated* resources and *actual* usage—costing companies thousands of dollars in wasted cloud infrastructure. Green Kube automates the detection of this "Waste Zone."

## 🏗️ Advanced Architecture & Tech Stack

![Green Kube Architechture](images/green_kube_architechture.png)

This project goes beyond standard CRUD apps, implementing a true Enterprise GitOps workflow and robust Operator SDK patterns:

* **Infrastructure as Code (Terraform):** The foundational cluster resources and initial GitOps bootstrap are provisioned declaratively via Terraform.
* **Continuous Deployment (ArgoCD & Helm):** True GitOps methodology. ArgoCD constantly monitors the GitHub repository and automatically syncs the Helm charts to the cluster with zero-downtime rolling updates.
* **The Engine (Java/Spring Boot Operator):**
  * **Shared Informers:** Efficiently caches the Kubernetes API state locally to monitor Deployment changes without overwhelming the control plane.
  * **Blocking Queues:** Decouples event ingestion from the processing logic, ensuring thread-safe, high-throughput waste analysis.
  * **Leader Election:** Enables High Availability (HA). Multiple replicas of Green Kube can be deployed, but only the elected leader will process events and fire alerts, preventing split-brain scenarios and duplicate Slack messages.
* **Security (Kyverno):** Enforces strict memory/CPU limit declarations on all new deployments via Mutating/Validating Webhooks.
* **Observability (Prometheus & Grafana):** Scrapes live container metrics and visualizes the exact resource waste metrics.



## 📊 Visualizing the Waste

Green Kube provides a "Single Pane of Glass" to view cluster efficiency. The custom dashboard highlights the exact gap between the hard limits (dashed ceilings) and live usage (solid lines), while monitoring CPU thresholds and pod stability in real-time.

![Green Kube Grafana Dashboard](images/dashboard.png)
> *The dashboard successfully tracking a memory-monster pod alongside highly optimized baseline workloads.*

## 🚀 Getting Started

### Prerequisites
* Minikube (or any K8s 1.19+ cluster)
* `terraform`, `kubectl`, and `helm` installed
* ArgoCD running on the cluster

### Installation

**1. Clone the repository**
```bash
git clone https://github.com/YOUR_USERNAME/green-kube.git
cd green-kube
```

**2. Provision Base Infrastructure via Terraform**
```bash
cd terraform
terraform init
terraform apply -auto-approve
cd ..
```

**3. Add your Slack Webhook Secret**
```bash
kubectl create secret generic slack-secrets \
  --from-literal=webhook-url="YOUR_SLACK_WEBHOOK_URL"
```

**4. Deploy via ArgoCD (GitOps)**
```bash
kubectl apply -f argocd/application.yaml
```
ArgoCD will now take over, syncing the Helm charts and spinning up the highly available Green Kube replicas.


### 🧪 Chaos Injection Testing
To prove the engine works under load, deploy the included memory-monster stress test. This pod intentionally spikes its RAM usage to trigger the Java blocking queue analyzer.
```bash
kubectl apply -f tests/memory-monster.yaml
```
Check your Slack channel! The elected Green Kube leader will instantly detect the baseline pods that are wasting resources compared to the monster.



### 👨‍💻 About the Developer
I am a 3rd-year CSE Core student based in Visakhapatnam with a deep, sustained focus on DevOps, Cloud Computing, and Software Engineering. I built Green Kube to tackle real-world distributed systems challenges and master production-grade infrastructure automation.

I am actively looking for a DevOps or Software Engineering Internship! Let's connect:

LinkedIn

Built with ❤️, Java, and a whole lot of YAML.


