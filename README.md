DropboxChangeProcessor
======================

A service that watches a Dropbox account and passes changes to a set of actors. Much more to come!

## Building and Artifacts ##

### Prerequisites ###

**Java 8**
:  This project requires Java 8.

**Gradle**
: This project builds with Gradle. You do not need to install anything, as the Gradle Wrapper is available.

**Dropbox App**
: To get the service running, you first need to get a Dropbox App set up. Go to the App Console at 
  https://www.dropbox.com/developers/apps and create a new Core API app with Full Permissions. At the end of the
  process you should get an App Key and App Secret.

### Building ###

    ./gradlew build
    
will run the build via Gradle Wrapper.

### Running ###

To set up local configuration, copy the example.properties file to a new location. Replace the "dropbox-app-key" and 
"dropbox-app-secret" with the values from Dropbox. For the first run, delete the "dropbox-access-token" key.

The app takes a single command line parameter, the path to the properties file. To run via Gradle Wrapper

    ./gradlew run -Pargs="<path to properties file>"

The first time you run you will get instructions for generating an access token to give it access to your Dropbox
account. Once you have it, copy it into your properties file. **DO NOT CHECK IN YOUR COPY OF THE PROPERTIES FILE!** 
It now has secret keys in it that you don't want distributed publicly.