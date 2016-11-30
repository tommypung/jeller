package org.svearike.jeller.acme;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.shredzone.acme4j.Authorization;
import org.shredzone.acme4j.Certificate;
import org.shredzone.acme4j.Registration;
import org.shredzone.acme4j.RegistrationBuilder;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.Status;
import org.shredzone.acme4j.challenge.Challenge;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeConflictException;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.exception.AcmeUnauthorizedException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.CertificateUtils;
import org.shredzone.acme4j.util.KeyPairUtils;

public class LetsEncrypt
{
	private static final File USER_KEY_FILE = new File("user.key");
	private static final File DOMAIN_KEY_FILE = new File("domain.key");
	private static final File DOMAIN_CERT_FILE = new File("domain.crt");
	private static final File CERT_CHAIN_FILE = new File("chain.crt");
	private static final File DOMAIN_CSR_FILE = new File("domain.csr");
	private static final String SERVER_URI = "acme://letsencrypt.org";

	private static final int KEY_SIZE = 2048;

	private static final Logger LOG = Logger.getLogger(LetsEncrypt.class.getName());

	/**
	 * Generates a certificate for the given domains. Also takes care for the registration
	 * process.
	 *
	 * @param domains
	 *            Domains to get a common certificate for
	 * @throws KeyStoreException 
	 * @throws CertificateException 
	 * @throws NoSuchAlgorithmException 
	 */
	public void fetchCertificate(String keystorePath, String keystorePassword, Collection<String> domains) throws IOException, AcmeException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
		// Load or create a key pair for the user's account
		boolean createdNewKeyPair = false;

		KeyPair userKeyPair;
		if (USER_KEY_FILE.exists()) {
			try (FileReader fr = new FileReader(USER_KEY_FILE)) {
				userKeyPair = KeyPairUtils.readKeyPair(fr);
			}
		} else {
			userKeyPair = KeyPairUtils.createKeyPair(KEY_SIZE);
			try (FileWriter fw = new FileWriter(USER_KEY_FILE)) {
				KeyPairUtils.writeKeyPair(userKeyPair, fw);
			}
			createdNewKeyPair = true;
		}

		// Create a session for Let's Encrypt
		// Use "acme://letsencrypt.org" for production server
		Session session = new Session(SERVER_URI, userKeyPair);

		// Register a new user
		Registration reg = null;
		try {
			reg = new RegistrationBuilder().create(session);
			LOG.info("Registered a new user, URI: " + reg.getLocation());
		} catch (AcmeConflictException ex) {
			reg = Registration.bind(session, ex.getLocation());
			LOG.info("Account does already exist, URI: " + reg.getLocation());
		}

		URI agreement = reg.getAgreement();
		LOG.info("Terms of Service: " + agreement);

		if (createdNewKeyPair)
			acceptAgreement(reg, agreement);

		for (String domain : domains) {
			// Create a new authorization
			Authorization auth = null;
			try {
				auth = reg.authorizeDomain(domain);
			} catch (AcmeUnauthorizedException ex) {
				// Maybe there are new T&C to accept?
				acceptAgreement(reg, agreement);
				// Then try again...
				auth = reg.authorizeDomain(domain);
			}
			LOG.info("New authorization for domain " + domain);

			// Uncomment a challenge...
			Challenge challenge = httpChallenge(auth, domain);

			if (challenge == null) {
				LOG.severe("Challenge was null");
				return;
			}

			// Trigger the challenge
			challenge.trigger();

			// Poll for the challenge to complete
			int attempts = 10;
			while (challenge.getStatus() != Status.VALID && attempts-- > 0) {
				if (challenge.getStatus() == Status.INVALID) {
					LOG.severe("Challenge failed (Status.INVALID)... Giving up.");
					return;
				}
				try {
					Thread.sleep(3000L);
				} catch (InterruptedException ex) {
					LOG.log(Level.SEVERE, "interrupted", ex);
				}
				challenge.update();
			}
			if (challenge.getStatus() != Status.VALID) {
				LOG.severe("Failed to pass the challenge... Giving up.");
				return;
			}
		}

		// Load or create a key pair for the domain
		KeyPair domainKeyPair;
		if (DOMAIN_KEY_FILE.exists()) {
			try (FileReader fr = new FileReader(DOMAIN_KEY_FILE)) {
				domainKeyPair = KeyPairUtils.readKeyPair(fr);
			}
		} else {
			domainKeyPair = KeyPairUtils.createKeyPair(KEY_SIZE);
			try (FileWriter fw = new FileWriter(DOMAIN_KEY_FILE)) {
				KeyPairUtils.writeKeyPair(domainKeyPair, fw);
			}
		}

		// Generate a CSR for the domain
		CSRBuilder csrb = new CSRBuilder();
		csrb.addDomains(domains);
		csrb.sign(domainKeyPair);

		try (Writer out = new FileWriter(DOMAIN_CSR_FILE)) {
			csrb.write(out);
		}

		// Request a signed certificate
		Certificate certificate = reg.requestCertificate(csrb.getEncoded());
		LOG.info("Success! The certificate for domains " + domains + " has been generated!");
		LOG.info("Certificate URI: " + certificate.getLocation());

		// Download the certificate
		X509Certificate cert = certificate.download();
		X509Certificate[] chain = certificate.downloadChain();

		// Write certificate only (e.g. for Apache's SSLCertificateFile)
		try (FileWriter fw = new FileWriter(DOMAIN_CERT_FILE)) {
			CertificateUtils.writeX509Certificate(cert, fw);
		}

		// Write chain only (e.g. for Apache's SSLCertificateChainFile)
		try (FileWriter fw = new FileWriter(CERT_CHAIN_FILE)) {
			CertificateUtils.writeX509CertificateChain(chain, fw);
		}
	
		// Create a Keystore
		KeyStore ks = KeyStore.getInstance("jks");

		char[] password = keystorePassword.toCharArray();
		ks.load(null, password);

		X509Certificate[] chains = new X509Certificate[chain.length + 1];
		chains[0] = cert;
		for(int i=0;i<chain.length;i++)
			chains[i + 1] = chain[i];
		ks.setKeyEntry("jetty", domainKeyPair.getPrivate(), password, chains);
		ks.setKeyEntry("server", domainKeyPair.getPrivate(), password, chain);

		// Store away the keystore.
		FileOutputStream fos = new FileOutputStream(keystorePath);
		ks.store(fos, password);
		fos.close();
	}

	/**
	 * Prepares HTTP challenge.
	 */
	public Challenge httpChallenge(Authorization auth, String domain) throws AcmeException {
		// Find a single http-01 challenge
		Http01Challenge challenge = auth.findChallenge(Http01Challenge.TYPE);
		if (challenge == null) {
			LOG.severe("Found no " + Http01Challenge.TYPE + " challenge, don't know what to do...");
			return null;
		}

		AcmeServlet.addChallenge(challenge.getToken(), challenge.getAuthorization());

		return challenge;
	}

	public boolean acceptAgreement(Registration reg, URI agreement) throws AcmeException {
		reg.modify().setAgreement(agreement).commit();
		return true;
	}
}
