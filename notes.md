# Helpful Notes

## Docker

### Restart a container with different run commands
Link to [Stackoverflow](https://stackoverflow.com/questions/32353055/how-to-start-a-stopped-docker-container-with-a-different-command)
- Make a backup of configurations: config.xml, jobs, .ssh, plugins
```bash
cd /path/to/docker/backup
docker cp jenkins:/var/jenkins_home/config.xml config.xml
docker cp jenkins:/var/jenkins_home/jobs/gitlab-merge-request/config.xml jobs/gitlab-merge-request/config.xml
docker cp jenkins:/var/jenkins_home/jobs/codediffparser/config.xml jobs/codediffparser/config.xml
docker cp jenkins:/var/jenkins_home/.ssh/. .ssh
docker cp jenkins:/var/jenkins_home/plugins/. plugins
find plugins/ -maxdepth 1 -mindepth 1 -type d -exec rm -rf {} \; # delete everything except the jpi files
```
- Stop the container `docker stop mycontainer`
- `docker ps -a` check the container's ID
- `docker commit <container_id> <new_image_name>` creates a new image with said container ID
- `docker run ..` use new commands along with new image
- copy the backup configs to the new container
```bash
cd /path/to/docker/backup
docker cp config.xml jenkins:/var/jenkins_home/config.xml
docker cp jobs/. jenkins:/var/jenkins_home/jobs
docker cp .ssh/. jenkins:/var/jenkins_home/.ssh
docker cp plugins/. jenkins:/var/jenkins_home/plugins
```

## Old Build information for CodeDiffParser
```bash
curdir=`pwd`
cd ${JENKINS_HOME}/jobs/${mergeRequestJobName}/builds/${mergeRequestBuildNumber}/archive/
find . -name '*sources.jar' -exec jar -xvf {} \;
cd ${curdir}
java -jar /opt/codediffparser/codediffparser.jar ${JENKINS_HOME}/jobs/${mergeRequestJobName}/builds/${mergeRequestBuildNumber}/archive/ ${JENKINS_HOME}/jobs/${mergeRequestJobName}/builds/${mergeRequestBuildNumber}/archive/ > "${gitlabMergeRequestId}.json"
```
