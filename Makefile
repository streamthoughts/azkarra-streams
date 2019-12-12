# Makefile used to build docker images for Azkarra

AZKARRA_VERSION := $(shell mvn org.apache.maven.plugins:maven-help-plugin:3.1.0:evaluate -Dexpression=project.version -q -DforceStdout)
GIT_COMMIT := $(shell git rev-parse --short HEAD)
GIT_BRANCH := $(shell git rev-parse --abbrev-ref HEAD)

REPOSITORY = streamthoughts
IMAGE = azkarra-streams-worker
DIST_DIR = ./azkarra-worker/target/distribution/azkarra-worker-${AZKARRA_VERSION}

.SILENT:

all: build-images clean-build

clean-containers:
	echo "Cleaning containers \n========================================== ";

clean-images:
	echo "Cleaning images \n========================================== ";
	for image in `docker images -qf "label=io.streamthoughts.docker.name"`; do \
	    echo "Removing image $${image} \n==========================================\n " ; \
        docker rmi -f $${image} || exit 1 ; \
    done

clean-build:
	echo "Cleaning build directory \n========================================== ";
	rm -rf ./docker-build;

build-images:
	echo "Building image \n========================================== ";
	echo "AZKARRA_VERSION="$(AZKARRA_VERSION)
	echo "GIT_COMMIT="$(GIT_COMMIT)
	echo "GIT_BRANCH="$(GIT_BRANCH)
	echo "==========================================\n "
	mkdir -p ./docker-build/classes && mkdir -p ./docker-build/dependencies && mkdir -p ./docker-build/resources;
	mvn clean package && \
	cp -r ${DIST_DIR}/bin ./docker-build/resources/ && \
	cp -r ${DIST_DIR}/etc ./docker-build/resources/ && \
	find ${DIST_DIR}/share/java/ -type f -regextype posix-egrep -not -iregex '^.*(?-worker)/(?azkarra-).*\.jar' -print0 | xargs -0 cp -t ./docker-build/dependencies && \
	find ${DIST_DIR}/share/java/ -type f -regextype posix-egrep -iregex '^.*(?-worker)/(?azkarra-).*\.jar' -print0 | xargs -0 cp -t ./docker-build/classes && \
	docker build \
	--build-arg azkarraVersion=${AZKARRA_VERSION} \
	--build-arg azkarraCommit=${GIT_COMMIT} \
	--build-arg azkarraBranch=${GIT_BRANCH} \
	-t ${REPOSITORY}/${IMAGE}:latest . || exit 1 ;
	docker tag ${REPOSITORY}/${IMAGE}:latest ${REPOSITORY}/azkarra-streams:${AZKARRA_VERSION} || exit 1 ;

clean: clean-containers clean-images clean-build
