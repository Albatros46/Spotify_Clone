package com.muzikApp.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// @Data is NOT used on entities: its equals/hashCode/toString would touch lazy relations
// (LazyInitializationException, endless toString loops). Getters/setters are enough.
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "song")
public class Song {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String title;
    @Column(nullable = false, length = 100)
    private String artist;
    @Column(nullable = false, length = 100)
    private String album;
    @Column(name = "song_url", nullable = false)
    private String songUrl;
    /** Cover image is optional. */
    @Column(name = "image_url")
    private String imageUrl;
    @CreationTimestamp
    @Column(name="created_at",nullable = false,updatable = false)
    private LocalDateTime createdAt;
    //----------------------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appuser_id", nullable = false)
    private AppUser appUser;
}
