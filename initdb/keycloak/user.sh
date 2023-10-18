#!/bin/bash
# run this as postgres

sed -i 's/^\([^#]*local.*\)md5/\1trust/g' /opt/bitnami/postgresql/conf/pg_hba.conf
pg_ctl reload -D $POSTGRESQL_DATA_DIR

psql postgres -tAc "SELECT 1 FROM pg_roles WHERE rolname='bn_keycloak'" | grep -q 1 || {
  echo "Creating role/user bn_keycloak"
  psql postgres -tAc "CREATE USER bn_keycloak WITH PASSWORD '"$KEYCLOAK_DATABASE_PASSWORD"'"
}

psql postgres -tAc "SELECT 1 FROM pg_database WHERE datname='bitnami_keycloak'" | grep -q 1 || {
  echo "Creating database bitnami_keycloak"
  createdb -T template0 -O bn_keycloak bitnami_keycloak -U postgres
}

