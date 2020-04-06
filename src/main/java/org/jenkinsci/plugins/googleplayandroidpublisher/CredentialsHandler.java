package org.jenkinsci.plugins.googleplayandroidpublisher;

import com.google.jenkins.plugins.credentials.oauth.GoogleOAuth2ScopeRequirement;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentials;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotPrivateKeyCredentials.AccountIdNotSetException;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotPrivateKeyCredentials.PrivateKeyNotSetException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang.exception.ExceptionUtils;
import hudson.model.Item;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;

import static hudson.Util.fixEmptyAndTrim;

public class CredentialsHandler {

    private final String googleCredentialsId;

    public CredentialsHandler(String googleCredentialsId) throws CredentialsException {
        String id = fixEmptyAndTrim(googleCredentialsId);
        if (id == null) {
            throw new CredentialsException("No credentials have been specified: You must add a Google Account via the "
                    + "Jenkins Credentials page, then configure this job to use those credentials");
        }
        this.googleCredentialsId = id;
    }

    /** @return The Google API credentials configured for this job. */
    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    public final GoogleRobotCredentials getServiceAccountCredentials(Item item) throws UploadException {
        try {
            GoogleOAuth2ScopeRequirement req = new AndroidPublisherScopeRequirement();
            GoogleRobotCredentials credentials = GoogleRobotCredentials.getById(googleCredentialsId, item);
            if (credentials == null) {
                throw new CredentialsException(String.format("The Google Service Account credential '%s' "
                        + "could not be found.%n\tIf you renamed the credential since configuring this job, you must "
                        + "re-configure this job, choosing the new credential name", googleCredentialsId));
            }
            return credentials.forRemote(req);
        } catch (AccountIdNotSetException | PrivateKeyNotSetException e) {
            throw new CredentialsException(String.format("The Google Service Account credential '%s' "
                    + "has not been configured correctly.%n\tUpdate the credential, ensuring that the required data "
                    + "have been entered, then try again", googleCredentialsId));
        } catch (NullPointerException e) {
            // This should really be handled by the Google OAuth plugin
            throw new UploadException("Failed to get Google service account info.\n" +
                    "\tCheck that the correct 'Client Secrets JSON' file has been uploaded for the " +
                    "'" + googleCredentialsId + "' credential.\n" +
                    "\tThe correct JSON file can be obtained by visiting the *old* Google APIs Console, selecting " +
                    "'API Access' and then clicking 'Download JSON' for the appropriate service account.\n" +
                    "\tSee: https://code.google.com/apis/console/?noredirect", e);
        } catch (IllegalStateException e) {
            if (ExceptionUtils.getRootCause(e) instanceof FileNotFoundException) {
                throw new UploadException("Failed to get Google service account info. Ensure that the JSON file and " +
                        "P12 private key for the '"+ googleCredentialsId +"' credential have both been uploaded.", e);
            }
            throw new UploadException(e);
        } catch (GeneralSecurityException e) {
            if (ExceptionUtils.getRootCause(e) instanceof IOException) {
                throw new EphemeralCredentialsException("Failed to validate Google Service Account credential " +
                        "against the Google API servers. Check internet connectivity on the Jenkins server and try " +
                        "again.", e);
            }
            throw new UploadException(e);
        }
    }

}
