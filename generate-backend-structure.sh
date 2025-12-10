#!/bin/bash

BASE_DIR="assetman-backend/src/main/kotlin/com/github/mlwilli/assetman"
RES_DIR="assetman-backend/src/main/resources"

echo "Creating backend folder structure..."

# SHARED
mkdir -p $BASE_DIR/shared/config
mkdir -p $BASE_DIR/shared/security
mkdir -p $BASE_DIR/shared/domain
mkdir -p $BASE_DIR/shared/web

# IDENTITY (Users / Roles / Auth)
mkdir -p $BASE_DIR/identity/domain
mkdir -p $BASE_DIR/identity/repo
mkdir -p $BASE_DIR/identity/service
mkdir -p $BASE_DIR/identity/web

# ASSETS
mkdir -p $BASE_DIR/asset/domain
mkdir -p $BASE_DIR/asset/repo
mkdir -p $BASE_DIR/asset/service
mkdir -p $BASE_DIR/asset/web
mkdir -p $BASE_DIR/asset/graphql

# LOCATIONS
mkdir -p $BASE_DIR/location/domain
mkdir -p $BASE_DIR/location/repo
mkdir -p $BASE_DIR/location/service
mkdir -p $BASE_DIR/location/web

# PROPERTIES
mkdir -p $BASE_DIR/property/domain
mkdir -p $BASE_DIR/property/repo
mkdir -p $BASE_DIR/property/service
mkdir -p $BASE_DIR/property/web

# MAINTENANCE
mkdir -p $BASE_DIR/maintenance/domain
mkdir -p $BASE_DIR/maintenance/repo
mkdir -p $BASE_DIR/maintenance/service
mkdir -p $BASE_DIR/maintenance/web

# AUDIT
mkdir -p $BASE_DIR/audit/domain
mkdir -p $BASE_DIR/audit/repo
mkdir -p $BASE_DIR/audit/service
mkdir -p $BASE_DIR/audit/web

echo "Backend folder structure created successfully!"
