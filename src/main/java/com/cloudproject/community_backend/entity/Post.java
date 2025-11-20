package com.cloudproject.community_backend.entity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String content;

    @ManyToOne
    @JoinColumn(name = "author_id", referencedColumnName = "id")
    private User author;

    @Column(name = "is_bad", nullable = false)
    private boolean isBad = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "board_type", nullable = false, length = 32)
    private PostBoardType boardType;

    @OneToOne(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("post-meeting")
    @JsonIgnoreProperties({"post", "hibernateLazyInitializer"})
    private MeetingPostDetail meetingDetails;

    @OneToOne(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("post-question")
    @JsonIgnoreProperties({"post", "hibernateLazyInitializer"})
    private QuestionPostDetail questionDetails;

    /**
     * 선배만 댓글 가능 여부
     */
    @Column(name = "senior_only_comment", nullable = false)
    private boolean seniorOnlyComment = false;

    private LocalDateTime createdAt = LocalDateTime.now();
}

