package cloud.cinder.cindercloud.transaction.service;

import cloud.cinder.cindercloud.transaction.model.Transaction;
import cloud.cinder.cindercloud.transaction.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.protocol.Web3j;
import rx.Observable;

@Service
public class TransactionService {

    @Autowired
    private Web3j web3j;
    @Autowired
    private TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public Observable<Transaction> findByAddress(final String address) {
        return Observable.from(() -> transactionRepository.findByAddressFromOrTo(address).iterator());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction save(final Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public Observable<Transaction> getTransactionsForBlock(final String blockHash) {
        return Observable.from(transactionRepository.findAllByBlockHash(blockHash));
    }

    @Transactional
    public Observable<Transaction> getTransaction(final String transactionHash) {
        return transactionRepository.findOne(transactionHash)
                .map(Observable::just)
                .orElse(getInternalTransaction(transactionHash));
    }

    private Observable<Transaction> getInternalTransaction(final String transactionHash) {
        try {
            return web3j.ethGetTransactionByHash(transactionHash)
                    .observable()
                    .filter(x -> x.getTransaction().isPresent())
                    .map(transaction -> transaction.getTransaction().get())
                    .map(tx ->
                            Transaction.builder()
                                    .blockHash(tx.getBlockHash())
                                    .fromAddress(tx.getFrom())
                                    .gas(tx.getGas())
                                    .hash(tx.getHash())
                                    .input(tx.getInput())
                                    .toAddress(tx.getTo())
                                    .value(tx.getValue())
                                    .gasPrice(tx.getGasPrice())
                                    .creates(tx.getCreates())
                                    .s(tx.getS())
                                    .r(tx.getR())
                                    .v(tx.getV())
                                    .nonce(tx.getNonce())
                                    .transactionIndex(tx.getTransactionIndex())
                                    .build())
                    .map(tx -> transactionRepository.save(tx));
        } catch (Exception ex) {
            return Observable.error(ex);
        }
    }

}
