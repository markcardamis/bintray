// ============================================================================
// (C) Copyright Schalk W. Cronje 2014
//
// This software is licensed under the Apache License 2.0
// See http://www.apache.org/licenses/LICENSE-2.0 for license details
//
// Unless required by applicable law or agreed to in writing, software distributed under the License is
// distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and limitations under the License.
//
// ============================================================================
package org.ysb33r.gradle.bintray

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.api.file.FileCollection

/**
 * Created by schalkc on 27/04/2014.
 */
class BintrayGenericUpload extends DefaultTask {

    @InputFiles
    List<File> artifacts

    @Input
    @Optional
    String baseUrl = BintrayAPI.API_BASE_URL

    @Input
    String repoOwner

    @Input
    String repoName

    @Input
    String packageName

    @Input
    @Optional
    String version = project.version

    @Input
    String userName

    @Input
    String apiKey

    @Input
    @Optional
    boolean md5 = true

    @Input
    @Optional
    boolean sha1 = false

    @Input
    @Optional
    boolean sha256 = false

    /** Package textual description
     *
     */
    @Input
    @Optional
    String description = ''

    /** Tags to apply to package
     *
     */
    @Input
    @Optional
    def tags = null

    /** Licenses to apply to package
     *
     */
    @Input
    @Optional
    def licenses = null

    /** Source Control URL
     *
     */
    @Input
    @Optional
    String vcsUrl = null


    /** If set, a non-existing package will be created prior to creating the version
     *
     */
    @Input
    @Optional
    boolean autoCreatePackage = false

    /** If set, a existing package will be updated prior to creating/updating the version
     *
     */
    @Input
    @Optional
    boolean updatePackage = false

    /** Set this uploaded packages require signing by Bintray
     *
     */
    @Input
    @Optional
    boolean gpgSign = false

    /** Set this if the signing key requires a passphrase
     *
     */
    @Input
    @Optional
    String gpgPassphrase

    def setTags (Object... t) {
        tags = t as List
    }

    def setLicenses (Object... t) {
        licenses = t as List
    }

    @TaskAction
    void exec() {
        BintrayAPI api=new BintrayAPI(
            'baseUrl'     : baseUrl,
            'repoOwner'   : repoOwner,
            'repoName'    : repoName,
            'packageName' : packageName,
            'version'     : version,
            'userName'    : userName,
            'apiKey'      : apiKey
        )

        boolean updated=false
        if(autoCreatePackage && !api.hasPackage()) {
            if(!api.createPackage(description,tags,licenses,vcsUrl)) {
                project.logger.info("Could not create package metadata on Bintray. Nothing will be uploaded.")
                return
            }
            updated=true
        }

        if(updatePackage && !updated) {
            if(!api.updatePackage(description,tags,licenses,vcsUrl)) {
                project.logger.info("Could not update package metadata on Bintray. Nothing will be uploaded.")
                return
            }
        }

        if(!api.hasVersion()) {
            api.createVersion(description)
        } else {
            api.updateVersion(description)
        }

        artifacts.each {
            def uploadList = [it]
            if(md5) {
                project.ant.checksum file: it, algorithm: 'MD5'
                uploadList.add "${it.absolutePath}.MD5"
            }
            if(sha1) {
                project.ant.checksum file: it, algorithm: 'SHA-1', fileext: '.SHA1'
                uploadList.add "${it.absolutePath}.SHA1"
            }
            if(sha256) {
                project.ant.checksum file: it, algorithm: 'SHA-256', fileext: '.SHA256'
                uploadList.add "${it.absolutePath}.SHA256"
            }

            uploadList.each { f ->
                logger.info "Uploading ${f}"
                api.uploadContent(project.file(f))
            }
        }

        if(gpgSign) {
            api.signVersion(gpgPassphrase)
        }

    }

    void username(final String u) {
        userName=u
    }

    void sources(final List inputs) {
        inputs.each { sources(it) }
    }

    void sources(final FileCollection inputs) {
        inputs.files.each { sources(it) }
    }


    void sources(final String input) {
        sources(new File(input))
    }

    void sources(final File input) {
        if(artifacts==null) {
            artifacts = []
        }
        artifacts.add(input)
    }

}
