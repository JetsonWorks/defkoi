#!/bin/bash
# DefKoi development environment

pytorchBuild=1.10.0
djlBuild=0.16.0
projdir=$(dirname $0)/..

# install system packages
sudo apt install -y gstreamer1.0-tools gstreamer1.0-rtsp openjdk-17-jdk mpi-default-dev libopenblas-dev graphviz

# Maven version 3.6.0 is too old.
# Use sdkman to manage SDKs
[ -d $HOME/.sdkman ] ||
  curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install maven 3.8.8

# extract DJL native libs
[ -d /home/$user/.djl.ai/pytorch/$djlBuild-cu102-linux-aarch64 ] ||
  docker cp defkoi-dkrest-1:/root/.djl.ai $HOME/.djl.ai

# build the app and the Docker images
cd $projdir/dkrest
mvn package
cd ..

# build images locally
docker compose --profile defkoi build

docker compose --profile defkoi up -d dkdb rtspproxy
