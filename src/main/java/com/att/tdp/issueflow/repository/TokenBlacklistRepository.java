package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.entity.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, Long> {

    boolean existsByTokenJti(String tokenJti);

    void deleteByExpiryBefore(Instant now);
}
