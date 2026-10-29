package com.muzikApp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.muzikApp.entities.AppUser;
import com.muzikApp.entities.Song;

public interface SongRepository extends JpaRepository<Song, Long> {

    /** Search in title, artist and album; the owner is fetched in the same query (no N+1). */
    @EntityGraph(attributePaths = "appUser")
    @Query("""
            select s from Song s
            where lower(s.title) like lower(concat('%', :q, '%'))
               or lower(s.artist) like lower(concat('%', :q, '%'))
               or lower(s.album) like lower(concat('%', :q, '%'))
            """)
    Page<Song> search(@Param("q") String query, Pageable pageable);

    @EntityGraph(attributePaths = "appUser")
    Page<Song> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = "appUser")
    Page<Song> findByAppUser(AppUser appUser, Pageable pageable);
}
