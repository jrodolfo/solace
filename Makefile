SHELL := /bin/bash

.PHONY: help start-api start-ui start-subscriber start-all test-api test-ui test-subscriber test

help:
	@echo "available targets:"
	@echo "  make start-api         - start solace-broker-api"
	@echo "  make start-ui          - start solace-publisher-ui"
	@echo "  make start-subscriber  - build and start solace-subscriber"
	@echo "  make start-all         - start api, ui, and subscriber together"
	@echo "  make test-api          - run broker api tests"
	@echo "  make test-ui           - run publisher ui tests"
	@echo "  make test-subscriber   - run subscriber tests"
	@echo "  make test              - run all test targets"

start-api:
	@./scripts/start-broker-api.sh

start-ui:
	@./scripts/start-publisher-ui.sh

start-subscriber:
	@./scripts/start-subscriber.sh

start-all:
	@./scripts/start-all.sh

test-api:
	@cd solace-broker-api && mvn test

test-ui:
	@cd solace-publisher-ui && npm test -- --run

test-subscriber:
	@cd solace-subscriber && mvn test

test: test-api test-ui test-subscriber
