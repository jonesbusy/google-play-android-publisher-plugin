# Google Play Android Publisher plugin for Jenkins

[![Jenkins plugin](https://img.shields.io/jenkins/plugin/v/google-play-android-publisher.svg)](https://plugins.jenkins.io/google-play-android-publisher)
[![Jenkins plugin installs](https://img.shields.io/jenkins/plugin/i/google-play-android-publisher?color=blue)](https://plugins.jenkins.io/google-play-android-publisher)
[![Build status](https://ci.jenkins.io/job/Plugins/job/google-play-android-publisher-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/google-play-android-publisher/job/master/)

Enables Jenkins to manage and upload Android app files (AAB or APK) to Google Play.

## Features
- Uploading Android App Bundle (AAB) or APK files to Google Play
  - This includes apps which use Multiple APK support
  - ProGuard `mapping.txt` files can also be associated with each app file, for deobfuscating stacktraces
-  Uploading APK expansion (.obb) files
   - With the option to re-use expansion files from existing APKs, e.g. for patch releases
- Assigning apps to internal, alpha, beta or production release tracks
  - This includes a build step for moving existing versions to a different track, or updating the rollout percentage   
    e.g. You can upload an alpha in one job, then later have another
        job promote it to beta
- Staged rollout of apps to any release track
- Assigning release notes to uploaded files, for various languages
- Changing the Jenkins build result to failed if the configuration is bad, or uploading or moving app files fails for some reason
-  Every configuration field supports variable and [token](https://wiki.jenkins.io/display/JENKINS/Token+Macro+Plugin) expansion, allowing release notes to be dynamically generated, for example
- Integration with the [Google OAuth
    Plugin](https://wiki.jenkins.io/display/JENKINS/Google+OAuth+Plugin), so that Google Play credentials can be entered once globally, stored securely, and shared between jobs
  - Multiple Google Play accounts are also supported via this mechanism

## Requirements
### Jenkins
Jenkins [version 2.138.4](https://jenkins.io/changelog-stable#v2.138.4) or newer is required.

### Google Play publisher account
For the initial setup only, you must have access to the Google account
which owns the [Google Play publisher
account](https://developer.android.com/distribute/googleplay/start.html).

This is required to enable API access from Jenkins to your Google Play account.

Note that having admin access is not enough; you need the account owner.  
You can see who the account owner is under [Settings → User accounts &
rights](https://play.google.com/apps/publish/#AdminPlace) in the Google Play developer console.

### Please note
- Any APKs uploaded will be published by Google Play immediately; they will not be held in a draft or pending state
- The app being uploaded must already exist in Google Play; you cannot use the API to upload brand new apps

## Setup
### One-time: Set up Google Play credentials
The following initial setup process is demonstrated in this video:
[https://www.youtube.com/watch?v=txdPSJF94RM](https://www.youtube.com/watch?v=txdPSJF94RM&list=PLhF0STyfNdUk1R3taEmgFR30yzp41yuRK&index=1) (note that Google has changed the Google API Console (at least twice) since this video was recorded; steps 3–13 in the "Create Google service account" section below have the updated info)

#### Install plugin
Install this plugin via the Jenkins plugin manager.  
Or if installing the plugin via other means, ensure that the
prerequisite [Google OAuth Plugin](https://wiki.jenkins.io/display/JENKINS/Google+OAuth+Plugin), [Token Macro Plugin](https://wiki.jenkins.io/display/JENKINS/Token+Macro+Plugin) and their dependencies are also installed.

#### Create Google service account
To enable automated access to your Google Play account, you must create a service account:

1.  Sign in to the [Google Play developer console](https://play.google.com/apps/publish/) as
    the account owner
2.  Select Settings → Developer account → API access
3.  Under Service Accounts, click "Create Service Account"
4.  Follow the link to the Google API Console
5.  Click the "Create service account" button
6.  Give the service account any name you like, e.g. "Jenkins"
7.  Choose Service Accounts > Service Account User for the "Role" field
8.  Enable "Furnish a new private key"
9.  Choose "JSON" as the key type (P12 works as well, but JSON is a little simpler)
10. Click the "Save" button
11. Note that a .json file is downloaded, named something like "api-xxxxxxxxx-xxxxx-xxxx.json"
12. Close the dialog that appears
13. Copy the email address of the new user (something like "jenkins@api-xxxxxxxxx-xxxxx-xxxx.iam.gserviceaccount.com")
14. You can now close the page

#### Assign permissions to the service account
1.  Return to the Google Play developer console page
2.  Click "Done" on the dialog
3.  Note that the service account has associated with the Google Play publisher account
    1.  If it hasn't, follow these additional steps before continuing:
    2.  Click "Users & permissions" in the menu
    3.  Click "Invite new user"
    4.  Paste in the email address you copied above
    5.  Continue from step 5
4.  Click the "Grant access" button for the account (e.g. "jenkins@api-xxxxxxxxx-xxxxx-xxxx.iam.gserviceaccount.com")
5.  Ensure that at least the following permissions are enabled:
    - **View app information** — this is required for the plugin to function
    - **Manage production releases** — optional, if you want to upload APKs to production
    - **Manage testing track releases** — if you want to upload APKs to alpha, beta, or internal  
6.  Click "Add user" (or "Send invitation", as appropriate)
7.  You can now log out of the Google Play publisher account

#### Add the service account credentials to Jenkins:
1. Navigate to your Jenkins instance
2. Select "Credentials" from the Jenkins sidebar
3. Choose a credentials domain and click "Add Credentials"
4. From the "Kind" drop-down, choose "Google Service Account from private key"
5. Enter a name for the credential — the actual value is not important
6. Choose the "JSON key" type
7. Upload the .json file that was downloaded by the Google API Console
8. Enter a meaningful name, as you'll need to select it during build configuration, or enter it into your Pipeline configuration
9. Click "OK" to create the credential

Jenkins now has the required credentials and permissions in order to publish to Google Play.

Once you've set up a job (see the next section) and confirmed that uploading works, either delete the downloaded JSON file or ensure that it's stored somewhere secure.

### Per-job configuration
#### Freestyle job configuration
##### Uploading an APK
The following job setup process is demonstrated in this video:
[https://www.youtube.com/watch?v=iu-bLY9-jkc](https://www.youtube.com/watch?v=iu-bLY9-jkc&list=PLhF0STyfNdUk1R3taEmgFR30yzp41yuRK&index=2)

1. Create a new free-style software project
2. Ensure, via whatever build steps you need, that the file(s) you want to upload will be available in the build's workspace
3. Add the "Upload Android AAB/APK to Google Play" post-build action
4. Select the credential name from the drop-down list
   - The credential must belong to the Google Play account which owns the app to be uploaded
5. Enter paths and/or patterns pointing to the AAB or APKs to be uploaded
   - This can be a glob pattern, e.g. `build/**/*-release.apk`, or a filename, both relative to the root of the workspace
   - Multiple patterns or filenames can be entered, if separated by commas
   - If nothing is entered, the default is `**/build/outputs/**/*.aab, **/build/outputs/**/*.apk`
6. Choose the track to which the APKs should be deployed
   - If nothing is entered, the default is `production`
7. Optionally specify a [rollout percentage](https://support.google.com/googleplay/android-developer/answer/3131213)
   - If nothing is entered, the default is to roll out to 100% of users
8. Optionally choose "Add language" to associate release notes with the uploaded APK(s)
   - You add entries for as many or as few of your supported language as you wish, but each language must already have been added to your app, under the "Store Listing" section in the Google Play Developer Console.

###### APK expansion files
You can optionally add up to two [expansion
files](https://developer.android.com/google/play/expansion-files.html) (main + patch)
for each APK being uploaded.

A list of expansion files can be specified in the same way as APKs, though note that they must be named in the format `[main|patch]``.<apk-version>.<package-name>.obb`.

You can also enable the "Re-use expansion files from existing APKs where necessary" option, which will automatically the most-recent expansion files to newly uploaded APKs.

Similarly, if you want to apply the same expansion file(s) to multiple APKs being uploaded, you can do so.  Name the expansion file(s) according to the _lowest_ version code being uploaded: the expansion file will then be uploaded, and applied to the remaining APKs with higher version codes.

See the inline help for more details.

##### Moving existing app versions to another release track
If you have already uploaded an app to the alpha track (for example), you can later use Jenkins to re-assign that version to the beta or production release track.

Under the "Build" section of the job configuration, add the "Move Android apps to a different release track" build step and configure the new release track.

You can tell Jenkins **which** version codes should be moved by either entering the values directly, or by providing AAB or APK files, from which the plugin will read the application ID and version codes for you.

#### Pipeline job configuration
As of version 1.5, this plugin supports the [Pipeline Plugin](https://wiki.jenkins.io/display/JENKINS/Pipeline+Plugin) syntax.

You can generate the required Pipeline syntax via the [Snippet Generator](https://jenkins.io/blog/2016/05/31/pipeline-snippetizer/), but some examples follow.

Note that you should avoid using these steps in a `parallel` block, as the Google Play API only allows one concurrent "edit session" to be open at a time.

##### Uploading an AAB or APK
The `androidApkUpload` build step lets you upload Android App Bundle (AAB) or APK files.

| Parameter                          | Type    | Example                           | Default                                                | Description                                                                                                            |
|------------------------------------|---------|-----------------------------------|--------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------|
| googlePlayCredentialsId            | string  | My Google Play account            | (none)                                                 | Name of the Google Service Account credential created in Jenkins                                                       |
| filesPattern                       | string  | `release/my-app.aab`              | `**/build/outputs/**/*.aab, **/build/outputs/**/*.apk` | Comma-separated glob patterns or filenames pointing to the app files to upload, relative to the root of the workspace  |
| trackName                          | string  | `internal`                        | `production`                                           | Google Play track to which the app files should be published                                                           |
| rolloutPercent                     | number  | `0.01`                            | `100.0`                                                | The rollout percentage to set on the track                                                                             |
| deobfuscationFiles<br>Pattern          | string  | `**/build/outputs/**/mapping.txt` | (none)                                                 | Comma-separated glob patterns or filenames pointing to ProGuard mapping files to associate with the uploaded app files |
| expansionFilesPattern              | string  | `**/*.obb`                        | (none)                                                 | Comma-separated glob patterns or filenames pointing to expansion files to associate with the uploaded app files        |
| usePreviousExpansion<br>FilesIfMissing | boolean | `false`                           | `true`                                                 | Whether to re-use the existing expansion files that have already been uploaded to Google Play for this app, if any expansion files are missing |
| recentChangeList                   | list    | (see below)                       | (empty)                                                | List of recent change texts to associate with the upload app files                                                     |

The only mandatory parameter is `googlePlayCredentialId`:
```groovy
androidApkUpload googleCredentialsId: 'My Google Play account'
```

This will find any app files in the workspace matching the pattern `**/build/outputs/**/*.aab, **/build/outputs/**/*.apk`, upload them to the Production track, and make them available to 100% of users.

A more complete example:
```groovy
androidApkUpload googleCredentialsId: 'My Google Play account',
                 filesPattern: '**/build/outputs/**/*.aab',
                 trackName: 'beta',
                 rolloutPercent: 25,
                 deobfuscationFilesPattern: '**/build/outputs/**/mapping.txt',
                 recentChangeList: [
                   [language: 'en-GB', text: "Please test the changes from Jenkins build ${env.BUILD_NUMBER}."],
                   [language: 'de-DE', text: "Bitte die Änderungen vom Jenkins Build ${env.BUILD_NUMBER} testen."]
                 ]
```

To upload expansion files, reusing those from the previous upload where possible:
```
androidApkUpload googleCredentialsId: 'My Google Play account',
                 filesPattern: '**/*.apk',
                 expansionFilesPattern: '**/patch.obb',
                 usePreviousExpansionFilesIfMissing: true
```

##### Updating release tracks with existing app versions
The `androidApkMove` build step lets you move existing Android app versions to another release track, and/or update the rollout percentage.

| Parameter               | Type    | Example              | Default                                                | Description                                                                                                                     |
|-------------------------|---------|----------------------|--------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| googlePlayCredentialsId | string  | My Google Play account | (none)                                                 | Name of the Google Service Account credential created in Jenkins                                                                |
| trackName               | string  | `internal`             | `production`                                           | Google Play release track to update with the given app versions                                                                 |
| rolloutPercent          | number  | `0.01`                 | `100.0`                                                | The rollout percentage to set on the given release track                                                                        |
| fromVersionCode         | boolean | `true`                 | `false`                                                | If true, the `applicationId` and `versionCodes` parameters will be used. Otherwise the `filesPattern` parameter will be used    |
| applicationId           | string  | `com.example.app`      | (none)                                                 | The application ID of the app to update                                                                                         |
| versionCodes            | string  | `1281, 1282, 1283`     | (none)                                                 | The version codes to set on the given release track                                                                             |
| filesPattern            | string  | `release/my-app.aab`   | `**/build/outputs/**/*.aab, **/build/outputs/**/*.apk` | Comma-separated glob patterns or filenames pointing to the files from which the application ID and version codes should be read |

The `googlePlayCredentialId` parameter is mandatory, plus either an application ID and version code(s), or AAB or APK file(s) to read this information from.

For example, this would move the given versions to the production track, and make them available to 100% of users:
```groovy
androidApkMove googleCredentialsId: 'My Google Play account',
               applicationId: 'com.example.app',
               versionCodes: '1281, 1282, 1283',  
```

Or moving versions from alpha (for example), to 50% of beta users, reading the application ID and version codes from APK files in the workspace:
```groovy
androidApkMove googleCredentialsId: 'My Google Play account',
               trackName: 'beta',
               rolloutPercent: 50,
               fromVersionCode: false,
               apkFilesPattern: '**/*.apk'
```

##### Backwards-compatibility
Version 3.0 of the plugin deprecated some parameters used by the build steps, but they will remain supported for the foreseeable future:
- `apkFilesPattern` is deprecated — `filesPattern` should be used instead
- `rolloutPercentage` is deprecated; it required a string rather than a number, which was not ideal — `rolloutPercent` should be used instead

In addition, version 3.0 introduced the default values shown in the tables above, so those parameters can optionally now be omitted.

## Troubleshooting
Error messages from the plugin (many of which come directly from the Google Play API) should generally be self-explanatory.  
If you're having trouble getting a certain config to work, try uploading the same APKs manually to Google Play. There you'll likely see the reason for failure, e.g. a version code conflict or similar.

Otherwise, please check the [existing bug reports][jira], and [file a new bug report][report] with details, including the build console log output, if necessary.

Some known error messages and their solutions are shown below:

### GoogleJsonResponseException: 401 Unauthorized
This means that the Google service account does not have permission to make the changes that you requested.

Make sure that you followed the setup instructions above, and confirm that the service account you are using in this Jenkins job has the appropriate permissions for the app that you are trying to change.

### GoogleJsonResponseException: 500 Internal Server Error
Unfortunately, the Google Play API sometimes is not particularly reliable, and will throw generic server errors for no apparent reason.

In these cases you can try running your build again, or wait a few hours before retrying, if the problem persists.

Please also consider [contacting Google Play Developer Support](https://support.google.com/googleplay/android-developer/contact/publishing?extra.IssueType=submitting&hl=en&ec=publish&cfsi=publish_cf&cfnti=escalationflow.email&cft=3&rd=1) to help make them aware that people use the Google Play API, and that it should preferably work in a reliable manner.

This plugin already recognises some temporary Google Play API server problems and works around them; more workarounds may be added in future, e.g. automatically retrying when a generic server error is encountered.

### Unable to retrieve an access token with the provided credentials
If you see this error message, look further down the error log to see what is causing it. Below are a couple of common causes:

#### Invalid JWT: Token must be a short-lived token and in a reasonable timeframe
Ensure that the time is correctly synchronised on the Jenkins master and build agent, and then try again.

#### java.net.SocketTimeoutException: connect timed out
This likely means your build machine is behind an HTTP proxy.

In this case, you should set up Jenkins as documented on the [JenkinsBehindProxy](https://wiki.jenkins.io/display/JENKINS/JenkinsBehindProxy#JenkinsBehindProxy-HowJenkinshandlesProxyServers) page.

This plugin only makes secure (HTTPS) requests, so you need to make sure that the `-Dhttps.proxyHost=<hostname>` and `-Dhttps.proxyPort=<port>` Java properties are set when starting Jenkins. Add the appropriate http versions of those flags too, if unsecured HTTP requests also need to go through the proxy.

## Frequently asked questions
### What if I want to upload APKs with multiple, different application IDs (i.e. build flavours)?
Using the build flavours feature of the Android Gradle build system, it's possible to have a single Android build which produces multiple APKs, each with a different application ID. e.g. You could have application IDs "com.example.app" and "com.example.app.pro" for free and paid versions.

As these may be built in a single Jenkins job, people have wondered why this plugin will refuse to upload APKs with differing application IDs in a single freestyle job.

However, as far as Google Play is concerned, these are completely separate apps. This is correct and, as such, they should be uploaded in separate Jenkins builds: one per application ID.

If the plugin did allow this and you were to attempt to upload, say three, completely different APKs in one Jenkins build, this would require opening and committing three separate "edit sessions" with the Google Play API. If any one of these were to fail — maybe because of an invalid APK, versionCode conflict, or due to an API failure (which, unfortunately, is not uncommon with the Google Play API) — you would end up with your Google Play account in an inconsistent state. Your Jenkins build would be marked as failed, but one or more applications will have actually been uploaded and published to Google Play, and you would have to fix the situation manually. Also, you would not be able to simply re-run the build, as it would fail due to already-existing APKs.

The best practice in this case would be to have one job that builds the different flavours (i.e. the APKs with different application IDs) and then, if the build is successful, it would archive the APKs and start multiple "downstream" Jenkins builds which individually publish each of the applications.  
This can be achieved, for example, with the Parameterized Trigger Plugin and the Copy Artifacts Plugin, i.e. the "upload" job could be generic, and would receive the APK information via parameter.

Alternatively, if you have version 1.5 of this plugin, and use the [Pipeline Plugin](https://wiki.jenkins.io/display/JENKINS/Pipeline+Plugin), you should be able to use the `androidApkUpload` step multiple times within a single build.

## Android apps using this plugin
There are several thousand people and companies using this plugin to upload their apps to Google Play, and it's always great to hear from people who are using the plugin.

Feel free to let us know via the feedback section below, or open a Pull Request and add yourself and your apps here! :)

## Contributing
You can potentially get a sense of what's being worked on via the [tickets on the Jenkins Jira][jira].

Please contact us (see below) before working on new features, as we may be working on something already, or at least be able to give advice or pointers.

## Feedback
If you have issues with the plugin that aren't solved via the Troubleshooting section, you can [file a bug report][report] with details, including the build console log output.

You can also send us an email with your comments, suggestions, or feedback:
- [@orrc](https://github.com/orrc)
- [@jhansche](https://github.com/jhansche)

## Changelog
See [CHANGELOG.md](https://github.com/jenkinsci/google-play-android-publisher-plugin/blob/master/CHANGELOG.md).

[jira]:https://issues.jenkins-ci.org/issues/?jql=project%20%3D%20JENKINS%20AND%20status%20in%20(Open%2C%20%22In%20Progress%22%2C%20Reopened)%20AND%20component%20%3D%20google-play-android-publisher-plugin
[report]:http://jenkins.io/redirect/report-an-issue



