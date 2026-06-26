SHELL := /bin/bash

.PHONY: help build-api build-ui build-subscriber build-all start-api start-ui start-subscriber start-all stop-all restart-all status-all docker-build-all docker-start docker-stop docker-status docker-restart docker-logs docker-scan dependency-freshness test-api test-ui test-subscriber test-scripts test release-check

help:
	@echo "available targets:"
	@echo "  make build-api         - build solace-broker-api"
	@echo "  make build-ui          - build solace-publisher-ui"
	@echo "  make build-subscriber  - build solace-subscriber"
	@echo "  make build-all         - build all three modules"
	@echo "  make start-api         - start solace-broker-api"
	@echo "  make start-ui          - start solace-publisher-ui"
	@echo "  make start-subscriber  - build and start solace-subscriber"
	@echo "  make start-all         - start api, ui, and subscriber together"
	@echo "  make stop-all          - stop api, ui, and subscriber when running"
	@echo "  make restart-all       - stop, build, and start the whole workspace"
	@echo "  make status-all        - show local status for api, ui, and subscriber"
	@echo "  make docker-build-all  - build Docker runtime images"
	@echo "  make docker-start      - prepare and start the full Docker runtime"
	@echo "  make docker-stop       - stop the full Docker runtime"
	@echo "  make docker-status     - show full Docker runtime status"
	@echo "  make docker-restart    - restart the full Docker runtime"
	@echo "  make docker-logs       - follow full Docker runtime logs"
	@echo "  make docker-scan       - scan Docker runtime images with Trivy"
	@echo "  make dependency-freshness - report available Maven, npm, and Docker image updates"
	@echo "  make test-api          - run broker api tests"
	@echo "  make test-ui           - run publisher ui tests"
	@echo "  make test-subscriber   - run subscriber tests"
	@echo "  make test-scripts      - run root script smoke tests"
	@echo "  make test              - run all test targets"
	@echo "  make release-check     - run the full pre-release validation gate"

build-api:
	@./scripts/build-broker-api.sh

build-ui:
	@./scripts/build-publisher-ui.sh

build-subscriber:
	@./scripts/build-subscriber.sh

build-all:
	@./scripts/build-all.sh

start-api:
	@./scripts/start-broker-api.sh

start-ui:
	@./scripts/start-publisher-ui.sh

start-subscriber:
	@./scripts/start-subscriber.sh

start-all:
	@./scripts/start-all.sh

stop-all:
	@./scripts/stop-all.sh

restart-all:
	@./scripts/restart-all.sh

status-all:
	@./scripts/status-all.sh

docker-build-all:
	@./scripts/docker-build-all.sh

docker-start:
	@./scripts/docker-start.sh

docker-stop:
	@./scripts/docker-stop.sh

docker-status:
	@./scripts/docker-status.sh

docker-restart:
	@./scripts/docker-restart.sh

docker-logs:
	@./scripts/docker-logs.sh

docker-scan:
	@./scripts/docker-scan.sh

dependency-freshness:
	@./scripts/dependency-freshness.sh

test-api:
	@cd solace-broker-api && mvn test

test-ui:
	@cd solace-publisher-ui && npm test -- --run

test-subscriber:
	@cd solace-subscriber && mvn test

test-scripts:
	@./scripts/test-scripts.sh

test: test-api test-ui test-subscriber test-scripts

release-check:
	@./scripts/release-check.sh
