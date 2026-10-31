#!/usr/bin/env bash

set -Eeuo pipefail

PROJECT_ROOT="${HOME}/projects/developer-content-creator"
MODULE_DIR="${PROJECT_ROOT}/backend/shared/event-contracts"

echo "============================================"
echo " Création du module event-contracts"
echo "============================================"

if [[ ! -f "${PROJECT_ROOT}/pom.xml" ]]; then
    echo "ERREUR : projet Maven introuvable : ${PROJECT_ROOT}"
    exit 1
fi

if [[ -e "${MODULE_DIR}" ]]; then
    echo "ERREUR : le répertoire existe déjà : ${MODULE_DIR}"
    echo "Aucun fichier n'a été modifié."
    exit 1
fi

mkdir -p \
    "${MODULE_DIR}/src/main/avro/common" \
    "${MODULE_DIR}/src/main/avro/repository" \
    "${MODULE_DIR}/src/main/avro/content" \
    "${MODULE_DIR}/src/main/avro/publication"

# --------------------------------------------------
# pom.xml
# --------------------------------------------------

cat > "${MODULE_DIR}/pom.xml" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.developercontentcreator</groupId>
        <artifactId>library-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../library-parent/pom.xml</relativePath>
    </parent>

    <artifactId>event-contracts</artifactId>
    <packaging>jar</packaging>

    <name>Event Contracts</name>
    <description>
        Centralized Avro event contracts shared between microservices.
    </description>

    <dependencies>
        <dependency>
            <groupId>org.apache.avro</groupId>
            <artifactId>avro</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.avro</groupId>
                <artifactId>avro-maven-plugin</artifactId>
                <version>${avro.version}</version>
                <executions>
                    <execution>
                        <id>generate-avro-sources</id>
                        <phase>generate-sources</phase>
                        <goals>
                            <goal>schema</goal>
                        </goals>
                        <configuration>
                            <sourceDirectory>
                                ${project.basedir}/src/main/avro
                            </sourceDirectory>
                            <outputDirectory>
                                ${project.build.directory}/generated-sources/avro
                            </outputDirectory>
                            <stringType>String</stringType>
                            <createSetters>true</createSetters>
                            <enableDecimalLogicalType>true</enableDecimalLogicalType>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

</project>
EOF

# --------------------------------------------------
# EventMetadata.avsc
# --------------------------------------------------

cat > "${MODULE_DIR}/src/main/avro/common/EventMetadata.avsc" <<'EOF'
{
  "type": "record",
  "name": "EventMetadata",
  "namespace": "io.developercontentcreator.contracts.common",
  "doc": "Common metadata for integration events.",
  "fields": [
    {
      "name": "eventId",
      "type": "string",
      "doc": "Unique event identifier."
    },
    {
      "name": "eventType",
      "type": "string",
      "doc": "Functional event type."
    },
    {
      "name": "occurredAt",
      "type": {
        "type": "long",
        "logicalType": "timestamp-millis"
      },
      "doc": "Event creation timestamp."
    },
    {
      "name": "producer",
      "type": "string",
      "doc": "Name of the producing service."
    },
    {
      "name": "subject",
      "type": "string",
      "doc": "Business resource identifier."
    },
    {
      "name": "correlationId",
      "type": "string",
      "doc": "Distributed correlation identifier."
    },
    {
      "name": "causationId",
      "type": ["null", "string"],
      "default": null,
      "doc": "Identifier of the event or command that caused this event."
    }
  ]
}
EOF

# --------------------------------------------------
# RepositoryAnalyzed.avsc
# --------------------------------------------------

cat > "${MODULE_DIR}/src/main/avro/repository/RepositoryAnalyzed.avsc" <<'EOF'
{
  "type": "record",
  "name": "RepositoryAnalyzed",
  "namespace": "io.developercontentcreator.contracts.repository",
  "doc": "Published when a GitHub repository analysis is completed.",
  "fields": [
    {
      "name": "metadata",
      "type": "io.developercontentcreator.contracts.common.EventMetadata"
    },
    {
      "name": "repositoryId",
      "type": "string"
    },
    {
      "name": "repositoryUrl",
      "type": "string"
    },
    {
      "name": "defaultBranch",
      "type": ["null", "string"],
      "default": null
    },
    {
      "name": "analysisId",
      "type": "string"
    },
    {
      "name": "analysisStatus",
      "type": "string"
    },
    {
      "name": "completedAt",
      "type": {
        "type": "long",
        "logicalType": "timestamp-millis"
      }
    }
  ]
}
EOF

# --------------------------------------------------
# ContentGenerated.avsc
# --------------------------------------------------

cat > "${MODULE_DIR}/src/main/avro/content/ContentGenerated.avsc" <<'EOF'
{
  "type": "record",
  "name": "ContentGenerated",
  "namespace": "io.developercontentcreator.contracts.content",
  "doc": "Published when technical content generation is completed.",
  "fields": [
    {
      "name": "metadata",
      "type": "io.developercontentcreator.contracts.common.EventMetadata"
    },
    {
      "name": "contentId",
      "type": "string"
    },
    {
      "name": "repositoryId",
      "type": "string"
    },
    {
      "name": "contentType",
      "type": "string"
    },
    {
      "name": "language",
      "type": "string"
    },
    {
      "name": "status",
      "type": "string"
    },
    {
      "name": "generatedAt",
      "type": {
        "type": "long",
        "logicalType": "timestamp-millis"
      }
    }
  ]
}
EOF

# --------------------------------------------------
# ContentPublished.avsc
# --------------------------------------------------

cat > "${MODULE_DIR}/src/main/avro/publication/ContentPublished.avsc" <<'EOF'
{
  "type": "record",
  "name": "ContentPublished",
  "namespace": "io.developercontentcreator.contracts.publication",
  "doc": "Published when content has been successfully published.",
  "fields": [
    {
      "name": "metadata",
      "type": "io.developercontentcreator.contracts.common.EventMetadata"
    },
    {
      "name": "publicationId",
      "type": "string"
    },
    {
      "name": "contentId",
      "type": "string"
    },
    {
      "name": "platform",
      "type": "string"
    },
    {
      "name": "externalPublicationId",
      "type": ["null", "string"],
      "default": null
    },
    {
      "name": "publishedAt",
      "type": {
        "type": "long",
        "logicalType": "timestamp-millis"
      }
    }
  ]
}
EOF

echo
echo "Module créé : ${MODULE_DIR}"
echo
echo "Fichiers générés :"
find "${MODULE_DIR}" -type f | sort

echo
echo "============================================"
echo " Création terminée"
echo "============================================"
