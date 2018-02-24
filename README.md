
# Sparse Conditional Linear Regression
This project runs a system of linear regression problems on a dataset. The goal is...

## Deployment

This project is designed to deploy an Akka Cluster on Kubernetes. By using Kubernetes, we hope to be able to scale our 
ComputeActor instances to run our linear regression fits quickly.

The project uses Docker to build several container images. The provided YAML files let us push these Docker images to 
Kubernetes . A new feature of Kubernetes `StatefulSet` lets us instantiate our nodes in a specific order so that we can hard-code a
set of "known" seed nodes for our Akka Cluster to use. These "seed node(s)" allow all nodes to register themselves with 
the cluster so it can bootstrap.

## Components
* [Akka](https://akka.io/): A free and open-source toolkit and runtime simplifying the construction of concurrent and distributed applications on the JVM.
* [Kubernetes](https://kubernetes.io): An open-source system for automating deployment, scaling, and management of containers.
* [Docker](https://www.docker.com): Container building,shipping and running platform
* [sbt](http://www.scala-sbt.org/): The interactive build tool for Scala
* [Scala](https://www.scala-lang.org/): A general-purpose programming language providing support for functional programming and a strong static type system.

## Notes
The setup for this project was inspired by the following projects:
* [SBT Docker Kubernetes](https://github.com/WarsawScala/sbt-docker-k8s): An example project that deploys multiple docker images to Kubernetes.
* [Lightbend Akka Cluster on Kubernetes](https://developer.lightbend.com/guides/akka-cluster-kubernetes-k8s-deploy/): A tutorial on deploying an Akka Cluster to Kubernetes.
* [Akka Cluster on GCP](https://developer.lightbend.com/guides/running-akka-cluster-on-google-platform/): An introduction to deploying an Akka Cluster to the Google Cloud Platform.
* [IBM Akka Cluster on Kubernetes](https://github.com/IBM/Akka-cluster-deploy-kubernetes): IBM's tutorial on deploying an Akka Cluster using Kubernetes.

# Steps
We can deploy and run on Google Kubernetes Engine (GKE) or locally!

Note that I have instructions for this using a Mac.
1. Clone project somewhere.
   - `git clone https://github.com/johnhainline/sclr.git`
   - `cd sclr`
1. Install the Kubernetes CLI.
   - Mac: `brew cask install google-cloud-sdk` which installs `gcloud` and other utilities.
   `gcloud components install kubectl` to get, `kubectl`.
1. Install docker.
   - Mac: `brew cask install docker`
1. Run docker.
1. Build our base docker image:
   - `cd src/main/resources/docker/; docker build -t local/openjdk-custom:latest .; cd ../../../../;`
   - This builds the docker image referenced in our build.sbt as `"local/openjdk-custom"`.
1. Create a secret in `kubectl` for our MySQL password.
   - `kubectl create secret generic mysql-password --from-literal=password=MYSQL_PASSWORD`
1. `kubectl` can point to the cloud, or to a local minikube instance.
   - `kubectl config get-contexts` and `kubectl cluster-info`

## Build and Run Locally
1. Install `minikube`, a locally running Kubernetes cluster.
   - Mac: `brew cask install minikube`
1. Start `minikube`, enable DNS support, connect to docker, and open the dashboard.
   - `minikube start`
   - `minikube addons enable kube-dns`
   - `eval $(minikube docker-env)`
   - `minikube dashboard`
1. Build project and push two docker images to our local docker install.
   - `sbt manage/docker:publishLocal`
   - `sbt compute/docker:publishLocal`
 

## Build and Run on Google Kubernetes Engine
1. See [GKE Quickstart](https://cloud.google.com/kubernetes-engine/docs/quickstart)
1. Get short-lived access to `us.gcr.io`, the Google Container Registry.
   - `gcloud docker -a`
1. Build project and publish it to the Google Container Registry.
   - `sbt manage/docker:publish`
   - `sbt compute/docker:publish`
1. Create a remote cluster for running our Kubernetes scripts on. 
   - `gcloud container clusters list`
   - `gcloud container clusters create sclr  --zone us-central1-a --num-nodes 1 --cluster-version=1.9.2-gke.1`
   - `gcloud container clusters get-credentials sclr`
   - `gcloud container clusters describe sclr`

## Common Deploy/Run commands
1. Deploy using Kubernetes scripts.
   - `cd src/main/resources/kubernetes/; kubectl create -f mysql.yaml; kubectl create -f compute-pods.yaml; kubectl create -f manage-pods.yaml; kubectl create -f frontend-pods.yaml; cd ../../../..;`
1. Check running pods, services, etc.
   - `kubectl get all -o wide`
1. Send a single POST request (from the `compute-0` pod) to the `http-service` endpoint. This kicks off the job.
   - `kubectl exec -ti compute-0 -- curl -vH "Content-Type: application/json" -X POST -d '{"name":"m1000","dnfSize":2, "mu":0.4}' http-service.default.svc.cluster.local:8080/begin`
1. Scale the `compute` nodes to 50
   - `kubectl scale statefulsets compute --replicas=50`
1. Make a connection to the MySQL server.
   - `kubectl run -it --rm --image=mysql:5.7 --restart=Never mysql-client -- mysql -h mysql-service -pMYSQL_PASSWORD`
1. Dump an entire schema from the MySQL server to our local directory.
   - `kubectl exec -ti MYSQL_POD_NAME -- mysqldump --add-drop-database --databases medium -pMYSQL_PASSWORD > backup.sql`
1. Delete all local Kubernetes pods, including MySQL, etc.
   - `kubectl delete pvc mysql-pv-claim; kubectl delete all -l app=sclr`
   - Note DOUBLE CHECK EVERYTHING IS DOWN. On error things may keep running, costing money.
