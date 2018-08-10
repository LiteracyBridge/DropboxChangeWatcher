~~DropboxChangeProcessor~~ This project has been deprecated.
======================

A service that watches a Dropbox account and passes changes to a set of actors. Much more to come!

## Building and Artifacts ##

### Prerequisites ###

**Java 7**
:  This project requires Java 7.

**Gradle**
: This project builds with Gradle. You do not need to install anything, as the Gradle Wrapper is available.

**Dropbox App**
: To get the service running, you first need to get a Dropbox App set up. Go to the App Console at 
  https://www.dropbox.com/developers/apps and create a new Core API app with Full Permissions. At the end of the
  process you should get an App Key and App Secret.

### Building ###
    ./gradlew build
    
will run the build via Gradle Wrapper.

### Configuration ###
Configuration is split into two files. The **Key File** contains application and account secrets and should never be
shared or checked in to source control. The **Configuration File** contains configuration values and can be shared. See
*key-example.properties* and *config-example.properties* for details on settings.

### Testing ###
There is a small set of integration tests that work against Dropbox by creating temporary files. You must
have gotten an access token for your Dropbox account to run them. No **Configuration File** is necessary, but you must provide
a **Key File** with app and account access codes (the tests will not copy config from Dropbox, even if specified).

    ./gradlew -DtestKeyFile=<path to Key File> integrationTest
    
The integration tests still leave a little directory cruft (but no files) that I will eventually clean up :)

### Running ###
To set up local configuration, copy the *key-example.properties* file to a new location. Replace the "dropbox-app-key" and 
"dropbox-app-secret" with the values from Dropbox. For the first run, comment out the "dropbox-access-token" key.

The app takes a single command line parameter, the path to the properties file. To run via Gradle Wrapper

    ./gradlew run -Pargs="<path to properties file>"

The first time you run you will get instructions for generating an access token to give it access to your Dropbox
account. Once you have it, copy it into your **Key File**. **DO NOT CHECK IN YOUR COPY OF THE KEY PROPERTIES FILE!** 
It now has secret keys in it that you don't want distributed publicly.

### Deployment ###
You can build a deployable version of the app with all dependencies and a startup script by running

    ./gradlew clean build installDist
    
The output is in the build/install/DropboxChangeProcessor directory. There will be a shell script 
