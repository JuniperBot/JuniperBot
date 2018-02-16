/*
 * This file is part of JuniperBotJ.
 *
 * JuniperBotJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBotJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBotJ. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.caramel.juniperbot.web.service;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.shredzone.acme4j.*;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeConflictException;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.CertificateUtils;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.web.utils.TomcatUtils;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.URI;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AcmeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcmeService.class);

    private static final int KEY_SIZE = 2048;

    @Value("${acme.enabled:false}")
    private boolean enabled;

    @Value("${acme.application.domain:}")
    private String applicationDomain;

    @Value("${acme.application.port:}")
    private Integer applicationPort;

    @Value("${acme.contact.email:}")
    private String contactEmail;

    @Value("${acme.endpoint:acme://letsencrypt.org/staging}")
    private String serverUri;

    @Value("${acme.user.key:user.key}")
    private String userKey;

    @Value("${acme.domain.key:domain.key}")
    private String domainKey;

    @Value("${acme.domain.csr:domain.csr}")
    private String domainCsr;

    @Value("${acme.domain-chain.crt:domain-chain.crt}")
    private String domainChainCsr;

    @Getter
    private Map<String, String> tokens = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (enabled && StringUtils.isNotEmpty(applicationDomain)) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Scheduled(cron = "0 0 0 1 1/2 ?")
    public void fetch() throws IOException, AcmeException {
        if (!enabled || StringUtils.isEmpty(applicationDomain)) {
            return;
        }
        try {
            KeyPair userKeyPair = loadOrCreateKeyPair(userKey);
            Session session = new Session(serverUri, userKeyPair);
            Registration reg = findOrRegisterAccount(session);
            authorize(reg, applicationDomain);

            KeyPair domainKeyPair = loadOrCreateKeyPair(domainKey);

            // Generate a CSR for all of the domains, and sign it with the domain key pair.
            CSRBuilder csrb = new CSRBuilder();
            csrb.addDomains(applicationDomain);
            csrb.sign(domainKeyPair);

            // Write the CSR to a file, for later use.
            try (Writer out = new FileWriter(domainCsr)) {
                csrb.write(out);
            }

            // Now request a signed certificate.
            Certificate certificate = reg.requestCertificate(csrb.getEncoded());

            LOGGER.info("Success! The certificate for domain {} has been generated! Certificate URL: {}",
                    applicationDomain, certificate.getLocation());

            // Download the leaf certificate and certificate chain.
            X509Certificate cert = certificate.download();
            X509Certificate[] chain = certificate.downloadChain();

            // Write a combined file containing the certificate and chain.
            try (FileWriter fw = new FileWriter(domainChainCsr)) {
                CertificateUtils.writeX509CertificateChain(fw, cert, chain);
            }
            if (applicationPort != null) {
                if (!TomcatUtils.restartConnector(applicationPort)) {
                    LOGGER.warn("Could not restart http connector. SSL may not be updated correctly!");
                }
            }
        } catch (Exception e) {
            LOGGER.error("Could not fetch new certificate", e);
            throw e;
        }
    }

    /**
     * Finds your {@link Registration} at the ACME server. It will be found by your user's
     * public key. If your key is not known to the server yet, a new registration will be
     * created.
     * <p>
     * This is a simple way of finding your {@link Registration}. A better way is to get
     * the URL of your new registration with {@link Registration#getLocation()} and store
     * it somewhere.
     *
     * @param session {@link Session} to bind with
     * @return {@link Registration} connected to your account
     */
    private Registration findOrRegisterAccount(Session session) throws AcmeException {
        Registration reg;
        try {
            RegistrationBuilder builder = new RegistrationBuilder();
            if (StringUtils.isNotEmpty(contactEmail)) {
                builder.addContact(contactEmail);
            }
            reg = builder.create(session);
            LOGGER.info("Registered a new user, URL: " + reg.getLocation());
            URI agreement = reg.getAgreement();
            reg.modify().setAgreement(agreement).commit();
        } catch (AcmeConflictException e) {
            reg = Registration.bind(session, e.getLocation());
            LOGGER.info("Account does already exist, URL: " + reg.getLocation());
        }
        return reg;
    }

    /**
     * Authorize a domain. It will be associated with your account, so you will be able to
     * retrieve a signed certificate for the domain later.
     * <p>
     * You need separate authorizations for subdomains (e.g. "www" subdomain). Wildcard
     * certificates are not currently supported.
     *
     * @param reg    {@link Registration} of your account
     * @param domain Name of the domain to authorize
     */
    private void authorize(Registration reg, String domain) throws AcmeException {
        LOGGER.info("Authorization for domain " + domain);
        Authorization auth = reg.authorizeDomain(domain);
        Http01Challenge challenge = auth.findChallenge(Http01Challenge.TYPE);
        if (challenge == null) {
            throw new AcmeException("No challenge found");
        }
        if (challenge.getStatus() == Status.VALID) {
            return;
        }

        try {
            tokens.put(challenge.getToken(), challenge.getAuthorization());
            challenge.trigger();
            int attempts = 20;
            while (challenge.getStatus() != Status.VALID && attempts-- > 0) {
                if (challenge.getStatus() == Status.INVALID) {
                    throw new AcmeException("Challenge failed... Giving up.");
                }
                Thread.sleep(3000L);
                challenge.update();
            }
        } catch (InterruptedException e) {
            LOGGER.error("Challenge interrupted", e);
            Thread.currentThread().interrupt();
        } finally {
            tokens.remove(challenge.getToken());
        }
        if (challenge.getStatus() != Status.VALID) {
            throw new AcmeException("Failed to pass the challenge for domain " + domain + ", ... Giving up.");
        }
    }

    /**
     * Loads a key pair from specified file. If the file does not exist,
     * a new key pair is generated and saved.
     *
     * @return {@link KeyPair}.
     */
    private static KeyPair loadOrCreateKeyPair(String fileName) throws IOException {
        File file = new File(fileName);
        if (file.exists()) {
            try (FileReader fr = new FileReader(file)) {
                return KeyPairUtils.readKeyPair(fr);
            }
        } else {
            KeyPair domainKeyPair = KeyPairUtils.createKeyPair(KEY_SIZE);
            try (FileWriter fw = new FileWriter(file)) {
                KeyPairUtils.writeKeyPair(domainKeyPair, fw);
            }
            return domainKeyPair;
        }
    }
}
