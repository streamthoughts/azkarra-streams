#!/bin/bash

last_commit="$(git log -1 --pretty=%B | cat | grep '\[update_api_docs\]')"

if [[ ${last_commit} == *[update_api_docs]* ]]; then
  echo "Generating and publish API docs"

  git config --global user.email circleci@circleci
  git config --global user.name CircleCI
  mvn javadoc:aggregate@copy-project-javadoc && git add site/static/apidocs
  git commit -m "docs(gh-pages): generate apidocs [skip ci]"
  git push
  exit $?
else
  echo "Skipping apidocs generation, lastest commit message doesn't contains [update_api_docs]"
fi

exit 0

