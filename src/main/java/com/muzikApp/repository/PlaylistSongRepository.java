package com.muzikApp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.muzikApp.entities.Playlist;
import com.muzikApp.entities.PlaylistSong;
import com.muzikApp.entities.Song;

public interface PlaylistSongRepository extends JpaRepository<PlaylistSong, Long> {

    @EntityGraph(attributePaths = "song")
    List<PlaylistSong> findByPlaylistOrderByPositionAsc(Playlist playlist);

    Optional<PlaylistSong> findByPlaylistAndSong(Playlist playlist, Song song);

    boolean existsByPlaylistAndSong(Playlist playlist, Song song);

    @Query("select coalesce(max(ps.position), 0) from PlaylistSong ps where ps.playlist = :playlist")
    int maxPosition(@Param("playlist") Playlist playlist);

    /** Closes the gap after a song was removed: 1,2,(3),4,5 -> 1,2,3,4. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PlaylistSong ps set ps.position = ps.position - 1 "
            + "where ps.playlist = :playlist and ps.position > :position")
    void shiftUpAfter(@Param("playlist") Playlist playlist, @Param("position") int position);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PlaylistSong ps where ps.playlist = :playlist")
    void deleteByPlaylist(@Param("playlist") Playlist playlist);

    @Query("select ps.playlist from PlaylistSong ps where ps.song = :song")
    List<Playlist> findPlaylistsContaining(@Param("song") Song song);
}
