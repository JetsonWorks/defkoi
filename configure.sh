#!/bin/bash
# select Spring app configuration source, prepare the environment

cd $(dirname $0)
envFile=.env
source $envFile

function promptUpdateProp() {
  chmod 600 $envFile
  key=$1
  value=${!key}
  read -rep "$key: " -i ${value:-""} value
  if grep -wq $key $envFile; then
    value=$(echo "$value" |sed -e 's/\\/\\\\/g' -e "s/&/\\\\&/g" -e "s,/,\\\\/,g")
    sed -i "s/^.*$key.*/$key=\'$value\'/" $envFile
  else
    echo "$key='$value'" >> $envFile
  fi
}

# either set SPRING_CONFIG_LOCATION or SPRING_CLOUD_CONFIG_URI
# until this script is run, both are commented out for default app props
echo
egrep -q "^ *SPRING_CLOUD_CONFIG_URI" docker-compose.yaml && cloud=y || cloud=n
read -ep "Configure dkrest to use Spring Cloud Config Server? " -i $cloud useCloud
[[ $useCloud =~ y || $useCloud =~ Y || $useCloud =~ n || $useCloud =~ N ]] || useCloud=$cloud
if [[ $useCloud =~ y || $useCloud =~ Y ]]; then
  sed -ri "s/^[ ]+(SPRING_CONFIG_LOCATION.*)/#      \1/" docker-compose.yaml
  sed -ri "s/^[ #]+(SPRING_CLOUD_CONFIG_URI.*)/      \1/" docker-compose.yaml
  echo "Checking settings"
  promptUpdateProp SPRING_CLOUD_CONFIG_URI
  promptUpdateProp SPRING_CLOUD_CONFIG_LABEL
  promptUpdateProp SPRING_CLOUD_CONFIG_USERNAME
  promptUpdateProp SPRING_CLOUD_CONFIG_PASSWORD
  SPRING_PROFILES_ACTIVE=docker promptUpdateProp SPRING_PROFILES_ACTIVE
else
  sed -ri "s/^[ ]+(SPRING_CLOUD_CONFIG_URI.*)/#      \1/" docker-compose.yaml
  sed -ri "s/^[ #]+(SPRING_CONFIG_LOCATION.*)/      \1/" docker-compose.yaml
  sed -ri -e "s/^ *# *(- type: bind.*)/      \1/" \
    -e "s/^ *# *(  source:.*dkDir.*)/      \1/" \
    -e "s/^ *# *(  target:.*defkoi.*)/      \1/" \
    docker-compose.yaml
  [ -f /app/defkoi/dkrest.properties ] ||
    cp dkrest/src/main/resources/application.properties /app/defkoi/dkrest.properties
  [ -f /app/defkoi/logback-rest.xml ] ||
    cp dkrest/src/main/resources/logback.xml /app/defkoi/logback-rest.xml
  echo -e "\nVerify settings in /app/defkoi/dkrest-native.properties"
fi

echo -e "\nPlease verify settings in the .env file."
echo "If you change any PostgreSQL or Keycloak passwords, you must drop the affected volume(s) so they can be recreated:"
echo "  docker stop defkoi-keycloakdb-1 defkoi-dkdb-1"
echo "  docker rm defkoi-keycloakdb-1 defkoi-dkdb-1"
echo "  docker volume rm defkoi_keycloakdb defkoi_dkdb"
