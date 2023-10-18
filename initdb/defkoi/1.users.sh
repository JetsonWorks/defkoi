#!/bin/bash
# run this as postgres

sed -i 's/^\([^#]*local.*\)md5/\1trust/g' /opt/bitnami/postgresql/conf/pg_hba.conf
pg_ctl reload -D $POSTGRESQL_DATA_DIR

psql postgres -tAc "SELECT 1 FROM pg_roles WHERE rolname='owner'" | grep -q 1 || {
  echo "Creating role owner"
  psql postgres -tAc "CREATE USER owner WITH PASSWORD '$ownerPassword'"
}

psql postgres -tAc "SELECT 1 FROM pg_database WHERE datname='"$dbName"'" | grep -q 1 || {
  echo "Creating database $dbName"
  createdb -T template0 -O owner $dbName -U postgres
}

