#!/bin/bash
set -euo pipefail

CERT_PASSWORD="${CERT_PASSWORD:-orbit-dev}"
CERTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_DIR="${CERTS_DIR}/generated"

echo "Creating certificates directory at ${OUTPUT_DIR}..."
rm -rf "${OUTPUT_DIR}"
mkdir -p "${OUTPUT_DIR}"

cd "${OUTPUT_DIR}"

echo "Generating Root CA..."
# Create CA private key
openssl genrsa -out ca.key 4096

# Create CA certificate
openssl req -x509 -new -nodes -key ca.key -sha256 -days 3650 -out ca.crt \
    -subj "/C=US/ST=State/L=City/O=Orbit/OU=IT/CN=Orbit Root CA" \
    -config "${CERTS_DIR}/openssl.cnf"

echo "Creating truststore.p12..."
# Try using keytool first (standard for Java/Spring Boot)
if command -v keytool >/dev/null 2>&1; then
    keytool -importcert -trustcacerts -noprompt -keystore truststore.p12 \
        -storepass "${CERT_PASSWORD}" -storetype PKCS12 -alias "orbit-ca" -file ca.crt
else
    # Fallback to openssl if keytool is not available
    openssl pkcs12 -export -nokeys -in ca.crt -out truststore.p12 -passout "pass:${CERT_PASSWORD}" -name "orbit-ca"
fi

SERVICES=("orbit-ingest" "orbit-processor" "orbit-orchestrator" "orbit-gateway")

for SERVICE in "${SERVICES[@]}"; do
    echo "Generating certificates for ${SERVICE}..."
    
    # Generate private key
    openssl genrsa -out "${SERVICE}.key" 2048
    
    # Create CSR configuration
    cat > "${SERVICE}.ext" <<EOF
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage = digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment
subjectAltName = @alt_names

[alt_names]
DNS.1 = ${SERVICE}
DNS.2 = localhost
DNS.3 = *.orbit.local
EOF
    
    # Generate CSR
    openssl req -new -key "${SERVICE}.key" -out "${SERVICE}.csr" \
        -subj "/C=US/ST=State/L=City/O=Orbit/OU=IT/CN=${SERVICE}" \
        -config "${CERTS_DIR}/openssl.cnf"
        
    # Sign CSR with CA
    openssl x509 -req -in "${SERVICE}.csr" -CA ca.crt -CAkey ca.key -CAcreateserial \
        -out "${SERVICE}.crt" -days 365 -sha256 -extfile "${SERVICE}.ext"
        
    # Create PKCS12 keystore
    openssl pkcs12 -export -in "${SERVICE}.crt" -inkey "${SERVICE}.key" \
        -certfile ca.crt -out "${SERVICE}.p12" \
        -name "${SERVICE}" -passout "pass:${CERT_PASSWORD}"
        
    echo "Done for ${SERVICE}"
done

echo "Cleaning up intermediate files..."
rm -f *.csr *.ext ca.srl

echo ""
echo "=========================================="
echo "Certificate Generation Summary"
echo "=========================================="
echo "Output Directory: ${OUTPUT_DIR}"
echo "Keystore / Truststore Password: ${CERT_PASSWORD}"
echo ""
echo "Files Generated:"
ls -lh *.p12 ca.crt ca.key
echo "=========================================="
