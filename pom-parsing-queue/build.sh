docker build -f Dockerfile-server -t pom-parsing-queue-manager:latest .
docker build -f Dockerfile-worker -t pom-parsing-queue-queue-worker:latest .