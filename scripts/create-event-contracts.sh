#!/usr/bin/env bash

set -Eeuo pipefail

# ==================================================
# Configuration
# ==================================================

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"

MODULE_DIR="${PROJECT_ROOT}/backend/shared/event-contracts"
SHARED_POM="${PROJECT_ROOT}/backend/shared/pom.xml"

MODULE_CREATED=false
COMPLETED=false
SHARED_POM_BACKED_UP=false
SHARED_POM_BACKUP=""

echo "============================================"
echo " Création du module event-contracts"
echo "============================================"

# ==================================================
# Validation préalable
# ==================================================

if [[ ! -f "${PROJECT_ROOT}/pom.xml" ]]; then
    echo "ERREUR : projet Maven introuvable."
    exit 1
fi

if [[ ! -f "${SHARED_POM}" ]]; then
    echo "ERREUR : agrégateur Maven introuvable : ${SHARED_POM}"
    exit 1
fi

if [[ -e "${MODULE_DIR}" ]]; then
    echo "ERREUR : le répertoire existe déjà : ${MODULE_DIR}"
    echo "Aucun fichier n'a été modifié."
    exit 1
fi

if grep -Fq '<module>event-contracts</module>' "${SHARED_POM}"; then
    echo "ERREUR : event-contracts est déjà déclaré dans shared/pom.xml."
    exit 1
fi

if ! grep -q '<modules>' "${SHARED_POM}" ||
   ! grep -q '</modules>' "${SHARED_POM}"; then
    echo "ERREUR : bloc <modules> absent dans shared/pom.xml."
    exit 1
fi

# Nettoyage du module si une erreur survient avant la fin.
cleanup() {
    local exit_code=$?

    if [[ "${exit_code}" -ne 0 && "${COMPLETED}" == false ]]; then
        echo
        echo "ERREUR : échec de création."

        if [[ "${MODULE_CREATED}" == true ]]; then
            echo "Nettoyage du module incomplet."
            rm -rf -- "${MODULE_DIR}"
        fi

        if [[ "${SHARED_POM_BACKED_UP}" == true ]]; then
            echo "Restauration de shared/pom.xml."
            cp -- "${SHARED_POM_BACKUP}" "${SHARED_POM}"
        fi
    fi

    if [[ -n "${SHARED_POM_BACKUP:-}" ]]; then
        rm -f -- "${SHARED_POM_BACKUP}"
    fi

    return "${exit_code}"
}

trap cleanup EXIT

# ==================================================
# Création de l'arborescence
# ==================================================

mkdir -p \
    "${MODULE_DIR}/src/main/avro/common" \
    "${MODULE_DIR}/src/main/avro/repository" \
    "${MODULE_DIR}/src/main/avro/content" \
    "${MODULE_DIR}/src/main/avro/publication"

MODULE_CREATED=true

# ==================================================
# pom.xml
# ==================================================

cat > "${MODULE_DIR}/pom.xml" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="
            http://maven.apache.org/POM/4.0.0
            https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.developercontentcreator</groupId>
        <artifactId>library-parent</artifactId>
        <version>${revision}</version>
        <relativePath>../../library-parent/pom.xml</relativePath>
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

                            <!--
                              Compile EventMetadata before the
                              event schemas that reference it.
                            -->
                            <imports>
                                <import>
                                    ${project.basedir}/src/main/avro/common/EventMetadata.avsc
                                </import>
                            </imports>

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

# ==================================================
# EventMetadata.avsc
#
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

# ==================================================
# Enregistrement dans l'agrégateur shared
# ==================================================

# Sauvegarde de l'agrégateur avant modification.
SHARED_POM_BACKUP="$(mktemp)"
cp -- "${SHARED_POM}" "${SHARED_POM_BACKUP}"
SHARED_POM_BACKED_UP=true

python3 - "${SHARED_POM}" <<'PYXML'
from pathlib import Path
import os
import sys
import tempfile
import xml.etree.ElementTree as ET

pom = Path(sys.argv[1])

ET.register_namespace("", "http://maven.apache.org/POM/4.0.0")
tree = ET.parse(pom)
root = tree.getroot()

namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
modules = root.find("m:modules", namespace)

if modules is None:
    raise SystemExit("ERREUR : bloc <modules> introuvable.")

if any((module.text or "").strip() == "event-contracts"
       for module in modules.findall("m:module", namespace)):
    raise SystemExit("ERREUR : event-contracts est déjà déclaré.")

module = ET.Element("{http://maven.apache.org/POM/4.0.0}module")
module.text = "event-contracts"
modules.append(module)

fd, temporary_path = tempfile.mkstemp(
    prefix=f".{pom.name}.",
    suffix=".tmp",
    dir=pom.parent
)

try:
    with os.fdopen(fd, "wb") as output:
        tree.write(output, encoding="UTF-8", xml_declaration=True)
        output.write(b"\n")

    ET.parse(temporary_path)
    os.replace(temporary_path, pom)
except Exception:
    if os.path.exists(temporary_path):
        os.unlink(temporary_path)
    raise

print("Module event-contracts enregistré dans shared/pom.xml.")
PYXML
# Toutes les opérations sont terminées.
COMPLETED=true

echo
echo "Module créé : ${MODULE_DIR}"
echo
echo "Fichiers générés :"
find "${MODULE_DIR}" -type f | sort

echo
echo "============================================"
echo " Création terminée"
echo "============================================"
