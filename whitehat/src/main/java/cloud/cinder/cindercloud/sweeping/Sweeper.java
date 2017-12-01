package cloud.cinder.cindercloud.sweeping;

import cloud.cinder.cindercloud.utils.WeiUtils;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.utils.Numeric;
import rx.functions.Action1;

import java.math.BigInteger;
import java.util.Objects;

@Component
@Slf4j
public class Sweeper {

    private static final BigInteger GAS_PRICE = BigInteger.valueOf(2000000000L);
    private static final BigInteger ETHER_TRANSACTION_GAS_LIMIT = BigInteger.valueOf(21000L);
    public static final BigInteger GAS_COST = GAS_PRICE.add(ETHER_TRANSACTION_GAS_LIMIT);


    @Autowired
    private Web3j web3j;

    @Value("${cloud.cinder.whitehat.address}")
    private String whitehatAddress;

    public void sweep(final String privateKey) {
        final ECKeyPair keypair = ECKeyPair.create(Numeric.decodeQuantity(privateKey.trim()));
        final String address = Keys.getAddress(keypair);

        web3j.ethGetBalance(prettifyAddress(address), DefaultBlockParameterName.LATEST).observable()
                .filter(Objects::nonNull)
                .subscribe(balanceFetched(keypair));
    }

    private Action1<EthGetBalance> balanceFetched(final ECKeyPair keyPair) {
        return balance -> {
            if (balance.getBalance().longValue() != 0L) {
                log.debug("[Sweeper] {} has a balance of about {}", Keys.getAddress(keyPair), WeiUtils.format(balance.getBalance()));

                //if balance is more than gasCost
                if (balance.getBalance().compareTo(GAS_COST) >= 0) {
                    final EthGetTransactionCount transactionCount = calculateNonce(keyPair);

                    if (transactionCount != null) {
                        final RawTransaction etherTransaction = RawTransaction.createEtherTransaction(
                                transactionCount.getTransactionCount(),
                                GAS_PRICE,
                                ETHER_TRANSACTION_GAS_LIMIT,
                                whitehatAddress,
                                balance.getBalance().subtract(GAS_COST)
                        );


                        final byte[] signedMessage = sign(keyPair, etherTransaction);
                        final String signedMessageAsHex = Hex.toHexString(signedMessage);
                        web3j.ethSendRawTransaction(signedMessageAsHex).observable()
                                .subscribe(x -> log.debug("Sent transaction with hash: {}", x.getTransactionHash()));
                    } else {
                        log.error("Noncecount returned an error for {}", Keys.getAddress(keyPair));
                    }
                } else {
                    log.debug("[Sweeper] but the balance is lower than the cost to transfer it");
                }

            }
        };
    }

    private byte[] sign(final ECKeyPair keyPair, final RawTransaction etherTransaction) {
        return TransactionEncoder.signMessage(etherTransaction, Credentials.create(keyPair));
    }

    private EthGetTransactionCount calculateNonce(final ECKeyPair keyPair) {
        return web3j.ethGetTransactionCount(
                Keys.getAddress(keyPair),
                DefaultBlockParameterName.LATEST
        ).observable().toBlocking().first();
    }

    private String prettifyAddress(final String address) {
        if (!address.startsWith("0x")) {
            return String.format("0x%s", address);
        } else {
            return address;
        }
    }

}
