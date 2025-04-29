package com.example.GateStatus.domain.figure.service.response;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureParty;

import java.util.ArrayList;
import java.util.List;

/**
 * API에서 데이터 가져올 때
 * @param figureId
 * @param name
 * @param englishName
 * @param birth
 * @param partyName
 * @param constituency
 * @param committeeName
 * @param committeePosition
 * @param electedCount
 * @param electedDate
 * @param reelection
 * @param profileUrl
 * @param education
 * @param career
 * @param email
 * @param homepage
 * @param blog
 * @param facebook
 */
public record FigureInfoDTO(String figureId,          // 의원 코드
                            String name,              // 이름
                            String englishName,       // 영문 이름
                            String birth,             // 생년월일
                            FigureParty partyName,         // 소속 정당
                            String constituency,      // 지역구
                            String committeeName,     // 소속 위원회
                            String committeePosition, // 위원회 직위
                            String electedCount,      // 당선 횟수
                            String electedDate,       // 당선일
                            String reelection,        // 재선 여부
                            String profileUrl,        // 프로필 사진 URL
                            List<String> education,   // 학력
                            List<String> career,      // 경력
                            String email,             // 이메일
                            String homepage,          // 홈페이지
                            String blog,              // 블로그
                            String facebook) {           // 페이스북

    /**
     * 웹사이트 URL 목록 반환
     * @return
     */
    public List<String> getLinkUrl() {
        List<String> urls = new ArrayList<>();
        if (homepage != null && !homepage.isEmpty()) urls.add(homepage);
        if (blog != null && !blog.isEmpty()) urls.add(blog);
        if (facebook != null && !facebook.isEmpty()) urls.add(facebook);
        return urls;
    }

    /**
     * 활동 내역
     * @return
     */
    public List<String> getActivities() {
        List<String> activities = new ArrayList<>();
        activities.add(electedCount + "대 국회의원");
        if (committeeName != null && !committeeName.isEmpty()) {
            activities.add("국회 " + committeeName + " " + (committeePosition != null ? committeePosition : "위원"));
        }
        return activities;
    }
}
