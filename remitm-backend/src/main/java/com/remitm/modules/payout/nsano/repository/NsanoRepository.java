package com.remitm.modules.payout.nsano.repository;

import com.remitm.modules.payout.nsano.entity.NsanoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NsanoRepository extends JpaRepository<NsanoEntity, Long> {

    /** Latest NSANO record for one of our transaction reference numbers. */
    Optional<NsanoEntity> findByTransactionId(String transactionId);

    /** Correlate an inbound NSANO callback / status poll back to a record. */
    Optional<NsanoEntity> findByNsanoTransactionId(String nsanoTransactionId);

    /**
     * Records that are not yet in a terminal state and have an NSANO id we can poll.
     * Used by the status-poll scheduler.
     */
    List<NsanoEntity> findByStatusNotInAndNsanoTransactionIdIsNotNull(List<String> statuses);
}
