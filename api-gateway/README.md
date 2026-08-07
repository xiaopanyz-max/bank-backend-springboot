# API gateway

The gateway runs on port 8080. It discovers `customer-service` from Nacos and routes `/api/customers/**` and `/api/transactions/**` to it.

Start Nacos at `${NACOS_SERVER_ADDR:-127.0.0.1:8848}`, then run `mvn spring-boot:run` in this directory.
