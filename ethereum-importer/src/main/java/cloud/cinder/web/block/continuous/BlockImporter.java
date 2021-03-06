package cloud.cinder.web.block.continuous;

import cloud.cinder.ethereum.block.domain.BlockImportJob;
import cloud.cinder.web.block.continuous.repository.BlockImportJobRepository;
import cloud.cinder.ethereum.block.domain.Block;
import cloud.cinder.web.block.repository.BlockRepository;
import cloud.cinder.web.block.service.BlockService;
import cloud.cinder.ethereum.web3j.Web3jGateway;
import io.reactivex.disposables.Disposable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.response.EthBlock;
import rx.Subscription;

import java.util.Date;
import java.util.Objects;

@Component
@Slf4j
@EnableScheduling
public class BlockImporter {

    @Autowired
    private Web3jGateway web3j;
    @Autowired
    private BlockService blockService;
    @Autowired
    private BlockRepository blockRepository;
    @Autowired
    private BlockImportJobRepository blockImportJobRepository;

    @Value("${cloud.cinder.ethereum.live-block-import:false}")
    private boolean autoBlockImport;


    private Disposable liveSubscription;

    @Scheduled(fixedRate = 60000)
    public void listenToBlocks() {
        if (autoBlockImport) {
            if (this.liveSubscription == null) {
                this.liveSubscription = subscribe();
            } else {
                final Disposable newSubscription = subscribe();
                this.liveSubscription.dispose();
                this.liveSubscription = newSubscription;
            }
        }
    }

    private Disposable subscribe() {
        return web3j.web3j().blockFlowable(false)
                .map(EthBlock::getBlock)
                .filter(Objects::nonNull)
                .map(Block::asBlock)
                .subscribe(block -> {
                    try {
                        log.trace("received live block");
                        blockService.save(block);
                    } catch (final Exception exc) {
                        log.error("unable to save block {}", block.getHeight());
                        log.error("error: ", exc);
                    }
                }, onError -> {
                    log.debug("Error while looking for new blocks", onError);
                    this.liveSubscription.dispose();
                    this.liveSubscription = subscribe();
                });
    }

    void execute(final BlockImportJob job) {
        log.debug("Starting job {}", job.getId());
        job.setActive(true);
        job.setStartTime(new Date());
        blockImportJobRepository.save(job);

        for (long i = job.getFromBlock(); i <= job.getToBlock(); i++) {
            if (i % 25 == 0) {
                log.debug("Historic Import: trying to import block: {}", i);
            }
            try {
                final EthBlock ethBlock = web3j.web3j().ethGetBlockByNumber(new DefaultBlockParameterNumber(i), false)
                        .flowable().blockingFirst(null);
                if (ethBlock != null && ethBlock.getBlock() != null && !blockRepository.findById(ethBlock.getBlock().getHash()).isPresent()) {
                    try {
                        blockService.save(Block.asBlock(ethBlock.getBlock()));
                    } catch (final Exception exc) {
                        log.debug("unable to save block", exc);
                    }
                } else {
                    log.trace("couldn't find {} in web3 or already imported", i);
                }
            } catch (final Exception exc) {
                log.debug("unable to get block");
            }
        }

        job.setActive(false);
        job.setEndTime(new Date());
        blockImportJobRepository.save(job);
    }
}