package com.example.volrabbit_be.persona.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class PersonaDto {

    @Getter
    @Builder
    public static class PersonaResponse {

        private long personaId;
        private String name;
        private String iconUrl;

        public static PersonaResponse of(long personaId, String name, String iconUrl) {
            return PersonaResponse.builder()
                    .personaId(personaId)
                    .name(name)
                    .iconUrl(iconUrl)
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PersonaAnalysisResponse {

        private long personaId;
        private String name;
        private String iconUrl;
        private String hashtag;
        private String graphUrl;
        private List<StatAnalysis> statAnalyses;
        private Recommendation recommendation;

        // 사전 서명된 URL로 대체
        public void replaceIconUrl(String iconUrl) {
            this.iconUrl = iconUrl;
        }

        public void replaceGraphUrl(String graphUrl) {
            this.graphUrl = graphUrl;
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class StatAnalysis {

        private String statKor;
        private String statEng;
        private int statLevel;
        private List<Detail> statDetails;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Recommendation {

        private String recommendationSuffix;
        private List<Detail> recommendationItems;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Detail {

        private String title;
        private String detail;
    }
}
