FROM nginx:1.15
RUN apt-get update
RUN apt-get install -y certbot python-certbot-nginx
RUN apt-get install -y procps

COPY nginx.conf /etc/nginx/nginx.conf
