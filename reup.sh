#!/bin/bash

./build.sh

docker stack rm services
docker stack deploy -c docker-compose-services.yml services