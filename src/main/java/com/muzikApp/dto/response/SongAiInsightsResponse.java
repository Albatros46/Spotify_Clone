package com.muzikApp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongAiInsightsResponse {

    private String analysis;
    private List<String> moods;
    private String genre;
    private Integer tempo;
    private String key;
    private Integer energy;
    private List<String> similarArtist;
    private String recommendedFor;

}
