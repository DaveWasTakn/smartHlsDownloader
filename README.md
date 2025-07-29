# HLS Video Downloader

Java-based tool for downloading HLS (HTTP Live Streaming) video content!

| GUI                             | Output                      |
|---------------------------------|-----------------------------|
| ![GUI](/misc/hlsDownloader.gif) | ![Output](/misc/output.png) |

## Features

- Can be used both using the CLI or the GUI
- Uses virtual threads to download and additionally validate segments in parallel
- Automatically retries downloading segments when a network error occurs
- Also redownloads a segment whenever the validation suggests it is corrupt
- Uses Ffmpeg to merge the segments into the final video file

## Prerequisites

- Java 24+
- Ffmpeg installed on the system

## Build

Run:

```shell
mvn clean package
```

And the built jar should be in the `target` directory.

## Usage

To launch the GUI run the jar without any parameters:

```shell
java -jar adaptiveHlsDownloader-1.0-SNAPSHOT.jar
```

Or use the CLI:

```shell
java -jar .\adaptiveHlsDownloader-1.0-SNAPSHOT.jar --help
Usage: <main class> [options] URL or URI of the HLS playlist
  Options:
    -c, --concurrent-downloads
      Number of concurrent segments to download
      Default: 10
    -cv, --concurrent-validations
      Number of concurrent segments to validate
      Default: 10
    -r, --retries
      Number of times to retry failed segment downloads
      Default: 100
    -sv, --skip-validation
      Whether to skip validation of each segment
      Default: false
    -ev, --extra-validation
      Whether to use ffmpeg to decode each segment for additional validation
      Default: false
    -i, --input
      URL or URI of the HLS playlist
    -o, --output
      Output file or directory name
    -h, --help
      Display help information
```
