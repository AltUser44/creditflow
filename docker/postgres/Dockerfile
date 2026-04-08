FROM postgres:16-alpine

COPY init/ /docker-entrypoint-initdb.d/
USER root
RUN chmod +x /docker-entrypoint-initdb.d/*.sh || true
USER postgres
