package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.response.CommentResponse;
import com.att.tdp.issueflow.dto.response.MentionedUserResponse;
import com.att.tdp.issueflow.dto.response.PaginatedMentionResponse;
import com.att.tdp.issueflow.entity.Comment;
import com.att.tdp.issueflow.entity.CommentMention;
import com.att.tdp.issueflow.entity.User;
import com.att.tdp.issueflow.repository.CommentMentionRepository;
import com.att.tdp.issueflow.repository.CommentRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MentionService {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

    private final CommentMentionRepository commentMentionRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public void syncMentions(Long commentId, String content) {
        commentMentionRepository.deleteByCommentId(commentId);

        Set<String> usernames = parseMentions(content);
        List<User> mentionedUsers = resolveUsers(usernames);

        for (User user : mentionedUsers) {
            CommentMention mention = CommentMention.builder()
                    .commentId(commentId)
                    .userId(user.getId())
                    .build();
            commentMentionRepository.save(mention);
        }
    }

    public List<MentionedUserResponse> getMentionedUsers(Long commentId) {
        List<CommentMention> mentions = commentMentionRepository.findByCommentId(commentId);
        return mentions.stream()
                .map(mention -> {
                    User user = userRepository.findById(mention.getUserId()).orElse(null);
                    if (user == null) return null;
                    return MentionedUserResponse.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .fullName(user.getFullName())
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public PaginatedMentionResponse getMentionsForUser(Long userId, int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize);
        Page<CommentMention> mentionPage = commentMentionRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageRequest);

        List<CommentResponse> commentResponses = mentionPage.getContent().stream()
                .map(mention -> {
                    Comment comment = commentRepository.findById(mention.getCommentId()).orElse(null);
                    if (comment == null) return null;
                    return CommentResponse.builder()
                            .id(comment.getId())
                            .ticketId(comment.getTicketId())
                            .authorId(comment.getAuthorId())
                            .content(comment.getContent())
                            .mentionedUsers(getMentionedUsers(comment.getId()))
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();

        return PaginatedMentionResponse.builder()
                .data(commentResponses)
                .total(mentionPage.getTotalElements())
                .page(page)
                .build();
    }

    Set<String> parseMentions(String content) {
        Set<String> usernames = new LinkedHashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            usernames.add(matcher.group(1));
        }
        return usernames;
    }

    private List<User> resolveUsers(Set<String> usernames) {
        return usernames.stream()
                .map(username -> userRepository.findByUsernameIgnoreCase(username).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
