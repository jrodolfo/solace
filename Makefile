SHELL := /bin/bash

.PHONY: help build-api build-ui build-subscriber build-all start-api start-ui start-subscriber start-all status-all test-api test-ui test-subscriber test-scripts test

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
	@echo "  make status-all        - show local status for api, ui, and subscriber"
	@echo "  make test-api          - run broker api tests"
	@echo "  make test-ui           - run publisher ui tests"
	@echo "  make test-subscriber   - run subscriber tests"
	@echo "  make test-scripts      - run root script smoke tests"
	@echo "  make test              - run all test targets"

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

status-all:
	@./scripts/status-all.sh

test-api:
	@cd solace-broker-api && mvn test

test-ui:
	@cd solace-publisher-ui && npm test -- --run

test-subscriber:
	@cd solace-subscriber && mvn test

test-scripts:
	@./scripts/test-scripts.sh

test: test-api test-ui test-subscriber test-scripts
