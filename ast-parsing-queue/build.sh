docker build -f Dockerfile-server -t ast-parsing-queue-manager:latest .
docker build  -f Dockerfile-worker -t ast-parsing-queue-worker:latest .