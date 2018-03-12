package cloud.cinder.cindercloud.wallet.service;

import cloud.cinder.cindercloud.event.domain.Event;
import cloud.cinder.cindercloud.event.domain.EventType;
import cloud.cinder.cindercloud.wallet.domain.GeneratedCredentials;
import cloud.cinder.cindercloud.wallet.domain.PrivateKey;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.web3j.crypto.*;

import java.io.IOException;
import java.util.Objects;

@Service
public class WalletService {

    private static final ObjectMapper walletmapper = new ObjectMapper();
    private ApplicationEventPublisher $;

    public WalletService(final ApplicationEventPublisher _$) {
        this.$ = _$;
    }

    static {
        walletmapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        walletmapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public Credentials login(final String privateKey) {
        validatePrivateKey(privateKey);
        try {
            return Credentials.create(privateKey);
        } catch (final Exception exc) {
            throw new IllegalArgumentException("The private key you provided was not valid");
        }
    }

    public String web3Login(final String address) {
        validateAddress(address);
        return address;
    }

    private void validateAddress(final String address) {
        if (address == null || (address.length() != 40 && address.length() != 42)) {
            throw new IllegalArgumentException("The address you provided is not valid");
        }
    }

    public Credentials login(final String password, final String wallet) {
        try {
            final WalletFile walletFile = walletmapper.readValue(wallet, WalletFile.class);
            return Credentials.create(Wallet.decrypt(password, walletFile));
        } catch (final IOException io) {
            throw new IllegalArgumentException("The keystore you provided is not valid");
        } catch (final CipherException cip) {
            throw new IllegalArgumentException("Unable to decrypt wallet. Is your password correct?");
        }
    }

    public GeneratedCredentials generateWallet(final String password, final boolean strong) {
        try {
            Objects.requireNonNull(password);
            final ECKeyPair ecKeyPair = Keys.createEcKeyPair();
            final WalletFile walletFile = strong ? Wallet.createStandard(password, ecKeyPair) : Wallet.createLight(password, ecKeyPair);
            $.publishEvent(
                    Event.builder()
                            .message(walletFile.getAddress())
                            .type(EventType.WALLET_CREATED)
                            .build(
                            ));
            return new GeneratedCredentials(walletmapper.writeValueAsString(walletFile), new PrivateKey(ecKeyPair.getPrivateKey()));
        } catch (final Exception ex) {
            throw new IllegalArgumentException("Unable to generate wallet at this point");
        }
    }


    private void validatePrivateKey(final String privateKey) {
        if (privateKey == null || (privateKey.length() != 64 && privateKey.length() != 66)) {
            throw new IllegalArgumentException("The private key you provided is not valid");
        }
    }
}
