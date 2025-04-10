package com.example.GateStatus.domain.figure.service.response;

import java.util.List;

public record CongressmanApiDTO(String memberId,          // 의원 코드
                                String name,              // 이름
                                String englishName,       // 영문 이름
                                String birth,             // 생년월일
                                String partyName,         // 소속 정당 (문자열)
                                String constituency,      // 지역구
                                String committeeName,     // 소속 위원회
                                String committeePosition, // 위원회 직위
                                String electedCount,      // 당선 횟수
                                String electedDate,       // 당선일
                                String profileUrl,        // 프로필 사진 URL
                                List<String> education,   // 학력
                                List<String> career,      // 경력 (문자열)
                                String email,             // 이메일
                                String homepage,          // 홈페이지
                                String blog,              // 블로그
                                String facebook           // 페이스북
) {
}
