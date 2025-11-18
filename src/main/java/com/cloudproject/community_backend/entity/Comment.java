package com.cloudproject.community_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments") // 테이블 이름 명시 (user, post와 충돌 방지)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500) // 글자수 제한
    private String content;

    @ManyToOne
    @JoinColumn(name = "author_id", referencedColumnName = "id") // FK: users.id
    private User author;   // 댓글 작성자

    @ManyToOne
    @JoinColumn(name = "post_id", referencedColumnName = "id")   // FK: post.id
    private Post post;     // 어떤 게시글의 댓글인지

    @ManyToOne
    @JoinColumn(name = "parent_id", referencedColumnName = "id") // FK: comments.id
    private Comment parent;   // 부모 댓글 (null이면 일반 댓글, 값이 있으면 대댓글)

    private LocalDateTime createdAt = LocalDateTime.now(); // 기본값 현재시간

    @Column(name = "is_bad", nullable = false)
    private boolean isBad = false;

    private String authorName;

}
