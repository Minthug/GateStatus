package com.example.GateStatus.domain.figure.service.response;

import com.example.GateStatus.domain.career.Career;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureParty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
                            List<Career> career,      // 경력
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

    public static FigureInfoDTO from(Figure figure) {
        // 기본 필드 매핑
        String committeeName = null;
        String committeePosition = null;
        String electedCount = null;
        String electedDate = null;
        String reelection = null;
        String email = null;
        String homepage = null;
        String blog = null;
        String facebook = null;

        // 활동 내역(activities) 분석
        if (figure.getActivities() != null) {
            for (String activity : figure.getActivities()) {
                // 위원회 정보 추출
                if (activity.contains("위원회") || activity.contains("상임위")) {
                    String[] parts = activity.split("\\s+");
                    if (parts.length >= 1) {
                        // "국회 OO위원회 위원장" 형태 가정
                        committeeName = activity.replaceAll("국회|위원$|위원장$", "").trim();

                        if (activity.contains("위원장")) {
                            committeePosition = "위원장";
                        } else if (activity.contains("위원")) {
                            committeePosition = "위원";
                        }
                    }
                }

                // 당선 횟수 추출 (예: "21대 국회의원")
                if (activity.matches(".*\\d+대.*국회의원.*")) {
                    electedCount = activity.replaceAll("[^0-9]", "");

                    // 재선 여부 판단 - 당선 여부가 여러번 언급되면 재선으로 간주
                    if (reelection == null) {
                        reelection = "초선";
                    } else {
                        reelection = "재선";
                    }
                }
            }
        }

        // 웹사이트 정보(sites) 분석
        if (figure.getSites() != null) {
            for (String site : figure.getSites()) {
                site = site.trim().toLowerCase();
                if (site.contains("@") || site.endsWith(".com") && !site.contains("://")) {
                    email = site;
                } else if (site.contains("facebook.com") || site.contains("fb.com")) {
                    facebook = site;
                } else if (site.contains("blog") || site.contains("naver.com") || site.contains("tistory.com")) {
                    blog = site;
                } else if (site.startsWith("http") || site.startsWith("www")) {
                    homepage = site;
                }
            }
        }

        // 경력(careers) 분석 - 수정된 Career 클래스 필드 구조 반영
        if (figure.getCareers() != null && figure.getCareers().size() > 0) {
            // 당선일 찾기 - 국회의원 관련 경력 탐색
            for (Career career : figure.getCareers()) {
                // Period 필드에서 날짜 추출 시도 (일반적으로 "YYYY-MM ~ YYYY-MM" 형식 가정)
                if (career.getPeriod() != null && !career.getPeriod().isEmpty()) {
                    // 국회의원 경력 확인 (position, organization, title 필드 검사)
                    boolean isAssemblymanCareer = false;

                    if (career.getPosition() != null &&
                            (career.getPosition().contains("국회의원") || career.getPosition().contains("의원"))) {
                        isAssemblymanCareer = true;
                    }

                    if (!isAssemblymanCareer && career.getOrganization() != null &&
                            (career.getOrganization().contains("국회") || career.getOrganization().contains("국회의원"))) {
                        isAssemblymanCareer = true;
                    }

                    if (!isAssemblymanCareer && career.getTitle() != null &&
                            (career.getTitle().contains("국회의원") || career.getTitle().contains("당선"))) {
                        isAssemblymanCareer = true;
                    }

                    if (isAssemblymanCareer) {
                        // 국회의원 경력이 여러 개 있으면 재선으로 간주
                        if (reelection == null || "초선".equals(reelection)) {
                            reelection = "재선";
                        }

                        // period에서 시작 날짜 추출 (예: "2020-04 ~ 현재" -> "2020-04")
                        String[] periodParts = career.getPeriod().split("~");
                        if (periodParts.length > 0) {
                            electedDate = periodParts[0].trim();
                        }

                        break; // 가장 처음 찾은 기록 사용
                    }
                }
            }
        }

        // 모든 추출 값 확인
        electedCount = electedCount != null ? electedCount + "대" : null;

        // DTO 생성
        return new FigureInfoDTO(
                figure.getFigureId(),
                figure.getName(),
                figure.getEnglishName(),
                figure.getBirth(),
                figure.getFigureParty(),
                figure.getConstituency(),
                committeeName,
                committeePosition,
                electedCount,
                electedDate,
                reelection,
                figure.getProfileUrl(),
                figure.getEducation() != null ? new ArrayList<>(figure.getEducation()) : new ArrayList<>(),
                figure.getCareers(), // Career 객체 리스트 그대로 전달
                email,
                homepage,
                blog,
                facebook
        );
    }
}
