# Proxy Service

This proxy service redirects requests to internal services. It uses Nginx to listen on port 80 and handle this request forwarding, and its configuration can be changed if desired by modifying the nginx.conf file. To build and run this proxy as a docker container, execute `./build.sh`.