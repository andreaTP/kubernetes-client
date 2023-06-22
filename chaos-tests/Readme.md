# Chaos Mesh tests for Kubernetes Client SharedInformer

This module will run automated Chaos Tests for the SharedInformers

### Setup

Start minikube, e.g.:

```bash
minikube start --driver=docker --memory 8192 --cpus 3
```

Install ChaosMesh on minikube:

```bash
curl -sSL https://mirrors.chaos-mesh.org/v2.6.0/install.sh | bash
```

Wait for the pods to be all ready:

```bash
kubectl wait --for=condition=Ready pods -n chaos-mesh --all --timeout=600s
```

Build the control and checker Docker images in the minikube docker-env:

```bash
eval $(minikube -p minikube docker-env)
mvn -Pitests -Phttpclient-jdk -Pchecker clean package k8s:build -pl chaos-tests -DskipTests
mvn -Pitests -Phttpclient-jdk -Pcontrol clean package k8s:build -pl chaos-tests -DskipTests
```

and finally run the test:

```bash
mvn -Pitests verify -pl chaos-tests
```

### Glossary

- checker: run the SharedInformer to get notifications over the shared resource
- control: apply timely changes to the shared resource
