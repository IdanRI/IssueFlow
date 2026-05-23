package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.entity.CommentMention;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentMentionRepository extends JpaRepository<CommentMention, Long> {

    List<CommentMention> findByCommentId(Long commentId);

    Page<CommentMention> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    void deleteByCommentId(Long commentId);
}
