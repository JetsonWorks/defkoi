# Introduction
The long-term goal of this project is to provide the system for an autonomous sentry.
It was inspired by the need to defend our koi pond from predators and the desire to explore ML inference on
SBCs.

The current target platform is the
[Jetson Nano](https://www.nvidia.com/en-us/autonomous-machines/embedded-systems/jetson-nano/education-projects/)
at JetPack 4.6, but other platforms will be supported.
With [DeepJavaLibrary](https://github.com/deepjavalibrary/djl),
it's possible to use PyTorch or one of the several other supported engines.
[GStreamer 1.x Java Core (gst1-java-core)](https://github.com/gstreamer-java/gst1-java-core)
brings the power of the GStreamer library to Java, enabling pipelines for video processing and streaming.

## Features

* Multiple inferencing engines supported by DJL
  * This project utilizes PyTorch native libraries for DJL compiled for aarch64
* Cross-platform compatibility
  * for example: developing on amd64 using the MXNet engine and deploying on aarch64 using the PyTorch engine
* Supports multiple active video sources, each processed using GStreamer pipelines
* Auto-probed CSI and V4L2 devices, and auto-configuration based on user-specified constraints
* Separate Node.js front-end allows tuning auto-configured video sources and monitoring performance via the dashboard
* Simple configuration and dependency management using Docker Compose
  * Use the defkoi profile on the Jetson and the defkon profile on your PC
* GPU-accelerated video processing from CSI sources via nvidia-l4t-gstreamer
* Back-end and front-end services utilize HTTPS and OIDC for security
* Publishing of camera feeds to the RTSP proxy

## Requirements

* A Jetson Nano
  * Or a similar SBC of your choice, compatible with PyTorch 1.12.1 on CUDA 10.2
* 32 GB storage for OS and apps
* 4 GB memory
* Additional resources or a separate machine for the front-end services

Docker Compose is needed for the front-end services.
If not already installed, please follow the instructions in [this guide](https://docs.docker.com/compose/install/).

# Usage
Follow these steps for quick setup.

## Installation

1. After completing OEM setup, clone this Git repo and execute ```prepare/system.sh``` <b>as root</b>
   1. This will ensure your system packages are updated and then install DefKoi dependencies
   1. When prompted, choose to install the package maintainer's version
   1. When prompted, choose to restart services automatically
   1. To conserve resources, I recommend setting the default runlevel to multi-user
1. After rebooting, pull the Docker images
   1. ```docker compose --profile defkoi pull```

## Configuration

### Configure DefKon and Keycloak on your PC
DefKon is the management console for DefKoi. Both DefKon and DefKoi rely on Keycloak as an authentication provider.
To get started quickly, follow these steps to utilize the sample certificate, domain, and Keycloak realm.

1. Import .certs/ca.pem into your PC's browser as a Certificate Authority (CA) certificate
1. Update /etc/hosts (or C:\Windows\System32\drivers\etc\hosts on Windows)
   1. Associate your PC's IP address with defkon.jit.com
   1. Associate your Jetson's IP address with defkoi.jit.com
1. Start the services
   1. ```docker compose --profile defkon up```
1. Log into the [Keycloak admin console](https://keycloak.jit.com:8443/) and create the DefKoi realm
   1. The admin password is found in the .env file
   1. From the realm drop-down, select Create Realm
   1. For the resource file, select initdb/keycloak/realm-import.json
   1. Click Create

This creates a user named "dkuser" with password "dkuser", and belonging to the operators group.

**I recommend changing all default passwords and replacing the self-signed certificate.**

### Configure DefKoi on your Jetson

1. Update /etc/hosts
   1. Associate your PC's IP address with defkon.jit.com
   1. Associate your Jetson's IP address with defkoi.jit.com
1. Edit docker-compose.yaml to add all your /dev/video* devices to the dkrest service
1. Start the services
   1. ```docker compose --profile defkoi up```

## Operation
Once the dkrest application has started on your Jetson, you may access it using [DefKon](https://defkon.jit.com:8086/).

1. Log in using the credentials you created earlier
1. Update settings on the Config tab, as desired
   1. Once settings are saved, the application will reinitialize
1. Adjust options for each video source and monitor performance on the Pipeline Configurations tab
1. Changes are saved, but not activated until you hit the ![activate](dknode/defkon/src/media/mystica-Arrow-set-with-deep-1.png) button
1. If needed, you can hit the ![reinit](dknode/defkon/src/media/0000_Refresh.png) button to probe video devices and reinitialize pipelines, but this should not normally be necessary

### Configuration
[![publish2](docs/th_Screenshot_20231021_074014.png)](docs/Screenshot_20231021_074014.png)

### Startup
It takes a few seconds for PyTorch to initialize.
Here we see one video device being activated and PyTorch initializing before starting object detection.

[![startup-csi](docs/th_startup-csi.png)](docs/startup-csi.mp4)

### Proxying
When publishing to RTSP is enabled, the RTSP proxy must be accessible when the video processing pipeline is initialized.
The default proxy is MediaMTX, running on the front-end server.
If you choose to provide an alternate proxy, update the base URL in the configuration.

The path of the stream is constructed from the type of stream and the name of the video device, after removing spaces.
For example, if name of a video device is "HD USB Camera", the live stream URL would be rtsp://defkon.jit.com:8554/live/HDUSBCamera.
These URLs will be displayed in the log.
The streams can be opened with software such as VLC on supported devices.
Refer to the [MediaMTX](https://github.com/bluenviron/mediamtx) page for more information.

[![publish2](docs/th_structurizr-1-DefKoidk-docker.png)](docs/structurizr-1-DefKoidk-docker.png)

## Further Configuration

### Replacing the Sample Domain and Certificate
I recommend using a certificate from [Let's Encrypt](https://letsencrypt.org) with subject alternative names.
You can model your cert after the sample cert for defkoi.jit.com, with SANs for defkon.jit.com and keycloak.jit.com.
Alternatively, the certs.sh script can be modified to generate a self-signed certificate.

Once you have your new certificate, you will need to provide copies in the dknode and dkrest directory structure on both devices.

* On the front-end server: export the cert and key (unencrypted) to cert.pem and cert.key under dknode/defkon
* On the Jetson: export the cert and key to a PKCS12 keystore named server.p12 under dkrest/src/main/resources

Finally:
1. Update the server.ssl.* and keycloak.auth-server-url properties in dkrest/src/main/resources/application.properties on both devices
1. Repackage dknode on the front-end server and rebuild the image:
    1. ```pushd dknode; mvn package; popd; docker compose --profile defkon build```
1. Repackage dkrest on the Jetson and rebuild the image:
   1. ```pushd dkrest; mvn package; popd; docker compose --profile defkoi build```

### Changing Passwords
When you change PostgreSQL or Keycloak passwords (in the .env file),
you will need to drop the Docker volumes so they can be recreated when services are restarted.
When the Keycloak volume is dropped, you will need to import the realm.

```
docker stop defkoi-keycloakdb-1 defkoi-dkdb-1
docker rm defkoi-keycloakdb-1 defkoi-dkdb-1
docker volume rm defkoi_dkdb defkoi_keycloakdb
```

### Running dkrest in Docker with Externalized Configuration
The dkrest configuration can be externalized to either a local properties file or a Spring config server.
Run ```configure.sh``` to help you switch between these options.
Follow the prompts, and this script will update docker-compose.yaml and .env accordingly.

### Running dkrest Natively on the Jetson

DefKoi runs better natively (see the memory leak issue below).
The ```prepare/dev.sh``` script can help you configure your Jetson for running dkrest natively by:

1. Installing the remaining dependencies
1. Extracting the PyTorch native libraries for DJL
1. Building the dkrest app

Use the bootRun wrapper script to launch the dkrest app.
When being run by Docker, dkrest uses Docker's DNS to locate the database service.
When running natively, the URLs to this service changes to reference localhost.
This alternate configuration is enabled with the SPRING_PROFILES_ACTIVE environment variable, which is set by bootRun.
This script also selects the "pytorch" Maven profile.

To run DefKoi natively:

1. Verify the settings in dkrest/src/main/resources/application-native.properties
   1. Or in alternate location if configuration is externalized
1. Execute ```./bootRun```
1. On the Config tab, update the URL to the RTSP proxy

If the defkoi.debug property is true, DefKoi will use the GStreamer API to generate diagrams of the main video pipelines and store them in /tmp as Graphviz DOT files.
When the app terminates, the pngPipeline script will convert these DOTs into PNGs and store them in the debug directory.

# Known Issues

The object detection functionality is leaking memory.
Memory leaks like this have been widely reported on the developer forum.
The effects of the leak are mitigated by reducing the max resolution for object detection and running dkrest natively.
I compiled PyTorch 1.12.1 (Nvidia did not publish it for JetPack 4.6) and the PyTorch native library for DJL 0.23.0 (which are the maximum compatible versions), hoping that one of the memory leak fixes would resolve this issue.

See also: [JetPack 4 Reaches End of Life](https://forums.developer.nvidia.com/t/jetpack-4-reaches-end-of-life/267563).

## Limitations
The "Tap live feed" option is disabled while running in Docker, as is RTSP streaming over UDP.

# Planned Features/Functionality

* Multithreaded object detection, to take full advantage of the GPU
* Motion detection, which can conditionally trigger object detection
* Finish the detected object RTSP stream
* Support for other SBCs
* Support for other inference engines
* RTSP feeds in browser
* Integration of other components and frameworks needed for project completion

