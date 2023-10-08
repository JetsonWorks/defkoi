# Introduction
The long-term goal of this project is to provide the software for an autonomous sentry for a Koi pond.
The target platform is the [Jetson Nano](https://www.nvidia.com/en-us/autonomous-machines/embedded-systems/jetson-nano/education-projects/), JetPack 4.6 or later.

# Summary
This project started by referencing the [CudaCamz](https://github.com/JoeTester1965/CudaCamz)
Python project, but quickly changed direction upon experiencing the effects of the Python GIL first hand.
With [DeepJavaLibrary](https://github.com/deepjavalibrary/djl), it's possible to use PyTorch or one of the several other 
supported engines.
[GStreamer 1.x Java Core (gst1-java-core)](https://github.com/gstreamer-java/gst1-java-core) brings the power of
the GStreamer library to Java, enabling pipelines for video transformation and streaming.

## Features

* Multiple inferencing engines supported by DJL
  * Pre-built PyTorch native libraries for aarch64 available
* Cross-platform compatibility (for example, developing on amd64 using the MXNet engine and deploying on aarch64 using the PyTorch engine)
* Multi-threaded GStreamer pipelines supporting multiple video devices
* Publishing of camera feeds to an on-board RTSP proxy
* Simple orchestration of Dockerized services using Compose
* Separate Node.js front-end allows tuning auto-configured video devices and monitoring performance via the dashboard

# Usage

## Installation
The ```system.sh``` script can install and configure dependencies and prepare your Jetson for DefKoi.

1. After completing OEM setup, clone this Git repo and execute ```prepare/system.sh``` <b>as root</b>
   1. This will ensure your system packages are updated and then install DefKoi dependencies
1. After rebooting, pull the Docker images
   1. ```docker compose pull```

## Configuration
Configure DefKon and Keycloak:

1. Update /etc/hosts on your Jetson
    1. Associate your PC's IP address with defkon.jit.com
    1. Associate your Jetson's IP address with defkoi.jit.com
1. Start the dkrest Docker service on your Jetson
    1. ```docker compose up -d dkrest```
1. Import .certs/ca.pem into your PC's browser as a Certificate Authority (CA) certificate
1. Update /etc/hosts on your PC
   1. Associate your PC's IP address with defkon.jit.com
   1. Associate your Jetson's IP address with defkoi.jit.com
1. Start the Docker services on your PC
   1. TODO: compose profile
1. Log into the [Keycloak admin console](https://keycloak.jit.com:8443/) and create the DefKoi realm
   1. From the realm drop-down, select Create Realm
   1. For the resource file, select initdb/keycloak/realm-export.json
   1. Click Create
1. Create a user for DefKoi
   1. See the .env file for the admin password
   1. Join one of the DefKoi groups
   1. Set the password

## Operation
Once the dkrest application has started on your Jetson, you may access it using [DefKon](https://defkon.jit.com:8086/).

1. Log in using the credentials you created earlier
1. Update settings as desired
   1. Once settings are saved, the application will reinitialize

TODO:
* configure.sh
* run rtspproxy if want to publish RTSP
  * and tweak mediamtx.yaml

