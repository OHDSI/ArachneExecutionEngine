# Hades March 30, 2023 release

This image is built using the Hades-wide release lockfiles found [here](https://github.com/OHDSI/Hades/blob/69f0db8a49d3c90ce297059de6cb0e9381130ff3/hadesWideReleases/2023Mar30/renv.lock#L1).

### Published Docker Image
```odysseusinc/r-hades:2023q3```

**Deployment Notes**

Build and publish image:
```
docker build -t odysseusinc/hades:2023q3 .
docker push odysseusinc/hades:2023q3
```

**Commands to build a tar.gz environment for Arachne Execution Engine**

```
docker run -it --rm -d --name test  executionengine.azurecr.io/hades:2023q3 bash
docker exec -it test tar --exclude /tmp --exclude /proc --exclude /sys -czf /tmp/hades2023q3.tar.gz /
docker cp test:/tmp/hades2023q3.tar.gz /tmp/hades2023q3.tar.gz
docker stop test
```

