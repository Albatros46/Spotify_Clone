package com.muzikApp.demo;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.muzikApp.entities.AppUser;
import com.muzikApp.entities.Playlist;
import com.muzikApp.entities.PlaylistSong;
import com.muzikApp.entities.Role;
import com.muzikApp.entities.Song;
import com.muzikApp.repository.AppUserRepository;
import com.muzikApp.repository.PlaylistRepository;
import com.muzikApp.repository.PlaylistSongRepository;
import com.muzikApp.repository.SongRepository;
import com.muzikApp.service.FileStorageService;

/**
 * Demo account with songs and playlists, so someone who clones the repository can log in and try
 * the app at once. OFF by default: it creates an account whose password is published in the README,
 * so it must never run on a real server. Enable locally with app.demo.enabled=true.
 *
 * The songs are short generated sine-tone chords (real, playable WAV files) with gradient covers:
 * no third-party music in the repository.
 */
@Component
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoDataSeeder {

    public static final String EMAIL = "demo-listener@example.com";
    public static final String PASSWORD = "Demo-Listen-2026";

    private static final Logger LOG = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final int SAMPLE_RATE = 16_000;

    private record DemoSong(String title, String artist, String album, int[] chordHz, Color from, Color to) {
    }

    private static final List<DemoSong> SONGS = List.of(
            new DemoSong("Midnight Drive", "Neon Coast", "City Lights", new int[] { 220, 277, 330 },
                    new Color(255, 94, 98), new Color(255, 195, 113)),
            new DemoSong("Ocean Breath", "Blue Hour", "Tides", new int[] { 196, 247, 294 },
                    new Color(43, 88, 118), new Color(78, 205, 196)),
            new DemoSong("Paper Planes", "The Wanderers", "Skylines", new int[] { 262, 330, 392 },
                    new Color(131, 96, 195), new Color(46, 191, 145)),
            new DemoSong("Golden Hour", "Sunset Club", "Late Summer", new int[] { 294, 370, 440 },
                    new Color(247, 151, 30), new Color(255, 210, 0)),
            new DemoSong("Northern Lights", "Aurora Fields", "Polar", new int[] { 175, 220, 262 },
                    new Color(17, 153, 142), new Color(56, 239, 125)),
            new DemoSong("Coffee & Rain", "Lo-Fi Kitchen", "Sunday Mornings", new int[] { 247, 294, 370 },
                    new Color(92, 37, 141), new Color(67, 137, 162)),
            new DemoSong("Electric Heart", "Pulse Theory", "Voltage", new int[] { 330, 415, 494 },
                    new Color(238, 9, 121), new Color(255, 106, 0)),
            new DemoSong("Slow Motion", "Velvet Room", "After Hours", new int[] { 165, 196, 247 },
                    new Color(35, 37, 38), new Color(65, 67, 69)));

    private final AppUserRepository users;
    private final SongRepository songs;
    private final PlaylistRepository playlists;
    private final PlaylistSongRepository entries;
    private final FileStorageService storage;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(AppUserRepository users, SongRepository songs, PlaylistRepository playlists,
            PlaylistSongRepository entries, FileStorageService storage, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.songs = songs;
        this.playlists = playlists;
        this.entries = entries;
        this.storage = storage;
        this.passwordEncoder = passwordEncoder;
    }

    /** Runs once per start; does nothing if the demo account already exists. */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (users.existsByEmailIgnoreCase(EMAIL)) {
            LOG.info("Demo data already present ({})", EMAIL);
            return;
        }
        AppUser demo = new AppUser();
        demo.setName("Demo Listener");
        demo.setEmail(EMAIL);
        demo.setPassword(passwordEncoder.encode(PASSWORD));
        demo.setRole(Role.USER);
        users.save(demo);

        List<Song> created = new ArrayList<>();
        for (DemoSong s : SONGS) {
            Song song = new Song();
            song.setTitle(s.title());
            song.setArtist(s.artist());
            song.setAlbum(s.album());
            song.setSongUrl(storage.storeSong("demo.wav", "audio/wav", wav(s.chordHz(), 6)));
            song.setImageUrl(storage.storeImage("cover.png", "image/png", cover(s.from(), s.to())));
            song.setAppUser(demo);
            created.add(songs.save(song));
        }
        playlist(demo, "Road Trip", true, created, 0, 2, 3, 6);
        playlist(demo, "Focus Mode", false, created, 1, 4, 5, 7);
        playlist(demo, "Evening Chill", true, created, 5, 7, 1);
        LOG.warn("Demo data created: log in as {} (see README). Never enable app.demo.enabled on a real server.",
                EMAIL);
    }

    private void playlist(AppUser owner, String name, boolean isPublic, List<Song> all, int... songIndexes) {
        Playlist playlist = new Playlist();
        playlist.setName(name);
        playlist.setPublic(isPublic);
        playlist.setAppUser(owner);
        playlists.save(playlist);
        int position = 1;
        for (int index : songIndexes) {
            PlaylistSong entry = new PlaylistSong();
            entry.setPlaylist(playlist);
            entry.setSong(all.get(index));
            entry.setPosition(position++);
            entries.save(entry);
        }
    }

    /** 16-bit mono PCM WAV: the chord's notes one after another, with a short fade in and out. */
    static byte[] wav(int[] chordHz, int seconds) {
        int samples = SAMPLE_RATE * seconds;
        ByteBuffer data = ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN);
        data.put("RIFF".getBytes()).putInt(36 + samples * 2).put("WAVE".getBytes())
                .put("fmt ".getBytes()).putInt(16).putShort((short) 1).putShort((short) 1)
                .putInt(SAMPLE_RATE).putInt(SAMPLE_RATE * 2).putShort((short) 2).putShort((short) 16)
                .put("data".getBytes()).putInt(samples * 2);
        for (int i = 0; i < samples; i++) {
            int frequency = chordHz[(int) ((long) i * chordHz.length / samples)];
            double fade = Math.min(1, Math.min(i / 800.0, (samples - i) / 800.0));
            data.putShort((short) (9000 * fade * Math.sin(2 * Math.PI * frequency * i / SAMPLE_RATE)));
        }
        return data.array();
    }

    static byte[] cover(Color from, Color to) {
        BufferedImage image = new BufferedImage(160, 160, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setPaint(new GradientPaint(0, 0, from, 160, 160, to));
        g.fillRect(0, 0, 160, 160);
        g.dispose();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
