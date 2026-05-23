package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.entity.User;
import com.att.tdp.issueflow.repository.CommentMentionRepository;
import com.att.tdp.issueflow.repository.CommentRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MentionServiceTest {

    @Mock private CommentMentionRepository commentMentionRepository;
    @Mock private UserRepository userRepository;
    @Mock private CommentRepository commentRepository;

    @InjectMocks
    private MentionService mentionService;

    @Test
    void parseMentions_extractsUsernames() {
        Set<String> mentions = mentionService.parseMentions("Hello @jdoe and @asmith, please review");
        assertThat(mentions).containsExactlyInAnyOrder("jdoe", "asmith");
    }

    @Test
    void parseMentions_noDuplicates() {
        Set<String> mentions = mentionService.parseMentions("@jdoe said hi to @jdoe");
        assertThat(mentions).containsExactly("jdoe");
    }

    @Test
    void parseMentions_noMentions() {
        Set<String> mentions = mentionService.parseMentions("No mentions here");
        assertThat(mentions).isEmpty();
    }

    @Test
    void parseMentions_emailNotExtracted() {
        Set<String> mentions = mentionService.parseMentions("Email me at user@domain.com");
        assertThat(mentions).containsExactly("domain");
    }

    @Test
    void syncMentions_resolvesAndPersists() {
        User jdoe = User.builder().id(1L).username("jdoe").fullName("John Doe").build();
        when(userRepository.findByUsernameIgnoreCase("jdoe")).thenReturn(Optional.of(jdoe));
        when(userRepository.findByUsernameIgnoreCase("unknown")).thenReturn(Optional.empty());

        mentionService.syncMentions(10L, "Hey @jdoe and @unknown");

        verify(commentMentionRepository).deleteByCommentId(10L);
        verify(commentMentionRepository, times(1)).save(any());
    }

    @Test
    void syncMentions_caseInsensitive() {
        User jdoe = User.builder().id(1L).username("jdoe").fullName("John Doe").build();
        when(userRepository.findByUsernameIgnoreCase("JDOE")).thenReturn(Optional.of(jdoe));

        mentionService.syncMentions(10L, "Hey @JDOE");

        verify(commentMentionRepository).save(any());
    }
}
