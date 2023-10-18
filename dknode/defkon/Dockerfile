FROM node:16.18

EXPOSE 8086
ENV PATH /app/node_modules/.bin:$PATH
RUN apt update && apt upgrade -y && apt install -y xsel && apt clean
RUN npm install --location=global serve

WORKDIR /app
COPY build cert.pem key.pem /app/
CMD ["serve", "-s", "/app", "-p", "8086", "-d", "--ssl-cert", "cert.pem", "--ssl-key", "key.pem"]