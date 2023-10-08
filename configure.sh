#!/bin/bash
# build the apps, select Spring app configuration source, prepare the environment, and prepare the databases

cd $(dirname $0)
envFile=.env
source $envFile

set -e
[[ -f dkrest/target/dkrest-${version}.jar ]] || {
  echo -e "\nBuilding and packaging"
  mvn package
}

function promptUpdateProp() {
  chmod 600 $envFile
  key=$1
  value=${!key}
  read -rep "$key: " -i ${value:-""} value
  if grep -wq $key $envFile; then
    value=$(echo "$value" |sed -e 's/\\/\\\\/g' -e "s/&/\\\\&/g" -e "s,/,\\\\/,g")
    sed -i "s/^.*$key.*/export $key=\'$value\'/" $envFile
  else
    echo "export $key='$value'" >> $envFile
  fi
  export $key
}

# either set SPRING_CONFIG_LOCATION or SPRING_CLOUD_CONFIG_URI
echo
egrep -q "^ *- SPRING_CONFIG_LOCATION" docker-compose.yaml && cloud=n || cloud=y
read -ep "Configure dkrest to use Spring Cloud Config Server? " -i $cloud useCloud
[[ $useCloud =~ y || $useCloud =~ Y || $useCloud =~ n || $useCloud =~ N ]] || useCloud=$cloud
if [[ $useCloud =~ y || $useCloud =~ Y ]]; then
  [[ $cloud =~ n || $cloud =~ N ]] && {
    sed -ri "s/ *(- SPRING_CONFIG_LOCATION.*)/#      \1/" docker-compose.yaml
    sed -ri "s/ *#* *(- SPRING_CLOUD_CONFIG_URI.*)/      \1/" docker-compose.yaml
  }
  promptUpdateProp SPRING_CLOUD_CONFIG_URI
  promptUpdateProp SPRING_CLOUD_CONFIG_LABEL
  promptUpdateProp SPRING_CLOUD_CONFIG_USERNAME
  promptUpdateProp SPRING_CLOUD_CONFIG_PASSWORD
  SPRING_PROFILES_ACTIVE=docker promptUpdateProp SPRING_PROFILES_ACTIVE
else
  [[ $cloud =~ y || $cloud =~ Y ]] && {
    sed -ri "s/ *(- SPRING_CLOUD_CONFIG_URI.*)/#      \1/" docker-compose.yaml
    sed -ri "s/ *#* *(- SPRING_CONFIG_LOCATION.*)/      \1/" docker-compose.yaml
  }
fi

# dkdb preparation
echo -e "\nChecking passwords"
promptUpdateProp pgPassword
promptUpdateProp ownerPassword

echo -e "\nThe $envFile file has been updated."

