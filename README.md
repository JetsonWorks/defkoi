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
  * The pre-built PyTorch native libraries for DJL on aarch64 are included
* Cross-platform compatibility (for example: developing on amd64 using the MXNet engine and deploying on aarch64 using the PyTorch engine)
* Multi-threaded GStreamer pipelines supporting multiple active video sources
* Auto-probed CSI and V4L2 devices, and auto-configuration based on user-specified constraints
* Separate Node.js front-end (DefKon) allows tuning auto-configured video sources and monitoring performance via the dashboard
* Simple configuration and dependency management using Docker Compose
  * Use the defkoi profile on the Jetson and the defkon profile on your PC
* GPU-accelerated video processing from CSI sources via nvidia-l4t-gstreamer
* Publishing of camera feeds to an on-board RTSP proxy

## Requirements

* A Jetson Nano
  * Or a similar SBC of your choice, compatible with PyTorch 1.10.0 on CUDA 10.2
* 32 GB storage for OS and apps
* A separate server for the front-end services

# Usage

## Installation
The ```system.sh``` script can install and configure dependencies and prepare your Jetson for DefKoi.

1. After completing OEM setup, clone this Git repo and execute ```prepare/system.sh``` <b>as root</b>
   1. This will ensure your system packages are updated and then install DefKoi dependencies
1. After rebooting, pull the Docker images
   1. ```docker compose --profile defkoi pull```

## Configuration

### Configure DefKon and Keycloak on your PC

1. Import .certs/ca.pem into your PC's browser as a Certificate Authority (CA) certificate
1. Update /etc/hosts
   1. Associate your PC's IP address with defkon.jit.com
   1. Associate your Jetson's IP address with defkoi.jit.com
1. Start the services
   1. ```docker compose --profile defkon up```
1. Log into the [Keycloak admin console](https://keycloak.jit.com:8443/) and create the DefKoi realm
   1. From the realm drop-down, select Create Realm
   1. For the resource file, select initdb/keycloak/realm-export.json
   1. Click Create
1. Create a user for DefKoi
   1. See the .env file for the admin password
   1. Join one of the DefKoi groups
   1. Set the password

### Configure DefKoi on your Jetson

1. Update /etc/hosts
   1. Associate your PC's IP address with defkon.jit.com
   1. Associate your Jetson's IP address with defkoi.jit.com
1. Start the services
   1. ```docker compose --profile defkoi up```

## Operation
Once the dkrest application has started on your Jetson, you may access it using [DefKon](https://defkon.jit.com:8086/).

1. Log in using the credentials you created earlier
1. Update settings as desired
   1. Once settings are saved, the application will reinitialize

Before you can publish live streams to the RTSP proxy, you must update the configuration.
When the rtspproxy service started, it saved the default mediamtx.yml in /var/media.
Update the configuration to disable any paths that would use the same video devices being used by DefKoi, and then define a "live" path for each camera you wish to stream.
Each URL ends with the display name of the camera (with no spaces), which is displayed on the Devices tab in DefKon.

TODO:
* configure.sh
* tweak mediamtx.yml for /live and /object

## Running dkrest on the Jetson host (not in Docker)

The ```prepare/dev.sh``` script can help you configure your Jetson for running dkrest natively by:

1. Installing the remaining dependencies
2. Extracting the PyTorch native libraries for DJL
3. Building the dkrest app and the Docker image

Use the ```bootRun``` wrapper script to launch the dkrest app.
When the app terminates, the pngPipeline script will convert any Graphviz DOT files saved in /tmp into PNGs and store them in the debug directory.

# Known Issues

The object detection functionality is leaking memory.
I suspect either the PyTorch or DJL library.

# Upcoming Features

* Motion detection, which can conditionally trigger object detection
* Multi-threaded object detection, to take full advantage of the GPU
* Support for JetPack 4.6.x (max for Jetson Nano)
    * With custom-built libraries, maxing out the compatibility matrices
        * PyTorch 1.12.1
            * upgraded from 1.10.0
            * not available from NVIDIA until JP 5
        * DJL 0.23.0
            * upgraded from 0.16.0
* Support for other SBCs (I broke my Nano's CSI interface and don't want to pay $500 for a Jetson Orin Nano)
* Fix the detected object RTSP stream
* Spatial awareness
* Support for other inference engines
* MediaMTX auto-configuration

# Contributing

* plug memory leak
* build native libs for tensor
* improve the interface

