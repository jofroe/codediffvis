# CodeDiffParser

## Installation for local setup
I.e. local docker instance ([docker desktop for windows](https://docs.docker.com/docker-for-windows/))
- It needs at least 6GB of RAM to run GitLab and Jenkins simultanously

### Download Images
- `>docker pull gitlab/gitlab-ce`
- `>docker pull jenkins/jenkins:jdk11`

Now the images can be started as local instances
- `>docker run --detach --name gitlab --hostname gitlab.example.com --publish 80:80 --publish 22:22 --env GITLAB_OMNIBUS_CONFIG="external_url 'http://gitlab.example.com:80'; gitlab_rails['gitlab_shell_ssh_port']=22;" gitlab/gitlab-ce:latest`
- `>docker run -p 8080:8080 -p 50000:50000 --name jenkins jenkins/jenkins:jdk11`

### Configuration
- Create a project in the local gitlab, i.e. a plain [scout project](https://www.eclipse.org/scout/)
- Create a webhook for merge-requests on GitLab. The correct URL should be provided by the Jenkins pipeline later
- Install necessary plugins for Jenkins: Maven Integration, GitLab, Build Name, Description Setter and Parametrized Trigger
- Either use a shared authentication token for the webhook or deactivate security on Jenkins such that no login is required (since it's a local instance)

#### Jenkins Build (create artifacts)
- Configure System: Set the maven repository to "Local to the executor". The MAVEN_OPTS `-Xmx512m -Xms256m` may be required to be set
- Global Tool Configuration: Add a Maven installation (i.e. v. 3.6.3) and let it install from Apache directly

##### Maven Project Build for MergeRequest
- Create a new maven project build
- Source Code Management: add for the GitLab, add credentials
- Build Triggers: select opened merge requests events
- Build Environment: Set the name of the builds to something like: `#${BUILD_NUMBER} - ${gitlabSourceBranch}`
- Build: use the pom.xml of the GitLab project, i.e. scout/pom.xml. Then perform a clean installation: `clean install -e`

##### CodeDiffParser Build
- This build can be triggered even when the merge request build has not yet finished
- Pass git parameters to this build and connect to gitlab (one-time initial manual connection with ssh to gitlab from jenkins container might be needed to establish trust between the servers)
  - `>docker exec -it jenkins /bin/bash`
  - `>ssh -A local.docker` (e.g. your gitlab url, make sure to set the ssh private key credentials in docker and the public one in gitlab)
- Configure the pre-steps to get source and target branch name via webhook
```bash
if [ ! -d ${gitlabSourceBranch} ] ; then
  git clone --single-branch --branch ${gitlabSourceBranch} git@local.docker:root/scout ${gitlabSourceBranch}
else
  cd ${gitlabSourceBranch}
  git pull
  cd ..
fi
if [ ! -d ${gitlabTargetBranch} ] ; then
  git clone --single-branch --branch ${gitlabTargetBranch} git@local.docker:root/scout ${gitlabTargetBranch}
else
  cd ${gitlabTargetBranch}
  git pull
  cd ..
fi
changed_files=$(git diff --name-only ${gitlabSourceBranch} ${gitlabTargetBranch} | grep -v ".git" | grep -E "${gitlabSourceBranch}|${gitlabTargetBranch}" | sed "s/^\($gitlabTargetBranch\|$gitlabSourceBranch\)//" | tr "\n" ",")

java -jar /opt/codediffparser/codediffparser.jar ${gitlabSourceBranch}/ ${gitlabTargetBranch}/ ${JENKINS_HOME}/.m2/ $changed_files > "${gitlabMergeRequestId}.json"
```
- The parameters of source files root and dependency files root must be passed to the CodeDiffParser. It uses the local Jenkins Maven .m2 repository to resolve dependencies.
- The ouput is saved to the workspace of the current Jenkins build, i.e. to `http://local.docker:8080/job/codediffparser/ws/`


