#!/bin/bash

# Spring prefers to access server certificates from a keystore.
# We also need a CA cert, so we use openssl.
# Copy the CA cert into src/main/resources, where Docker will read it to add to the Java trust store.
# Copy the server cert and key (unencrypted) into dknode/defkon to secure the Node.js app.
# These are also bind-mounted into the Keycloak container.

certdir=$(dirname $0)/.certs
mkdir -p $certdir
rm -fv $certdir/{ca-key.pem,cakey.pem,ca.pem,server-cert.pem,server-key.pem} dknode/defkon/{cert,key}.pem

openssl req -x509 -sha256 -days 3650 -newkey rsa:4096 -keyout $certdir/ca-key.pem -out $certdir/ca.pem
openssl pkey -in $certdir/ca-key.pem -out $certdir/cakey.pem

openssl req -new -newkey rsa:4096 -keyout $certdir/server-key.pem -out $certdir/server.csr

cat > $certdir/extfile.cnf <<EOF
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
subjectAltName = DNS:defkoi.jit.com,DNS:defkon.jit.com,DNS:keycloak.jit.com
extendedKeyUsage = serverAuth
EOF

openssl x509 -req -days 3650 -sha256 -in $certdir/server.csr -CA $certdir/ca.pem -CAkey $certdir/ca-key.pem \
  -CAcreateserial -out $certdir/server-cert.pem -extfile $certdir/extfile.cnf
rm -fv $certdir/server.csr

keytool -delete -cacerts -alias DKRootCA -storepass changeit
echo yes |keytool -import -cacerts -file $certdir/ca.pem -alias DKRootCA -storepass changeit

openssl pkcs12 -export -out dkrest/src/main/resources/server.p12 -inkey $certdir/server-key.pem -in $certdir/server-cert.pem
cp $certdir/ca.pem dkrest/src/main/resources/

cp $certdir/server-cert.pem dknode/defkon/cert.pem
chmod -v a+r dknode/defkon/cert.pem
openssl pkey -in $certdir/server-key.pem -out dknode/defkon/key.pem
chmod -v a+r dknode/defkon/key.pem

