#Docker Image for data node
 
>Type to get docker build

## 1.0.0

```sh
docker build -t hub.arachnenetwork.com/r-env:1.0.0 .
docker push hub.arachnenetwork.com/r-env:1.0.0
```

## 1.0.1

Changes:

1. Updated DatabaseConnector version
1. Updated Cyclops version 1.3.1
1. Added required library cairo-dev

```sh
docker build -t hub.arachnenetwork.com/r-env:1.0.1 .
docker push hub.arachnenetwork.com/r-env:1.0.1
```

## 1.0.2

Changes:
1. Updated SqlRender version to 1.4.7 (due to Redshift issues)
```sh
docker build -t hub.arachnenetwork.com/r-env:1.0.2 .
docker push hub.arachnenetwork.com/r-env:1.0.2
```

## 1.0.3

Changes:

1. Added PatientLevelPrediction 1.2.2 to R libraries since one is required by PatientLevelPrediction Analysis execution.

```sh
docker build -t hub.arachnenetwork.com/r-env:1.0.3 .
docker push hub.arachnenetwork.com/r-env:1.0.3
```

## 1.0.4

Changes:

1. FeatureExtraction migrated to 2.1.1 version

```sh
docker build -t odysseusinc/r-env:1.0.4 .
docker push odysseusinc/r-env:1.0.4
```
