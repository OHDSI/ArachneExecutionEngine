#!/bin/bash

echo "Test docker client"
if docker >> /dev/null
then
	echo "SUCCESS"
else
	echo "Docker is not installed. Exiting"
	exit 1
fi

echo "Test docker server"
if docker version
then
	echo "SUCCESS"
else
	echo "Docker Server is not accessable."
	echo "SET DOCKER_HOST"
	exit 1
fi

echo "Removing container if exists"
if docker stop execution_engine
then
    docker rm -f execution_engine
    echo "REMOVED"
else
    echo "NOT EXISTS"
fi

echo "Removing image if exists"
if docker rmi -f hub.arachnenetwork.com/execution_engine
then
    echo "REMOVED"
else
    echo "NOT EXISTS"
fi

echo "Building docker image"
if mvn -Dmaven.test.skip clean package -P "$1"
then
    echo "SUCCESS"
else
    echo ""
    exit 1
fi

echo "Creating container"
if docker create --restart=always -v /tmp:/tmp --name execution_engine --net="host" hub.arachnenetwork.com/execution_engine
then
    echo GOOD WORK! To start container: docker start execution_engine
else
    echo "SOME PROBLEMS WAS HERE UNTIL CONTAINER WAS CREATING"
    exit 1
fi
exit 0