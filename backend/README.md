

IssueController의 searchIssues && getIssueBySlug에 대한 내용입니다.
조금 더 유저 친화적인 RestAPI 설계에 대한 내용 
/**
* 🎉 범용적인 이유:
* 1. API 엔드포인트 개수 감소: 3개 → 1개
* 2. 프론트엔드에서 검색 방식을 자유롭게 선택 가능
* 3. 하나의 검색창으로 다양한 검색 지원
* 4. URL이 일관성 있고 이해하기 쉬움
     */

// ============================================
// 🏷️ Slug란 무엇인가? (SEO 친화적 URL)
// ============================================

/**
* Slug = URL에 안전하고 읽기 좋은 형태의 문자열
*
* 원본 이슈명 → Slug 변환 예시:
* "부동산 정책" → "budongsan-jeongchaek"
* "코로나19 대응책" → "corona19-daeeungchaek"
* "경제정책 & 투자가이드" → "gyeongje-jeongchaek-tooja-guide"
  */

/**
* 🤔 왜 Slug를 사용하는가?
  */

// ❌ 사용자 친화적이지 않은 URL들
GET /v1/issues/507f1f77bcf86cd799439011  // ID - 의미 없음
GET /v1/issues/by-name/부동산%20정책      // 인코딩 - 복잡함

// ✅ Slug 사용한 깔끔한 URL
GET /v1/issues/budongsan-jeongchaek      // 의미 있고 깔끔!

/**
* Slug의 장점:
* 1. SEO 친화적 - 검색엔진이 URL 내용을 이해 가능
* 2. 사용자 친화적 - URL만 봐도 내용 예상 가능
* 3. 공유하기 좋음 - 인코딩 문제 없음
* 4. 브랜딩 효과 - 전문적으로 보임
     */