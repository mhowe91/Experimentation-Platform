#ARG BASE_IMAGE=drupalwxt/site-wxt:latest
#FROM $BASE_IMAGE as src
FROM nginx:alpine
COPY ./docker/conf/nginx.conf /etc/nginx/nginx.conf
RUN mkdir -p /var/www/html
RUN apk update
RUN apk add git
WORKDIR /var/www/html
RUN git clone https://github.com/amonsour/analytics-api.git .
COPY ./docker/images/pageperformance_nginx/themes-dist-6.0-gcweb.zip /var/www/html/
RUN unzip themes-dist-6.0-gcweb.zip
RUN mv themes-dist-6.0-gcweb gcweb
COPY docker/images/pageperformance_php/mongo_connect.php /var/www/html/php/


