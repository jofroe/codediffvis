#!/bin/bash

if [ ! $4 ] ; then
  echo "usage: $0 <source> <target> <gitlab_project> <maven_repository>"
  echo "example: $0 feature-branch master https://gitlab.com/group/project.git" ~/.m2/
  exit 1
else
  source_branch=$1
  target_branch=$2
  git_project_url=$3
  maven_repository=$4
fi

curdir=$(pwd)
if [ ! -d ${source_branch} ] ; then
  git clone --single-branch --branch ${source_branch} ${git_project_url} ${source_branch}
else
  cd ${source_branch}
  git pull
  cd $curdir
fi
if [ ! -d ${target_branch} ] ; then
  git clone --single-branch --branch ${target_branch} ${git_project_url} ${target_branch}
else
  cd ${target_branch}
  git pull
  cd $curdir
fi

# analyze changed files
git diff --name-status -M ${source_branch} ${target_branch} | grep -v ".git" | sed -E "s|\t|\n|g" | grep -e "^\($source_branch\|$target_branch\)" | sed -E "s|^($source_branch\|$target_branch)||" > changed_files.txt

java -jar codediffparser.jar ${source_branch}/ ${target_branch}/ ${maven_repository}/ changed_files.txt > "out.json"
