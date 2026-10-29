package com.muzikApp.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.muzikApp.dto.request.SongRequest;
import com.muzikApp.dto.response.PaginatedResponse;
import com.muzikApp.dto.response.SongResponse;
import com.muzikApp.service.SongService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/songs")
public class SongController {

    private final SongService songService;

    public SongController(SongService songService) {
        this.songService = songService;
    }

    /** multipart/form-data: title, artist, album, file (audio), image (optional cover). */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public SongResponse upload(@Valid @ModelAttribute SongRequest request,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return songService.upload(request, file, image);
    }

    /** ?search=...&page=0&size=20&sort=title,asc */
    @GetMapping
    public PaginatedResponse<SongResponse> list(@RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return songService.list(search, pageable);
    }

    @GetMapping("/me")
    public PaginatedResponse<SongResponse> mine(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return songService.mine(pageable);
    }

    @GetMapping("/{id}")
    public SongResponse get(@PathVariable Long id) {
        return songService.get(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        songService.delete(id);
    }
}
