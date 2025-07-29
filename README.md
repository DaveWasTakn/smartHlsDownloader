# Smart HLS Video Downloader

A Java-based tool for downloading HLS (HTTP Live Streaming) video content with both GUI and CLI interfaces.

| GUI                             | Output                      |
|---------------------------------|-----------------------------|
| ![GUI](/misc/hlsDownloader.gif) | ![Output](/misc/output.png) |

## Features

- **Dual Interface**: Use either the GUI or command-line interface
- **Parallel Download**: Uses Java virtual threads for concurrent segment downloading and validation
- **Retry on Error**: Automatically retries failed downloads with configurable retry limit
- **Segment Validation**: Validates downloaded segments and re-downloads corrupted ones
- **Video Assembly**: Uses FFmpeg to merge segments into the final video file
- **Different Validation Modes**: Optional extra validation using FFmpeg decoding

## Prerequisites

- **Java 24+**
- **FFmpeg** - Must be installed and accessible from system PATH. See [FFmpeg](https://ffmpeg.org/).

## Build

Clone the repository and build using Maven:

```shell
git clone git@github.com:DaveWasTakn/smartHlsDownloader.git
cd smartHlsDownloader
mvn clean package
```

The executable JAR will be generated in the `target/` directory as `smartHlsDownloader-1.0-SNAPSHOT.jar`.

## Usage

### GUI Mode

Launch the graphical interface by running the JAR without any parameters:

```shell
java -jar smartHlsDownloader-1.0-SNAPSHOT.jar
```

### CLI Mode

Or the command-line interface:

```shell
java -jar smartHlsDownloader-1.0-SNAPSHOT.jar [options] <HLS_PLAYLIST_URL>
```

#### CLI Options

| Option | Long Form                  | Description                                        | Default  |
|--------|----------------------------|----------------------------------------------------|----------|
| `-c`   | `--concurrent-downloads`   | Number of concurrent segments to download          | 10       |
| `-cv`  | `--concurrent-validations` | Number of concurrent segments to validate          | 10       |
| `-r`   | `--retries`                | Maximum retry attempts for failed downloads        | 100      |
| `-sv`  | `--skip-validation`        | Skip segment validation (faster but less reliable) | false    |
| `-ev`  | `--extra-validation`       | Use FFmpeg decoding for thorough validation        | false    |
| `-i`   | `--input`                  | HLS playlist URL or URI                            | Required |
| `-o`   | `--output`                 | Output file and directory name                     | Optional |
| `-h`   | `--help`                   | Display help information                           | -        |

#### Examples

Download with default settings:

```shell
java -jar smartHlsDownloader-1.0-SNAPSHOT.jar "https://example.com/playlist.m3u8"
```

Download with custom output and higher concurrency:

```shell
java -jar smartHlsDownloader-1.0-SNAPSHOT.jar \
  -c 20 -cv 15 -o "myVideoName" \
  "https://example.com/playlist.m3u8"
```

Fast download with validation disabled:

```shell
java -jar smartHlsDownloader-1.0-SNAPSHOT.jar \
  -sv -c 25 "https://example.com/playlist.m3u8"
```

## License

This extension is an open source project released under the [MIT](LICENSE.txt) license.

## TODOs

Right now the main functionality works! I.e., downloading M3U8 HLS playlists.
But there are still some TODOs, like implementing support for MPEG-DASH streams, and quite some refactoring to do, so it
is still a WIP :)

