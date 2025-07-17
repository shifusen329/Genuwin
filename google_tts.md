# Create audio from text by using client libraries

bookmark_border Stay organized with collections Save and categorize content based on your preferences.

This quickstart walks you through the process of using client libraries to make a request to Text-to-Speech, creating audio from text.

To learn more about the fundamental concepts in Text-to-Speech, read [Text-to-Speech Basics](https://cloud.google.com/text-to-speech/docs/basics). To see which synthetic voices are available for your language, see the [supported voices and languages page](https://cloud.google.com/text-to-speech/docs/voices).

## Before you begin

Before you can send a request to the Text-to-Speech API, you must have completed the following actions. See the [before you begin](https://cloud.google.com/text-to-speech/docs/before-you-begin) page for details.

- Enable Text-to-Speech on a Google Cloud project.
- Make sure billing is enabled for Text-to-Speech.
- After [installing](https://cloud.google.com/sdk/docs/install) the Google Cloud CLI, [sign in to the gcloud CLI with your federated identity](https://cloud.google.com/iam/docs/workforce-log-in-gcloud) and then [initialize](https://cloud.google.com/sdk/docs/initializing) it by running the following command:
    
    See more code actions.
    
    ```
    gcloud init
    ```
    
- Create local authentication credentials for your user account:
    
    See more code actions.
    
    ```
    gcloud auth application-default login
    ```
    
    If an authentication error is returned, and you are using an external identity provider (IdP), confirm that you have [signed in to the gcloud CLI with your federated identity](https://cloud.google.com/iam/docs/workforce-log-in-gcloud).
    

## Install the client library

<a id="aria-tab-go"></a>[Go](#go)<a id="aria-tab-java"></a>[Java](#java)<a id="aria-tab-node.js"></a>[Node.js](#node.js)<a id="aria-tab-python"></a>[Python](#python)<a id="aria-tab-additional-languages"></a>[Additional languages](#additional-languages)

If you are using [Maven](https://maven.apache.org/), add the following to your `pom.xml` file. For more information about BOMs, see [The Google Cloud Platform Libraries BOM](https://cloud.google.com/java/docs/bom).

[](https://shell.cloud.google.com/cloudshell/editor?show=ide&cloudshell_git_repo=https://github.com/googleapis/google-cloud-java&cloudshell_open_in_editor=java-texttospeech/README.md)

See more code actions.

```
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.google.cloud</groupId>
      <artifactId>libraries-bom</artifactId>
      <version>26.61.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>google-cloud-texttospeech</artifactId>
  </dependency>
</dependencies>
```

If you are using [Gradle](https://gradle.org/), add the following to your dependencies:

[](https://shell.cloud.google.com/cloudshell/editor?show=ide&cloudshell_git_repo=https://github.com/googleapis/google-cloud-java&cloudshell_open_in_editor=java-texttospeech/README.md)

See more code actions.

```
implementation 'com.google.cloud:google-cloud-texttospeech:2.68.0'
```

If you are using [sbt](https://www.scala-sbt.org/), add the following to your dependencies:

[](https://shell.cloud.google.com/cloudshell/editor?show=ide&cloudshell_git_repo=https://github.com/googleapis/google-cloud-java&cloudshell_open_in_editor=java-texttospeech/README.md)

See more code actions.

```
libraryDependencies += "com.google.cloud" % "google-cloud-texttospeech" % "2.68.0"
```

If you're using Visual Studio Code, IntelliJ, or Eclipse, you can add client libraries to your project using the following IDE plugins:

- [Cloud Code for VS Code](https://cloud.google.com/code/docs/vscode/client-libraries)
- [Cloud Code for IntelliJ](https://cloud.google.com/code/docs/intellij/client-libraries)
- [Cloud Tools for Eclipse](https://cloud.google.com/eclipse/docs/libraries)

The plugins provide additional functionality, such as key management for service accounts. Refer to each plugin's documentation for details.

**Note:** Cloud Java client libraries do not currently support Android.

## Create audio data

Now you can use Text-to-Speech to create an audio file of synthetic human speech. Use the following code to send a [`synthesize`](https://cloud.google.com/text-to-speech/docs/reference/rest/v1beta1/text/synthesize) request to the Text-to-Speech API.

<a id="aria-tab-go"></a>[Go](#go)<a id="aria-tab-java"></a>[Java](#java)<a id="aria-tab-node.js"></a>[Node.js](#node.js)<a id="aria-tab-python"></a>[Python](#python)

[](https://shell.cloud.google.com/cloudshell/editor?show=ide&cloudshell_git_repo=https://github.com/GoogleCloudPlatform/java-docs-samples&cloudshell_open_in_editor=texttospeech/snippets/src/main/java/com/example/texttospeech/QuickstartSample.java)

See more code actions.

```
// Imports the Google Cloud client library
import com.google.cloud.texttospeech.v1.[AudioConfig](https://cloud.google.com/java/docs/reference/google-cloud-texttospeech/latest/com.google.cloud.texttospeech.v1.AudioConfig.html);
import com.google.cloud.texttospeech.v1.[AudioEncoding](https://cloud.google.com/java/docs/reference/google-cloud-texttospeech/latest/com.google.cloud.texttospeech.v1.AudioEncoding.html);
import com.google.cloud.texttospeech.v1.[SsmlVoiceGender](https://cloud.google.com/java/docs/reference/google-cloud-texttospeech/latest/com.google.cloud.texttospeech.v1.SsmlVoiceGender.html);
import com.google.cloud.texttospeech.v1.[SynthesisInput](https://cloud.google.com/java/docs/reference/google-cloud-texttospeech/latest/com.google.cloud.texttospeech.v1.SynthesisInput.html);
import com.google.cloud.texttospeech.v1.[SynthesizeSpeechResponse](https://cloud.google.com/java/docs/reference/google-cloud-texttospeech/latest/com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse.html);
import com.google.cloud.texttospeech.v1.[TextToSpeechClient](https://cloud.google.com/java/docs/reference/google-cloud-texttospeech/latest/com.google.cloud.texttospeech.v1.TextToSpeechClient.html);
import com.google.cloud.texttospeech.v1.[VoiceSelectionParams](https://cloud.google.com/java/docs/reference/google-cloud-texttospeech/latest/com.google.cloud.texttospeech.v1.VoiceSelectionParams.html);
import com.google.protobuf.[ByteString](https://cloud.google.com/java/docs/reference/protobuf/latest/com.google.protobuf.ByteString.html);
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Google Cloud TextToSpeech API sample application. Example usage: mvn package exec:java
 * -Dexec.mainClass='com.example.texttospeech.QuickstartSample'
 */
public class QuickstartSample {

  /** Demonstrates using the Text-to-Speech API. */
  public static void main(String... args) throws Exception {
    // Instantiates a client
    try ([TextToSpeechClient](https://cloud.google.com/java/docs/reference/google-cloud-texttospeech/latest/com.google.cloud.texttospeech.v1.TextToSpeechClient.html) textToSpeechClient = [TextToSpeechClient](https://cloud.google.com/java/docs/reference/google-cloud-texttospeech/latest/com.google.cloud.texttospeech.v1.TextToSpeechClient.html).create()) {
      // Set the text input to be synthesized
      [SynthesisInput](https://cloud.google.com/java/docs/reference/google-cloud-texttospeech/latest/com.google.cloud.texttospeech.v1.SynthesisInput.html) input = [SynthesisInput](https://cloud.google.com/java/docs/reference/google-cloud-texttospeech/latest/com.google.cloud.texttospeech.v1.SynthesisInput.html).newBuilder().setText("Hello, World!").build();

      // Build the voice request, select the language code ("en-US") and the ssml voice gender
      // ("neutral")
      [VoiceSelectionParams](https://cloud.google.com/java/docs/reference/google-cloud-texttospeech/latest/com.google.cloud.texttospeech.v1.VoiceSelectionParams.html) voice =
          [VoiceSelectionParams](https://cloud.google.com/java/docs/reference/google-cloud-texttospeech/latest/com.google.cloud.texttospeech.v1.VoiceSelectionParams.html).newBuilder()
              .setLanguageCode("en-US")
              .setSsmlGender([SsmlVoiceGender](https://cloud.google.com/java/docs/reference/google-cloud-texttospeech/latest/com.google.cloud.texttospeech.v1.SsmlVoiceGender.html).NEUTRAL)
              .build();

      // Select the type of audio file you want returned
      [AudioConfig](https://cloud.google.com/java/docs/reference/google-cloud-texttospeech/latest/com.google.cloud.texttospeech.v1.AudioConfig.html) audioConfig =
          [AudioConfig](https://cloud.google.com/java/docs/reference/google-cloud-texttospeech/latest/com.google.cloud.texttospeech.v1.AudioConfig.html).newBuilder().setAudioEncoding([AudioEncoding](https://cloud.google.com/java/docs/reference/google-cloud-texttospeech/latest/com.google.cloud.texttospeech.v1.AudioEncoding.html).MP3).build();

      // Perform the text-to-speech request on the text input with the selected voice parameters and
      // audio file type
      [SynthesizeSpeechResponse](https://cloud.google.com/java/docs/reference/google-cloud-texttospeech/latest/com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse.html) response =
          textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);

      // Get the audio contents from the response
      [ByteString](https://cloud.google.com/java/docs/reference/protobuf/latest/com.google.protobuf.ByteString.html) audioContents = response.[getAudioContent](https://cloud.google.com/java/docs/reference/google-cloud-texttospeech/latest/com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse.html#com_google_cloud_texttospeech_v1_SynthesizeSpeechResponse_getAudioContent__)();

      // Write the response to the output file.
      try (OutputStream out = new FileOutputStream("output.mp3")) {
        out.write(audioContents.[toByteArray](https://cloud.google.com/java/docs/reference/protobuf/latest/com.google.protobuf.ByteString.html#com_google_protobuf_ByteString_toByteArray__)());
        System.out.println("Audio content written to file \"output.mp3\"");
      }
    }
  }
}
```

Congratulations! You've sent your first request to Text-to-Speech.

## How did it go?

## Clean up

To avoid incurring charges to your Google Cloud account for the resources used on this page, follow these steps.

- Use the [Google Cloud console](https://console.cloud.google.com/) to delete your project if you don't need it.

## What's next

- Learn more about Cloud Text-to-Speech by reading the [basics](https://cloud.google.com/text-to-speech/docs/basics).
- Review the list of [available voices](https://cloud.google.com/text-to-speech/docs/voices) you can use for synthetic speech.
