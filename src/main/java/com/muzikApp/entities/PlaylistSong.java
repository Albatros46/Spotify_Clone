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
@Table(name = "playlist_song",
        // the same song only once per playlist
        uniqueConstraints = @UniqueConstraint(name = "uk_playlist_song", columnNames = { "playlist_id", "song_id" }))
public class PlaylistSong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    //---------------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;
    //--------------------------
    @CreationTimestamp
    @Column(name = "added_at",nullable = false,updatable = false)
    private LocalDateTime addedAt;
    /** 1-based, no gaps: removing a song moves the following ones up. */
    @Column(nullable = false)
    private Integer position;
}
