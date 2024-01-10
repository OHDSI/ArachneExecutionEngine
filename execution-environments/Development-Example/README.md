# Minimal Docker Image

This image contains the minimum required environment to run studies with debugging option.
It includes R, database drivers as well as the OHDSI R packages and their dependencies.

**Deployment Notes**
```
docker build -t ohdsi/r-base .
docker run -it --rm ohdsi/r-base
docker push executionengine.azurecr.io/darwin-base:v0.1
```