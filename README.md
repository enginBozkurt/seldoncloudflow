# Seldon Cloudflow integration

## TF GRPC

TF GRPC implementation is based on this [blog post](https://medium.com/@junwan01/a-java-client-for-tensorflow-serving-grpc-api-d37b5ad747aa)
After copying proto files remove this [one](protocol/src/main/protobuf/tensorflow/core/protobuf/conv_autotuning.proto)

## Seldon deployment using Helm

Here are the steps to install Seldon on k8 cluster (based on [this](https://github.com/SeldonIO/seldon-core/blob/master/notebooks/seldon_core_setup.ipynb)). Make sure that you are using Helm3
````
kubectl create namespace seldon
kubectl config set-context $(kubectl config current-context) --namespace=seldon
kubectl create namespace seldon-system
helm install seldon-core-operator helm-charts/seldon-core-operator  --set ambassador.enabled=true --set usageMetrics.enabled=true --namespace seldon-system
kubectl rollout status deploy/seldon-controller-manager -n seldon-system
helm repo add stable https://kubernetes-charts.storage.googleapis.com/
helm repo update
helm install ambassador stable/ambassador --set crds.keep=false
kubectl rollout status deployment.apps/ambassador
````
This will install Seldon and Ambassador

## Seldon deployment using Kustomize

Install Kustomize using
````
brew install kustomize
````
Clone Seldon project
````
git clone git@github.com:SeldonIO/seldon-core.git
````
Go to /operator directory
````
cd seldon-core/operator/
````
***Note*** For kubernetes <1.15 comment the patch_object_selector [here](https://github.com/SeldonIO/seldon-core/blob/master/operator/config/webhook/kustomization.yaml)

Run these 2 commands to install Seldon:
````
make install-cert-manager
make deploy-cert
````
Install Ambassador using (We are using Helm 2 here):
````
kubectl create namespace seldon
kubectl config set-context $(kubectl config current-context) --namespace=seldon
helm repo add stable https://kubernetes-charts.storage.googleapis.com/
helm repo update
helm install --name ambassador stable/ambassador --set crds.keep=false
kubectl rollout status deployment.apps/ambassador
````
## Validate Ambassador install

To validate Ambassador install, run:
````
kubectl port-forward $(kubectl get pods -n seldon -l app.kubernetes.io/name=ambassador -o jsonpath='{.items[0].metadata.name}') -n seldon 8003:8877
````
This will connect to ambassador-admin, which you can see at:

http://localhost:8003/ambassador/v0/diag/

## Creating Seldon TF deployment

To use S3 for model, first create a secret:
````
kubectl create secret generic s3-credentials --from-literal=accessKey=<YOUR-ACCESS-KEY> --from-literal=secretKey=<YOUR-SECRET-KEY>
````
After installation is complete, use deployment yaml files for [Rest](/deployments/model_tfserving_rest.yaml)
and [GRPC](/deployments/model_tfserving_grpc.yaml).

***Note:*** To scale Seldon deployment specify the amount of `replicas` that you need. This will start replicated `SeldonDeployments` accessed through the same service. Typically, in this case, you would also need to scale a streamlet accessing this service.

To verify that REST deployment works correctly, run the following command:
````
curl -X POST http://localhost:8003/seldon/seldon/rest-tfserving/v1/models/recommender/:predict -H "Content-Type: application/json" -d '{"signature_name":"","inputs":{"products":[[1.0],[2.0],[3.0],[4.0]],"users":[[10.0],[10.0],[10.0],[10.0]]}}'
````
To verify GRPC deployment run this [simple test](/grpcclient/src/main/scala/com/lightbend/tf/grpc/SimpleTest.scala)

## Cloudflow 

Install [cloudflow enterprise installer](https://developer.lightbend.com/docs/cloudflow/current/install/index.html)

To scale pipeline deployment use the following command:
````
kubectl cloudflow scale <app> <streamlet> <n>
````
where:
* `app` is the name of deployed cloudflow application. To get deployed application's names execute `kubectl cloudflow list` 
and pick the appropriate application, for example, `seldon-grpc`
* `streamlet` is streamlet name, as defined in the blueprint, for example, `model-serving` 
* `n` - the amount of required instances of streamlet, for example, `5`

Install [Ingress](/deployments/NGNIXIngressInstall)

Install [LB console Ingress](/deployments/es-console.yaml)

Build and deploy pipelines following [documentation](https://cloudflow.io/docs/current/get-started/deploy-to-gke-cluster.html)

Create ingresses for models serving statistics using the following [yaml file](/deployments/modelservers.yaml)

## Fraud detection

Training notebook is [here](/frauddetector/python/FraudDetection.ipynb).
Training data (used for serving as well) is [here](/data/fraud/data/creditcard.zip)
Resulting model (based on Autoencoder) is [here](/data/fraud/model). Model name is `model_1`

To quickly test the model run it locally using tensorflow serving:
````
docker run -p 8501:8501 --rm --name tfserving_fraud --mount type=bind,source=/Users/boris/Projects/TFGRPC/data/fraud/model,target=/models/fraud -e MODEL_NAME=fraud -t tensorflow/serving:1.14.0
```` 
Once this is done, validate deployment using:
````
http://localhost:8501/v1/models/fraud/versions/1
http://localhost:8501/v1/models/fraud/versions/1/metadata
````
To try prediction, you can try:
````
curl -X POST http://localhost:8501/v1/models/fraud/versions/1:predict -d '{"signature_name":"","inputs":{"transaction":[[-1.3598071336738,-0.0727811733098497,2.53634673796914,1.37815522427443,-0.338320769942518,0.462387777762292,0.239598554061257,0.0986979012610507,0.363786969611213,0.0907941719789316,-0.551599533260813,-0.617800855762348,-0.991389847235408,-0.311169353699879,1.46817697209427,-0.470400525259478,0.207971241929242,0.0257905801985591,0.403992960255733,0.251412098239705,-0.018306777944153,0.277837575558899,-0.110473910188767,0.0669280749146731,0.128539358273528,-0.189114843888824,0.133558376740387,-0.0210530534538215,149.62]]}}'
````

Copyright (C) 2020 Lightbend Inc. (https://www.lightbend.com).

