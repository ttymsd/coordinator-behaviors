#! /bin/bash

echo "$TRAVIS_BRANCH"
if [ "$TRAVIS_BRANCH" = "master" ]; then
  ./gradlew bintrayUpload
fi

if [[ "$TRAVIS_BRANCH" =~ ^develop/ ]]; then
  ./gradlew bintrayUpload
fi
